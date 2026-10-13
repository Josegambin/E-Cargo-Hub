[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidateRange(1, 2147483647)]
    [long]$VehicleId,

    [string]$Serial = "emulator-5556",

    [string]$BackendUrl = "http://localhost:8081",

    [ValidateRange(250, 30000)]
    [int]$PollIntervalMs = 1000
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($env:ECARGO_CONTROL_API_TOKEN)) {
    throw "Set ECARGO_CONTROL_API_TOKEN in this PowerShell session before starting the bridge."
}

$sdkRoot = $env:ANDROID_HOME
if ([string]::IsNullOrWhiteSpace($sdkRoot)) {
    $sdkRoot = $env:ANDROID_SDK_ROOT
}
if ([string]::IsNullOrWhiteSpace($sdkRoot)) {
    throw "Set ANDROID_HOME or ANDROID_SDK_ROOT to the Android SDK directory."
}

$adb = Join-Path $sdkRoot "platform-tools\adb.exe"
if (-not (Test-Path $adb)) {
    throw "ADB was not found at '$adb'."
}

$BackendUrl = $BackendUrl.TrimEnd("/")
$controlHeaders = @{ "X-Control-Token" = $env:ECARGO_CONTROL_API_TOKEN }
$reportedVhalErrors = @{}
$climateSync = @{}

function Invoke-BackendGet {
    param([Parameter(Mandatory = $true)][string]$Path)

    return Invoke-RestMethod -Method Get -Uri "$BackendUrl$Path" -Headers $controlHeaders -TimeoutSec 10
}

function Send-ClimateCommand {
    param(
        [Parameter(Mandatory = $true)][string]$Command,
        [Parameter(Mandatory = $true)][double]$Value
    )

    $body = @{ command = $Command; value = $Value } | ConvertTo-Json -Compress
    Invoke-RestMethod -Method Post `
        -Uri "$BackendUrl/api/v1/vehicles/$VehicleId/commands" `
        -Headers $controlHeaders `
        -ContentType "application/json" `
        -Body $body `
        -TimeoutSec 10 | Out-Null
}

function Get-VhalNumber {
    param(
        [Parameter(Mandatory = $true)][string]$Property,
        [Parameter(Mandatory = $true)][int]$AreaId
    )

    $output = & $adb -s $Serial shell cmd car_service get-property-value $Property $AreaId 2>&1
    if ($LASTEXITCODE -ne 0) {
        $details = ($output | ForEach-Object { "$_" }) -join " "
        throw "$Property is not readable from car_service: $details"
    }

    $text = ($output | ForEach-Object { "$_" }) -join "`n"
    if ($text -match "(?im)\bvalue\s*[:=]\s*true\b") {
        return 1.0
    }
    if ($text -match "(?im)\bvalue\s*[:=]\s*false\b") {
        return 0.0
    }
    $match = [regex]::Match($text, "(?im)\bvalue\s*[:=]\s*(-?\d+(?:\.\d+)?)\b")
    if (-not $match.Success) {
        $match = [regex]::Match($text, "(?m)^\s*(-?\d+(?:\.\d+)?)\s*$")
    }
    if (-not $match.Success) {
        throw "Could not parse the VHAL value for $Property (area $AreaId): $text"
    }

    return [double]::Parse($match.Groups[1].Value, [Globalization.CultureInfo]::InvariantCulture)
}

function Invoke-VhalInjection {
    param(
        [Parameter(Mandatory = $true)][string]$Property,
        [Parameter(Mandatory = $true)][int]$AreaId,
        [Parameter(Mandatory = $true)][double]$Value
    )

    $formattedValue = $Value.ToString("0.###", [Globalization.CultureInfo]::InvariantCulture)
    $output = & $adb -s $Serial shell cmd car_service inject-vhal-event $Property $AreaId $formattedValue 2>&1
    if ($LASTEXITCODE -ne 0) {
        $details = ($output | ForEach-Object { "$_" }) -join " "
        if (-not $reportedVhalErrors.ContainsKey($Property)) {
            Write-Warning "VHAL injection failed for ${Property}: $details"
            $reportedVhalErrors[$Property] = $true
        }
        return
    }

    if ($reportedVhalErrors.ContainsKey($Property)) {
        Write-Host "VHAL injection recovered for $Property."
        $reportedVhalErrors.Remove($Property)
    }
}

