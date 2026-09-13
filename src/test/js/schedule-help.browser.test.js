const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { chromium } = require('playwright');

test('delivery and redelivery show scheduling guidance below the input', async () => {
    const jsp = fs.readFileSync('src/main/webapp/WEB-INF/jsp/admin/requests/detail.jsp', 'utf8');
    const forms = jsp.match(/<form class="request-operation-form[^>]*>[\s\S]*?<\/form>/g);
    assert.equal(forms.length, 2);
    const browser = await chromium.launch({ channel: 'msedge', headless: true });
    try {
        for (const [index, form] of forms.entries()) {
            for (const width of [1440, 390]) {
                const page = await browser.newPage({ viewport: { width, height: 600 } });
                await page.setContent(`<body class="admin-body"><main class="admin-main"><section class="request-actions request-delivery-actions">${form.replace(/\$\{[^}]*\}/g, '')}</section></main></body>`);
                for (const css of ['app.css', 'admin-data.css', 'recipient-request.css']) {
                    await page.addStyleTag({ path: path.resolve('src/main/webapp/resources/css/' + css) });
                }
                await page.addScriptTag({ path: path.resolve('src/main/webapp/resources/js/field-guide.js') });
                await page.evaluate(() => document.dispatchEvent(new Event('DOMContentLoaded')));
                const input = page.locator('[name="scheduledAt"]');
                const helper = page.locator('#schedule-help');
                assert.equal(await input.getAttribute('aria-describedby'), 'schedule-help');
                assert.match(await helper.textContent(), /비워두면 즉시 발송 대상으로 등록됩니다/);
                assert.match(await helper.textContent(), /자동 발송이 중지된 경우/);
                const fieldBox = await input.boundingBox();
                const helpBox = await helper.boundingBox();
                assert.ok(helpBox.y - fieldBox.y - fieldBox.height >= 8, 'guidance has space below input');
                assert.ok(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), 'no horizontal overflow');
                if (width > 900) {
                    const submit = await page.locator('form > button').boundingBox();
                    assert.ok(Math.abs(submit.y + submit.height - fieldBox.y - fieldBox.height) <= 1, 'submit stays aligned to input');
                }
                if (process.env.SEAL_SCHEDULE_SCREENSHOTS && index === 0) await page.screenshot({ path: `target/schedule-help-${width}.png`, fullPage: true });
                await page.close();
            }
        }
    } finally { await browser.close(); }
});

test('cancellation reason has enough room for its example and stays responsive', async () => {
    const jsp = fs.readFileSync('src/main/webapp/WEB-INF/jsp/admin/requests/detail.jsp', 'utf8');
    const form = jsp.match(/<form class="cancel-request"[^>]*>[\s\S]*?<\/form>/)[0]
        .replace(/\$\{[^}]*\}/g, '');
    const browser = await chromium.launch({ channel: 'msedge', headless: true });
    try {
        for (const width of [1440, 390]) {
            const page = await browser.newPage({ viewport: { width, height: 600 } });
            await page.setContent(`<body class="admin-body"><main class="admin-main"><section class="request-actions request-delivery-actions">${form}</section></main></body>`);
            for (const css of ['app.css', 'admin-data.css', 'recipient-request.css']) {
                await page.addStyleTag({ path: path.resolve('src/main/webapp/resources/css/' + css) });
            }
            await page.addScriptTag({ path: path.resolve('src/main/webapp/resources/js/field-guide.js') });
            await page.evaluate(() => document.dispatchEvent(new Event('DOMContentLoaded')));
            const field = await page.locator('.cancel-request .request-field').boundingBox();
            const formBox = await page.locator('.cancel-request').boundingBox();
            if (width > 900) assert.ok(field.width >= 400, 'desktop cancellation field is wide enough for its example');
            else assert.ok(Math.abs(field.width - formBox.width) <= 1, 'mobile cancellation field uses the available row width');
            assert.ok(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), 'no horizontal overflow');
            await page.close();
        }
    } finally { await browser.close(); }
});
