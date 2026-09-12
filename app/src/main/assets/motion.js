'use strict';
(() => {
 const get=id=>document.getElementById(id), reduced=matchMedia('(prefers-reduced-motion: reduce)');
 const area=get('timeline'), content=get('scrollContent'), jump=get('jumpLatest');
 let position=0,velocity=0,frame=0,lastTime=0,drag=null,newReplies=0;
 const haptic=()=>{if(typeof window.Native?.haptic==='function')window.Native.haptic();};
 function paint(){content.style.transform=position?`translateY(${position*.55}px) scaleY(${1+Math.abs(position)/Math.max(area.clientHeight,1)*.16})`:'';content.style.transformOrigin=position>0?'center top':'center bottom';}
 function spring(time){
  const dt=Math.min((time-lastTime)/1000||.016,.032);lastTime=time;
  velocity+=(-220*position-24*velocity)*dt;position+=velocity*dt;
  if(Math.abs(position)<.15&&Math.abs(velocity)<.8){position=0;frame=0;paint();return;}
  paint();frame=requestAnimationFrame(spring);
 }
 function release(){drag=null;if(frame)cancelAnimationFrame(frame);if(reduced.matches){position=0;paint();return;}lastTime=performance.now();frame=requestAnimationFrame(spring);}
 area.addEventListener('touchstart',e=>{
  if(reduced.matches||e.touches.length!==1||e.target.closest('button,a,textarea')||getSelection()?.toString())return;
  cancelAnimationFrame(frame);frame=0;velocity=0;position=0;paint();
  drag={x:e.touches[0].clientX,y:e.touches[0].clientY,edge:0,buzzed:false};
 },{passive:true});
 area.addEventListener('touchmove',e=>{
  if(!drag||e.touches.length!==1||getSelection()?.toString()){release();return;}
  const dy=e.touches[0].clientY-drag.y,dx=e.touches[0].clientX-drag.x;
  if(Math.abs(dx)>Math.abs(dy)&&!drag.edge){drag=null;return;}
  const top=area.scrollTop<=1,bottom=area.scrollHeight-area.clientHeight-area.scrollTop<=1;
  if(!drag.edge){if(top&&dy>0)drag.edge=1;else if(bottom&&dy<0)drag.edge=-1;else{drag.y=e.touches[0].clientY;return;}}
  if(!e.cancelable)return;
  e.preventDefault();const distance=Math.max(0,dy*drag.edge);
  position=drag.edge*72*(1-Math.exp(-distance/190));paint();
  if(distance>90&&!drag.buzzed){drag.buzzed=true;haptic();}
 },{passive:false});
 area.addEventListener('touchend',release,{passive:true});area.addEventListener('touchcancel',release,{passive:true});
 function jumpState(){const far=area.scrollHeight-area.clientHeight-area.scrollTop>180;jump.hidden=!far||!document.body.classList.contains('connected');if(!far)newReplies=0;jump.firstChild.textContent=newReplies?`${newReplies} new ${newReplies===1?'reply':'replies'} `:'Latest messages ';}
 area.addEventListener('scroll',jumpState,{passive:true});
 jump.onclick=()=>{haptic();area.scrollTo({top:area.scrollHeight,behavior:reduced.matches?'instant':'smooth'});};
 new ResizeObserver(()=>{jumpState();jump.style.bottom='16px';}).observe(area);
 function openSheet(id){const sheet=get(id);if(sheet.open)return;sheet.showModal();if(!reduced.matches)sheet.animate([{transform:'translateY(48px)',opacity:0},{transform:'translateY(0)',opacity:1}],{duration:300,easing:'cubic-bezier(.2,.8,.2,1)'});}
 async function closeSheet(id){const sheet=get(id);if(!sheet.open||sheet.dataset.closing)return;sheet.dataset.closing='true';if(!reduced.matches)await sheet.animate([{transform:'translateY(0)',opacity:1},{transform:'translateY(36px)',opacity:0}],{duration:180,easing:'ease-in'}).finished.catch(()=>{});sheet.close();delete sheet.dataset.closing;}
 for(const sheet of document.querySelectorAll('dialog')){
  sheet.addEventListener('cancel',e=>{e.preventDefault();closeSheet(sheet.id);});
  sheet.addEventListener('click',e=>{if(e.target===sheet&&e.clientY<sheet.getBoundingClientRect().top)closeSheet(sheet.id);});
  const grip=sheet.querySelector('.sheet-grip');if(!grip)continue;let start;
  grip.addEventListener('pointerdown',e=>{start=e.clientY;grip.setPointerCapture(e.pointerId);});
  grip.addEventListener('pointermove',e=>{if(start!==undefined&&!reduced.matches)sheet.style.transform=`translateY(${Math.max(0,e.clientY-start)*.65}px)`;});
  const end=e=>{if(start===undefined)return;const distance=e.clientY-start;start=undefined;sheet.style.transform='';if(distance>70)closeSheet(sheet.id);};
  grip.addEventListener('pointerup',end);grip.addEventListener('pointercancel',()=>{start=undefined;sheet.style.transform='';});
 }
 document.addEventListener('click',e=>{const button=e.target.closest('button');if(button&&!button.disabled&&button!==jump)haptic();});
 window.motion={openSheet,closeSheet,jumpState,arrived:count=>{newReplies+=count;jumpState();},toLatest:smooth=>area.scrollTo({top:area.scrollHeight,behavior:smooth&&!reduced.matches?'smooth':'instant'})};
})();
