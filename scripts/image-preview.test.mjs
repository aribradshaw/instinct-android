import {test} from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import vm from 'node:vm';
const source=readFileSync(new URL('../app/src/main/assets/app.js',import.meta.url),'utf8');
const helper=source.slice(source.indexOf('function appendImagePreview('),source.indexOf('function addLinks('));
function render(value){
 const children=[];
 const context={document:{createElement:()=>({addEventListener(){}})}};
 vm.createContext(context);vm.runInContext(helper,context);
 context.appendImagePreview({append:image=>children.push(image)},value,'photo.jpg');return children;
}
test('renders a local decoded image with accessible filename',()=>{
 const images=render('data:image/jpeg;base64,YWJj');assert.equal(images.length,1);assert.equal(images[0].alt,'photo.jpg');assert.equal(images[0].className,'image-preview');
});
test('does not render remote URLs, SVG, or malformed data',()=>{
 for(const source of [undefined,'','https://example.com/tracker.png','data:image/svg+xml;base64,YWJj','data:image/jpeg;base64,<script>'])assert.equal(render(source).length,0);
});
test('both message attachments and composer attachments use previews',()=>{
 assert.match(source,/appendImagePreview\(bubble,preview,attachment\)/);
 assert.match(source,/appendImagePreview\(chip,data.files/);
});
