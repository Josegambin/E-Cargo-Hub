$adb = "C:\Users\Desktop\AppData\Local\Android\Sdk\platform-tools\adb.exe"
$out = Join-Path $env:TEMP "ecargo_inspect.txt"
$s = "emulator-5554"
$r = @()
$r += "=== build.type ==="
$r += & $adb -s $s shell getprop ro.build.type 2>&1 | Out-String
$r += "=== PERF_VEHICLE_SPEED (m/s) ==="
$r += & $adb -s $s shell cmd car_service get-property-value PERF_VEHICLE_SPEED 0 2>&1 | Out-String
$r += "=== FUEL_LEVEL ==="
$r += & $adb -s $s shell cmd car_service get-property-value FUEL_LEVEL 0 2>&1 | Out-String
$r += "=== FUEL_CAPACITY ==="
$r += & $adb -s $s shell cmd car_service get-property-value FUEL_CAPACITY 0 2>&1 | Out-String
$r += "=== EV_BATTERY_LEVEL ==="
$r += & $adb -s $s shell cmd car_service get-property-value EV_BATTERY_LEVEL 0 2>&1 | Out-String
$r += "=== EV_CURRENT_BATTERY_CAPACITY ==="
$r += & $adb -s $s shell cmd car_service get-property-value EV_CURRENT_BATTERY_CAPACITY 0 2>&1 | Out-String
$r += "=== RANGE_REMAINING ==="
$r += & $adb -s $s shell cmd car_service get-property-value RANGE_REMAINING 0 2>&1 | Out-String
$r += "=== locale ==="
$r += & $adb -s $s shell getprop persist.sys.locale 2>&1 | Out-String
$r += & $adb -s $s shell getprop ro.product.locale 2>&1 | Out-String
$r += "=== measurement/locale settings ==="
$r += & $adb -s $s shell settings get secure system_locales 2>&1 | Out-String
$r += & $adb -s $s shell getprop persist.sys.language 2>&1 | Out-String
$r += & $adb -s $s shell getprop persist.sys.country 2>&1 | Out-String
$r += "=== try inject FUEL_LEVEL test ==="
$r += & $adb -s $s shell cmd car_service inject-vhal-event PERF_VEHICLE_SPEED 0 33.3 2>&1 | Out-String
$r -join "`n" | Out-File -Encoding utf8 $out
"DONE" | Out-File -Encoding utf8 $out -Append
