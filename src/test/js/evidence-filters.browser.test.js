const {test}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const path=require('node:path');
const {chromium}=require('playwright');

test('evidence search controls fit desktop and mobile with visible labels',async()=>{
 const jsp=fs.readFileSync('src/main/webapp/WEB-INF/jsp/admin/evidence/list.jsp','utf8');
 let form=jsp.match(/<form method="get" class="list-filter">[\s\S]*?<\/form>/)[0]
  .replace(/<c:forEach[\s\S]*?<\/c:forEach>/g,'')
  .replace(/<c:out[^>]*\/>/g,'')
  .replace(/\$\{[^}]*\}/g,'');
 const browser=await chromium.launch({channel:'msedge',headless:true});
 try {
  for(const width of [1440,390]) {
   const page=await browser.newPage({viewport:{width,height:900}});
   await page.setContent('<main style="padding:16px">'+form+'</main>');
   await page.addStyleTag({path:path.resolve('src/main/webapp/resources/css/app.css')});
   await page.addStyleTag({path:path.resolve('src/main/webapp/resources/css/admin-data.css')});
   for(const id of ['q','from','to','status']) {
    assert.equal(await page.locator('#'+id).isVisible(),true);
    assert.ok((await page.locator('label[for="'+id+'"]').textContent()).trim());
   }
   await page.locator('#q').fill('교육');
   await page.locator('#from').fill('2026-09-01');
   await page.locator('#to').fill('2026-09-29');
   assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),true);
   const heights=await page.locator('input,select').evaluateAll(nodes=>nodes.map(n=>n.getBoundingClientRect().height));
   assert.equal(new Set(heights).size,1);
   await page.close();
  }
 } finally {await browser.close();}
});

test('pagination preserves evidence date bounds and keyword',()=>{
 const jsp=fs.readFileSync('src/main/webapp/WEB-INF/jsp/fragments/admin/pagination.jsp','utf8');
 for(const name of ['q','from','to']) assert.equal((jsp.match(new RegExp('name="'+name+'"','g'))||[]).length,2);
});
