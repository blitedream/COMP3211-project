# Assemble the COMP3211 submission ZIP.
#
# Required before running (see the course project description):
#   Honour_Declaration.pdf   at the repository root, filled and exported to PDF
#   video/demo.mp4           the <= 4 minute system demonstration
#   video/presentation.mp4   the <= 5 minute group presentation recording
#
# Usage:
#   .\make_submission.ps1
#   .\make_submission.ps1 -Preview  # clearly marked incomplete review archive
#
# The script only reads the repository; it writes the staging folder and the ZIP
# under build/ and refuses to clear anything outside that folder.

param([switch]$Preview)

$ErrorActionPreference = 'Stop'
$root = [IO.Path]::GetFullPath($PSScriptRoot)
$stageName = if ($Preview) { 'submission-preview' } else { 'submission' }
$archiveName = if ($Preview) { 'COMP3211_PREVIEW_NOT_FOR_SUBMISSION_' } else { 'COMP3211_submission_' }
$stage = [IO.Path]::GetFullPath((Join-Path $root "build/$stageName"))
$zip = Join-Path $root ('build/' + $archiveName + (Get-Date -Format 'yyyyMMdd_HHmmss_fff') + '.zip')

$required = @(
    'Honour_Declaration.pdf',
    'video/demo.mp4',
    'video/presentation.mp4'
)
$missing = @()
foreach ($rel in $required) {
    $full = Join-Path $root $rel
    if (-not (Test-Path -LiteralPath $full -PathType Leaf) -or (Get-Item -LiteralPath $full).Length -eq 0) {
        $missing += $rel
    }
}
if ($missing.Count -gt 0 -and -not $Preview) {
    throw ('Missing or empty required submission files: ' + ($missing -join ', ') + '. Complete these human deliverables; use -Preview only for an incomplete review archive.')
}

# Safety: only ever clear the staging folder inside build/.
$buildRoot = [IO.Path]::GetFullPath((Join-Path $root 'build')) + [IO.Path]::DirectorySeparatorChar
if (-not $stage.StartsWith($buildRoot, [StringComparison]::OrdinalIgnoreCase)) {
    throw "Refusing to clear a staging path outside build/: $stage"
}
if (Test-Path -LiteralPath $stage) {
    if ((Get-Item -LiteralPath $stage).Attributes -band [IO.FileAttributes]::ReparsePoint) {
        throw "Refusing to clear a linked staging folder: $stage"
    }
    Remove-Item -LiteralPath $stage -Recurse -Force
}
New-Item -ItemType Directory -Path $stage | Out-Null

# Files that must sit at the ZIP root (the Honour Declaration must be one of them).
$rootFiles = @(
    'Requirements_Coverage.pdf',
    'Model_Line_Coverage.pdf',
    ([string][char]0x542F + [char]0x52A8 + ' PIM.cmd'),
    'run.ps1',
    'run_tests.ps1',
    'run_d_tests.ps1',
    'pim.sh',
    'run_coverage.ps1'
)
foreach ($f in $rootFiles) {
    $full = Join-Path $root $f
    if (-not (Test-Path -LiteralPath $full)) { throw "Missing file: $f" }
    Copy-Item -LiteralPath $full -Destination $stage
}
if ($missing -notcontains 'Honour_Declaration.pdf') {
    Copy-Item -LiteralPath (Join-Path $root 'Honour_Declaration.pdf') -Destination $stage
}

# Use the same reading guide, with links matching the actual archive layout.
# Repository-only instructions refer to authoring files that are not submitted.
$readme = Get-Content -LiteralPath (Join-Path $root 'README.md') -Raw -Encoding UTF8
$readme = ($readme -split '<!-- REPOSITORY-ONLY:', 2)[0].TrimEnd()
$readme = $readme.Replace('(docs/final/', '(documents/').Replace('`docs/final/`', '`documents/`')
[IO.File]::WriteAllText((Join-Path $stage 'README.md'), $readme + "`n", (New-Object Text.UTF8Encoding($false)))
if ($Preview) {
    $notice = "NOT FOR SUBMISSION - preview archive for technical checks only.`n"
    $notice += "Missing or empty human deliverables: " + ($missing -join ', ') + "`n"
    $notice += "Video content, timing, identity requirements and the signed declaration require human review.`n"
    [IO.File]::WriteAllText((Join-Path $stage 'NOT_FOR_SUBMISSION.txt'), $notice)
}

# Repository folders copied into the ZIP under a stable name.
$folders = [ordered]@{
    'src'             = 'src'
    'lib'             = 'lib'
    'coverage-report' = 'coverage-report'
    'video'           = 'video'
}
foreach ($name in $folders.Keys) {
    $from = Join-Path $root $name
    if (-not (Test-Path -LiteralPath $from)) { throw "Missing folder: $name" }
    Copy-Item -LiteralPath $from -Destination (Join-Path $stage $folders[$name]) -Recurse -Force
}

# Copy the five course documents; recording guides stay in the working project.
New-Item -ItemType Directory -Path (Join-Path $stage 'documents') | Out-Null
foreach ($name in @('01_SRS.pdf','02_Design.pdf','03_User_Manual.pdf','04_Developer_Manual.pdf','05_Presentation.pdf')) {
    $sourceDocument = Join-Path $root "docs/final/$name"
    if (-not (Test-Path -LiteralPath $sourceDocument -PathType Leaf) -or (Get-Item -LiteralPath $sourceDocument).Length -eq 0) {
        throw "Missing or empty formal document: $name"
    }
    Copy-Item -LiteralPath $sourceDocument -Destination (Join-Path $stage "documents/$name")
}
$checkedLinks = 0
foreach ($match in [regex]::Matches($readme, '\]\(([^)]+)\)')) {
    $link = $match.Groups[1].Value
    if ($link -match '^(https?://|#)') { continue }
    $target = [IO.Path]::GetFullPath((Join-Path $stage $link))
    if (-not $target.StartsWith($stage + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase) -or -not (Test-Path -LiteralPath $target -PathType Leaf)) {
        throw "Broken or external file link in packaged README: $link"
    }
    $checkedLinks++
}
"Verified $checkedLinks packaged README file links and all five required formal PDFs."

$staged = Get-ChildItem -LiteralPath $stage -Recurse -File | Measure-Object -Property Length -Sum
'Staged {0} files, {1:N1} MB' -f $staged.Count, ($staged.Sum / 1MB)

# Build the ZIP entry by entry with '/' separators. Compress-Archive on Windows
# PowerShell writes '\' separators, which macOS and Linux unzip tools do not
# treat as directory separators.
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
$stream = [IO.File]::Open($zip, [IO.FileMode]::CreateNew)
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

$inspection = [IO.Compression.ZipFile]::OpenRead($zip)
try {
    $top = $inspection.Entries |
        ForEach-Object { ($_.FullName -split '/')[0] } |
        Sort-Object -Unique
} finally {
    $inspection.Dispose()
}
'ZIP root contains: ' + ($top -join ', ')
if (-not $Preview -and $top -notcontains 'Honour_Declaration.pdf') {
    throw 'Honour_Declaration.pdf is not at the ZIP root; do not submit this archive.'
}
