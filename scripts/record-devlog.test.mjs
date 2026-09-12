import {test} from 'node:test';
import assert from 'node:assert/strict';
import {mkdtempSync,readFileSync,writeFileSync,mkdirSync,rmSync} from 'node:fs';
import {tmpdir} from 'node:os';
import {join,dirname,resolve} from 'node:path';
import {execFileSync} from 'node:child_process';
test('records all missed commits, aligns versions, and ignores repeat and bot runs',()=>{
 const dir=mkdtempSync(join(tmpdir(),'instinct-release-'));
 const git=(...args)=>execFileSync('git',args,{cwd:dir,encoding:'utf8'}).trim();
 const script=resolve('scripts/record-devlog.mjs');
 const run=()=>execFileSync(process.execPath,[script],{cwd:dir,encoding:'utf8'});
 const read=p=>JSON.parse(readFileSync(join(dir,p),'utf8'));
 try {
  for(const p of ['package.json','package-lock.json','config/devlog-releases.json','app/build.gradle','app/src/main/assets/index.html','app/src/main/assets/app.js']){mkdirSync(dirname(join(dir,p)),{recursive:true});writeFileSync(join(dir,p),readFileSync(p));}
  git('init');git('config','user.name','Test');git('config','user.email','test@example.org');git('add','.');git('commit','-m','Baseline');
  writeFileSync(join(dir,'config/devlog-state.json'),JSON.stringify({lastCommit:git('rev-parse','HEAD')}));
  git('add','.');git('commit','-m','First source change');git('commit','--allow-empty','-m','Second source change');
  const old=read('package.json').version;const count=read('config/devlog-releases.json').length;
  run();const version=read('package.json').version;
  assert.notEqual(version,old);assert.equal(read('config/devlog-releases.json').length,count+2);
  assert.equal(read('package-lock.json').packages[''].version,version);
  assert.ok(readFileSync(join(dir,'app/build.gradle'),'utf8').includes(`versionName '${version}'`));
  assert.match(run(),/No unrecorded/);
  git('add','.');git('commit','-m','chore(release): generated');assert.match(run(),/No unrecorded/);
  git('commit','--allow-empty','-m','Third source change');run();assert.equal(read('config/devlog-releases.json').length,count+3);
 }finally{rmSync(dir,{recursive:true,force:true});}
});
