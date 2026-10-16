param(
    [string]$Serial = 'emulator-5554',
    [string]$Adb = "$env:ANDROID_HOME/platform-tools/adb.exe",
    [switch]$HelpersOnly
)
$ErrorActionPreference = 'Stop'
if ($Serial -notlike 'emulator-*') { throw 'This smoke test intentionally supports AVDs only.' }
$projectRoot = Split-Path $PSScriptRoot -Parent
$output = Join-Path $projectRoot '.local/smoke'
New-Item -ItemType Directory -Force $output | Out-Null
$package = 'com.napreth.doihave.debug'
$activity = "$package/com.napreth.doihave.MainActivity"
$checks = [Collections.Generic.List[string]]::new()

function Invoke-Adb {
    $result = & $Adb -s $Serial @args
    if ($LASTEXITCODE -ne 0) { throw "ADB failed: $args`n$result" }
    return $result
}
function Read-Ui {
    Invoke-Adb shell uiautomator dump /sdcard/doihave-ui.xml | Out-Null
    Invoke-Adb pull /sdcard/doihave-ui.xml (Join-Path $output 'ui.xml') | Out-Null
    return [xml](Get-Content -Raw -LiteralPath (Join-Path $output 'ui.xml'))
}
function Tap-Node($node) {
    if (-not $node) { throw 'Expected UI element was not found.' }
    if ($node.bounds -notmatch '\[(\d+),(\d+)\]\[(\d+),(\d+)\]') { throw 'UI bounds missing.' }
    Invoke-Adb shell input tap ([int](($Matches[1] -as [int]) + ($Matches[3] -as [int])) / 2) ([int](($Matches[2] -as [int]) + ($Matches[4] -as [int])) / 2) | Out-Null
}
function Tap-Id([string]$id) {
    $ui = Read-Ui
    Tap-Node $ui.SelectSingleNode("//node[@resource-id='$package`:id/$id']")
}
function Search([string]$query) {
    $ui = Read-Ui
    $clear = $ui.SelectSingleNode("//node[@resource-id='$package`:id/clear_search']")
    if ($clear) { Tap-Node $clear }
    Tap-Id 'search_input'
    Invoke-Adb shell input text $query | Out-Null
}
function Assert-Ui([string]$xpath, [string]$message) {
    $ui = Read-Ui
    if (-not $ui.SelectSingleNode($xpath)) { throw $message }
    $checks.Add($message)
}
function Assert-Absent([string]$xpath, [string]$message) {
    $ui = Read-Ui
    if ($ui.SelectSingleNode($xpath)) { throw $message }
    $checks.Add($message)
}
function Center-X($node) {
    if ($node.bounds -notmatch '\[(\d+),(\d+)\]\[(\d+),(\d+)\]') { throw 'UI bounds missing.' }
    return ([int]$Matches[1] + [int]$Matches[3]) / 2
}
function Assert-NavigationAlignment {
    $ui = Read-Ui
    foreach ($id in @('nav_permissions', 'nav_ops', 'nav_settings')) {
        $tab = $ui.SelectSingleNode("//node[@resource-id='$package`:id/$id']")
        $icon = $tab.SelectSingleNode("./node[@class='android.widget.FrameLayout']")
        $label = $tab.SelectSingleNode("./node[@class='android.widget.TextView']")
        if ([Math]::Abs((Center-X $icon) - (Center-X $label)) -gt 1) { throw "Tab icon and label misaligned: $id" }
    }
    $checks.Add('All navigation icons and labels share the same horizontal center')
}
function Save-Screenshot([string]$name) {
    Invoke-Adb shell screencap -p /sdcard/doihave-screen.png | Out-Null
    Invoke-Adb pull /sdcard/doihave-screen.png (Join-Path $output "$name.png") | Out-Null
}

if ($HelpersOnly) { return }

# Reset only this test application's data, never the AVD or other applications.
Invoke-Adb install -r (Join-Path $projectRoot 'app/build/outputs/apk/debug/app-debug.apk')
Invoke-Adb shell pm clear $package | Out-Null
Invoke-Adb shell am start -W -n $activity | Out-Null
Assert-Ui "//node[@resource-id='$package`:id/nav_permissions']" 'Launch opens the app without a permission dialog'
Assert-Absent "//node[@text='DO I HAVE' or contains(@text, 'Nothing is requested on launch') or contains(@text, '进入应用时不会自动请求授权')]" 'Removed header and helper copy are absent'
Assert-NavigationAlignment
Save-Screenshot 'permissions-initial'

foreach ($fixture in @(@('INTERNET','INTERNET'), @('WRITE_SECURE_SETTINGS','SECURE'), @('READ_EXTERNAL_STORAGE','EXTERNAL'))) {
    $unrequestable = $fixture[0]
    Search $fixture[1]
    Assert-Absent "//node[contains(@content-desc, 'android.permission.$unrequestable,')]" "No filler permission row: $unrequestable"
}

