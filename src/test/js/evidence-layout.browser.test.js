const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { chromium } = require('playwright');

test('evidence key summary and disclosure align with the card at desktop and mobile widths', async () => {
    const jsp = fs.readFileSync('src/main/webapp/WEB-INF/jsp/admin/evidence/detail.jsp', 'utf8');
    const markup = jsp.match(/<section class="work-panel evidence-detail-panel">[\s\S]*?<\/section>/)[0]
        .replace(/<c:out value="([^"]*)"\s*\/>/g, (_, value) => {
            if (value.includes('templateTitle')) return '개인정보 수집·이용 동의 안내';
            if (value.includes('statusLabel')) return '생성 완료';
            if (value.includes('requestCode')) return 'CR-20260826-UCSUSUAW';
            if (value.includes('dateTime')) return '2026-08-31 13:13:02';
            if (value.includes('files().size')) return '6';
            if (value.includes('relativePath')) return 'package/'.repeat(24);
            if (/sha256|Sha256/.test(value)) return '09da9577ecdb3968'.repeat(4);
            return 'evidence.pdf';
        })
        .replace(/<\/?(?:c|sec):[^>]*>/g, '')
        .replace(/\$\{[^}]*\}/g, '');
    const browser = await chromium.launch({ channel: 'msedge', headless: true });
    try {
        for (const width of [1440, 390]) {
            const page = await browser.newPage({ viewport: { width, height: 900 } });
            await page.setContent(`<body class="admin-body"><main class="admin-main">${markup}</main></body>`);
            for (const css of ['app.css', 'admin-data.css']) {
                await page.addStyleTag({ path: path.resolve('src/main/webapp/resources/css/' + css) });
            }
            const heading = await page.locator('.evidence-panel-heading h2').boundingBox();
            const userHeading = await page.getByRole('heading', { name: '증적 핵심 정보', exact: true }).boundingBox();
            const disclosure = page.locator('details > summary');
            const summaryBox = await disclosure.boundingBox();
            assert.ok(Math.abs(heading.x - userHeading.x) <= 1, `summary alignment at ${width}`);
            assert.ok(Math.abs(heading.x - summaryBox.x) <= 1, `disclosure alignment at ${width}`);
            const panelHeading = await page.locator('.evidence-panel-heading').boundingBox();
            assert.ok(userHeading.y - panelHeading.y - panelHeading.height >= 17, 'summary top spacing');
            assert.equal(await page.locator('.evidence-summary-grid > div').count(), 6);
            const columns = await page.locator('.evidence-summary-grid').evaluate(el => getComputedStyle(el).gridTemplateColumns.split(' ').length);
            assert.equal(columns, width === 1440 ? 3 : 1, `summary columns at ${width}`);
            assert.equal(await page.locator('details').evaluate(el => el.open), false);
            if (process.env.SEAL_EVIDENCE_SCREENSHOTS) await page.screenshot({ path: `target/evidence-layout-${width}-closed.png`, fullPage: true });
            await disclosure.focus();
            await page.keyboard.press('Enter');
            assert.equal(await page.locator('details').evaluate(el => el.open), true);
            const metadata = await page.locator('.evidence-metadata dt').first().boundingBox();
            assert.ok(Math.abs(heading.x - metadata.x) <= 1, 'expanded metadata alignment');
            assert.ok(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth), 'no page overflow');
            if (process.env.SEAL_EVIDENCE_SCREENSHOTS) await page.screenshot({ path: `target/evidence-layout-${width}-open.png`, fullPage: true });
            await page.close();
        }
    } finally {
        await browser.close();
    }
});