function Sync-HvacValue {
    param(
        [Parameter(Mandatory = $true)][string]$Property,
        [Parameter(Mandatory = $true)][int]$AreaId,
        [Parameter(Mandatory = $true)][string]$Command,
        [Parameter(Mandatory = $true)][double]$Value
    )

    $normalizedValue = [math]::Round($Value, 1)
    $state = $climateSync[$Property]
    if ($null -eq $state) {
        $state = @{ value = $normalizedValue; lastAttempt = [datetime]::MinValue; applied = $false }
        $climateSync[$Property] = $state
    } elseif ([math]::Abs($state.value - $normalizedValue) -gt 0.05) {
        $state.value = $normalizedValue
        $state.applied = $false
    }

    if ($state.applied -or ([datetime]::UtcNow - $state.lastAttempt).TotalSeconds -lt 5) {
        return
    }

    $state.lastAttempt = [datetime]::UtcNow
    try {
        Send-ClimateCommand -Command $Command -Value $normalizedValue
        $state.applied = $true
        Write-Host "AAOS $Property=$normalizedValue sent to vehicle $VehicleId."
    } catch {
        Write-Warning "Could not sync $Property to vehicle ${VehicleId}: $($_.Exception.Message)"
    }
}

function Get-EstimatedRpm {
    param(
        [double]$SpeedKmh,
        [string]$GearPosition
    )

    if ($SpeedKmh -le 0) {
        return 0.0
    }

    $gear = 1
    if ($GearPosition -match "^D([1-6])$") {
        $gear = [int]$Matches[1]
    }
    $ratios = @(3.8, 2.3, 1.5, 1.1, 0.85, 0.65)
    $wheelRpm = $SpeedKmh * 1000.0 / 60.0 / 2.0
    return [math]::Round($wheelRpm * $ratios[$gear - 1] * 3.5)
}

function Get-GearSelection {
    param([string]$GearPosition)

    switch -Regex ($GearPosition) {
        "^P$" { return 4 }
        "^R$" { return 2 }
        "^N$" { return 1 }
        "^D([1-6])?$" { return 8 }
        default { throw "Unsupported simulated gear position '$GearPosition'." }
    }
}

function Get-CurrentGear {
    param([string]$GearPosition)

    if ($GearPosition -match "^D([1-6])$") {
        $gearValues = @(16, 32, 64, 128, 256, 512)
        return $gearValues[[int]$Matches[1] - 1]
    }
    if ($GearPosition -eq "R") {
        return -1
    }
    return 0
}

$state = & $adb -s $Serial get-state 2>&1
if ($LASTEXITCODE -ne 0 -or "$state".Trim() -ne "device") {
    throw "ADB target '$Serial' is not online."
}
$buildType = (& $adb -s $Serial shell getprop ro.build.type 2>&1 | Out-String).Trim()
if ($buildType -notin @("userdebug", "eng")) {
    throw "Target '$Serial' runs build type '$buildType'. The VHAL bridge requires an AAOS userdebug or eng image."
}

$config = & $adb -s $Serial shell cmd car_service get-carpropertyconfig HVAC_FAN_SPEED 2>&1
if ($LASTEXITCODE -ne 0) {
    $details = ($config | ForEach-Object { "$_" }) -join " "
    throw "AAOS car_service is not ready or HVAC_FAN_SPEED is unavailable: $details"
}

$null = Invoke-BackendGet -Path "/api/v1/vehicles/$VehicleId"
Write-Host "AAOS VHAL bridge active for vehicle $VehicleId on $Serial. Press Ctrl+C to stop."

