import {test} from 'node:test';import assert from 'node:assert/strict';
import '../app/src/main/assets/vault-cards.js';
const {extract}=globalThis.VaultCards;
const link='https://app.instinct.com/session/vault/fill?r=example_request&src=email';
test('extracts a request while preserving surrounding prose and the exact target',()=>{
 assert.deepEqual(extract('Add your login here:\n'+link+'\n\nThanks.'),{text:'Add your login here:\n\nThanks.',urls:[link]});
 assert.deepEqual(extract(link),{text:'',urls:[link]});
});
test('accepts markdown, punctuation and repeated links without duplicate cards',()=>{
 const result=extract(`[Add credentials](${link})\n${link}.`);assert.deepEqual(result.urls,[link]);assert.equal(result.text,'');
});
test('does not promote other hosts, non-HTTPS URLs, credentials, or unrelated routes',()=>{
 for(const bad of [link.replace('https:','http:'),link.replace('app.instinct.com','app.instinct.com.example.org'),link.replace('app.instinct.com','app.instinct.com@example.org'),link.replace('app.instinct.com','user@app.instinct.com'),link.replace('/fill','/other'),link.replace('example_request',''),link.replace('app.instinct.com','app.instinct.com:8443')])assert.deepEqual(extract(bad),{text:bad,urls:[]});
});
test('preserves code blocks and text without vault links',()=>{
 const text='Example:\n```\n'+link+'\n```';assert.deepEqual(extract(text),{text,urls:[]});
 assert.deepEqual(extract('Visit https://example.org.'),{text:'Visit https://example.org.',urls:[]});
});
test('keeps distinct requests and encoded query values intact',()=>{
 const second=link.replace('example_request','another%2Brequest');assert.deepEqual(extract(link+'\n'+second).urls,[link,second]);
});
