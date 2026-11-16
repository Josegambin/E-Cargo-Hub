$adb = "C:\Users\Desktop\AppData\Local\Android\Sdk\platform-tools\adb.exe"
$s = "emulator-5554"
$td = "c:\Users\Desktop\Desktop\E-Cargo Hub\_toolcheck"
$out = Join-Path $env:TEMP "ecargo_i5.txt"
$r = @()

# 1) ¿Qué marca la aguja Fuel ahora? captura limpia vía archivo temporal en el device
& $adb -s $s shell screencap -p /sdcard/before.png
& $adb -s $s pull /sdcard/before.png (Join-Path $td "before.png") | Out-Null

# 2) Inyectar FUEL_LEVEL = 7500 y capturar
& $adb -s $s shell cmd car_service inject-vhal-event FUEL_LEVEL 0 7500 2>&1 | Out-Null
Start-Sleep -Milliseconds 1200
& $adb -s $s shell screencap -p /sdcard/after_fuel.png
& $adb -s $s pull /sdcard/after_fuel.png (Join-Path $td "after_fuel.png") | Out-Null

$r += "FUEL_LEVEL now:"
$r += & $adb -s $s shell cmd car_service get-property-value FUEL_LEVEL 0 2>&1 | Out-String

# 3) Cambiar locale del sistema a es-ES (metrico -> km/h)
& $adb -s $s shell settings put system system_locales es-ES 2>&1 | Out-Null
& $adb -s $s shell "am broadcast -a com.android.intent.action.SET_LOCALE --es com.android.intent.extra.LOCALE es-ES" 2>&1 | Out-Null
$r += "system_locales now:"
$r += & $adb -s $s shell settings get system system_locales 2>&1 | Out-String

$r -join "`n" | Out-File -Encoding utf8 $out
"DONE" | Out-File -Encoding utf8 $out -Append
