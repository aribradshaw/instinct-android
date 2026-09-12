import {getDevLogCollection,resolveDevLogSourceMeta} from '@aribradshaw/devlog';
import releases from '../config/devlog-releases.json';
const repositoryUrl='https://github.com/aribradshaw/instinct-android';
const list=document.getElementById('releases');
function render(){
 const collection=getDevLogCollection(releases,{query:document.getElementById('search').value,page:1,pageSize:10});
 const entries=collection.visibleEntries;
 list.replaceChildren();
 for(const release of entries){
  const article=document.createElement('article');article.className='release';
  const top=document.createElement('div');top.className='release-top';
  const pill=document.createElement('span');pill.className='version-pill';pill.textContent='v'+release.version;
  const date=document.createElement('time');date.dateTime=release.date;date.textContent=new Date(release.date+'T12:00:00').toLocaleDateString('en-US',{month:'long',day:'numeric',year:'numeric'});
  top.append(pill,date);article.append(top);
  const title=document.createElement('h3');title.textContent=release.title;article.append(title);
  const summary=document.createElement('p');summary.textContent=release.summary;article.append(summary);
  const changes=document.createElement('ul');for(const change of release.notes){const li=document.createElement('li');li.textContent=change;changes.append(li);}article.append(changes);
  const source=resolveDevLogSourceMeta(release,{repositoryUrl,currentVersion:releases[0].version,buildCommit:BUILD_COMMIT});
  const link=document.createElement('a');link.href=source.commit?.url||repositoryUrl+'/releases/tag/v'+release.version;link.textContent='Source';article.append(link);list.append(article);
 }
 document.getElementById('resultCount').textContent=entries.length?entries.length+' release'+(entries.length===1?'':'s'):'No matching releases. Try a different search.';
 document.getElementById('version').textContent=releases[0].version;
}
document.getElementById('search').addEventListener('input',render);render();
