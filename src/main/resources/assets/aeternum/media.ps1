param([ValidateSet('status','previous','next','toggle','serve')][string]$Action='status')
$ErrorActionPreference='Stop'
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
Add-Type -AssemblyName System.Runtime.WindowsRuntime
$null=[Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager,Windows.Media.Control,ContentType=WindowsRuntime]
$null=[Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties,Windows.Media.Control,ContentType=WindowsRuntime]
$asTask=[System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object { $_.Name -eq 'AsTask' -and $_.IsGenericMethod -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1' } | Select-Object -First 1
function Await-Result($Operation,[Type]$Type) {
    $task=$asTask.MakeGenericMethod($Type).Invoke($null,@($Operation))
    if(-not $task.Wait(3500)){throw 'Media request timed out'}
    $task.Result
}
function Read-ThumbnailBase64($Reference) {
    # Interface-valued WinRT results are opaque COM objects in Windows PowerShell.
    # Invoke through their declared interfaces instead of PowerShell member lookup.
    $referenceType=[Windows.Storage.Streams.IRandomAccessStreamReference,Windows.Storage.Streams,ContentType=WindowsRuntime]
    $contentType=[Windows.Storage.Streams.IRandomAccessStreamWithContentType,Windows.Storage.Streams,ContentType=WindowsRuntime]
    $randomType=[Windows.Storage.Streams.IRandomAccessStream,Windows.Storage.Streams,ContentType=WindowsRuntime]
    $operation=$referenceType.GetMethod('OpenReadAsync').Invoke($Reference,@())
    $stream=Await-Result $operation $contentType
    $managed=$null;$memory=$null
    try {
        $size=$randomType.GetProperty('Size').GetValue($stream)
        if($size -gt 8388608){throw 'Thumbnail exceeds 8 MB'}
        $inputStream=$randomType.GetMethod('GetInputStreamAt').Invoke($stream,@([uint64]0))
        $adapter=[System.IO.WindowsRuntimeStreamExtensions].GetMethods() | Where-Object { $_.Name -eq 'AsStreamForRead' -and $_.GetParameters().Count -eq 1 } | Select-Object -First 1
        $managed=$adapter.Invoke($null,@($inputStream))
        $memory=[IO.MemoryStream]::new();$buffer=[byte[]]::new(65536)
        while(($count=$managed.Read($buffer,0,$buffer.Length)) -gt 0){
            if($memory.Length+$count -gt 8388608){throw 'Thumbnail exceeds 8 MB'}
            $memory.Write($buffer,0,$count)
        }
        if($memory.Length -eq 0){throw 'Player supplied an empty thumbnail'}
        [Convert]::ToBase64String($memory.ToArray())
    } finally {
        if($null -ne $managed){$managed.Dispose()}
        if($null -ne $memory){$memory.Dispose()}
        # Do not let COM cleanup hide an otherwise successfully decoded image.
        try{[IDisposable].GetMethod('Dispose').Invoke($stream,@()) | Out-Null}catch{}
    }
}
function Invoke-MediaAction([string]$Request) {
    $session=$manager.GetCurrentSession()
    if($null -eq $session){$script:cachedMedia=$null;$script:lastArtKey='';return @{available=$false}}
    $ok=$true
    switch($Request) {
        'next' {$ok=Await-Result ($session.TrySkipNextAsync()) ([bool])}
        'toggle' {$ok=Await-Result ($session.TryTogglePlayPauseAsync()) ([bool])}
        'previous' {
            $timeline=$session.GetTimelineProperties()
            $seconds=($timeline.Position-$timeline.StartTime).TotalSeconds
            if($session.GetPlaybackInfo().PlaybackStatus.ToString() -eq 'Playing'){$seconds += [Math]::Max(0,([DateTimeOffset]::Now-$timeline.LastUpdatedTime).TotalSeconds)}
            if($seconds -le 3){$ok=Await-Result ($session.TrySkipPreviousAsync()) ([bool])}
            else {$ok=Await-Result ($session.TryChangePlaybackPositionAsync($timeline.StartTime.Ticks)) ([bool])}
        }
    }
    $now=[DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
    $source=[string]$session.SourceAppUserModelId
    if($null -eq $script:cachedMedia -or $source -ne $script:cachedSource -or $now -ge $script:metadataUntil -or $Request -ne 'status'){
        $script:cachedMedia=Await-Result ($session.TryGetMediaPropertiesAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties])
        $script:cachedSource=$source;$script:metadataUntil=$now+1000
    }
    $media=$script:cachedMedia
    $timeline=$session.GetTimelineProperties()
    $playing=$session.GetPlaybackInfo().PlaybackStatus.ToString() -eq 'Playing'
    $position=($timeline.Position-$timeline.StartTime).TotalSeconds
    if($playing){$position += [Math]::Max(0,([DateTimeOffset]::Now-$timeline.LastUpdatedTime).TotalSeconds)}
    $duration=($timeline.EndTime-$timeline.StartTime).TotalSeconds
    $result=@{available=$true;title=[string]$media.Title;artist=[string]$media.Artist;album=[string]$media.AlbumTitle;position=$position;duration=$duration;playing=$playing;measuredAt=[DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds();commandOk=$ok}
    $key=$source+'|'+$media.Title+'|'+$media.Artist+'|'+$media.AlbumTitle+'|'+$duration
    if($key -ne $script:lastArtKey -or $now -ge $script:artRetryAt){
        $result.art='';$result.artError=''
        try{if($null -ne $media.Thumbnail){$result.art=Read-ThumbnailBase64 $media.Thumbnail}}
        catch{$result.artError=$_.Exception.GetBaseException().Message}
        $script:lastArtKey=$key
        $script:artRetryAt=if($result.art){[long]::MaxValue}else{$now+5000}
    }
    return $result
}
$manager=Await-Result ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager])
if($Action -eq 'serve'){
    # One helper for the game session: avoid starting PowerShell for each refresh.
    while($null -ne ($request=[Console]::ReadLine())){
        if($request -notin @('status','previous','next','toggle')){continue}
        try{$result=Invoke-MediaAction $request}
        catch{$result=@{available=$false;error=$_.Exception.GetBaseException().Message}}
        [Console]::WriteLine(($result | ConvertTo-Json -Compress))
    }
}else{Invoke-MediaAction $Action | ConvertTo-Json -Compress}
