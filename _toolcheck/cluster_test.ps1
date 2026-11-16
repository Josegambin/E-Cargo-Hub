$adb = "C:\Users\Desktop\AppData\Local\Android\Sdk\platform-tools\adb.exe"
$s = "emulator-5554"
$td = "c:\Users\Desktop\Desktop\E-Cargo Hub\_toolcheck"
$out = Join-Path $env:TEMP "ecargo_i7.txt"
$r = @()

# Capturar el display del clúster (id 2)
& $adb -s $s shell screencap -p -d 2 /sdcard/cluster0.png
& $adb -s $s pull /sdcard/cluster0.png (Join-Path $td "cluster0.png") | Out-Null

# Inyectar EV_BATTERY_LEVEL = 50% de 150000 = 75000
& $adb -s $s shell cmd car_service inject-vhal-event EV_BATTERY_LEVEL 0 75000 2>&1 | Out-Null
Start-Sleep -Milliseconds 1500
& $adb -s $s shell screencap -p -d 2 /sdcard/cluster_ev50.png
& $adb -s $s pull /sdcard/cluster_ev50.png (Join-Path $td "cluster_ev50.png") | Out-Null
$r += "EV_BATTERY_LEVEL after inject 75000:"
$r += & $adb -s $s shell cmd car_service get-property-value EV_BATTERY_LEVEL 0 2>&1 | Out-String

# Inyectar FUEL_LEVEL = 7500
& $adb -s $s shell cmd car_service inject-vhal-event FUEL_LEVEL 0 7500 2>&1 | Out-Null
Start-Sleep -Milliseconds 1500
& $adb -s $s shell screencap -p -d 2 /sdcard/cluster_fuel.png
& $adb -s $s pull /sdcard/cluster_fuel.png (Join-Path $td "cluster_fuel.png") | Out-Null
$r += "FUEL_LEVEL after inject 7500:"
$r += & $adb -s $s shell cmd car_service get-property-value FUEL_LEVEL 0 2>&1 | Out-String

$r -join "`n" | Out-File -Encoding utf8 $out
"DONE" | Out-File -Encoding utf8 $out -Append
