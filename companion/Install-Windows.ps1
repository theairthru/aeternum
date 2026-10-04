$ErrorActionPreference='Stop'
$command=Get-Command spicetify -ErrorAction SilentlyContinue
if($null -eq $command){
    $candidate=Join-Path $env:LOCALAPPDATA 'spicetify/spicetify.exe'
    if(Test-Path -LiteralPath $candidate){$exe=$candidate}else{throw 'Spicetify was not found. Open a terminal where spicetify works and run this installer again.'}
}else{$exe=$command.Source}
$config=(& $exe -c | Out-String).Trim()
if($LASTEXITCODE -ne 0 -or -not (Test-Path -LiteralPath $config -PathType Leaf)){throw 'Could not find the existing Spicetify configuration.'}
$extensions=Join-Path (Split-Path -Parent $config) 'Extensions'
New-Item -ItemType Directory -Path $extensions -Force | Out-Null
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'aeternum-companion.js') -Destination (Join-Path $extensions 'aeternum-companion.js') -Force
& $exe config extensions aeternum-companion.js
if($LASTEXITCODE -ne 0){throw 'Could not enable the companion.'}
& $exe apply
if($LASTEXITCODE -ne 0){throw 'Spicetify could not apply. Check whether your Spotify installation needs spicetify backup apply after an update.'}
Write-Host 'Installed. In Minecraft copy the pairing code from Lyrics > Spicy Lyrics / Spicetify setup, then open Connect Aeternum in Spotify profile menu.'
