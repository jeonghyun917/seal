const {test}=require('node:test');
const assert=require('node:assert/strict');
const path=require('node:path');
const {chromium}=require('playwright');

test('long admin forms warn on unsaved edits but not after intentional submit',async()=>{
 const browser=await chromium.launch({channel:'msedge',headless:true});
 try{
  const page=await browser.newPage();
  await page.setContent('<body class="admin-body"><form class="data-form" method="post"><input name="title"><button type="submit">저장</button></form></body>');
  await page.addScriptTag({path:path.resolve('src/main/webapp/resources/js/admin-ui.js')});
  await page.evaluate(()=>document.dispatchEvent(new Event('DOMContentLoaded')));
  const shouldWarn=()=>page.evaluate(()=>{const event=new Event('beforeunload',{cancelable:true});window.dispatchEvent(event);return event.defaultPrevented;});
  assert.equal(await shouldWarn(),false);
  await page.locator('input').fill('수정한 내용');
  assert.equal(await shouldWarn(),true);
  await page.evaluate(()=>{document.addEventListener('submit',event=>event.preventDefault());document.querySelector('form').requestSubmit(document.querySelector('button'));});
  assert.equal(await shouldWarn(),false);
 }finally{await browser.close();}
});

test('statistics download prevents duplicates and recovers from expired login without disabling filters',async()=>{
 const browser=await chromium.launch({channel:'msedge',headless:true});
 try{
  const page=await browser.newPage({viewport:{width:390,height:800}});
  await page.setContent('<form method="get"><input name="from" value="2026-09-01"><button id="search">조회</button><button id="download" type="submit" data-download data-template-download formaction="https://test.invalid/export"><span data-download-label>엑셀 다운로드</span></button></form><p role="status" data-download-status></p>');
  await page.evaluate(()=>{window.calls=0;window.fetch=()=>{window.calls++;return new Promise(resolve=>window.finish=resolve);};});
  await page.addScriptTag({path:path.resolve('src/main/webapp/resources/js/template-download.js')});
  await page.evaluate(()=>{const form=document.querySelector('form'),button=document.querySelector('#download');form.requestSubmit(button);form.requestSubmit(button);});
  assert.equal(await page.evaluate(()=>window.calls),1);
  assert.equal(await page.locator('#download').getAttribute('aria-busy'),'true');
  assert.equal(await page.locator('#search').isEnabled(),true);
  await page.evaluate(()=>window.finish(new Response('<html>login</html>',{headers:{'content-type':'text/html'}})));
  await page.waitForFunction(()=>!document.querySelector('#download').hasAttribute('aria-busy'));
  assert.match(await page.locator('[data-download-status]').textContent(),/로그인 상태/);
  assert.equal(await page.locator('[data-download-label]').textContent(),'엑셀 다운로드');
  await page.evaluate(()=>{window.fetch=async url=>{window.downloadUrl=String(url);return new Response('fake workbook',{headers:{'content-type':'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'}});};document.querySelector('form').requestSubmit(document.querySelector('#download'));});
  await page.waitForFunction(()=>document.querySelector('[data-download-status]').textContent.includes('완료'));
  assert.match(await page.evaluate(()=>window.downloadUrl),/from=2026-09-01/);
 }finally{await browser.close();}
});

test('bulk recipient selection clearly reports truncation and enforces 1000',async()=>{
 const browser=await chromium.launch({channel:'msedge',headless:true});
 try{
  const page=await browser.newPage({viewport:{width:1440,height:900}});
  await page.setContent('<button id="openRecipientPicker">선택</button><div id="selectedRecipientCount"></div><div id="selectedRecipientPreview"></div><div id="selectedRecipientInputs"></div><dialog id="recipientPickerDialog" data-options-url="https://test.invalid/options"><input id="recipientSearch"><div id="recipientResults"></div><div id="dialogSelectedRecipients"></div><span id="dialogSelectedCount"></span><span id="recipientResultStatus" aria-live="polite"></span><span id="recipientPageLabel"></span>'+['closeRecipientPicker','cancelRecipientSelection','applyRecipientSelection','searchRecipients','previousRecipientPage','nextRecipientPage','selectCurrentPage','selectAllResults'].map(id=>`<button id="${id}" type="button">${id}</button>`).join('')+'</dialog>');
  await page.evaluate(()=>{window.fetch=async url=>new Response(JSON.stringify({items:Array.from({length:new URL(url).searchParams.has('all')?1000:1},(_,i)=>({recipientId:'id-'+i,maskedName:'김*동',maskedEmail:'a***@example.test'})),page:1,hasNext:true}),{headers:{'content-type':'application/json'}});});
  await page.addScriptTag({path:path.resolve('src/main/webapp/resources/js/recipient-picker.js')});
  await page.locator('#openRecipientPicker').click();
  await page.waitForFunction(()=>document.querySelector('#recipientResultStatus').textContent.includes('표시'));
  await page.locator('#selectAllResults').click();
  await page.waitForFunction(()=>document.querySelector('#recipientResultStatus').textContent.includes('전체 검색 결과가 아닙니다'));
  await page.locator('#applyRecipientSelection').click();
  assert.equal(await page.locator('input[name="recipientIds"]').count(),1000);
 }finally{await browser.close();}
});
