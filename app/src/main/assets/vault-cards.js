(function(root){
 'use strict';
 function vaultUrl(value){
  try{const url=new URL(value);return url.protocol==='https:'&&url.hostname==='app.instinct.com'&&!url.port&&!url.username&&!url.password&&/^\/session\/vault\/fill\/?$/.test(url.pathname)&&url.searchParams.get('r')?.trim()?value:null;}catch{return null;}
 }
 function extract(text){
  const urls=[];
  // Preserve code examples. Only turn actual links outside fenced code into cards.
  const body=text.split(/(```[\s\S]*?```)/g).map(part=>part.startsWith('```')?part:part.replace(/\[([^\]\n]*)\]\((https?:\/\/[^\s<>]+)\)|(https?:\/\/[^\s<>]+)/g,(match,label,markdown,plain)=>{
   const candidate=markdown||plain;
   const url=candidate.replace(/[.,;!?)\]]+$/,'');
   if(!vaultUrl(url))return match;
   if(!urls.includes(url))urls.push(url);
   return markdown?'':candidate.slice(url.length);
  }).replace(/^[ \t]*[).,;]*[ \t]*$/gm,'').replace(/\n{3,}/g,'\n\n')).join('').trim();
  return {text:urls.length?body:text,urls};
 }
 function card(url,open){
  const element=document.createElement('a');element.className='vault-card';element.href=url;
  element.setAttribute('aria-label','Open Instinct vault request on app.instinct.com');
  const top=document.createElement('div');top.className='vault-card-top';
  const icon=document.createElement('span');icon.className='vault-card-icon';icon.setAttribute('aria-hidden','true');
  const svg=document.createElementNS('http://www.w3.org/2000/svg','svg');svg.setAttribute('viewBox','0 0 24 24');
  for(const d of ['M7 10V7a5 5 0 0 1 10 0v3','M5 10h14v11H5z','M12 14v3']){const path=document.createElementNS(svg.namespaceURI,'path');path.setAttribute('d',d);svg.append(path);}icon.append(svg);
  const label=document.createElement('span');label.className='vault-card-label';label.textContent='INSTINCT VAULT';top.append(icon,label);
  const title=document.createElement('strong');title.className='vault-card-title';title.textContent='Add credentials';
  const detail=document.createElement('span');detail.className='vault-card-detail';detail.textContent='Open this request in Instinct.';
  const action=document.createElement('span');action.className='vault-card-action';action.textContent='Open vault';
  const arrow=document.createElement('span');arrow.textContent='↗';arrow.setAttribute('aria-hidden','true');action.append(arrow);
  const host=document.createElement('span');host.className='vault-card-host';host.textContent='app.instinct.com';
  element.append(top,title,detail,action,host);
  element.addEventListener('click',event=>{event.preventDefault();open(url);});
  return element;
 }
 root.VaultCards={extract,card};
})(globalThis);
