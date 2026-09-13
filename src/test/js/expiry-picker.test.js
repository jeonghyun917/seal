const test = require("node:test");
const assert = require("node:assert/strict");
const { combineExpiry, expiryAfterDays } = require("../../main/webapp/resources/js/expiry-picker.js");

test("날짜와 시·분을 서버 datetime-local 형식으로 결합한다", () => {
    assert.equal(combineExpiry("2026-09-10", "18", "00"), "2026-09-10T18:00");
});

test("빠른 선택은 지정 일수 뒤 날짜의 18시를 반환한다", () => {
    const now = new Date(2026, 7, 27, 11, 20);
    assert.deepEqual(expiryAfterDays(now, 14), {
        date: "2026-09-10",
        hour: "18",
        minute: "00"
    });
});