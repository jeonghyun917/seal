# Field Example Placeholders Implementation Plan

**Goal:** Reuse each existing field-guide example as a grey placeholder in eligible text inputs and textareas without changing labels, tooltips, select boxes, values, or existing placeholders.

**Architecture:** Extend the existing project-wide `field-guide.js` enhancement pass so the guide metadata remains the single source of truth. Apply placeholders only to supported text-entry controls, including controls added dynamically through the current mutation observer.

**Tech Stack:** Vanilla JavaScript, JSP tag-based static asset loading, Node test runner, Playwright.

---

### Task 1: Specify placeholder behavior

- [x] Add browser tests for eligible text inputs and textareas.
- [x] Add exclusions for select, password, number, date, and existing placeholders.
- [x] Verify dynamically added guided fields receive the same placeholder.
- [x] Run the focused test and confirm it fails before implementation.

### Task 2: Implement the common enhancement

- [x] Add a minimal placeholder helper to `field-guide.js`.
- [x] Reuse the guide example with the `예: ` prefix.
- [x] Keep guide icons/tooltips and existing field values unchanged.
- [x] Bump the static asset version in `fieldGuides.tag`.

### Task 3: Verify the project-wide behavior

- [x] Run the focused browser test.
- [x] Run the full JavaScript test suite once.
- [x] Run the relevant Maven tests and package build.
- [x] Inspect representative desktop and mobile form behavior through the existing browser suite.
