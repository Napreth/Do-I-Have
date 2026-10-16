param(
    [string]$Serial = 'emulator-5554',
    [string]$Adb = "$env:ANDROID_HOME/platform-tools/adb.exe"
)
$ErrorActionPreference = 'Stop'
. "$PSScriptRoot/smoke-test.ps1" -Serial $Serial -Adb $Adb -HelpersOnly
. "$PSScriptRoot/test-cleanup.ps1"
if ([int](Invoke-Adb shell getprop ro.build.version.sdk) -lt 31) {
    Write-Host 'SKIP: exact-alarm special access requires Android 12.'
    return
}

# Change only the isolated debug app, and restore both its op and battery exemption.
$uidLine = Invoke-Adb shell cmd package list packages -U $package
$uidMatch = [regex]::Match(($uidLine -join "`n"), '(?m)^package:' + [regex]::Escape($package) + ' uid:(\d+)\s*$')
if (-not $uidMatch.Success) { throw 'Install the debug APK before running this regression test.' }
$appUid = $uidMatch.Groups[1].Value
$before = Invoke-Adb shell cmd appops get $appUid SCHEDULE_EXACT_ALARM
$modeMatch = [regex]::Match(($before -join "`n"), 'SCHEDULE_EXACT_ALARM:\s*(allow|ignore|deny|default)\b')
$originalMode = if ($modeMatch.Success) { $modeMatch.Groups[1].Value } else { 'default' }
$whitelist = Invoke-Adb shell cmd deviceidle whitelist
$wasExempt = ($whitelist -join "`n") -match (',' + [regex]::Escape($package) + ',')

$testFailure = $null
try {
    Invoke-Adb shell cmd deviceidle whitelist "+$package" | Out-Null
    Invoke-Adb shell cmd appops set --uid $package SCHEDULE_EXACT_ALARM deny | Out-Null
    Invoke-Adb shell am force-stop $package | Out-Null
    Invoke-Adb shell am start -W -n $activity | Out-Null
    Search 'SCHEDULE'
    Assert-Ui "//node[contains(@content-desc,'android.permission.SCHEDULE_EXACT_ALARM,') and (contains(@content-desc,'Not granted') or contains(@content-desc,'未授权'))]" 'Exact-alarm row stays ungranted despite a battery exemption'

    Tap-Id 'nav_ops'
    Search '107'
    $ui = Read-Ui
    Tap-Node $ui.SelectSingleNode("//node[contains(@content-desc,'#107, android:schedule_exact_alarm,')]")
    Assert-Ui "//node[contains(@text,'Special access: Not granted') or contains(@text,'特殊访问授权：未授权')]" 'Exact-alarm details report the denied special-access op'
    Assert-Ui "//node[contains(@text,'Capability: Available') or contains(@text,'能力：可用')]" 'Exact-alarm capability is independently available through the exemption'
    Save-Screenshot 'exact-alarm-independent-state'
} catch {
    $testFailure = $_
} finally {
    $cleanupFailures = @(Invoke-TestCleanup -Actions @(
        { Invoke-Adb shell cmd appops set --uid $package SCHEDULE_EXACT_ALARM $originalMode },
        { if (-not $wasExempt) { Invoke-Adb shell cmd deviceidle whitelist "-$package" } },
        { Invoke-Adb shell am force-stop $package },
        { Invoke-Adb shell am start -W -n $activity }
    ))
}

if ($testFailure) {
    foreach ($failure in $cleanupFailures) { Write-Warning "Cleanup also failed: $failure" }
    throw $testFailure
}
if ($cleanupFailures.Count -gt 0) {
    throw ('Cleanup failed: ' + (($cleanupFailures | ForEach-Object { $_.ToString() }) -join '; '))
}

$checks | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $output 'exact-alarm-results.json') -Encoding utf8
$checks | ForEach-Object { Write-Host "PASS: $_" }
