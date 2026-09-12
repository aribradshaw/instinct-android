'use strict';
const $=id=>document.getElementById(id);
let connected=false,busy=false,messages=[],lastFingerprint='',noticeTimer,refreshPressTimer,refreshHintTimer,skipRefreshClick=false;
let notificationEnabled=false;
const openSheet=id=>window.motion?window.motion.openSheet(id):$(id).showModal();
const closeSheet=id=>window.motion?window.motion.closeSheet(id):$(id).close();
const native=(method,...args)=>{if(window.Native&&typeof window.Native[method]==='function')window.Native[method](...args);};
function applyTheme(theme){
 const light=theme==='light';document.documentElement.dataset.theme=light?'light':'dark';
 const label=light?'Switch to dark mode':'Switch to light mode';$('themeButton').setAttribute('aria-label',label);$('themeButton').title=label;
}
applyTheme(window.matchMedia('(prefers-color-scheme: light)').matches?'light':'dark');
$('themeButton').onclick=()=>{const theme=document.documentElement.dataset.theme==='light'?'dark':'light';applyTheme(theme);native('setTheme',theme);};
function notice(message){$('notice').textContent=message;$('notice').hidden=false;clearTimeout(noticeTimer);noticeTimer=setTimeout(()=>$('notice').hidden=true,6500);}
function setConnected(value){connected=value;document.body.classList.toggle('connected',value);$('welcome').hidden=value;$('composer').hidden=!value;$('messages').hidden=!value;$('empty').hidden=!value||messages.length>0;$('statusText').textContent=value?'Connected through Gmail':'Gmail not connected';updateComposer();window.motion?.jumpState();}
function updateComposer(){const input=$('draft');input.style.height='auto';input.style.height=Math.min(input.scrollHeight,140)+'px';$('sendButton').disabled=!connected||busy||!input.value.trim();input.disabled=busy;}
function addLinks(element,text){const parts=text.split(/(https?:\/\/[^\s<>]+)/g);for(const part of parts){if(/^https?:\/\//.test(part)){const a=document.createElement('a');a.href=part;a.textContent=part;a.addEventListener('click',e=>{e.preventDefault();native('openUrl',part);});element.append(a);}else element.append(document.createTextNode(part));}}
function render(data){
 const fingerprint=JSON.stringify(data);if(fingerprint===lastFingerprint)return;
 const previousIds=new Set(messages.map(m=>m.id)),first=!lastFingerprint;messages=data;lastFingerprint=fingerprint;
 const area=$('timeline'),nearBottom=area.scrollHeight-area.scrollTop-area.clientHeight<160||!$('messages').children.length,oldTop=area.scrollTop;
 const anchor=[...$('messages').children].find(row=>row.getBoundingClientRect().bottom>area.getBoundingClientRect().top),anchorY=anchor?.getBoundingClientRect().top;
 const existing=new Map([...$('messages').children].map(row=>[row.dataset.key,row]));let day='';const desired=[];
 for(const message of data){
  const date=new Date(message.time),label=date.toLocaleDateString(undefined,{month:'short',day:'numeric',year:'numeric'});
  if(label!==day){day=label;const key='day:'+label,d=existing.get(key)||document.createElement('div');d.dataset.key=key;d.className='day';d.textContent=label;desired.push(d);}
  const key='message:'+message.id,signature=JSON.stringify(message),old=existing.get(key);
  if(old&&old.dataset.signature===signature){desired.push(old);continue;}
  const row=document.createElement('article');row.dataset.key=key;row.dataset.signature=signature;row.className='message'+(message.outgoing?' outgoing':'')+(!first&&!previousIds.has(message.id)?' arriving':'');
  if(!message.outgoing){const who=document.createElement('div');who.className='who';who.textContent='↗  INSTINCT';row.append(who);}
  const bubble=document.createElement('div');bubble.className='bubble';addLinks(bubble,message.text||'(No text in this email)');
  for(const attachment of message.attachments||[]){const button=document.createElement('button');button.className='attachment';button.textContent='↗ '+attachment+' · Open in Gmail';button.onclick=()=>native('openGmail');bubble.append(button);}
  row.append(bubble);const meta=document.createElement('div');meta.className='meta';const time=document.createElement('span');
  time.textContent=date.toLocaleTimeString(undefined,{hour:'numeric',minute:'2-digit'});meta.append(time);
  if(message.outgoing){const state=document.createElement('span');state.textContent=message.status==='sent'?'✓ Sent via Gmail':message.status==='failed'?'Not sent':'Unconfirmed · check Gmail';if(message.status!=='sent')state.className='state-warning';meta.append(state);}
  if(message.raw){const original=document.createElement('button');original.textContent='Original';original.onclick=()=>{$('originalBody').textContent=message.raw;openSheet('original');};meta.append(original);}
  row.append(meta);desired.push(row);
 }
 const keep=new Set(desired);for(const row of $('messages').children)if(!keep.has(row))row.dataset.remove='true';
 $('messages').querySelectorAll('[data-remove]').forEach(row=>row.remove());
 desired.forEach((row,index)=>{if($('messages').children[index]!==row)$('messages').insertBefore(row,$('messages').children[index]||null);});
 $('empty').hidden=!connected||data.length>0;requestAnimationFrame(()=>{
  if(nearBottom){if(window.motion)window.motion.toLatest(!first);else area.scrollTop=area.scrollHeight;}
  else{area.scrollTop=anchor?.isConnected?oldTop+anchor.getBoundingClientRect().top-anchorY:oldTop;window.motion?.arrived(data.filter(m=>!m.outgoing&&!previousIds.has(m.id)).length);}
 });
}
window.receive=(type,data)=>{
 if(type==='theme')applyTheme(data.theme);
 if(type==='notifications'){notificationEnabled=data.enabled;$('notificationToggle').setAttribute('aria-checked',String(data.enabled));$('notificationState').textContent=!data.enabled?'Off':data.allowed?'On · Android permission allowed':'Blocked by Android · tap Sound & permissions';$('notificationSummary').textContent=data.enabled&&data.allowed?'On ↗':'Off ↗';$('testNotification').disabled=!data.enabled||!data.allowed;}
 if(type==='sync'){$('refreshButton').disabled=data.busy;$('refreshButton').classList.toggle('syncing',data.busy);$('refreshButton').setAttribute('aria-label',data.busy?'Refreshing messages':'Refresh messages');}
 if(type==='init'||type==='connected'){if(data.account){$('accountLabel').textContent=data.account;$('accountValue').textContent=data.account;$('peerValue').textContent=data.peer;}}
 if(type==='init'){setConnected(data.connected);$('draft').value=data.draft||'';render(data.messages||[]);updateComposer();}
 if(type==='connected'){setConnected(data.connected);$('settings').close();}
 if(type==='messages')render(data.messages||[]);
 if(type==='status')$('statusText').textContent=data.label;
 if(type==='notice')notice(data.message);
 if(type==='sent'){busy=false;$('sendButton').classList.remove('sending');$('draft').value='';native('saveDraft','');$('sendWarning').hidden=true;updateComposer();window.motion?.toLatest(true);}
 if(type==='sendError'){busy=false;$('sendButton').classList.remove('sending');$('sendWarning').textContent=data.message;$('sendWarning').hidden=false;updateComposer();notice(data.message);}
};
$('connectButton').onclick=()=>native('connect');
$('settingsButton').onclick=()=>openSheet('settings');
$('closeSettings').onclick=()=>closeSheet('settings');
$('closeOriginal').onclick=()=>closeSheet('original');
function showRefreshHint(){
 const hint=$('refreshHint');clearTimeout(refreshHintTimer);hint.hidden=false;hint.classList.remove('exiting');void hint.offsetWidth;hint.classList.add('visible');
 refreshHintTimer=setTimeout(()=>{hint.classList.remove('visible');hint.classList.add('exiting');setTimeout(()=>{hint.hidden=true;hint.classList.remove('exiting');},220);},1800);
}
const refreshButton=$('refreshButton');
refreshButton.addEventListener('pointerdown',()=>{if(refreshButton.disabled)return;skipRefreshClick=false;refreshPressTimer=setTimeout(()=>{skipRefreshClick=true;showRefreshHint();},550);});
['pointerup','pointercancel','pointerleave'].forEach(type=>refreshButton.addEventListener(type,()=>clearTimeout(refreshPressTimer)));
refreshButton.addEventListener('contextmenu',event=>event.preventDefault());
refreshButton.onclick=()=>{if(skipRefreshClick){skipRefreshClick=false;return;}native('refresh');};
$('gmailButton').onclick=()=>native('openGmail');
$('reconnectButton').onclick=()=>{$('settings').close();native('connect');};
$('disconnectButton').onclick=()=>{$('settings').close();native('disconnect');};
$('draft').addEventListener('input',()=>{updateComposer();native('saveDraft',$('draft').value);});
$('sendButton').onclick=()=>{const text=$('draft').value.trim();if(!text||busy||!connected)return;busy=true;$('sendButton').classList.add('sending');updateComposer();native('send',text);};
$('notificationsButton').onclick=()=>{$('filterPeer').textContent=$('peerValue').textContent;native('notificationState');openSheet('notifications');};
$('closeNotifications').onclick=()=>closeSheet('notifications');
$('notificationToggle').onclick=()=>native('setNotifications',!notificationEnabled);
$('androidNotifications').onclick=()=>native('openNotificationSettings');
$('testNotification').onclick=()=>native('testNotification');
$('gmailFilterHelp').onclick=()=>native('openUrl','https://support.google.com/mail/answer/6579');

$('devlogButton').onclick=()=>native('openUrl','https://aribradshaw.github.io/instinct-android/');
