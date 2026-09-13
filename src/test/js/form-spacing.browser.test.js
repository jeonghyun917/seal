const { test, before, after } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { chromium } = require('playwright');
let browser;
before(async () => { browser = await chromium.launch({ channel: 'msedge', headless: true }); });
after(async () => { await browser?.close(); });

test('rights detail separates its fields and keeps decision buttons grouped', async () => {
    const jsp = fs.readFileSync('src/main/webapp/WEB-INF/jsp/admin/rights/detail.jsp', 'utf8');
    const form = jsp.match(/<form class="stack-form"[^>]*\/verify">[\s\S]*?<\/form>/)[0]
        .replace(/\$\{[^}]*\}/g, '');
    for (const width of [1440, 390]) {
        const page = await setup('<section class="work-panel">' + form + '</section>', width);
        const input = await page.locator('input[name="channel"]').boundingBox();
        const next = await page.locator('label').filter({ has: page.locator('textarea') }).boundingBox();
        assert.ok(next.y - input.y - input.height >= 17, 'rights field gap');
        assert.equal(await page.locator('.form-actions button').count(), 1);
        await page.close();
    }
});

test('request detail heading buttons do not inherit the operation panel box', async () => {
    const jsp = fs.readFileSync('src/main/webapp/WEB-INF/jsp/admin/requests/detail.jsp', 'utf8');
    const start = jsp.indexOf('<div class="page-heading-row">');
    const end = jsp.indexOf('<c:if test="${not empty notice}">');
    const heading = jsp.slice(start, end)
        .replace(/<\/?c:[^>]*>/g, '')
        .replace(/\$\{[^}]*\}/g, '상세');
    assert.match(heading, /class="page-heading-buttons"/);
    assert.doesNotMatch(heading, /class="request-actions"/);

    const page = await setup(heading, 1440);
    const styles = await page.locator('.page-heading-buttons').evaluate(element => {
        const computed = getComputedStyle(element);
        return { background: computed.backgroundColor, border: computed.borderTopWidth, padding: computed.paddingTop };
    });
    assert.equal(styles.background, 'rgba(0, 0, 0, 0)');
    assert.equal(styles.border, '0px');
    assert.equal(styles.padding, '0px');
    assert.ok((await page.getByRole('link', { name: '목록' }).boundingBox()).height >= 44);
    await page.close();
});

test('evidence list leads with the consent form name and keeps the request number secondary', () => {
    const jsp = fs.readFileSync('src/main/webapp/WEB-INF/jsp/admin/evidence/list.jsp', 'utf8');
    assert.match(jsp, /<th>동의서<\/th>/);
    const summary = jsp.match(/<div class="evidence-request-summary">[\s\S]*?<\/div>/)[0];
    assert.ok(summary.indexOf('templateTitle()') < summary.indexOf('requestCode()'));
    assert.match(summary, /요청번호/);
});
async function setup(markup, width) {
    const page = await browser.newPage({ viewport: { width, height: 900 } });
    await page.setContent(`<body class="admin-body"><main class="admin-main">${markup}</main></body>`);
    for (const css of ['app.css', 'admin-data.css', 'template.css', 'recipient-request.css']) {
        await page.addStyleTag({ path: path.resolve('src/main/webapp/resources/css/' + css) });
    }
    await page.addScriptTag({ path: path.resolve('src/main/webapp/resources/js/field-guide.js') });
    await page.addScriptTag({ path: path.resolve('src/main/webapp/resources/js/simple-editor.js') });
    await page.evaluate(() => document.dispatchEvent(new Event('DOMContentLoaded')));
    return page;
}

test('notice fields and basic-info groups have a clear vertical separation', async () => {
    // Use the real notice section, replacing only Spring binding tags with browser controls.
    const jsp = fs.readFileSync('src/main/webapp/WEB-INF/jsp/admin/templates/form.jsp', 'utf8');
    const section = jsp.match(/<section class="work-panel"><h2>안내문<\/h2>[\s\S]*?<\/section>/)[0]
        .replace(/<form:errors[^>]*\/>/g, '')
        .replace(/<form:input path="title"\/>/, '<input id="title" name="title">')
        .replace(/<form:textarea[^>]*\/>/, '<textarea id="introductionHtml" name="introductionHtml" data-simple-editor></textarea>');
    for (const width of [1440, 390]) {
        const page = await setup(`<form method="post" class="template-editor">${section}<section class="work-panel"><h2>기본 정보</h2><div class="template-grid" id="basic"><label>양식명<input></label></div><fieldset id="activity"><legend>업무 선택</legend>선택 내용</fieldset></section></form>`, width);
        const title = await page.locator('#title').boundingBox();
        const label = await page.locator('label[for="introductionHtml"]').boundingBox();
        assert.ok(label.y - title.y - title.height >= 17, `notice field gap at ${width}px`);
        const group = await page.locator('#basic').boundingBox();
        const next = await page.locator('#activity').boundingBox();
        assert.ok(next.y - group.y - group.height >= 17, `basic group gap at ${width}px`);
        assert.ok(await page.locator('[data-field-guide-key="introductionHtml"]').count());
        if (process.env.SEAL_SPACING_SCREENSHOTS) await page.screenshot({ path: `target/form-spacing-${width}.png`, fullPage: true });
        await page.close();
    }
});

