const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { chromium } = require('playwright');
test('request search pages preserve a deliberately selected request and escape labels', async () => {
    const browser = await chromium.launch({ channel: 'msedge', headless: true });
    try {
        const page = await browser.newPage();
        await page.route('https://picker.test/options*', route => {
            const second = new URL(route.request().url()).searchParams.get('page') === '2';
            return route.fulfill({ contentType: 'application/json', headers: { 'Access-Control-Allow-Origin': '*' }, body: JSON.stringify({ items: [{ requestId: second ? 'second' : 'first', requestCode: second ? 'CR-2' : 'CR-1', recipientLabel: '<img src=x>', templateName: '동의서' }], page: second ? 2 : 1, hasNext: !second }) });
        });
        await page.setContent('<div data-request-picker data-options-url="https://picker.test/options"><input type="search" aria-label="요청 검색"><button type="button" data-request-search>검색</button><select name="consentRequestId" required><option value="">선택</option></select><p data-request-status></p><button type="button" data-request-prev>이전</button><button type="button" data-request-next>다음</button></div>');
        const script = path.resolve('src/main/webapp/resources/js/request-options.js');
        if (fs.existsSync(script)) await page.addScriptTag({ path: script });
        await page.getByRole('button', { name: '검색', exact: true }).click();
        await page.waitForFunction(() => document.querySelector('option[value=first]'));
        assert.equal(await page.locator('option[value=first]').count(), 1);
        await page.locator('select').selectOption('first');
        await page.getByRole('button', { name: '다음', exact: true }).click();
        await page.waitForFunction(() => document.querySelector('option[value=second]'));
        assert.equal(await page.locator('select').inputValue(), 'first');
        assert.equal(await page.locator('img').count(), 0);
    } finally { await browser.close(); }
});
