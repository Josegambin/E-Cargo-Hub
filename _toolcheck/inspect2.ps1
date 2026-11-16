$adb = "C:\Users\Desktop\AppData\Local\Android\Sdk\platform-tools\adb.exe"
$out = Join-Path $env:TEMP "ecargo_inspect2.txt"
$s = "emulator-5554"
$r = @()
# ¿El emulador se reporta como EV?
foreach ($p in @("INFO_MODEL","INFO_EV_CONNECTOR_TYPE","INFO_MODEL_YEAR","INFO_VIN","EV_BATTERY_CAPACITY","FUEL_DOOR_OPEN")) {
  $r += "=== $p ==="
  $r += & $adb -s $s shell cmd car_service get-property-value $p 0 2>&1 | Out-String
}
# Unidades del sistema
$r += "=== car unit props ==="
$r += & $adb -s $s shell getprop | Select-String -Pattern "locale|unit|car" 2>&1 | Out-String
# Cambiar locale a metrico es-ES y reiniciar? primero ver cómo está
$r += "=== settings global/system locale ==="
$r += & $adb -s $s shell settings get system time_12_24 2>&1 | Out-String
$r += & $adb -s $s shell cmd car_service list 2>&1 | Select-String -Pattern "unit|Unit" 2>&1 | Out-String
# ¿Se puede inyectar FUEL_LEVEL?
$r += "=== test inject FUEL_LEVEL 14280 ==="
$r += & $adb -s $s shell cmd car_service inject-vhal-event FUEL_LEVEL 0 14280 2>&1 | Out-String
$r += & $adb -s $s shell cmd car_service get-property-value FUEL_LEVEL 0 2>&1 | Out-String
$r -join "`n" | Out-File -Encoding utf8 $out
"DONE" | Out-File -Encoding utf8 $out -Append