while ($true) {
    try {
        $vehicle = Invoke-BackendGet -Path "/api/v1/vehicles/$VehicleId"
        $status = Invoke-BackendGet -Path "/api/v1/vehicles/$VehicleId/status"
        $controls = Invoke-BackendGet -Path "/api/v1/vehicles/$VehicleId/aaos/control-state"

        $speedKmh = 0.0
        if ($null -ne $status.speedKmh) {
            $speedKmh = [double]$status.speedKmh
        }
        $gearPosition = "$($status.gearPosition)"
        if ([string]::IsNullOrWhiteSpace($gearPosition)) {
            $gearPosition = "P"
        }
        $energyPercent = 100.0
        if ($null -ne $status.energyPercent) {
            $energyPercent = [double]$status.energyPercent
        } elseif ($null -ne $vehicle.energyPercent) {
            $energyPercent = [double]$vehicle.energyPercent
        }
        $energyCapacity = 0.0
        if ($null -ne $vehicle.energyCapacity) {
            $energyCapacity = [double]$vehicle.energyCapacity
        }
        $rangeKm = 0.0
        if ($energyCapacity -gt 0 -and [double]$vehicle.energyConsumptionPer100 -gt 0) {
            $rangeKm = $energyCapacity * $energyPercent / [double]$vehicle.energyConsumptionPer100
        }
        $rpm = Get-EstimatedRpm -SpeedKmh $speedKmh -GearPosition $gearPosition

        Invoke-VhalInjection -Property "PERF_VEHICLE_SPEED" -AreaId 0 -Value ($speedKmh / 3.6)
        Invoke-VhalInjection -Property "PERF_VEHICLE_SPEED_DISPLAY" -AreaId 0 -Value ($speedKmh / 3.6)
        Invoke-VhalInjection -Property "ENGINE_RPM" -AreaId 0 -Value $rpm
        Invoke-VhalInjection -Property "RANGE_REMAINING" -AreaId 0 -Value ($rangeKm * 1000.0)
        Invoke-VhalInjection -Property "GEAR_SELECTION" -AreaId 0 -Value (Get-GearSelection -GearPosition $gearPosition)
        Invoke-VhalInjection -Property "CURRENT_GEAR" -AreaId 0 -Value (Get-CurrentGear -GearPosition $gearPosition)

        if ($vehicle.energyType -eq "FUEL") {
            Invoke-VhalInjection -Property "FUEL_LEVEL" -AreaId 0 `
                -Value ($energyCapacity * $energyPercent / 100.0 * 1000.0)
        } else {
            Invoke-VhalInjection -Property "EV_BATTERY_LEVEL" -AreaId 0 `
                -Value ($energyCapacity * $energyPercent / 100.0 * 1000.0)
            Invoke-VhalInjection -Property "EV_CURRENT_BATTERY_CAPACITY" -AreaId 0 `
                -Value ($energyCapacity * 1000.0)
        }

        foreach ($binding in @(
            @{ property = "HVAC_POWER_ON"; command = "SET_CLIMATE_POWER"; area = 1 },
            @{ property = "HVAC_FAN_SPEED"; command = "SET_CLIMATE_FAN_SPEED"; area = 1 },
            @{ property = "HVAC_TEMPERATURE_SET"; command = "SET_CLIMATE_TEMPERATURE"; area = 1 }
        )) {
            try {
                $value = Get-VhalNumber -Property $binding.property -AreaId $binding.area
                if ($reportedVhalErrors.ContainsKey($binding.property)) {
                    Write-Host "AAOS property read recovered for $($binding.property)."
                    $reportedVhalErrors.Remove($binding.property)
                }
                Sync-HvacValue -Property $binding.property -AreaId $binding.area `
                    -Command $binding.command -Value $value
            } catch {
                if (-not $reportedVhalErrors.ContainsKey($binding.property)) {
                    Write-Warning "Could not read $($binding.property) from AAOS: $($_.Exception.Message)"
                    $reportedVhalErrors[$binding.property] = $true
                }
            }
        }
    } catch {
        Write-Warning "AAOS VHAL bridge iteration failed: $($_.Exception.Message)"
    }

    Start-Sleep -Milliseconds $PollIntervalMs
}
