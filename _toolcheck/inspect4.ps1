$adb = "C:\Users\Desktop\AppData\Local\Android\Sdk\platform-tools\adb.exe"
$s = "emulator-5554"
$td = "c:\Users\Desktop\Desktop\E-Cargo Hub\_toolcheck"
$out = Join-Path $env:TEMP "ecargo_inspect4.txt"
$r = @()

# captura baseline
& $adb -s $s exec-out screencap -p | Set-Content -Path (Join-Path $td "shot_baseline.png") -Encoding Byte

# ¿Qué lee el clúster ahora de FUEL y EV?
$r += "=== FUEL_LEVEL before ==="
$r += & $adb -s $s shell cmd car_service get-property-value FUEL_LEVEL 0 2>&1 | Out-String
$r += "=== EV_BATTERY_LEVEL before ==="
$r += & $adb -s $s shell cmd car_service get-property-value EV_BATTERY_LEVEL 0 2>&1 | Out-String

# Inyectar FUEL_LEVEL = 7500 (≈50%) y capturar
& $adb -s $s shell cmd car_service inject-vhal-event FUEL_LEVEL 0 7500 2>&1 | Out-Null
Start-Sleep -Milliseconds 800
& $adb -s $s exec-out screencap -p | Set-Content -Path (Join-Path $td "shot_fuel50.png") -Encoding Byte
$r += "=== FUEL_LEVEL after inject 7500 ==="
$r += & $adb -s $s shell cmd car_service get-property-value FUEL_LEVEL 0 2>&1 | Out-String

# También inyectar EV_BATTERY_LEVEL = 50% de 150000 = 75000 y capturar
& $adb -s $s shell cmd car_service inject-vhal-event EV_BATTERY_LEVEL 0 75000 2>&1 | Out-Null
Start-Sleep -Milliseconds 800
& $adb -s $s exec-out screencap -p | Set-Content -Path (Join-Path $td "shot_ev50.png") -Encoding Byte
$r += "=== EV_BATTERY_LEVEL after inject 75000 ==="
$r += & $adb -s $s shell cmd car_service get-property-value EV_BATTERY_LEVEL 0 2>&1 | Out-String

$r -join "`n" | Out-File -Encoding utf8 $out
"DONE" | Out-File -Encoding utf8 $out -Append
