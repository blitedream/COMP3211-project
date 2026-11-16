param([switch]$Plain)

$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$outDir = Join-Path $root 'build/classes'
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
# The CLI reads and writes UTF-8; switch the console to UTF-8 so typed non-ASCII text is not garbled.
try { chcp 65001 | Out-Null } catch { }
$sources = @(Get-ChildItem (Join-Path $root 'src/main/java') -Recurse -Filter '*.java' | ForEach-Object FullName)
& javac -encoding UTF-8 -Xlint:-options -source 8 -target 8 -d $outDir @sources
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
$consoleHandle = [IntPtr]::Zero
$originalMode = [uint32]0
$colorEnabled = $false
if (-not $Plain -and -not [Console]::IsOutputRedirected -and -not (Test-Path Env:NO_COLOR)) {
    try {
        if (-not ('PimConsoleMode' -as [type])) {
            Add-Type -TypeDefinition @'
using System;
using System.Runtime.InteropServices;
public static class PimConsoleMode {
    [DllImport("kernel32.dll")] public static extern IntPtr GetStdHandle(int id);
    [DllImport("kernel32.dll")] public static extern bool GetConsoleMode(IntPtr handle, out uint mode);
    [DllImport("kernel32.dll")] public static extern bool SetConsoleMode(IntPtr handle, uint mode);
}
'@
        }
        $consoleHandle = [PimConsoleMode]::GetStdHandle(-11)
        if ([PimConsoleMode]::GetConsoleMode($consoleHandle, [ref]$originalMode)) {
            $colorEnabled = [PimConsoleMode]::SetConsoleMode($consoleHandle, ($originalMode -bor 5))
        }
    } catch { $colorEnabled = $false }
}
$displayMode = if ($colorEnabled) { '--color' } else { '--plain' }
$columns = if ($colorEnabled -and [Console]::WindowWidth -ge 40) { [Console]::WindowWidth } else { 100 }
try {
    & java '-Dfile.encoding=UTF-8' "-Dpim.columns=$columns" -cp $outDir cli.PIMCli $displayMode
    $pimExitCode = $LASTEXITCODE
} finally {
    if ($colorEnabled) { [PimConsoleMode]::SetConsoleMode($consoleHandle, $originalMode) | Out-Null }
}
exit $pimExitCode

