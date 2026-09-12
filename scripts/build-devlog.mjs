import { readFile, mkdir, copyFile, writeFile } from 'node:fs/promises';
import { build } from 'esbuild';
import { validateDevLogEntries, assertDevLogReleaseAlignment } from '@aribradshaw/devlog';
const releases = JSON.parse(await readFile('config/devlog-releases.json', 'utf8'));
const pkg = JSON.parse(await readFile('package.json', 'utf8'));
if (!validateDevLogEntries(releases, { rejectAuthorEmail: true, rejectTicketTitle: true })) throw new Error('Invalid public DevLog entries');
assertDevLogReleaseAlignment({currentVersion:pkg.version,latestDevLogVersion:releases[0].version,dependencyVersion:pkg.dependencies['@aribradshaw/devlog']});
const android = await readFile('app/build.gradle','utf8');
if (!android.includes(`versionName '${pkg.version}'`)) throw new Error('Android version must match the DevLog.');
const appHtml = await readFile('app/src/main/assets/index.html','utf8');
const visibleVersion = appHtml.match(/class="version"[^>]*>[^<]*?\b(\d+\.\d+\.\d+)\b/)?.[1];
if (visibleVersion !== pkg.version) throw new Error('Visible app version must match the DevLog.');
await mkdir('site/dist', {recursive:true});
for (const file of ['index.html', 'devlog.html', 'style.css']) await copyFile(`site/${file}`, `site/dist/${file}`);
for (const file of ['index.html', 'devlog.html']) {
  const html = await readFile(`site/dist/${file}`, 'utf8');
  await writeFile(`site/dist/${file}`, html.replace(/(<span id="version">)[^<]*/, `$1${pkg.version}`));
}
await build({ entryPoints:['site/main.js'],bundle:true,minify:true,format:'esm',outfile:'site/dist/main.js',define:{'BUILD_COMMIT':JSON.stringify(process.env.BUILD_SOURCE_SHA||process.env.GITHUB_SHA||'')} });
console.log(`Public DevLog built for ${pkg.version} using @aribradshaw/devlog.`);
