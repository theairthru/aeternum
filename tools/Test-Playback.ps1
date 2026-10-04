$ErrorActionPreference='Stop'
$source=Get-Content (Join-Path $PSScriptRoot '../src/main/resources/assets/aeternum/media.ps1') -Raw
Invoke-Expression ($source.Substring(0,$source.IndexOf('$manager=Await-Result')))
$script:status='Paused'
$session=[pscustomobject]@{SourceAppUserModelId='fixture'}
$session | Add-Member ScriptMethod GetTimelineProperties { [pscustomobject]@{Position=[TimeSpan]::FromSeconds(12);StartTime=[TimeSpan]::Zero;EndTime=[TimeSpan]::FromSeconds(120);LastUpdatedTime=[DateTimeOffset]::Now} }
$session | Add-Member ScriptMethod GetPlaybackInfo { [pscustomobject]@{PlaybackStatus=$script:status} }
$manager=[pscustomobject]@{}
$manager | Add-Member ScriptMethod GetCurrentSession { $session }
$script:cachedMedia=[pscustomobject]@{Title='Fixture';Artist='Test';AlbumTitle='';Thumbnail=$null}
$script:cachedSource='fixture';$script:metadataUntil=[long]::MaxValue
$script:lastArtKey='fixture|Fixture|Test||120';$script:artRetryAt=[long]::MaxValue
$first=Invoke-MediaAction status
if($first.playing -or $first.position -ne 12){throw 'Paused snapshot incorrect'}
$script:status='Playing'
$second=Invoke-MediaAction status
if(-not $second.playing){throw 'Playback state was cached'}
if($second.ContainsKey('art')){throw 'Unchanged artwork was unnecessarily resent'}
$script:status='Paused'
$third=Invoke-MediaAction status
if($third.playing){throw 'Pause state was cached'}
'PASS: production helper reads current playback status on every request while retaining cached metadata/artwork.'
