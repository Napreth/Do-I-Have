$ErrorActionPreference = 'Stop'
. "$PSScriptRoot/../test-cleanup.ps1"

function Assert-Equal($expected, $actual, [string]$message) {
    if ($expected -ne $actual) { throw "$message; expected '$expected', got '$actual'" }
}

$script:attempts = [Collections.Generic.List[string]]::new()
$failures = @(Invoke-TestCleanup -Actions @(
    { $script:attempts.Add('op'); throw 'op restore failed' },
    { $script:attempts.Add('whitelist') },
    { $script:attempts.Add('stop') },
    { $script:attempts.Add('start') }
))
Assert-Equal 'op,whitelist,stop,start' ($script:attempts -join ',') 'A failure must not skip later cleanup'
Assert-Equal 1 $failures.Count 'The failed restore must be reported'
Assert-Equal 'op restore failed' $failures[0].ToString() 'The original restore error must be preserved'

$script:attempts.Clear()
$failures = @(Invoke-TestCleanup -Actions @(
    { $script:attempts.Add('op'); throw 'first failure' },
    { $script:attempts.Add('whitelist'); throw 'second failure' },
    { $script:attempts.Add('start') }
))
Assert-Equal 'op,whitelist,start' ($script:attempts -join ',') 'All cleanup actions must run after multiple failures'
Assert-Equal 2 $failures.Count 'All restore failures must be reported'

$failures = @(Invoke-TestCleanup -Actions @({ 'ignored command output' }, { 42 }))
Assert-Equal 0 $failures.Count 'Successful command output must not be mistaken for a failure'
Write-Host 'PASS: 6 cleanup failure/recovery assertions'
