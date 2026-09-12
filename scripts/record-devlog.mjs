import {readFileSync, writeFileSync} from 'node:fs';
import {execFileSync} from 'node:child_process';
import {nextCalendarVersion, validateDevLogEntries} from '@aribradshaw/devlog';
const git=(...args)=>execFileSync('git',args,{encoding:'utf8'}).trim();
const read=p=>JSON.parse(readFileSync(p,'utf8'));
const save=(p,v)=>writeFileSync(p,JSON.stringify(v,null,2)+'\n');
const state=read('config/devlog-state.json');
const head=git('rev-parse','HEAD');
git('merge-base','--is-ancestor',state.lastCommit,head);
const commits=git('rev-list','--reverse',`${state.lastCommit}..${head}`).split('\n').filter(Boolean);
const releases=read('config/devlog-releases.json');
const pkg=read('package.json');
const old=pkg.version;
let count=0;
for(const sha of commits){
 const subject=git('show','-s','--format=%s',sha);
 if(subject.startsWith('chore(release):'))continue;
 if(releases.some(r=>r.commit===sha))continue;
 const at=git('show','-s','--format=%cI',sha);
 const date=new Intl.DateTimeFormat('en-CA',{timeZone:'America/Phoenix',year:'numeric',month:'2-digit',day:'2-digit'}).format(new Date(at));
 const version=nextCalendarVersion(releases[0].version,{latestReleaseDate:releases[0].date,releaseAt:at,timeZone:'America/Phoenix'});
 releases.unshift({version,date,title:subject,summary:'Source update. See the linked commit for details.',notes:[subject],commit:sha,author:{name:'Ari Bradshaw',githubLogin:'aribradshaw'}});
 count++;
}
if(!count){console.log('No unrecorded source changes.');process.exit(0);}
if(!validateDevLogEntries(releases,{rejectAuthorEmail:true,rejectTicketTitle:true}))throw Error('Invalid release metadata');
pkg.version=releases[0].version;
const lock=read('package-lock.json');lock.version=pkg.version;lock.packages[''].version=pkg.version;
let gradle=readFileSync('app/build.gradle','utf8');
gradle=gradle.replace(/versionCode (\d+)/,(_,n)=>`versionCode ${Number(n)+count}`).replace(/versionName '[^']+'/ ,`versionName '${pkg.version}'`);
let html=readFileSync('app/src/main/assets/index.html','utf8');
html=html.replace(/(class="version"[^>]*>[^<]*?)\b\d+\.\d+\.\d+\b/,`$1${pkg.version}`);
save('package.json',pkg);save('package-lock.json',lock);save('config/devlog-releases.json',releases);
writeFileSync('app/build.gradle',gradle);writeFileSync('app/src/main/assets/index.html',html);
const appJsPath='app/src/main/assets/app.js';
let appJs=readFileSync(appJsPath,'utf8');
if(appJs.includes('const devlogEntries=')) {
 appJs=appJs.replace(/^const devlogEntries=.*;$/m,()=> 'const devlogEntries='+JSON.stringify(releases)+';');
 writeFileSync(appJsPath,appJs);
}
save('config/devlog-state.json',{lastCommit:head});
console.log(`${old} -> ${pkg.version}; recorded ${count} source commits.`);
