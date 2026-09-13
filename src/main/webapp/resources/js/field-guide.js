(() => {
    "use strict";

    const guides = {
        "organizationCode": { text: "조직을 시스템에서 구분할 영문·숫자 코드를 입력합니다. 생성 후에는 변경할 수 없습니다.", example: "EDU-OPS" },
        "organizationName": { text: "기관 조직도와 동일한 공식 조직명을 입력합니다.", example: "한국문화예술교육진흥원" },
        "parentId": { text: "현재 조직의 바로 위 상위 조직을 선택합니다. 최상위 조직이면 선택하지 않습니다.", example: "문화예술교육본부" },
        "active": { text: "비활성으로 바꾸면 신규 업무 배정이나 선택 대상에서 제외됩니다.", example: "운영 중인 조직은 활성" },
        "status": { text: "현재 항목을 신규 업무에서 사용할지 선택합니다. 중지해도 기존 이력과 확정 자료는 유지됩니다.", example: "신규 동의 요청에 사용하려면 사용" },
        "policyCode": { text: "보유정책을 구분할 영문·숫자 코드를 입력합니다. 업무와 연도를 알아볼 수 있게 작성합니다.", example: "EDU-RET-01" },
        "policyName": { text: "어떤 개인정보의 보유 기준인지 알아볼 수 있는 관리 명칭을 입력합니다.", example: "교육 프로그램 신청자 3년 보유" },
        "ownerOrganizationUnitId": { text: "이 업무 또는 기준정보를 책임지고 관리할 내부 조직을 선택합니다.", example: "교육사업팀" },
        "startEvent": { text: "보유기간을 계산하기 시작할 날짜를 선택합니다. 수신자 응답 제출일을 선택하면 응답자는 실제 제출일, 미응답자는 동의 요청 만료일을 자동으로 사용합니다.", example: "9월 3일 응답자는 9월 3일부터, 미응답자는 9월 8일 만료일부터 계산" },
        "periodValue": { text: "보유할 기간의 숫자만 입력합니다. 단위는 옆 항목에서 선택합니다.", example: "3" },
        "periodUnit": { text: "보유기간에 적용할 일·개월·년 단위를 선택합니다.", example: "년" },
        "gracePeriodDays": { text: "보유기간 종료 후 실제 파기 작업까지 허용할 유예 일수를 입력합니다. 즉시 파기하면 0입니다.", example: "30" },
        "destructionMethod": { text: "파기 작업에 기록할 방식을 선택합니다. 개인정보 삭제는 직접 식별값 제거, 익명 처리는 비식별 통계·감사 이력만 유지, 파일 완전 삭제는 PDF·JSON 등 증적 파일 제거가 필요한 경우입니다. 어떤 방식을 선택해도 시스템에 저장된 개인정보·메일 본문·세션 식별값은 공통으로 제거됩니다.", example: "일반적인 동의 요청 개인정보 파기는 개인정보 삭제" },
        "activityName": { text: "개인정보를 어떤 업무에 사용하는지 알아볼 수 있는 이름을 입력합니다.", example: "교육 참여자 모집 및 운영" },
        "retentionPolicyVersionId": { text: "이 업무의 개인정보에 적용할 승인 완료 보유정책 버전을 선택합니다.", example: "교육 프로그램 신청자 3년 보유 v1" },
        "legalBasisType": { text: "개인정보를 수집·이용할 수 있는 근거를 선택합니다. 정보주체 동의는 직접 동의를 받은 경우, 법령상 의무는 법에서 개인정보 수집·이용을 요구하는 경우, 공공업무 수행은 법령에 따른 공공기관의 업무인 경우, 계약 체결·이행은 계약을 준비하거나 수행하는 데 필요한 경우, 기타는 앞의 유형에 해당하지 않는 경우입니다.", example: "신청서에서 동의를 받아 수집하면 정보주체 동의" },
        "legalBasisDetail": { text: "선택한 근거를 확인할 수 있도록 동의를 받는 방법이나 관련 법령·계약 내용을 구체적으로 입력합니다.", example: "교육 신청 시 개인정보 수집·이용 동의를 받음" },
        "processingPurpose": { text: "개인정보를 왜 수집하고 어떻게 이용하는지 구체적으로 작성합니다.", example: "참여자를 확인하고 교육 일정과 변경 사항을 안내하기 위해" },
        "itemName#": { text: "해당 업무에서 수집·이용할 일반 개인정보 항목을 한 가지씩 입력합니다.", example: "이름, 이메일, 소속기관" },
        "necessityReason#": { text: "이 개인정보 항목이 업무 수행에 필요한 이유를 입력합니다.", example: "교육 일정 및 변경사항 안내를 위해 필요" },
        "required#": { text: "서비스 제공에 반드시 필요한 항목만 필수로 선택합니다.", example: "이메일은 일정 안내에 필수" },
        "items[].itemName": { text: "해당 업무에서 수집·이용할 일반 개인정보 항목을 한 가지씩 입력합니다.", example: "이름, 이메일, 소속기관" },
        "items[].necessityReason": { text: "이 개인정보 항목이 업무 수행에 필요한 이유를 입력합니다.", example: "교육 일정 및 변경사항 안내를 위해 필요" },
        "templateCode": { text: "동의서 양식을 구분할 영문·숫자 코드를 입력합니다. 생성 후에는 변경할 수 없습니다.", example: "CONSENT-EDU-2026" },
        "templateName": { text: "관리자 목록에서 용도를 알아볼 수 있는 내부 관리 명칭을 입력합니다.", example: "2026 교육 프로그램 참여 동의서" },
        "processingActivityVersionId": { text: "동의서 내용의 기준이 될 승인된 개인정보 이용 업무를 선택합니다. 개인정보 이용 목적과 보유기간이 연결됩니다.", example: "교육 참여자 모집 및 운영 v1" },
        "title": { text: "이메일과 웹 동의 화면에서 정보주체에게 표시할 제목을 입력합니다.", example: "교육 프로그램 참여를 위한 개인정보 수집·이용 동의" },
        "introductionHtml": { text: "수신자가 동의 전에 읽을 안내문을 작성합니다. 일반 문서처럼 입력하고 굵게·문단·목록 버튼을 사용할 수 있습니다. 외부 링크와 실행 코드는 포함되지 않습니다.", example: "아래 내용을 확인한 후 동의 여부를 선택해 주세요." },
        "items[].type": { text: "개인정보 수집·이용은 기관이 업무 목적으로 직접 수집하고 이용하는 경우입니다. 제3자 제공은 개인정보를 다른 기관이나 사업자에게 전달하는 경우이며 제공받는 자를 반드시 입력합니다. 기타 선택 동의는 서비스 제공에 필수가 아닌 추가 목적의 동의이며 필수 동의로 설정할 수 없습니다.", example: "교육 신청 접수는 개인정보 수집·이용, 위탁 운영기관에 전달하면 제3자 제공" },
        "items[].title": { text: "정보주체에게 표시할 개별 동의 항목 제목을 입력합니다.", example: "개인정보 수집·이용 동의" },
        "items[].required": { text: "업무 제공에 반드시 필요한 동의만 필수로 지정합니다. 선택 동의를 필수로 지정하지 않습니다.", example: "교육 신청 처리에 필요한 수집·이용 동의" },
        "items[].purpose": { text: "이 동의 항목에서 개인정보를 이용하는 목적을 구체적으로 작성합니다.", example: "교육 신청 접수, 선정 결과 안내 및 출결 관리" },
        "items[].retentionText": { text: "정보주체에게 표시할 보유기간 안내입니다. 승인된 보유정책과 다르게 작성할 수 없습니다.", example: "교육 종료일로부터 3년" },
        "items[].refusalNotice": { text: "동의를 거부할 권리와 거부 시 발생하는 제한을 함께 안내합니다.", example: "동의를 거부할 수 있으나 교육 신청 접수가 제한될 수 있습니다." },
        "items[].thirdPartyRecipient": { text: "제3자 제공 유형일 때만 개인정보를 제공받는 기관의 공식 명칭을 입력합니다.", example: "한국문화예술교육진흥원 위탁 교육운영기관" },
        "items[].processingDataItemIds": { text: "이 동의 항목에서 수집·이용하거나 제3자에게 제공할 개인정보 항목만 선택합니다.", example: "이름, 이메일" },
        "organizationUnitId": { text: "이 데이터의 관리·권한·조회 범위를 결정하는 우리 기관 내부 조직을 선택합니다.", example: "교육사업팀" },
        "affiliationName": { text: "동의 요청을 받을 수신자가 실제로 소속된 외부 회사·기관명을 입력합니다. 개인 또는 무소속이면 비워 둘 수 있습니다.", example: "주식회사 예술교육" },
        "jobTitle": { text: "수신자의 소속기관에서 사용하는 직책이나 직위를 입력합니다. 직책이 없거나 확인되지 않으면 비워 둘 수 있습니다.", example: "교육팀장" },
        "name": { text: "동의 요청을 받을 정보주체의 실명을 입력합니다. 관리자 목록에서는 마스킹됩니다.", example: "김가은" },
        "email": { text: "동의 안내 메일을 받을 정확한 이메일 주소를 입력합니다.", example: "gildong@example.com" },
        "collectionBasis": { text: "수신자의 이름과 이메일을 어떤 경로와 근거로 확보하여 시스템에 등록했는지 기록합니다.", example: "2026년 교육 프로그램 신청서 접수" },
        "file": { text: "제공된 5열 양식의 .xlsx 파일을 선택합니다. 5MB 이하, 최대 1,000명이며 각 행에 수신자 정보 등록 근거를 입력해야 합니다.", example: "교육수신자_2026-08-25.xlsx" },
        "recipientId": { text: "이번 동의 요청 또는 권리요청의 수신자를 선택합니다.", example: "김** · vh*****@arte.or.kr" },
        "templateVersionId": { text: "현재 발행 상태인 동의서 양식 버전을 선택합니다. 확정하면 발행 동의서 내용이 고정됩니다.", example: "CONSENT-EDU-2026 · 참여 동의서 v1" },
        "expiryDate": { text: "정보주체가 동의 링크에 응답할 마지막 날짜를 선택합니다. 시와 분을 별도로 고르거나 빠른 선택 버튼을 사용할 수 있습니다.", example: "2026-09-10 · 18시 00분" },
        "expiresAt": { text: "정보주체가 링크를 열어 응답할 수 있는 마지막 일시를 입력합니다.", example: "2026-09-30 18:00" },
        "consentRequestId": { text: "권리행사의 대상이 되는 동의 요청을 선택합니다. 삭제·처리제한·동의철회 유형은 필수입니다.", example: "CR-2026-000123" },
        "type": { text: "정보주체가 요청한 권리 유형을 선택합니다.", example: "열람, 정정, 삭제, 처리제한 또는 동의철회" },
        "receivedDate": { text: "기관이 권리요청을 공식 접수한 날짜를 입력합니다.", example: "2026-08-25" },
        "dueDate": { text: "법정·내부 처리기준에 따라 답변을 완료해야 하는 날짜를 입력합니다.", example: "2026-09-04" },
        "detail": { text: "정보주체의 요청 내용과 대상 정보의 범위를 개인정보 원문을 과도하게 포함하지 않도록 작성합니다.", example: "2026년 교육 신청 내역과 보유 개인정보 열람 요청" },
        "loginId": { text: "관리자가 로그인할 때 사용할 고유 ID를 영문·숫자로 입력합니다. 생성 후에는 변경할 수 없습니다.", example: "edu_manager01" },
        "displayName": { text: "감사 이력과 승인 화면에 표시할 관리자 실명을 입력합니다.", example: "김가은" },
        "temporaryPassword": { text: "최초 로그인 후 변경할 임시 비밀번호를 입력합니다. 12자 이상으로 영문·숫자·특수문자를 조합합니다.", example: "Arte!2026Temp" },
        "accountStatus": { text: "활성은 로그인 허용, 잠금은 실패 누적 차단, 비활성은 사용 중지 상태입니다.", example: "재직 중인 관리자는 활성" },
        "roleIds": { text: "관리자에게 실제 업무 수행에 필요한 역할만 선택합니다.", example: "동의업무 담당자" },
        "organizationScopeIds": { text: "관리자가 조회·처리할 수 있는 조직 범위를 선택합니다.", example: "교육사업팀 및 하위 조직" },
        "roleCode": { text: "역할을 구분할 영문 대문자·숫자 코드를 입력합니다. 생성 후에는 변경할 수 없습니다.", example: "CONSENT_MANAGER" },
        "roleName": { text: "담당자가 역할의 업무 범위를 알아볼 수 있는 명칭을 입력합니다.", example: "동의업무 담당자" },
        "permissionIds": { text: "해당 역할이 수행해야 하는 기능 권한만 최소한으로 선택합니다.", example: "대상자 조회, 동의요청 생성, 메일 조회" },
        "comment": { text: "승인하지 않는 이유와 작성자가 수정해야 할 내용을 구체적으로 기록합니다.", example: "보유기간 안내를 승인 보유정책과 동일하게 수정해 주세요." },
        "channel": { text: "기관이 본인확인을 수행한 실제 경로를 기록합니다.", example: "OFFLINE_DOCUMENT" },
        "note": { text: "본인확인 일시·확인 자료·담당자를 추적할 수 있도록 기록하되 주민등록번호 등 불필요한 원문은 입력하지 않습니다.", example: "2026-08-25 신분증 대면 확인, 담당자 김가은" },
        "result": { text: "처리 결과와 정보주체에게 통지한 내용을 기록합니다.", example: "요청 자료를 이메일로 제공하고 2026-08-28 통지 완료" },
        "reason": { text: "승인 또는 반려 결정의 구체적인 근거를 입력합니다.", example: "본인확인 완료 및 요청 범위가 보유 자료와 일치함" },
        "reasonCode": { text: "보유기간 예외 사유에 해당하는 관리 코드를 선택하거나 입력합니다.", example: "LEGAL_HOLD" },
        "reasonText": { text: "자동 파기에서 제외해야 하는 구체적인 사유와 근거를 기록합니다.", example: "진행 중인 분쟁 관련 자료로 보존 필요" },
        "anchorType": { text: "보유기간 계산의 기준이 되는 사건 유형을 선택합니다.", example: "동의 완료일" },
        "anchorDate": { text: "선택한 기준 사건이 실제 발생한 날짜를 입력합니다.", example: "2026-08-25" },
        "expiresOn": { text: "보유 예외를 다시 검토하거나 종료할 날짜를 입력합니다.", example: "2026-12-31" },
        "scheduledAt": { text: "메일을 발송 대기열에 넣을 예약 일시를 입력합니다.", example: "2026-08-26 09:00" }
    };

    const groupKeys = new Set(["roleIds", "organizationScopeIds", "permissionIds", "processingActivityVersionId", "items[].processingDataItemIds"]);
    let sequence = 0;
    let lastPointerType = "";

    function normalizeName(name, control) {
        const normalized = (name || "").replace(/\[\d+]/g, "[]");
        if (normalized === "items[].required" && control?.closest(".processing-data-items")) return "required#";
        if (/^itemName\d+$/.test(normalized)) return "itemName#";
        if (/^necessityReason\d+$/.test(normalized)) return "necessityReason#";
        if (/^required\d+$/.test(normalized)) return "required#";
        return normalized;
    }

    function createGuide(key, guide) {
        sequence += 1;
        const tooltipId = `field-guide-${sequence}`;
        const wrapper = document.createElement("span");
        wrapper.className = "field-guide";
        wrapper.dataset.fieldGuideKey = key;

        const trigger = document.createElement("button");
        trigger.type = "button";
        trigger.className = "field-guide-trigger";
        trigger.setAttribute("aria-label", "작성 가이드 열기");
        trigger.setAttribute("aria-expanded", "false");
        trigger.setAttribute("aria-describedby", tooltipId);
        trigger.textContent = "i";

        const tooltip = document.createElement("span");
        tooltip.id = tooltipId;
        tooltip.className = "field-guide-tooltip";
        tooltip.setAttribute("role", "tooltip");

        const heading = document.createElement("strong");
        heading.textContent = "작성 가이드";
        const description = document.createElement("span");
        description.textContent = guide.text;
        const example = document.createElement("span");
        example.className = "field-guide-example";
        example.textContent = `예시) ${guide.example}`;
        tooltip.append(heading, description, example);
        wrapper.append(trigger, tooltip);
        return wrapper;
    }

    function findLabel(control) {
        if (control.labels && control.labels.length) return control.labels[0];
        const wrappingLabel = control.closest("label");
        if (wrappingLabel) return wrappingLabel;
        if (!control.placeholder) return null;

        if (!control.id) control.id = `guided-field-${++sequence}`;
        const generatedLabel = document.createElement("label");
        generatedLabel.className = "field-guide-generated-label";
        generatedLabel.htmlFor = control.id;
        generatedLabel.textContent = control.placeholder;
        control.before(generatedLabel);
        return generatedLabel;
    }

    function labelLine(anchor, control) {
        const existing = Array.from(anchor.children).find(child => child.classList.contains("field-guide-label-line"));
        if (existing) return existing;

        const line = document.createElement("span");
        line.className = "field-guide-label-line";
        if (anchor.tagName === "LABEL" && anchor.contains(control)) {
            const nodes = Array.from(anchor.childNodes);
            const controlIndex = nodes.indexOf(control);
            const labelNodes = ["checkbox", "radio"].includes(control.type)
                ? nodes.slice(controlIndex + 1)
                : nodes.slice(0, controlIndex);
            labelNodes.forEach(node => line.append(node));
            if (["checkbox", "radio"].includes(control.type)) control.after(line);
            else anchor.insertBefore(line, control);
        } else {
            Array.from(anchor.childNodes).forEach(node => line.append(node));
            anchor.append(line);
        }
        return line;
    }

    function placeGuide(control, key, guide) {
        let anchor;
        if (groupKeys.has(key)) {
            const fieldset = control.closest("fieldset");
            anchor = fieldset ? fieldset.querySelector("legend") : findLabel(control);
        } else {
            anchor = findLabel(control);
        }
        if (!anchor || anchor.querySelector(`.field-guide[data-field-guide-key="${key}"]`)) return;

        labelLine(anchor, control).append(createGuide(key, guide));
    }

    function placeholderRepeatsLabel(control) {
        const label = control.labels?.[0] || control.closest("label");
        if (!label) return false;
        const copy = label.cloneNode(true);
        copy.querySelectorAll("input, select, textarea, button, .field-guide, .field-error")
            .forEach(element => element.remove());
        const labelText = copy.textContent.replace(/\s+/g, " ").trim();
        return control.placeholder.trim() === labelText;
    }

    function applyExamplePlaceholder(control, guide) {
        const isTextEntry = control.tagName === "TEXTAREA"
            || (control.tagName === "INPUT" && ["text", "email", "tel", "url", "search"].includes(control.type));
        const hasMeaningfulPlaceholder = control.hasAttribute("placeholder") && !placeholderRepeatsLabel(control);
        if (!isTextEntry || hasMeaningfulPlaceholder) return;
        control.placeholder = `예: ${guide.example}`;
    }

    function enhance(root = document) {
        const forms = new Set();
        if (root.matches?.("form")) forms.add(root);
        const containingForm = root.closest?.("form");
        if (containingForm) forms.add(containingForm);
        root.querySelectorAll?.("form").forEach(form => forms.add(form));

        forms.forEach(form => {
            if ((form.method || "").toLowerCase() === "get") return;
            form.querySelectorAll("input[name], select[name], textarea[name]").forEach(control => {
                if (["hidden", "submit", "button"].includes(control.type) || control.readOnly || control.disabled) return;
                const key = normalizeName(control.name, control);
                const guide = guides[key];
                if (guide) {
                    applyExamplePlaceholder(control, guide);
                    placeGuide(control, key, guide);
                }
            });
        });
    }

    function closeAll(except) {
        document.querySelectorAll(".field-guide.is-open").forEach(wrapper => {
            if (wrapper === except) return;
            wrapper.classList.remove("is-open", "align-right");
            wrapper.querySelector(".field-guide-trigger")?.setAttribute("aria-expanded", "false");
        });
    }

    document.addEventListener("click", event => {
        const trigger = event.target.closest(".field-guide-trigger");
        if (!trigger) return;
        event.preventDefault();
        event.stopPropagation();
        if (event.detail > 0 && lastPointerType === "mouse" && window.matchMedia("(hover: hover)").matches) {
            closeAll();
            trigger.blur();
            return;
        }

        const wrapper = trigger.closest(".field-guide");
        const opening = !wrapper.classList.contains("is-open");
        closeAll(wrapper);
        wrapper.classList.toggle("is-open", opening);
        trigger.setAttribute("aria-expanded", String(opening));
        wrapper.classList.remove("align-right");
        if (opening) {
            const tooltip = wrapper.querySelector(".field-guide-tooltip");
            if (tooltip.getBoundingClientRect().right > window.innerWidth - 12) wrapper.classList.add("align-right");
        }
    });

    document.addEventListener("pointerdown", event => {
        lastPointerType = event.pointerType;
        if (!event.target.closest(".field-guide")) closeAll();
    });

    document.addEventListener("focusin", event => {
        if (!event.target.closest(".field-guide")) closeAll();
    });

    document.addEventListener("keydown", event => {
        if (event.key === "Escape") {
            closeAll();
            document.activeElement?.blur();
        }
    });

    document.addEventListener("DOMContentLoaded", () => {
        enhance();
        new MutationObserver(mutations => {
            mutations.forEach(mutation => mutation.addedNodes.forEach(node => {
                if (node.nodeType === Node.ELEMENT_NODE) enhance(node.matches("form") ? node.parentElement : node);
            }));
        }).observe(document.body, { childList: true, subtree: true });
    });
})();
