const { test, before, after } = require('node:test');
const assert = require('node:assert/strict');
const path = require('node:path');
const { chromium } = require('playwright');

let browser;

before(async () => {
    browser = await chromium.launch({ channel: 'msedge', headless: true });
});

after(async () => {
    await browser?.close();
});

async function setup(markup) {
    const page = await browser.newPage({ viewport: { width: 1280, height: 800 } });
    await page.setContent(`<form method="post">${markup}</form>`);
    await page.addScriptTag({ path: path.resolve('src/main/webapp/resources/js/field-guide.js') });
    await page.evaluate(() => document.dispatchEvent(new Event('DOMContentLoaded')));
    return page;
}

test('guided text fields reuse their examples as placeholders without changing values or tooltips', async () => {
    const page = await setup(`
        <label>업무명<input name="activityName"></label>
        <label>처리 목적<textarea name="processingPurpose"></textarea></label>
        <label>수신자 이름<input name="name" value="김가은"></label>
    `);

    assert.equal(await page.locator('[name="activityName"]').getAttribute('placeholder'), '예: 교육 참여자 모집 및 운영');
    assert.equal(await page.locator('[name="processingPurpose"]').getAttribute('placeholder'), '예: 참여자를 확인하고 교육 일정과 변경 사항을 안내하기 위해');
    assert.equal(await page.locator('[name="name"]').inputValue(), '김가은');
    assert.equal(await page.locator('[name="name"]').getAttribute('placeholder'), '예: 김가은');
    assert.equal(await page.locator('[data-field-guide-key="activityName"] .field-guide-example').textContent(), '예시) 교육 참여자 모집 및 운영');

    await page.close();
});

test('placeholder enhancement leaves selects, non-text controls, and existing placeholders unchanged', async () => {
    const page = await setup(`
        <label>소유 조직<select name="ownerOrganizationUnitId"><option>교육사업팀</option></select></label>
        <label>보유기간<input type="number" name="periodValue"></label>
        <label>임시 비밀번호<input type="password" name="temporaryPassword"></label>
        <label>접수일<input type="date" name="receivedDate"></label>
        <label>업무명<input name="activityName" placeholder="업무명을 입력해 주세요"></label>
    `);

    assert.equal(await page.locator('[name="ownerOrganizationUnitId"]').getAttribute('placeholder'), null);
    assert.equal(await page.locator('[name="periodValue"]').getAttribute('placeholder'), null);
    assert.equal(await page.locator('[name="temporaryPassword"]').getAttribute('placeholder'), null);
    assert.equal(await page.locator('[name="receivedDate"]').getAttribute('placeholder'), null);
    assert.equal(await page.locator('[name="activityName"]').getAttribute('placeholder'), '업무명을 입력해 주세요');

    await page.close();
});

test('label-only placeholders are replaced by the field guide examples', async () => {
    const page = await setup(`
        <label>항목명<input name="items[0].itemName" placeholder="항목명"></label>
        <label>이 항목이 필요한 이유<input name="items[0].necessityReason" placeholder="이 항목이 필요한 이유"></label>
    `);

    assert.equal(await page.locator('[name="items[0].itemName"]').getAttribute('placeholder'), '예: 이름, 이메일, 소속기관');
    assert.equal(await page.locator('[name="items[0].necessityReason"]').getAttribute('placeholder'), '예: 교육 일정 및 변경사항 안내를 위해 필요');

    await page.close();
});

test('dynamically added guided text fields receive example placeholders', async () => {
    const page = await setup('<div id="items"></div>');

    await page.locator('#items').evaluate(container => {
        container.innerHTML = '<label>항목명<input name="items[0].itemName"></label>';
    });

    await page.waitForFunction(() => document.querySelector('[name="items[0].itemName"]')?.placeholder);
    assert.equal(await page.locator('[name="items[0].itemName"]').getAttribute('placeholder'), '예: 이름, 이메일, 소속기관');

    await page.close();
});
