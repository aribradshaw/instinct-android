import {execFileSync} from 'node:child_process';
const files=execFileSync('git',['ls-files','--cached','-z'],{encoding:'utf8'}).split('\0').filter(Boolean);
if(!files.length)throw new Error('Stage release files before running the public-source check.');
let errors=[];
for(const file of files){
 if(/(^|\/)(private|evidence|node_modules|build|\.gradle)\/|local\.properties$|\.(enc|jks|keystore|apk)$/.test(file)){errors.push(`${file}: private or generated file staged`);continue;}
 const buffer=execFileSync('git',['show',':'+file]);if(buffer.includes(0))continue;const text=buffer.toString('utf8');
 const emails=text.match(/[A-Za-z0-9._+-]+@(?:gmail\.com|mail\.instinct\.com)/g)||[];
 if(emails.some(email=>!['tester@gmail.com','tester@mail.instinct.com','you@gmail.com','your-agent@mail.instinct.com'].includes(email)))errors.push(`${file}: non-example account address`);
 if(/gh[pousr]_[A-Za-z0-9]{30,}|-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----/.test(text))errors.push(`${file}: potential secret`);
}
if(errors.length)throw new Error(errors.join('\n'));
console.log(`Public-source check passed for ${files.length} staged files.`);
