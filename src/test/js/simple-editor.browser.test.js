const { test, before, after } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { chromium } = require('playwright');
let browser;
before(async () => { browser = await chromium.launch({ channel: 'msedge', headless: true }); });
after(async () => { await browser?.close(); });
async function setup() {
    const page = await browser.newPage();
    await page.setContent('<form><label for="intro">안내문</label><textarea id="intro" name="introductionHtml" data-simple-editor>&lt;p&gt;기존 안내&lt;/p&gt;</textarea></form>');
    const script = path.resolve('src/main/webapp/resources/js/simple-editor.js');
    if (fs.existsSync(script)) await page.addScriptTag({ path: script });
    return page;
}
test('plain editing preserves initial paragraphs and synchronizes HTML for submission', async () => {
    const page = await setup();
    assert.equal(await page.locator('[contenteditable=true]').count(), 1);
    assert.equal(await page.locator('[contenteditable=true]').innerText(), '기존 안내');
    await page.locator('[contenteditable=true]').fill('새 안내');
    assert.equal(await page.locator('textarea').evaluate(el => { const t = document.createElement('template'); t.innerHTML = el.value; return t.content.textContent; }), '새 안내');
    await page.close();
});
test('rich text paste strips executable nodes and attributes while keeping allowed text', async () => {
    const page = await setup();
    assert.equal(await page.locator('[contenteditable=true]').count(), 1);
    await page.locator('[contenteditable=true]').evaluate(editor => {
        editor.focus();
        const data = new DataTransfer();
        data.setData('text/html', '<p onclick="alert(1)">안전<strong>굵게</strong><script>alert(2)</script><img src=x onerror=alert(3)></p>');
        editor.dispatchEvent(new ClipboardEvent('paste', { clipboardData: data, bubbles: true, cancelable: true }));
    });
    const value = await page.locator('textarea').inputValue();
    assert.ok(value.includes('안전<strong>굵게</strong>'));
    assert.ok(!/script|onclick|onerror|<img/.test(value));
    await page.close();
});
