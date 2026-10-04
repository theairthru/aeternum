# Run with Windows PowerShell: powershell.exe -NoProfile -File tools/Test-Thumbnail.ps1
$ErrorActionPreference='Stop'
$project=Split-Path $PSScriptRoot -Parent
$script=Get-Content (Join-Path $project 'src/main/resources/assets/aeternum/media.ps1') -Raw
$start=$script.IndexOf('Add-Type -AssemblyName')
$end=$script.IndexOf('$manager=Await-Result')
# Load only the bridge helpers; never access or control a desktop media session.
. ([scriptblock]::Create($script.Substring($start,$end-$start)))
$null=[Windows.Storage.StorageFile,Windows.Storage,ContentType=WindowsRuntime]
$null=[Windows.Storage.Streams.RandomAccessStreamReference,Windows.Storage.Streams,ContentType=WindowsRuntime]
$path=Join-Path $project 'src/main/resources/assets/aeternum/textures/gui/clock.png'
$file=Await-Result ([Windows.Storage.StorageFile]::GetFileFromPathAsync($path)) ([Windows.Storage.StorageFile])
$reference=[Windows.Storage.Streams.RandomAccessStreamReference]::CreateFromFile($file)
$actual=Read-ThumbnailBase64 $reference
$expected=[Convert]::ToBase64String([IO.File]::ReadAllBytes($path))
if($actual -ne $expected){throw 'Thumbnail bytes changed'}
Write-Output 'PASS: production WinRT thumbnail reader returned the original image bytes.'
