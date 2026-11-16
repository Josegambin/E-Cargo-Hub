$adb = "C:\Users\Desktop\AppData\Local\Android\Sdk\platform-tools\adb.exe"
$out = Join-Path $env:TEMP "ecargo_inspect3.txt"
$s = "emulator-5554"
$r = @()

# (a) ¿FUEL_LEVEL es seteable? inyectar y releer tras 1.5s
$r += "=== FUEL_LEVEL before ==="
$r += & $adb -s $s shell cmd car_service get-property-value FUEL_LEVEL 0 2>&1 | Out-String
$r += "=== inject FUEL_LEVEL=14280 ==="
$r += & $adb -s $s shell cmd car_service inject-vhal-event FUEL_LEVEL 0 14280 2>&1 | Out-String
Start-Sleep -Milliseconds 1500
$r += "=== FUEL_LEVEL after 1.5s ==="
$r += & $adb -s $s shell cmd car_service get-property-value FUEL_LEVEL 0 2>&1 | Out-String

# (b) unidades: buscar ajuste de unidades del coche
$r += "=== car_service list (unit/distance) ==="
$r += (& $adb -s $s shell cmd car_service list 2>&1 | Select-String -Pattern "unit|distance|metric" ) -join "`n"
$r += "=== settings secure/global metric keys ==="
$r += & $adb -s $s shell "settings list global 2>/dev/null | grep -i -E 'unit|metric|locale'" 2>&1 | Out-String
$r += & $adb -s $s shell "settings list system 2>/dev/null | grep -i -E 'unit|metric|locale'" 2>&1 | Out-String

# (c) ¿tenemos root para cambiar locale?
$r += "=== adb root check ==="
$r += & $adb -s $s shell id 2>&1 | Out-String
$r += & $adb -s $s shell getprop ro.product.locale 2>&1 | Out-String

$r -join "`n" | Out-File -Encoding utf8 $out
"DONE" | Out-File -Encoding utf8 $out -Append