test('wrapped labels get inner gaps while existing grids and search rows keep their spacing', async () => {
    for (const width of [1440, 390]) {
        const page = await setup(`<section class="work-panel"><form method="post" class="data-form"><label id="wrapped">접수일<input name="receivedDate" type="date"></label><div class="form-field" id="existing"><label for="known">업무 이름</label><input id="known" name="activityName"></div></form></section><section class="work-panel"><form class="list-filter"><label>검색<input></label><button>조회</button></form></section>`, width);
        const label = await page.locator('#wrapped .field-guide-label-line').boundingBox();
        const control = await page.locator('#wrapped input').boundingBox();
        assert.ok(control.y - label.y - label.height >= 7, `wrapped field gap at ${width}px`);
        assert.equal(await page.locator('.data-form').evaluate(el => getComputedStyle(el).rowGap), '20px');
        assert.equal(await page.locator('#existing').evaluate(el => getComputedStyle(el).rowGap), '8px');
        assert.equal(await page.locator('.list-filter').evaluate(el => getComputedStyle(el).gap), '12px');
        await page.close();
    }
});

test('retention period value and unit share one row and stack on small screens', async () => {
    const markup = `<section class="work-panel"><form class="data-form">
        <div class="form-field" id="start-field"><label for="startEvent">보유기간 시작 기준</label><select id="startEvent"><option>수신자 응답 제출일</option></select></div>
        <div class="retention-period-fields">
            <div class="form-field"><label for="periodValue">보유기간</label><input id="periodValue" type="number" value="1"></div>
            <div class="form-field"><label for="periodUnit">보유기간 단위</label><select id="periodUnit"><option>년</option></select></div>
        </div>
    </form></section>`;
    for (const width of [1440, 390]) {
        const page = await setup(markup, width);
        const value = await page.locator('#periodValue').boundingBox();
        const unit = await page.locator('#periodUnit').boundingBox();
        const start = await page.locator('#startEvent').boundingBox();
        const periodGroup = await page.locator('.retention-period-fields').boundingBox();
        assert.equal(value.height, unit.height, `retention input and select height at ${width}px`);
        if (width > 700) {
            assert.ok(Math.abs(value.y - unit.y) <= 1, 'fields share one row');
            assert.ok(Math.abs(value.width - unit.width) <= 1, 'fields split the row evenly');
            assert.ok(Math.abs(start.y - value.y) <= 1, 'period fields stay beside start criterion');
            assert.ok(Math.abs(start.width - periodGroup.width) <= 1, 'period group only uses the original right-hand area');
        } else {
            assert.ok(value.y > start.y + start.height, 'period group follows start criterion on mobile');
            assert.ok(unit.y > value.y + value.height, 'period fields stack on mobile');
        }
        assert.ok(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth));
        if (process.env.SEAL_RETENTION_SCREENSHOTS) {
            await page.screenshot({ path: `target/retention-period-${width}.png`, fullPage: true });
        }
        await page.close();
    }
});

test('single-line inputs and selects use the same height across admin form patterns', async () => {
    const markup = `<section class="work-panel">
        <form class="data-form" id="data-form"><label>텍스트<input type="text"></label><label>선택<select><option>선택</option></select></label></form>
        <form class="template-editor" id="template-form"><label>텍스트<input type="text"></label><label>선택<select><option>선택</option></select></label></form>
        <form class="list-filter" id="list-filter"><label>검색<input type="search"></label><label>상태<select><option>전체</option></select></label></form>
    </section>`;
    for (const width of [1440, 390]) {
        const page = await setup(markup, width);
        for (const formId of ['data-form', 'template-form', 'list-filter']) {
            const inputHeight = await page.locator(`#${formId} input`).evaluate(element => element.getBoundingClientRect().height);
            const selectHeight = await page.locator(`#${formId} select`).evaluate(element => element.getBoundingClientRect().height);
            assert.equal(inputHeight, selectHeight, `${formId} control height at ${width}px`);
            assert.equal(inputHeight, 44, `${formId} standard height at ${width}px`);
        }
        await page.close();
    }
});

test('paired expiry controls preserve their shared 50px height', async () => {
    const markup = `<section class="work-panel"><form class="data-form">
        <div class="expiry-picker-controls">
            <input type="date" aria-label="만료일">
            <select aria-label="만료 시"><option>18시</option></select>
            <select aria-label="만료 분"><option>00분</option></select>
        </div>
    </form></section>`;
    for (const width of [1440, 390]) {
        const page = await setup(markup, width);
        const heights = await page.locator('.expiry-picker-controls > *').evaluateAll(elements =>
            elements.map(element => element.getBoundingClientRect().height));
        assert.deepEqual(heights, [50, 50, 50], `expiry control heights at ${width}px`);
        await page.close();
    }
});
