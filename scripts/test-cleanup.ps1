function Invoke-TestCleanup {
    param([Parameter(Mandatory)][scriptblock[]]$Actions)
    $ErrorActionPreference = 'Stop'
    $failures = [Collections.Generic.List[Management.Automation.ErrorRecord]]::new()
    foreach ($action in $Actions) {
        try { & $action | Out-Null }
        catch { $failures.Add($_) }
    }
    # Return failures after attempting every action; the caller retains the original test error.
    return $failures.ToArray()
}
