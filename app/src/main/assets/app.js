'use strict';
const $=id=>document.getElementById(id);
let connected=false,busy=false,messages=[],lastFingerprint='',noticeTimer;
const native=(method,...args)=>{if(window.Native&&typeof window.Native[method]==='function')window.Native[method](...args);};
function applyTheme(theme){
 const light=theme==='light';document.documentElement.dataset.theme=light?'light':'dark';
 const label=light?'Switch to dark mode':'Switch to light mode';$('themeButton').setAttribute('aria-label',label);$('themeButton').title=label;
}
applyTheme(window.matchMedia('(prefers-color-scheme: light)').matches?'light':'dark');
$('themeButton').onclick=()=>{const theme=document.documentElement.dataset.theme==='light'?'dark':'light';applyTheme(theme);native('setTheme',theme);};
function notice(message){$('notice').textContent=message;$('notice').hidden=false;clearTimeout(noticeTimer);noticeTimer=setTimeout(()=>$('notice').hidden=true,6500);}
function setConnected(value){connected=value;document.body.classList.toggle('connected',value);$('welcome').hidden=value;$('composer').hidden=!value;$('messages').hidden=!value;$('empty').hidden=!value||messages.length>0;$('statusText').textContent=value?'Connected through Gmail':'Gmail not connected';updateComposer();}
function updateComposer(){const input=$('draft');input.style.height='auto';input.style.height=Math.min(input.scrollHeight,140)+'px';$('sendButton').disabled=!connected||busy||!input.value.trim();input.disabled=busy;}
function addLinks(element,text){const parts=text.split(/(https?:\/\/[^\s<>]+)/g);for(const part of parts){if(/^https?:\/\//.test(part)){const a=document.createElement('a');a.href=part;a.textContent=part;a.addEventListener('click',e=>{e.preventDefault();native('openUrl',part);});element.append(a);}else element.append(document.createTextNode(part));}}
function render(data){
 messages=data;const fingerprint=JSON.stringify(data);if(fingerprint===lastFingerprint)return;lastFingerprint=fingerprint;
 const area=$('timeline'),nearBottom=area.scrollHeight-area.scrollTop-area.clientHeight<160||!$('messages').children.length,oldTop=area.scrollTop;
 $('messages').replaceChildren();let day='';
 for(const message of data){
  const date=new Date(message.time),label=date.toLocaleDateString(undefined,{month:'short',day:'numeric',year:'numeric'});
  if(label!==day){day=label;const d=document.createElement('div');d.className='day';d.textContent=label;$('messages').append(d);}
  const row=document.createElement('article');row.className='message'+(message.outgoing?' outgoing':'');
  if(!message.outgoing){const who=document.createElement('div');who.className='who';who.textContent='↗  INSTINCT';row.append(who);}
  const bubble=document.createElement('div');bubble.className='bubble';addLinks(bubble,message.text||'(No text in this email)');
  for(const attachment of message.attachments||[]){const button=document.createElement('button');button.className='attachment';button.textContent='↗ '+attachment+' · Open in Gmail';button.onclick=()=>native('openGmail');bubble.append(button);}
  row.append(bubble);const meta=document.createElement('div');meta.className='meta';const time=document.createElement('span');
  time.textContent=date.toLocaleTimeString(undefined,{hour:'numeric',minute:'2-digit'});meta.append(time);
  if(message.outgoing){const state=document.createElement('span');state.textContent=message.status==='sent'?'✓ Sent via Gmail':message.status==='failed'?'Not sent':'Unconfirmed · check Gmail';if(message.status!=='sent')state.className='state-warning';meta.append(state);}
  if(message.raw){const original=document.createElement('button');original.textContent='Original';original.onclick=()=>{$('originalBody').textContent=message.raw;$('original').showModal();};meta.append(original);}
  row.append(meta);$('messages').append(row);
 }
 $('empty').hidden=!connected||data.length>0;requestAnimationFrame(()=>{area.scrollTop=nearBottom?area.scrollHeight:oldTop;});
}
window.receive=(type,data)=>{
 if(type==='theme')applyTheme(data.theme);
 if(type==='init'||type==='connected'){if(data.account){$('accountLabel').textContent=data.account;$('accountValue').textContent=data.account;$('peerValue').textContent=data.peer;}}
 if(type==='init'){messages=data.messages||[];setConnected(data.connected);$('draft').value=data.draft||'';render(messages);updateComposer();}
 if(type==='connected'){setConnected(data.connected);$('settings').close();}
 if(type==='messages')render(data.messages||[]);
 if(type==='status')$('statusText').textContent=data.label;
 if(type==='notice')notice(data.message);
 if(type==='sent'){busy=false;$('draft').value='';native('saveDraft','');$('sendWarning').hidden=true;updateComposer();$('timeline').scrollTop=$('timeline').scrollHeight;}
 if(type==='sendError'){busy=false;$('sendWarning').textContent=data.message;$('sendWarning').hidden=false;updateComposer();notice(data.message);}
};
$('connectButton').onclick=()=>native('connect');
$('settingsButton').onclick=()=>$('settings').showModal();
$('closeSettings').onclick=()=>$('settings').close();
$('closeOriginal').onclick=()=>$('original').close();
$('refreshButton').onclick=()=>native('refresh');
$('gmailButton').onclick=()=>native('openGmail');
$('reconnectButton').onclick=()=>{$('settings').close();native('connect');};
$('disconnectButton').onclick=()=>{$('settings').close();native('disconnect');};
$('draft').addEventListener('input',()=>{updateComposer();native('saveDraft',$('draft').value);});
$('sendButton').onclick=()=>{const text=$('draft').value.trim();if(!text||busy||!connected)return;busy=true;updateComposer();native('send',text);};
for(const id of ['settings','original'])$(id).addEventListener('click',e=>{if(e.target===$(id)){const rect=$(id).getBoundingClientRect();if(e.clientY<rect.top)$(id).close();}});

$('devlogButton').onclick=()=>native('openUrl','https://aribradshaw.github.io/instinct-android/');
