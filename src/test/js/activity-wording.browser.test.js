const { test } = require('node:test');
const assert = require('node:assert/strict');
const path = require('node:path');
const { chromium } = require('playwright');

test('activity help stays in i tooltips without shifting fields and added rows use the same labels', async () => {
    const browser = await chromium.launch({ channel: 'msedge', headless: true });
    try {
        for (const width of [1440, 390]) {
            const page = await browser.newPage({ viewport: { width, height: 900 } });
            await page.setContent(`<body class="admin-body"><main class="admin-main"><section class="work-panel">
                <form method="post" class="data-form"><div class="form-field"><label for="activityName">업무 이름</label><input id="activityName" name="activityName"></div>
                <div class="form-field"><label for="processingPurpose">개인정보 이용 목적</label><textarea id="processingPurpose" name="processingPurpose"></textarea></div>
                <fieldset class="processing-data-items"><button type="button" data-data-item-add>항목 추가</button><div data-data-item-list data-max-items="50"></div><span data-data-item-count></span></fieldset>
                </form></section></main></body>`);
            for (const file of ['app.css', 'admin-data.css']) await page.addStyleTag({ path: path.resolve('src/main/webapp/resources/css/' + file) });
            for (const file of ['field-guide.js', 'processing-activity-items.js']) await page.addScriptTag({ path: path.resolve('src/main/webapp/resources/js/' + file) });
            await page.evaluate(() => document.dispatchEvent(new Event('DOMContentLoaded')));
            const guide = page.locator('[data-field-guide-key="activityName"]');
            const before = await page.locator('#processingPurpose').boundingBox();
            assert.equal(await guide.locator('button').getAttribute('aria-expanded'), 'false');
            assert.equal(await guide.locator('[role="tooltip"]').isVisible(), false);
            await guide.locator('button').click();
            assert.match(await guide.textContent(), /개인정보를 어떤 업무에 사용하는지/);
            assert.match(await guide.textContent(), /교육 참여자 모집 및 운영/);
            assert.equal(await guide.locator('[role="tooltip"]').isVisible(), true);
            const after = await page.locator('#processingPurpose').boundingBox();
            assert.deepEqual(after, before);
            await page.getByRole('button', { name: '항목 추가', exact: true }).click();
            const row = page.locator('[data-data-item-row]');
            await row.locator('[data-field-guide-key="items[].necessityReason"]').waitFor();
            assert.match(await row.textContent(), /이 항목이 필요한 이유/);
            assert.doesNotMatch(await row.textContent(), /필요성 설명/);
            await page.close();
        }
    } finally { await browser.close(); }
});
