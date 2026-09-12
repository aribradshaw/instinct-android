'use strict';
const $=id=>document.getElementById(id);
let connected=false,busy=false,messages=[],lastFingerprint='',refreshPressTimer,refreshHintTimer,refreshHideTimer,skipRefreshClick=false;
let notificationEnabled=false,accent='default',cadenceMode='foreground',whatsappConfigured=false,whatsappAccess=false,whatsappReplyReady=false,activeChannel='gmail';
let draftFiles=[],replyId='',fileBusy=false,lastChecked=0,statusOverride='';
const devlogEntries=[{"version":"1.0.7","date":"2026-09-11","title":"Render image attachments inline in chats and drafts","summary":"Source update. See the linked commit for details.","notes":["Render image attachments inline in chats and drafts"],"commit":"cc9576328d88b332fbcce00de047c7e5d553c205","author":{"name":"Ari Bradshaw","githubLogin":"aribradshaw"}},{"version":"1.0.6","date":"2026-09-11","title":"Show incoming Instinct vault requests as animated cards","summary":"Source update. See the linked commit for details.","notes":["Show incoming Instinct vault requests as animated cards"],"commit":"430ebb3719ef812502dbdda8ce4dfc652bd704c3","author":{"name":"Ari Bradshaw","githubLogin":"aribradshaw"}},{"version":"1.0.5","date":"2026-09-11","title":"Keep notices above active sheets","summary":"Source update. See the linked commit for details.","notes":["Keep notices above active sheets"],"commit":"d3248678c75ca8ee7c3d33b16cc5a6c32ba8b39f","author":{"name":"Ari Bradshaw","githubLogin":"aribradshaw"}},{"version":"1.0.4","date":"2026-09-11","title":"Polish in-app notices and streamline README","summary":"Source update. See the linked commit for details.","notes":["Polish in-app notices and streamline README"],"commit":"20ee669ee9f0f7ac87cc4e33125452219a1d6bdc","author":{"name":"Ari Bradshaw","githubLogin":"aribradshaw"}},{"version":"1.0.3","date":"2026-09-11","title":"Automatically version source updates and backfill the DevLog","summary":"Source update. See the linked commit for details.","notes":["Automatically version source updates and backfill the DevLog"],"commit":"6e0177e4cf9a7411a7a92ce1698b10a30180333e","author":{"name":"Ari Bradshaw","githubLogin":"aribradshaw"}},{"version":"1.0.2","date":"2026-09-11","title":"Attachments and everyday reliability","summary":"Send files, find past messages, and recover failed sends.","notes":["Attach up to 8 files totaling 12 MB, with encrypted attachment drafts and removable file chips.","Search saved messages, copy text, reply to a selected message, and dictate an editable draft.","Load older history and catch up new messages using a saved sync cursor.","Fix first-reply alerts after an empty initial sync and show when messages were last checked.","Recover definite failures for editing; uncertain sends keep their Gmail check action.","Tap the lightning logo to refresh, or hold it for an animated hint.","Set foreground and background refresh intervals from settings.","Choose an accent color, including the Android system color, and use the compact account settings layout."],"author":{"name":"Ari Bradshaw","githubLogin":"aribradshaw"},"commit":"a62c96efe346f272b9b99d25629bd8e598cb9845"},{"version":"1.0.1","date":"2026-09-11","title":"First release","summary":"Android email chat with light and dark themes.","notes":["Send and receive through Gmail; keep history and drafts encrypted on your phone.","Elastic scrolling, tap feedback, and a button to jump to new replies.","Notification controls and instructions for muting Instinct emails in Gmail.","Publish the source under MIT with setup instructions, security guidance, and dependency notices.","Make the Gradle wrapper executable for Linux builds.","Simplify the homepage and move the searchable DevLog to its own page.","Remove the homepage icon and extra sections; use matching fonts with off-white and lime words."],"author":{"name":"Ari Bradshaw","githubLogin":"aribradshaw"},"includedCommits":[{"sha":"23779cb60c918ba71b7f3138f28a5a5afeff837d","subject":"Initial Instinct Android companion with public DevLog"},{"sha":"0d12d80733b968437e548a2c1fde331dc8bbde96","subject":"Make Gradle wrapper executable for source builds"},{"sha":"e3066b8732e1ade068e91e95faaa6bb1ca04ba08","subject":"Start year.month.version at 1.0.1 and simplify homepage"},{"sha":"bc1918c9381f7ce4ab9b4928097f57645993aa47","subject":"Separate DevLog page and simplify homepage branding"}]}];
const openSheet=id=>window.motion?window.motion.openSheet(id):$(id).showModal();
const closeSheet=id=>window.motion?window.motion.closeSheet(id):$(id).close();
const native=(method,...args)=>{if(window.Native&&typeof window.Native[method]==='function')window.Native[method](...args);};
function applyTheme(theme){
 const light=theme==='light';document.documentElement.dataset.theme=light?'light':'dark';
 const label=light?'Switch to dark mode':'Switch to light mode';$('themeButton').setAttribute('aria-label',label);$('themeButton').title=label;
}
function applyAccent(value,color){accent=value;const root=document.documentElement;if(value==='default'){root.style.removeProperty('--lime');root.style.removeProperty('--accent-text');root.style.removeProperty('--accent-line');root.style.removeProperty('--accent-surface');}else{const chosen=color||value;root.style.setProperty('--lime',chosen);root.style.setProperty('--accent-text',chosen);root.style.setProperty('--accent-line',chosen+'88');root.style.setProperty('--accent-surface',chosen+'18');}document.querySelectorAll('.accent-choice').forEach(button=>button.classList.toggle('selected',button.dataset.accent===value));$('accentSummary').textContent=value==='system'?'Android color':value==='default'?'Green':document.querySelector(`.accent-choice[data-accent="${value}"]`)?.textContent||'Custom';}
applyTheme(window.matchMedia('(prefers-color-scheme: light)').matches?'light':'dark');
$('themeButton').onclick=()=>{const theme=document.documentElement.dataset.theme==='light'?'dark':'light';applyTheme(theme);native('setTheme',theme);};
function notice(message){window.notices.show(message);}
function setConnected(value){connected=value;document.body.classList.toggle('connected',value);$('welcome').hidden=value;$('composer').hidden=!value;$('messages').hidden=!value;$('empty').hidden=!value||messages.length>0;$('statusText').textContent=value?'Connected through Gmail':'Gmail not connected';updateComposer();window.motion?.jumpState();}
function selectChannel(channel){if(channel==='whatsapp'&&!whatsappConfigured)return;activeChannel=channel;document.querySelectorAll('#channelPicker button').forEach(button=>button.classList.toggle('selected',button.dataset.channel===channel));$('channelHint').textContent=channel==='whatsapp'?(whatsappReplyReady?'WhatsApp reply ready':'Open chat to prime replies'):'Email thread';$('attachButton').hidden=channel==='whatsapp';$('composeExtras').hidden=channel==='whatsapp'||!draftFiles.length;$('draft').maxLength=channel==='whatsapp'?4096:30000;updateComposer();}
function updateComposer(){const input=$('draft');input.style.height='auto';input.style.height=Math.min(input.scrollHeight,140)+'px';const hasContent=activeChannel==='whatsapp'?!!input.value.trim():!!input.value.trim()||!!draftFiles.length;const transportReady=activeChannel==='whatsapp'?whatsappConfigured&&whatsappAccess&&whatsappReplyReady:connected;$('sendButton').disabled=!transportReady||busy||fileBusy||!hasContent;input.disabled=busy;$('attachButton').disabled=busy||fileBusy;$('cancelReply').disabled=busy;$('dictateButton').disabled=busy;document.querySelectorAll('#fileChips button').forEach(button=>button.disabled=busy||fileBusy);$('conversationTools').hidden=!connected;}
function composeState(data){draftFiles=data.files||[];replyId=data.replyId||'';$('fileChips').replaceChildren();draftFiles.forEach((file,index)=>{const chip=document.createElement('button');chip.className='file-chip';chip.textContent=`${file.name} · ${(file.size/1024).toFixed(0)} KB ×`;chip.setAttribute('aria-label',`Remove ${file.name}`);chip.onclick=()=>native('removeAttachment',index);$('fileChips').append(chip);});const reply=messages.find(m=>m.id===replyId);$('replyPreview').hidden=!replyId;$('replyLabel').textContent=replyId?'Replying to: '+(reply?.text||'Selected message').slice(0,90):'';updateComposer();}
function updateHealth(){if(!connected||statusOverride)return;const minutes=Math.max(0,Math.floor((Date.now()-lastChecked)/60000));$('statusText').textContent=lastChecked?`Checked ${minutes<1?'just now':minutes+' min ago'}`:'Waiting for first sync';}
setInterval(updateHealth,30000);
function filterMessages(){const query=$('searchInput').value.trim().toLowerCase();let count=0;document.querySelectorAll('#messages .message').forEach(row=>{const message=messages.find(m=>'message:'+m.id===row.dataset.key);row.hidden=!!query&&!((message?.text||'')+' '+(message?.attachments||[]).join(' ')).toLowerCase().includes(query);if(!row.hidden)count++;});document.querySelectorAll('#messages .day').forEach(row=>row.hidden=!!query);$('searchCount').textContent=query?`${count} matches in saved messages`:'';}
function addMessageText(element,text){const parts=text.split(/(```[\s\S]*?```)/g);for(const part of parts){if(part.startsWith('```')){const pre=document.createElement('pre'),code=document.createElement('code');code.textContent=part.slice(3,-3).replace(/^[a-zA-Z0-9_+-]*\n/,'');pre.append(code);element.append(pre);}else addLinks(element,part);}}
function appendImagePreview(parent,source,name){
 if(typeof source!=='string'||!/^data:image\/jpeg;base64,[A-Za-z0-9+/=]+$/.test(source))return;
 const image=document.createElement('img');image.className='image-preview';image.alt=name;image.src=source;image.decoding='async';
 image.addEventListener('error',()=>image.remove());
 image.addEventListener('load',()=>{const area=$('timeline');if(area.scrollHeight-area.scrollTop-area.clientHeight<440)window.motion?.toLatest(false);});
 parent.append(image);
}
function addLinks(element,text){const parts=text.split(/(https?:\/\/[^\s<>]+)/g);for(const part of parts){if(/^https?:\/\//.test(part)){const a=document.createElement('a');a.href=part;a.textContent=part;a.addEventListener('click',e=>{e.preventDefault();native('openUrl',part);});element.append(a);}else element.append(document.createTextNode(part));}}
function renderDevlog(){const target=$('devlogEntries');if(target.childElementCount)return;for(const entry of devlogEntries){const article=document.createElement('article');article.className='devlog-entry';const meta=document.createElement('div');meta.className='devlog-meta';meta.textContent=`${entry.version} · ${entry.date}`;const title=document.createElement('h3');title.textContent=entry.title;const summary=document.createElement('p');summary.textContent=entry.summary;const notes=document.createElement('ul');for(const note of entry.notes){const item=document.createElement('li');item.textContent=note;notes.append(item);}article.append(meta,title,summary,notes);target.append(article);}}
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
  const vault=!message.outgoing?VaultCards.extract(message.text||''):{text:message.text||'',urls:[]};
  const bubble=document.createElement('div');bubble.className='bubble';if(vault.text||!vault.urls.length)addMessageText(bubble,vault.text||'(Attachment)');
  (message.attachments||[]).forEach((attachment,index)=>{const preview=(message.previews||[]).find(item=>item.index===index)?.preview;appendImagePreview(bubble,preview,attachment);const button=document.createElement('button');button.className='attachment';button.textContent='↗ '+attachment+' · Open in Gmail';button.onclick=()=>native('openGmail');bubble.append(button);});
  if(bubble.childNodes.length)row.append(bubble);
  for(const url of vault.urls)row.append(VaultCards.card(url,url=>native('openUrl',url)));
  const meta=document.createElement('div');meta.className='meta';const time=document.createElement('span');
  time.textContent=date.toLocaleTimeString(undefined,{hour:'numeric',minute:'2-digit'});meta.append(time);
  const channel=document.createElement('span');channel.className='channel-tag';channel.textContent=message.channel==='whatsapp'?'WhatsApp':'Gmail';meta.append(channel);
  if(message.outgoing){const state=document.createElement('span');state.textContent=message.channel==='whatsapp'?'✓ Handed to WhatsApp':message.status==='sent'?'✓ Sent via Gmail':message.status==='failed'?'Not sent':'Unconfirmed · check Gmail';if(message.status!=='sent'&&message.channel!=='whatsapp')state.className='state-warning';meta.append(state);}
  if(message.raw){const original=document.createElement('button');original.textContent='Original';original.onclick=()=>{$('originalBody').textContent=message.raw;openSheet('original');};meta.append(original);}
  const copy=document.createElement('button');copy.textContent='Copy';copy.onclick=()=>native('copy',message.text||'');meta.append(copy);
  if(message.status==='received'||message.status==='sent'||message.status==='submitted'){const reply=document.createElement('button');reply.textContent='Reply';reply.onclick=()=>{if(busy)return;if(message.channel==='whatsapp')selectChannel('whatsapp');else{selectChannel('gmail');native('reply',message.id);}$('draft').focus();};meta.append(reply);}
  if(message.status==='failed'){const recover=document.createElement('button');recover.textContent='Edit and resend';recover.onclick=()=>native('recover',message.id);meta.append(recover);}
  if(message.status==='unconfirmed'){const check=document.createElement('button');check.textContent='Check in Gmail';check.onclick=()=>native('openGmail');meta.append(check);}
  row.append(meta);desired.push(row);
 }
 const keep=new Set(desired);for(const row of $('messages').children)if(!keep.has(row))row.dataset.remove='true';
 $('messages').querySelectorAll('[data-remove]').forEach(row=>row.remove());
 desired.forEach((row,index)=>{if($('messages').children[index]!==row)$('messages').insertBefore(row,$('messages').children[index]||null);});
 filterMessages();$('empty').hidden=!connected||data.length>0;requestAnimationFrame(()=>{
  if(nearBottom){if(window.motion)window.motion.toLatest(!first);else area.scrollTop=area.scrollHeight;}
  else{area.scrollTop=anchor?.isConnected?oldTop+anchor.getBoundingClientRect().top-anchorY:oldTop;window.motion?.arrived(data.filter(m=>!m.outgoing&&!previousIds.has(m.id)).length);}
 });
}
window.receive=(type,data)=>{
 if(type==='compose'){composeState(data);[...$('fileChips').children].forEach((chip,index)=>appendImagePreview(chip,data.files?.[index]?.preview,data.files?.[index]?.name||'Attached image'));}
 if(type==='fileBusy'){fileBusy=data.busy;updateComposer();if(fileBusy)notice('Preparing attachment…');}
 if(type==='health'){lastChecked=data.lastChecked||0;$('olderButton').hidden=!data.hasOlder;updateHealth();}
 if(type==='draft'){$('draft').value=data.text||'';updateComposer();}
 if(type==='dictation'&&!busy){$('draft').value=($('draft').value+' '+data.text).trim().slice(0,30000);native('saveDraft',$('draft').value);updateComposer();}
 if(type==='theme')applyTheme(data.theme);
 if(type==='appearance')applyAccent(data.accent,data.color);
 if(type==='cadence'){const foreground=data.foregroundSeconds,background=data.backgroundMinutes;$('foregroundCadence').textContent=`${foreground} sec while open`;$('backgroundCadence').textContent=`~${background} min in background`;}
 if(type==='notifications'){notificationEnabled=data.enabled;$('notificationToggle').setAttribute('aria-checked',String(data.enabled));$('notificationState').textContent=!data.enabled?'Off':data.allowed?'On · Android permission allowed':'Blocked by Android · tap Sound & permissions';$('notificationSummary').textContent=data.enabled&&data.allowed?'On ↗':'Off ↗';$('testNotification').disabled=!data.enabled||!data.allowed;}
 if(type==='whatsapp'){whatsappConfigured=!!data.configured;whatsappAccess=!!data.access;whatsappReplyReady=!!data.replyReady;$('channelPicker').hidden=!whatsappConfigured;$('whatsappSummary').textContent=!whatsappConfigured?'Connect ↗':!whatsappAccess?'Allow access ↗':whatsappReplyReady?'Ready ✓':'Waiting for reply';$('openWhatsAppButton').hidden=!whatsappConfigured;if(!whatsappConfigured&&activeChannel==='whatsapp')activeChannel='gmail';selectChannel(activeChannel);}
 if(type==='sync'){$('refreshButton').disabled=data.busy;$('refreshButton').classList.toggle('syncing',data.busy);$('refreshButton').setAttribute('aria-label',data.busy?'Refreshing messages':'Refresh messages');}
 if(type==='init'||type==='connected'){if(data.account){$('accountLabel').textContent=data.account;$('accountValue').textContent=data.account;$('peerValue').textContent=data.peer;}}
 if(type==='init'){setConnected(data.connected);$('draft').value=data.draft||'';render(data.messages||[]);updateComposer();}
 if(type==='connected'){setConnected(data.connected);$('settings').close();}
 if(type==='messages')render(data.messages||[]);
 if(type==='status'){statusOverride=data.label||'';if(statusOverride)$('statusText').textContent=statusOverride;else updateHealth();}
 if(type==='notice')notice(data.message);
 if(type==='sent'){busy=false;$('sendButton').classList.remove('sending');$('draft').value='';native('saveDraft','');$('sendWarning').hidden=true;updateComposer();window.motion?.toLatest(true);}
 if(type==='sendError'){busy=false;$('sendButton').classList.remove('sending');$('sendWarning').textContent=data.message;$('sendWarning').hidden=false;updateComposer();notice(data.message);}
};
$('connectButton').onclick=()=>native('connect');
$('settingsButton').onclick=()=>openSheet('settings');
$('closeSettings').onclick=()=>closeSheet('settings');
$('closeOriginal').onclick=()=>closeSheet('original');
$('closeDevlog').onclick=()=>closeSheet('devlog');
$('closeCadence').onclick=()=>closeSheet('cadence');
$('closeAppearance').onclick=()=>closeSheet('appearance');
function showRefreshHint(){
 const hint=$('refreshHint');clearTimeout(refreshHintTimer);clearTimeout(refreshHideTimer);hint.hidden=false;hint.classList.remove('exiting','visible');void hint.offsetWidth;hint.classList.add('visible');
 refreshHintTimer=setTimeout(()=>{hint.classList.remove('visible');hint.classList.add('exiting');refreshHideTimer=setTimeout(()=>{hint.hidden=true;hint.classList.remove('exiting');},220);},1800);
}
const refreshButton=$('refreshButton');
refreshButton.addEventListener('pointerdown',event=>{if(refreshButton.disabled||event.button!==0)return;clearTimeout(refreshPressTimer);skipRefreshClick=false;refreshPressTimer=setTimeout(()=>{skipRefreshClick=true;showRefreshHint();},550);});
['pointerup','pointercancel','pointerleave'].forEach(type=>refreshButton.addEventListener(type,()=>clearTimeout(refreshPressTimer)));
refreshButton.addEventListener('contextmenu',event=>event.preventDefault());
refreshButton.onclick=event=>{if(skipRefreshClick&&event.detail!==0){skipRefreshClick=false;return;}skipRefreshClick=false;native('refresh');};
refreshButton.addEventListener('focus',()=>{if(refreshButton.matches(':focus-visible'))showRefreshHint();});
$('gmailButton').onclick=()=>native('openGmail');
$('whatsappButton').onclick=()=>{$('settings').close();native('connectWhatsApp');};
$('openWhatsAppButton').onclick=()=>native('openWhatsApp');
$('reconnectButton').onclick=()=>{$('settings').close();native('connect');};
$('disconnectButton').onclick=()=>{$('settings').close();native('disconnect');};
$('draft').addEventListener('input',()=>{updateComposer();native('saveDraft',$('draft').value);});
$('sendButton').onclick=()=>{const text=$('draft').value.trim();if((!text&&(!draftFiles.length||activeChannel==='whatsapp'))||busy||fileBusy||!connected)return;busy=true;$('sendButton').classList.add('sending');updateComposer();native('send',text,activeChannel);};
$('channelPicker').querySelectorAll('button').forEach(button=>button.onclick=()=>selectChannel(button.dataset.channel));
$('attachButton').onclick=()=>{ $('composeExtras').hidden=false;native('attach');};
$('cancelReply').onclick=()=>native('reply','');
$('dictateButton').onclick=()=>native('dictate');
$('olderButton').onclick=()=>native('loadOlder');
$('searchInput').oninput=filterMessages;
$('closeSearch').onclick=()=>{$('searchInput').value='';$('searchPanel').hidden=true;filterMessages();};
$('notificationsButton').onclick=()=>{$('filterPeer').textContent=$('peerValue').textContent;native('notificationState');openSheet('notifications');};
$('closeNotifications').onclick=()=>closeSheet('notifications');
$('notificationToggle').onclick=()=>native('setNotifications',!notificationEnabled);
$('androidNotifications').onclick=()=>native('openNotificationSettings');
$('testNotification').onclick=()=>native('testNotification');
$('gmailFilterHelp').onclick=()=>native('openUrl','https://support.google.com/mail/answer/6579');

$('devlogButton').onclick=async()=>{renderDevlog();await closeSheet('settings');openSheet('devlog');};
function openCadence(mode){cadenceMode=mode;const foreground=mode==='foreground';$('cadenceEyebrow').textContent=foreground?'WHILE INSTINCT IS OPEN':'IN THE BACKGROUND';$('cadenceTitle').textContent=foreground?'Foreground refresh':'Background notification check';$('cadenceDescription').textContent=foreground?'How often Instinct checks Gmail while this app is open. Shorter intervals use more battery and data.':'How often Android asks Instinct to check Gmail in the background. Android requires at least 15 minutes and may delay checks to save battery.';$('cadenceUnit').textContent=foreground?'seconds':'minutes';$('cadenceValue').min=foreground?'5':'15';$('cadenceValue').max=foreground?'300':'1440';$('cadenceValue').value=foreground?Number($('foregroundCadence').textContent.match(/\d+/)?.[0]||15):Number($('backgroundCadence').textContent.match(/\d+/)?.[0]||15);$('cadencePresets').replaceChildren();for(const value of foreground?[5,15,30,60]:[15,30,60,120]){const button=document.createElement('button');button.textContent=`${value} ${foreground?'sec':'min'}`;button.onclick=()=>{$('cadenceValue').value=value;};$('cadencePresets').append(button);}openSheet('cadence');}
$('foregroundCadence').onclick=()=>openCadence('foreground');
$('backgroundCadence').onclick=()=>openCadence('background');
$('saveCadence').onclick=()=>{const value=Number($('cadenceValue').value);const min=cadenceMode==='foreground'?5:15,max=cadenceMode==='foreground'?300:1440;if(!Number.isInteger(value)||value<min||value>max){notice(`Choose a whole number from ${min} to ${max}.`);return;}native('setCadence',cadenceMode,value);closeSheet('cadence');};
$('appearanceButton').onclick=()=>openSheet('appearance');
document.querySelectorAll('.accent-choice').forEach(button=>button.onclick=()=>native('setAccent',button.dataset.accent));
$('settingsSearchButton').onclick=async()=>{await closeSheet('settings');$('searchPanel').hidden=false;$('searchInput').focus();};
$('updatesButton').onclick=()=>native('openUrl','https://github.com/aribradshaw/instinct-android/releases/latest');
$('guideButton').onclick=()=>native('openUrl','https://github.com/aribradshaw/instinct-android#connect-your-account');
