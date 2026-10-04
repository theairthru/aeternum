# Release checks — Aeternum 0.6.0

Target: Minecraft 1.21.11, Java 21, Fabric Loader 0.19.5, Fabric API 0.141.6+1.21.11. Checks ran in a separate development game directory on Windows, using the Fabric client game test runner.

## Passed

- Java compilation, resource processing, and production JAR remapping. Inspected a packaged camera injection to confirm production Minecraft names were remapped.
- Temporary single-player world creation. The original 0.1.0 release was also tested with standalone Fabric; the 0.5.0 revision was tested with the performance profile.
- Client startup and gameplay test with the exact Sodium, Lithium, FerriteCore, and ImmediatelyFast versions in the performance profile.
- Module grid, module settings, and searchable block picker rendering. Search input was exercised.
- Armour icons, durability bars and low-durability warning rendering.
- Overlay dragging, centre snapping, saving, and reloading the saved position.
- Freelook activation, independent rotation, release, and restoration of first-person perspective.
- Freecam activation, camera movement, no corresponding player movement, and deactivation.
- Low-health threshold activation and warning rendering. The hotbar translation mixin has been removed.
- Digital and antialiased analogue clock rendering; second-hand position uses fractional seconds.
- Projected coloured hitbox outlines visually verified on a test mob.
- X-ray rendering at partial and zero background opacity, including a selected ore behind a stone wall. Fluid opacity uses the vanilla fluid geometry path while X-ray is active so it remains compatible with the performance renderer.
- Return to the title screen without leaving freecam active.
- Windows media script syntax.
- Only the menu binding appears in Minecraft Controls; private module bindings still activate freelook/freecam/zoom.
- Mouse-wheel zoom adjustment, cinematic camera activation, and restoration after release.
- Correct freecam right-strafe direction.
- Scrollable square module grid, monochrome transparent panel, and rounded controls.
- Empty armour slots hidden during gameplay; occupied slots drawn opaque.
- Potion-effect icons rendered on the left.
- Media layout and album-art decoding/upload using synthetic test metadata and artwork. Actual OS media extraction/playback is not covered by that fixture.

## Limits of these checks

Actual media playback has **not** been verified. The isolated Windows build session's media-session request returned “The specified service does not exist as an installed service.” The bridge is implemented, but requires a working media session in the normal desktop user session. Linux/playerctl behavior is also untested here.

These checks do not measure an FPS improvement, test multiplayer server compatibility, exercise every block/entity/resource-pack combination, or establish compatibility with shaders or arbitrary additional mods. The FPS shown in test screenshots comes from an automated, tick-controlled test and is not a benchmark.

No normal Minecraft installation, personal world, account, or desktop media playback was changed by the tests.


## Rendering regression check

The HUD test enables the analogue clock and media/potion overlays and enforces a budget below 100 shape quads per frame (excluding text, item rendering and hitboxes). The clock itself uses 21 quads rather than the approximately 1,700–5,200 pixel rectangles produced by 0.2.0 at GUI scales 2–4 for representative hand angles. These are geometry counts, not an FPS benchmark. Rounded corners use filtered antialiased masks; clock ticks are cached in one texture.


## 0.3.0 checks

- Independent overlay resize, saved scale reload, and right-click navigation to the correct module settings.
- Removal of the duplicated menu shortcut from module settings.
- Cape PNG import, brush input, undo action, save, local-player skin override, no override on a remote player, and restoration when disabled.
- Media artwork layout without the album name.

The native operating-system file picker itself is not automated; PNG import is exercised directly. Actual live system-media metadata/playback remains subject to the limitations above.


## 0.5.0 thumbnail regression

Reproduced the old Windows PowerShell failure: the returned thumbnail stream is a System.__ComObject with no directly accessible GetInputStreamAt method or Size property. The production Read-ThumbnailBase64 helper now uses WinRT interface reflection and a managed stream adapter. A real Windows Runtime RandomAccessStreamReference backed by a local PNG returned all 12,418 bytes unchanged. Those extracted bytes are supplied to the in-game artwork rendering test. This tests the Windows stream conversion that previously failed; live Spotify/YouTube session access is still not available in the isolated build account.


## 0.5.0 checks

- Production WinRT thumbnail reader returns exact source bytes from a Windows Runtime stream (`tools/Test-Thumbnail.ps1`).
- Production playback handler observes playing/paused changes on consecutive requests without refreshing unchanged metadata or artwork (`tools/Test-Playback.ps1`, mocked session).
- Game test passes JPEG and lossless WebP import, exact WebP pixel preservation, transparent artwork corners, partially transparent antialiased edges, opaque artwork centre, LRC offsets/multiple timestamps, current-line selection, and immediate application of paused snapshots.
- Synced lyrics and rounded artwork use authored fixtures for visual validation; no personal media account is accessed.
- Live Spotify/YouTube state latency and live LRCLIB matching are not end-to-end verified in the isolated test environment. The configured Windows polling interval is 500 ms; this is not a guarantee about a player's own publishing delay.

## 0.5.0 validation scope

The presentation clock is checked against backward timestamp jitter, deliberate backward seek, and paused time. A 600 x 960 authored source is fitted into a 3840 x 1920 atlas with an exact source pixel preserved. Crop preview zoom, drag, apply and original-atlas import are exercised in the client game test. Lyrics bounds and visibility are checked independently of the media overlay, including the 26-pixel single-line mode and right-click settings. Musixmatch parser checks cover timed lyric parsing, retained copyright credit, and restricted-content handling. The key-entry screen uses a masked field and is visually checked with no real key.

No live Musixmatch API account is available in the test environment. API access and individual tracks must be checked with the user's own key; no successful live fallback is claimed. Small seek corrections under 1.5 seconds are intentionally smoothed rather than treated as discontinuities. High-resolution cape imports are bounded as documented to avoid unbounded texture allocation.

## 0.6.0 checks

- Full Fabric client smoke test with the pinned performance mods passed.
- Authored Spicy Lyrics syllable fixture checks text assembly, word character offsets and contributor URLs; visual checks confirm partial-word fill and persistent credits in the HUD and full-lyrics view.
- A real loopback HTTP test confirms paired metadata is accepted, missing authentication returns 401, unexpected browser origins return 403, and disconnect removes the current track.
- Node test (`tools/Test-Companion.cjs`) exercises the production extension against a mocked Spicetify API: track URI extraction, millisecond conversion, authenticated localhost requests, ads and disconnect.
- No Spicy Lyrics client key or live Spicetify session is available in the isolated environment. Live API responses, installed Spotify rendering/CORS, account setup and the Windows installer have not been end-to-end verified. No keys or real provider lyric data are bundled.
