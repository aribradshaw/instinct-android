import {readFile} from 'node:fs/promises';
import {nextCalendarVersion} from '@aribradshaw/devlog';
const [latest]=JSON.parse(await readFile('config/devlog-releases.json','utf8'));
const releaseAt=process.argv[2] || new Date();
console.log(nextCalendarVersion(latest.version,{latestReleaseDate:latest.date,releaseAt,timeZone:'America/Phoenix'}));
