'use strict';
(() => {
 const card=document.getElementById('notice'),text=document.getElementById('noticeText'),close=document.getElementById('closeNotice');
 const reduced=matchMedia('(prefers-reduced-motion: reduce)');
 let timer,deadline=0,remaining=6500,animation,sequence=0,drag=null,hovered=false,returnFocus=null;
 function pause(){clearTimeout(timer);if(deadline)remaining=Math.max(0,deadline-performance.now());deadline=0;}
 function resume(){
  clearTimeout(timer);
  if(card.hidden||drag||hovered||card.contains(document.activeElement)||document.hidden)return;
  deadline=performance.now()+remaining;timer=setTimeout(()=>dismiss(),remaining);
 }
 function cancelAnimation(){animation?.cancel();animation=null;}
 function position(){const composer=document.getElementById('composer');const sheet=document.querySelector('dialog[open]');card.style.bottom=(sheet?16:Math.max(16,composer.hidden?16:composer.getBoundingClientRect().height+12))+'px';}
 async function dismiss(direction=0){
  if(card.hidden)return;
  const current=++sequence;pause();drag=null;cancelAnimation();
  const from={transform:card.style.transform||'translateX(0)',opacity:card.style.opacity||1};
  if(!reduced.matches){
   animation=card.animate([from,{transform:direction?`translateX(${direction*(innerWidth+card.offsetWidth)}px)`:'translateY(8px)',opacity:0}],{duration:direction?210:160,easing:'cubic-bezier(.2,.8,.2,1)',fill:'forwards'});
   await animation.finished.catch(()=>{});
  }
  if(current!==sequence)return;
  if(card.contains(document.activeElement)&&returnFocus?.isConnected)returnFocus.focus({preventScroll:true});
  if(typeof card.hidePopover==='function'&&card.matches(':popover-open'))card.hidePopover();card.hidden=true;cancelAnimation();card.style.transform='';card.style.opacity='';
 }
 function show(message){
  if(!message)return;
  const visible=!card.hidden,duplicate=visible&&text.textContent===message;
  ++sequence;pause();cancelAnimation();drag=null;card.style.transform='';card.style.opacity='';
  if(!visible)returnFocus=document.activeElement;
  if(!duplicate)text.textContent=message;
  remaining=Math.min(14000,Math.max(6500,message.length*45));card.hidden=false;position();
  if(typeof card.showPopover==='function'&&!card.matches(':popover-open'))card.showPopover();
  if(!visible&&!reduced.matches)animation=card.animate([{transform:'translateY(8px)',opacity:0},{transform:'none',opacity:1}],{duration:200,easing:'ease-out'});
  resume();
 }
 function settle(){cancelAnimation();const from={transform:card.style.transform||'none',opacity:card.style.opacity||1};card.style.transform='';card.style.opacity='';if(!reduced.matches)animation=card.animate([from,{transform:'none',opacity:1}],{duration:180,easing:'cubic-bezier(.2,.8,.2,1)'});resume();}
 card.addEventListener('pointerdown',event=>{
  if(event.button!==0||drag||event.target.closest('button'))return;
  pause();cancelAnimation();drag={id:event.pointerId,x:event.clientX,y:event.clientY,dx:0,started:performance.now(),horizontal:false};
 });
 card.addEventListener('pointermove',event=>{
  if(!drag||drag.id!==event.pointerId)return;
  const dx=event.clientX-drag.x,dy=event.clientY-drag.y;
  if(!drag.horizontal){
   if(Math.abs(dy)>10&&Math.abs(dy)>Math.abs(dx)){drag=null;settle();return;}
   if(Math.abs(dx)<8)return;
   drag.horizontal=true;card.setPointerCapture(event.pointerId);
  }
  drag.dx=dx;
  if(!reduced.matches){card.style.transform=`translateX(${dx}px)`;card.style.opacity=String(Math.max(.25,1-Math.abs(dx)/card.offsetWidth));}
 });
 function end(event,cancel=false){
  if(!drag||drag.id!==event.pointerId)return;
  const gesture=drag;drag=null;if(card.hasPointerCapture(event.pointerId))card.releasePointerCapture(event.pointerId);
  const distance=Math.abs(gesture.dx),speed=distance/Math.max(1,performance.now()-gesture.started);
  if(!cancel&&gesture.horizontal&&(distance>Math.min(90,card.offsetWidth*.25)||(distance>25&&speed>.55))){window.Native?.haptic?.();dismiss(Math.sign(gesture.dx));}
  else settle();
 }
 card.addEventListener('pointerup',event=>end(event));card.addEventListener('pointercancel',event=>end(event,true));card.addEventListener('lostpointercapture',event=>end(event,true));
 card.addEventListener('pointerenter',event=>{if(event.pointerType==='mouse'){hovered=true;pause();}});
 card.addEventListener('pointerleave',event=>{if(event.pointerType==='mouse'){hovered=false;resume();}if(drag&&!drag.horizontal){drag=null;settle();}});
 card.addEventListener('focusin',pause);card.addEventListener('focusout',()=>queueMicrotask(resume));
 card.addEventListener('keydown',event=>{if(event.key==='Escape'){event.preventDefault();event.stopPropagation();dismiss();}});
 close.onclick=()=>dismiss();document.addEventListener('visibilitychange',()=>document.hidden?pause():resume());
 new ResizeObserver(position).observe(document.getElementById('composer'));
 new MutationObserver(position).observe(document.body,{subtree:true,attributes:true,attributeFilter:['open']});
 window.addEventListener('resize',position);
 window.notices={show,dismiss};
})();
