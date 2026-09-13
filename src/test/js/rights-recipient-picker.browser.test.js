const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { chromium } = require('playwright');

test('rights request selects one searched recipient and loads only that recipients requests', async () => {
    const browser = await chromium.launch({ channel: 'msedge', headless: true });
    try {
        const page = await browser.newPage({ viewport: { width: 1440, height: 900 } });
        page.setDefaultTimeout(3000);
        const pageErrors = [];
        const requestedUrls = [];
        page.on('pageerror', error => pageErrors.push(error.message));
        page.on('request', request => requestedUrls.push(request.url()));
        await page.route(/https:\/\/rights\.test\/recipients(?:\?.*)?$/, route => route.fulfill({
            contentType: 'application/json', headers: { 'Access-Control-Allow-Origin': '*' },
            body: JSON.stringify({ items: [
                { recipientId: 'recipient-1', maskedName: '김*술', maskedEmail: 'ar****@example.com', affiliationName: '예술교육팀', jobTitle: '팀장', collectionBasis: '교육 신청' },
                { recipientId: 'recipient-2', maskedName: '이*화', maskedEmail: 'cu****@example.com', affiliationName: '문화교육팀', jobTitle: '담당자', collectionBasis: '사업 신청' }
            ], page: 1, hasNext: false })
        }));
        await page.route(/https:\/\/rights\.test\/requests(?:\?.*)?$/, route => {
            assert.equal(new URL(route.request().url()).searchParams.get('recipientId'), 'recipient-2');
            return route.fulfill({ contentType: 'application/json', headers: { 'Access-Control-Allow-Origin': '*' },
                body: JSON.stringify([{ requestId: 'request-2', requestCode: 'CR-2', status: 'ACTIVE' }]) });
        });
        await page.setContent(`<body class="admin-body"><main class="admin-main"><form class="work-panel data-form">
            <div class="form-field recipient-picker-field">
                <label>대상자</label>
                <div class="recipient-selector-summary"><div><strong id="selectedRecipientCount">대상자 미선택</strong><p id="selectedRecipientPreview">선택된 대상자가 없습니다.</p></div><button id="openRecipientPicker" type="button">대상자 선택</button></div>
                <div id="selectedRecipientInputs"></div><div id="selectedRecipientSeeds" hidden></div>
                <dialog id="recipientPickerDialog" class="recipient-picker-dialog" data-selection-mode="single" data-input-name="recipientId" data-options-url="https://rights.test/recipients">
                    <div class="recipient-dialog-shell"><header><h2>대상자 선택</h2><button id="closeRecipientPicker" type="button">닫기</button></header>
                    <div class="recipient-dialog-search"><input id="recipientSearch" type="search"><button id="searchRecipients" type="button">검색</button></div>
                    <div class="recipient-dialog-actions"><span id="recipientResultStatus"></span></div>
                    <div class="recipient-dialog-body"><section class="recipient-results-panel"><div id="recipientResults"></div><nav class="recipient-pagination"><button id="previousRecipientPage" type="button">이전</button><span id="recipientPageLabel"></span><button id="nextRecipientPage" type="button">다음</button></nav></section>
                    <aside class="recipient-selected-panel"><div><h3>선택한 대상자</h3><strong id="dialogSelectedCount"></strong></div><div id="dialogSelectedRecipients"></div></aside></div>
                    <footer><button id="cancelRecipientSelection" type="button">취소</button><button id="applyRecipientSelection" type="button">선택 완료</button></footer></div>
                </dialog>
            </div>
            <div class="form-field"><label for="consentRequestId">동의요청</label><select id="consentRequestId" name="consentRequestId" data-options-url="https://rights.test/requests"><option value="">대상자를 먼저 선택해 주세요</option></select><span id="consentRequestStatus"></span></div>
        </form></main></body>`);
        for (const css of ['app.css', 'admin-data.css', 'recipient-request.css']) {
            await page.addStyleTag({ path: path.resolve('src/main/webapp/resources/css/' + css) });
        }
        await page.addScriptTag({ path: path.resolve('src/main/webapp/resources/js/recipient-picker.js') });
        const rightsScript = path.resolve('src/main/webapp/resources/js/rights-request-form.js');
        if (fs.existsSync(rightsScript)) await page.addScriptTag({ path: rightsScript });
        await page.evaluate(() => document.dispatchEvent(new Event('DOMContentLoaded')));

        await page.getByRole('button', { name: '대상자 선택' }).click();
        await page.waitForTimeout(300);
        assert.deepEqual(pageErrors, []);
        const pickerState = await page.evaluate(() => ({
            initialized: document.querySelector('#recipientPickerDialog').dataset.pickerInitialized,
            open: document.querySelector('#recipientPickerDialog').open,
            status: document.querySelector('#recipientResultStatus').textContent,
            resultCount: document.querySelectorAll('.recipient-result-item').length
        }));
        assert.equal(pickerState.initialized, 'true', JSON.stringify(pickerState));
        assert.equal(pickerState.open, true, JSON.stringify(pickerState));
        assert.notEqual(pickerState.status, '', JSON.stringify(pickerState));
        await page.waitForTimeout(1000);
        const resultCount = await page.locator('.recipient-result-item').count();
        assert.equal(resultCount, 2, JSON.stringify({ pickerState, requestedUrls,
            status: await page.locator('#recipientResultStatus').textContent() }));
        assert.equal(await page.locator('.recipient-result-item input[type="radio"]').count(), 2);
        await page.locator('.recipient-result-item').nth(1).click();
        await page.getByRole('button', { name: '선택 완료' }).click();
        assert.equal(await page.locator('input[name="recipientId"]').inputValue(), 'recipient-2');
        assert.match(await page.locator('#selectedRecipientPreview').textContent(), /이\*화/);
        await page.waitForSelector('#consentRequestId option[value="request-2"]', { state: 'attached' });
        assert.equal(await page.locator('#consentRequestId option').count(), 2);
        assert.equal(await page.locator('input[name="recipientIds"]').count(), 0);
        assert.ok(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth));
    } finally { await browser.close(); }
});