foreach ($fixture in @(@('MANAGE','All files access','访问所有文件'), @('MANAGE','Manage media files','管理媒体文件'), @('USAGE','Usage access','访问使用情况'), @('ACCEPT','Continue calls from other apps','接续其他应用的通话'))) {
    Search $fixture[0]
    Assert-Ui "//node[@class='android.widget.TextView' and (@text='$($fixture[1])' or @text='$($fixture[2])')]" "Permission has a readable display name: $($fixture[0])"
    Assert-Absent "//node[@class='android.widget.TextView' and contains(@text,'.permission.')]" 'No permission identifier is used as a display name'
}

# Camera authorization must be triggered by a tap.
Search 'CAMERA'
$ui = Read-Ui
Tap-Node $ui.SelectSingleNode("//node[contains(@content-desc, 'android.permission.CAMERA,')]")
$ui = Read-Ui
Tap-Node $ui.SelectSingleNode("//node[contains(@resource-id, 'permission_allow_foreground_only_button')]")
Assert-Ui "//node[contains(@content-desc, 'android.permission.CAMERA,') and (contains(@content-desc, 'Granted') or contains(@content-desc, '已授权'))]" 'Camera becomes checked after user authorization'
Assert-Absent "//node[@class='android.widget.TextView' and (@text='CAMERA' or @text='Runtime permission' or @text='运行时权限')]" 'Permission rows omit technical names and types'
Save-Screenshot 'camera-granted'

Tap-Id 'nav_ops'
Search '26'
Assert-Ui "//node[contains(@content-desc, '#26, android:camera,')]" 'Ops includes CAMERA with AOSP operation number 26'
Assert-Ui "//node[@resource-id='$package`:id/op_status' and contains(@text, '4')]" 'Raw foreground status is displayed in the trailing status view'
Assert-Absent "//node[contains(@resource-id, 'ops_category') or @text='全部分类']" 'Custom functional categories are removed'
$ui = Read-Ui
foreach ($names in @(@('All','全部'), @('Allowed','允许'), @('Foreground','仅前台'), @('Ignored','忽略'), @('Denied','拒绝'), @('Default','默认'), @('Unknown','未知'))) {
    if (-not $ui.SelectSingleNode("//node[@resource-id='$package`:id/state_filters']/node[@text='$($names[0])' or @text='$($names[1])']")) { throw "Missing inline status filter: $names" }
}
$checks.Add('All status filters are visible directly on the page')
Tap-Node $ui.SelectSingleNode("//node[@resource-id='$package`:id/state_filters']/node[@text='Allowed' or @text='允许']")
Assert-Absent "//node[contains(@content-desc, '#26, android:camera,')]" 'Allowed filter excludes foreground-only camera'
$ui = Read-Ui
Tap-Node $ui.SelectSingleNode("//node[@resource-id='$package`:id/state_filters']/node[@text='Foreground' or @text='仅前台']")
Assert-Ui "//node[contains(@content-desc, '#26, android:camera,')]" 'Foreground filter includes camera'
Assert-Absent "//node[@resource-id='android:id/select_dialog_listview']" 'Status filters act inline without opening a dialog'
$ui = Read-Ui
Tap-Node $ui.SelectSingleNode("//node[@resource-id='$package`:id/state_filters']/node[@text='All' or @text='全部']")
Tap-Id 'nav_settings'
Tap-Id 'nav_ops'
Save-Screenshot 'ops-camera'

Tap-Id 'nav_settings'
Assert-Ui "//node[@text='0.0.1-debug']" 'Version is displayed exactly as the debug versionName'
Tap-Id 'setting_language'
$ui = Read-Ui
Tap-Node $ui.SelectSingleNode("//node[@text='简体中文']")
Assert-Ui "//node[@text='偏好设置']" 'Language switches to Simplified Chinese'
Tap-Id 'setting_theme'
$ui = Read-Ui
Tap-Node $ui.SelectSingleNode("//node[@text='深色']")
Assert-Ui "//node[@text='深色']" 'Dark theme preference is applied'
Save-Screenshot 'settings-dark-zh'
Tap-Id 'setting_license'
Assert-Ui "//node[contains(@text, 'Permission is hereby granted') and contains(@text, 'Napreth')]" 'About displays the full MIT license'
Invoke-Adb shell input keyevent 4 | Out-Null

# Recreate the process and verify persisted preferences.
Invoke-Adb shell am force-stop $package | Out-Null
Invoke-Adb shell am start -W -n $activity | Out-Null
Assert-Ui "//node[@resource-id='$package`:id/nav_permissions' and @content-desc='权限']" 'Chinese persists after process restart'
Tap-Id 'nav_settings'
Assert-Ui "//node[@text='深色']" 'Theme preference persists after process restart'

$checks | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $output 'results.json') -Encoding utf8
$checks | ForEach-Object { Write-Host "PASS: $_" }
Write-Host "Screenshots and results: $output"
