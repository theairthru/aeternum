# Aeternum Spicetify companion

This extension sends the current Spotify track ID, title, artist, album, duration, playing state and position to Aeternum on **127.0.0.1:38467**. It sends no Spotify tokens, account details, lyrics or API keys. It cannot control Spotify. Data is kept in memory and expires after five seconds without updates.

## Windows setup

1. Install Aeternum 0.6.0 in your Fabric instance, replacing the old Aeternum JAR, and restart Minecraft.
2. Extract the entire companion ZIP. Run `Install-Windows.cmd`. This copies the companion into your existing Spicetify Extensions folder, adds it to the enabled extensions, and reapplies Spicetify (Spotify may restart). Existing extensions stay enabled.
3. In Minecraft open **Right Shift > Lyrics > Spicy Lyrics / Spicetify setup**. Click **Copy Spicetify pairing code**.
4. In Spotify's profile menu click **Connect Aeternum**, paste that code and click Pair. This is a local pairing code, not your Spicy Lyrics key.
5. Create your application at https://developers.spicylyrics.org/dashboard/applications. Enable client access, obtain a publishable **sl_pk_** key, and enable the explicitly supported **no origin header / native client** option. Do not use a secret **sl_sk_** key. Paste the publishable key into Aeternum's setup screen and save it.
6. Play a track in Spotify. Spicy Lyrics is tried first using its exact ID; LRCLIB remains the fallback. The game and Spotify must run on the same computer. Ads, episodes and local-only tracks have no Spotify catalogue track ID and do not use Spicy Lyrics.

The setup screen shows pairing status. LRCLIB still works with no Spicy key or companion. In Lyrics settings, View full lyrics shows source credits as clickable links. Credits in the overlay can be clicked while Edit overlays is open.

If your developer application form asks for public source, publish the provided MIT-licensed Aeternum source ZIP to your own public repository and use that repository URL. The source has been prepared locally; no repository has been published for you. Do not upload your Minecraft `config` folder or any keys.

## Manual / Linux install

Find your configuration with `spicetify -c`. Copy `aeternum-companion.js` into the `Extensions` folder next to it, then run:

```
spicetify config extensions aeternum-companion.js
spicetify apply
```

Then pair as above. Linux media artwork/control support still uses playerctl; this companion supplies Spotify track identity and lyric timing independently.

## Disconnect / remove

In Minecraft click Disconnect companion to revoke the old pairing code. In Spotify select Disconnect Aeternum to erase its saved copy. To disable the extension run:

```
spicetify config extensions aeternum-companion.js-
spicetify apply
```

Source references: https://spicetify.app/docs/customization/extensions and https://developers.spicylyrics.org/docs/keys . This is an unofficial Aeternum integration, not a Spicy Lyrics or Spotify product.
