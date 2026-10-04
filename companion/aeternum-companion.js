// Aeternum Spotify companion. MIT license. Sends metadata only to your own Minecraft instance.
(async function aeternumCompanion() {
  while (!globalThis.Spicetify?.Player?.data || !Spicetify.Menu?.Item || !Spicetify.PopupModal) {
    await new Promise(resolve => setTimeout(resolve, 500));
  }
  const storage = "aeternum-pair-token";
  let token = localStorage.getItem(storage) || "", busy = false, retryAt = 0;
  new Spicetify.Menu.Item("Connect Aeternum", false, () => {
    const content = document.createElement("div");
    const label = document.createElement("p");
    label.textContent = "In Minecraft: Lyrics > Spicy Lyrics / Spicetify setup > Copy Spicetify pairing code. Paste it here.";
    const input = document.createElement("input"); input.type = "password"; input.placeholder = "Pairing code";
    input.style.cssText = "width:100%;padding:10px;margin:12px 0;color:#fff;background:#222;border:1px solid #777";
    const button = document.createElement("button"); button.textContent = "Pair";
    button.onclick = () => {
      const value = input.value.trim();
      if (!/^[a-f0-9]{64}$/.test(value)) { Spicetify.showNotification("Copy the pairing code from Aeternum first.", true); return; }
      token = value; localStorage.setItem(storage, token); retryAt = 0; Spicetify.PopupModal.hide(); send();
    };
    content.append(label, input, button); Spicetify.PopupModal.display({title:"Connect Aeternum",content});
  }).register();
  new Spicetify.Menu.Item("Disconnect Aeternum", false, () => { token = ""; localStorage.removeItem(storage); Spicetify.showNotification("Aeternum companion disconnected."); }).register();
  async function send() {
    if (!token || busy || Date.now() < retryAt) return;
    const item = Spicetify.Player.data?.item;
    const id = /^spotify:track:([A-Za-z0-9]{22})$/.exec(item?.uri || "")?.[1] || "";
    const metadata = item?.metadata || {};
    const artist = item?.artists?.map(a => a.name).join(", ") || metadata.artist_name || "";
    const payload = id ? {id,title:item.name || metadata.title || "",artist,album:item.album?.name || metadata.album_title || "",duration:Spicetify.Player.getDuration()/1000,position:Spicetify.Player.getProgress()/1000,playing:Spicetify.Player.isPlaying()} : {id:""};
    busy = true;
    try {
      const response = await fetch("http://127.0.0.1:38467/track", {method:"POST",headers:{"Content-Type":"application/json",Authorization:"Bearer "+token},body:JSON.stringify(payload),signal:AbortSignal.timeout(2000)});
      if (!response.ok) retryAt = Date.now()+5000;
    } catch { retryAt = Date.now()+5000; } finally { busy = false; }
  }
  Spicetify.Player.addEventListener("songchange", send);
  Spicetify.Player.addEventListener("onplaypause", send);
  setInterval(send, 500); send();
})();
