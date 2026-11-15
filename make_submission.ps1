# Assemble the COMP3211 submission ZIP.
#
# Required before running (see the course project description):
#   Honour_Declaration.pdf   at the repository root, filled and exported to PDF
#   video/demo.mp4           the <= 4 minute system demonstration
#   video/presentation.mp4   the <= 5 minute group presentation recording
#
# Usage:
#   .\make_submission.ps1
#
# The script only reads the repository; it writes the staging folder and the ZIP
# under build/ and refuses to clear anything outside that folder.

$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$stage = Join-Path $root 'build/submission'
$zip = Join-Path $root ('build/COMP3211_submission_' + (Get-Date -Format 'yyyyMMdd_HHmm') + '.zip')

$required = @(
    'Honour_Declaration.pdf',
    'video/demo.mp4',
    'video/presentation.mp4'
)
foreach ($rel in $required) {
    $full = Join-Path $root $rel
    if (-not (Test-Path -LiteralPath $full)) {
        throw "Missing required submission file: $rel (fill Honour_Declaration_TEMPLATE.docx and export it as Honour_Declaration.pdf; record both videos into video/)"
    }
}

# Safety: only ever clear the staging folder inside build/.
$buildRoot = (Join-Path $root 'build') + [IO.Path]::DirectorySeparatorChar
if (-not $stage.StartsWith($buildRoot, [StringComparison]::OrdinalIgnoreCase)) {
    throw "Refusing to clear a staging path outside build/: $stage"
}
if (Test-Path -LiteralPath $stage) {
    Remove-Item -LiteralPath $stage -Recurse -Force
}
New-Item -ItemType Directory -Path $stage | Out-Null

# Files that must sit at the ZIP root (the Honour Declaration must be one of them).
$rootFiles = @(
    'README.md',
    'Honour_Declaration.pdf',
    'Requirements_Coverage.pdf',
    'Model_Line_Coverage.pdf',
    'run.ps1',
    'run_tests.ps1',
    'run_d_tests.ps1',
    'run_coverage.ps1'
)
foreach ($f in $rootFiles) {
    $full = Join-Path $root $f
    if (-not (Test-Path -LiteralPath $full)) { throw "Missing file: $f" }
    Copy-Item -LiteralPath $full -Destination $stage
}

# Repository folders copied into the ZIP under a stable name.
$folders = [ordered]@{
    'src'             = 'src'
    'lib'             = 'lib'
    'coverage-report' = 'coverage-report'
    'docs/final'      = 'documents'
    'presentation_v2' = 'presentation'
    'video'           = 'video'
}
foreach ($name in $folders.Keys) {
    $from = Join-Path $root $name
    if (-not (Test-Path -LiteralPath $from)) { throw "Missing folder: $name" }
    Copy-Item -LiteralPath $from -Destination (Join-Path $stage $folders[$name]) -Recurse -Force
}

$staged = Get-ChildItem -LiteralPath $stage -Recurse -File | Measure-Object -Property Length -Sum
'Staged {0} files, {1:N1} MB' -f $staged.Count, ($staged.Sum / 1MB)

# Build the ZIP entry by entry with '/' separators. Compress-Archive on Windows
# PowerShell writes '\' separators, which macOS and Linux unzip tools do not
# treat as directory separators.
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
$stream = [IO.File]::Open($zip, [IO.FileMode]::Create)
try {
    $archive = New-Object IO.Compression.ZipArchive($stream, [IO.Compression.ZipArchiveMode]::Create)
    try {
        Get-ChildItem -LiteralPath $stage -Recurse -File | ForEach-Object {
            $relative = $_.FullName.Substring($stage.Length + 1).Replace('\', '/')
            [IO.Compression.ZipFileExtensions]::CreateEntryFromFile(
                $archive, $_.FullName, $relative,
                [IO.Compression.CompressionLevel]::Optimal) | Out-Null
        }
    } finally {
        $archive.Dispose()
    }
} finally {
    $stream.Dispose()
}
'Created ' + $zip

$top = [IO.Compression.ZipFile]::OpenRead($zip).Entries |
    ForEach-Object { ($_.FullName -split '/')[0] } |
    Sort-Object -Unique
'ZIP root contains: ' + ($top -join ', ')
if ($top -notcontains 'Honour_Declaration.pdf') {
    throw 'Honour_Declaration.pdf is not at the ZIP root; do not submit this archive.'
}
