const { test, before, after } = require('node:test');
const assert = require('node:assert/strict');
const path = require('node:path');
const { chromium } = require('playwright');
test('download submission keeps filters and buttons usable', async () => {
    const page = await setup('<form method="get"><button type="submit" data-download>엑셀 다운로드</button><button type="submit" id="search">조회</button></form>');
    await page.evaluate(() => {
        document.addEventListener('submit', e => e.preventDefault());
        document.querySelector('form').requestSubmit(document.querySelector('[data-download]'));
    });
    assert.equal(await page.locator('form').getAttribute('aria-busy'), null);
    assert.equal(await page.locator('#search').getAttribute('aria-disabled'), null);
    assert.equal(await page.locator('[data-download]').textContent(), '엑셀 다운로드');
    await page.close();
});
let browser;
before(async () => { browser = await chromium.launch({ channel: 'msedge', headless: true }); });
after(async () => { await browser?.close(); });

async function setup(content, width = 390) {
    const page = await browser.newPage({ viewport: { width, height: 800 } });
    await page.setContent(`<body class="admin-body">${content}</body>`);
    await page.addScriptTag({ path: path.resolve('src/main/webapp/resources/js/admin-ui.js') });
    await page.evaluate(() => document.dispatchEvent(new Event('DOMContentLoaded')));
    return page;
}

test('closed mobile navigation is untabbable and desktop restores it', async () => {
    const page = await setup('<button class="admin-menu-button">메뉴</button><nav id="adminPrimaryNav"><a href="#main">목록</a></nav><main class="admin-main"></main>');
    assert.equal(await page.locator('nav').evaluate(n => n.inert), true);
    await page.locator('.admin-menu-button').click();
    assert.equal(await page.locator('nav').evaluate(n => n.inert), false);
    await page.keyboard.press('Escape');
    assert.equal(await page.locator('nav').evaluate(n => n.inert), true);
    await page.setViewportSize({ width: 1280, height: 800 });
    await page.waitForFunction(() => !document.querySelector('nav').inert);
    await page.close();
});

test('destructive confirmation cancels without submit then preserves the chosen submitter', async () => {
    const page = await setup('<form data-confirm-title="파기 확인" data-confirm-message="선택한 수신자의 정보를 파기합니다."><button name="action" value="execute">파기 실행</button></form>');
    await page.evaluate(() => {
        window.submissions = [];
        document.addEventListener('submit', event => {
            if (!event.defaultPrevented) window.submissions.push(event.submitter.value);
            event.preventDefault();
        });
    });
    await page.locator('form > button').click();
    assert.equal(await page.evaluate(() => submissions.length), 0);
    await page.getByRole('button', { name: '취소', exact: true }).click();
    assert.equal(await page.evaluate(() => submissions.length), 0);
    await page.locator('form > button').click();
    await page.getByRole('dialog').getByRole('button', { name: '확인 후 실행', exact: true }).click();
    assert.deepEqual(await page.evaluate(() => submissions), ['execute']);
    await page.close();
});

test('submission guard prevents duplicate requestSubmit and pageshow restores controls', async () => {
    const page = await setup('<form><button type="submit">저장</button></form>');
    const count = await page.evaluate(() => {
        let count = 0;
        document.addEventListener('submit', e => { if (!e.defaultPrevented) count++; e.preventDefault(); });
        const form = document.querySelector('form');
        form.requestSubmit(form.querySelector('button'));
        form.requestSubmit(form.querySelector('button'));
        window.dispatchEvent(new PageTransitionEvent('pageshow', { persisted: true }));
        return count;
    });
    assert.equal(count, 1);
    assert.equal(await page.locator('button').textContent(), '저장');
    assert.equal(await page.locator('form').getAttribute('aria-busy'), null);
    await page.close();
});

test('mobile main keeps the available width when navigation is fixed', async () => {
    const page = await setup('<div class="admin-layout"><nav class="admin-nav"></nav><main class="admin-main">본문</main></div>');
    await page.addStyleTag({ path: path.resolve('src/main/webapp/resources/css/app.css') });
    const width = await page.locator('main').evaluate(el => el.getBoundingClientRect().width);
    assert.ok(width >= 375, `main width was ${width}`);
    await page.close();
});
