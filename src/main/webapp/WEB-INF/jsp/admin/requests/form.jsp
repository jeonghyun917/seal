<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<!doctype html>
<html lang="ko">
<head>
    <meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
    <title>${editing ? '동의 요청 수정' : '동의 요청 생성'}</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/app.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/admin-data.css?v=20260906-3">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/recipient-request.css?v=20260831-2">
    <script defer src="${pageContext.request.contextPath}/resources/js/expiry-picker.js?v=20260827-1"></script>
    <c:if test="${not editing}"><script defer src="${pageContext.request.contextPath}/resources/js/recipient-picker.js?v=20260929-1"></script></c:if>
</head>
<body class="admin-body">
<jsp:include page="/WEB-INF/jsp/fragments/admin/header.jsp"/>
<div class="admin-layout"><jsp:include page="/WEB-INF/jsp/fragments/admin/sidebar.jsp"/>
<main class="admin-main">
    <div class="page-heading-row"><div><p class="section-kicker">CONSENT REQUEST</p><h1>${editing ? '동의 요청 수정' : '동의 요청 생성'}</h1><p>${editing ? '활성 동의 수신자와 동의서 양식을 변경합니다.' : '여러 수신자를 선택하면 수신자별로 독립된 동의 요청 초안이 생성됩니다.'}</p></div></div>
    <c:set var="formAction" value="${editing ? pageContext.request.contextPath.concat('/admin/requests/').concat(requestId) : pageContext.request.contextPath.concat('/admin/requests')}"/>
    <section class="work-panel"><form:form method="post" action="${formAction}" modelAttribute="requestForm" cssClass="data-form">
        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"><form:hidden path="idempotencyKey"/><form:hidden path="versionNo"/>
        <c:choose>
            <c:when test="${editing}">
                <div class="form-field"><form:label path="recipientId">동의 수신자</form:label><form:select path="recipientId"><form:option value="" label="선택"/><c:forEach var="recipient" items="${recipients}"><form:option value="${recipient.recipientId()}" label="${empty recipient.affiliationName() ? '' : recipient.affiliationName().concat(' · ')}${empty recipient.jobTitle() ? '' : recipient.jobTitle().concat(' · ')}${recipient.maskedName()} · ${recipient.maskedEmail()}"/></c:forEach></form:select><form:errors path="recipientId" cssClass="field-error"/></div>
            </c:when>
            <c:otherwise>
                <div class="form-field recipient-picker-field">
                    <label>동의 수신자</label>
                    <div class="recipient-selector-summary">
                        <div><strong id="selectedRecipientCount" aria-live="polite">0명 선택</strong><p id="selectedRecipientPreview">선택된 수신자가 없습니다.</p></div>
                        <button id="openRecipientPicker" class="secondary-button" type="button">수신자 선택</button>
                    </div>
                    <div id="selectedRecipientInputs"><c:forEach var="recipient" items="${selectedRecipients}"><input type="hidden" name="recipientIds" value="${recipient.recipientId()}"></c:forEach></div>
                    <div id="selectedRecipientSeeds" hidden><c:forEach var="recipient" items="${selectedRecipients}"><div class="selected-recipient-seed" data-recipient-id="${recipient.recipientId()}"><span class="seed-name"><c:out value="${recipient.maskedName()}"/></span><span class="seed-email"><c:out value="${recipient.maskedEmail()}"/></span><span class="seed-affiliation"><c:out value="${recipient.affiliationName()}"/></span><span class="seed-job-title"><c:out value="${recipient.jobTitle()}"/></span><span class="seed-collection-basis"><c:out value="${recipient.collectionBasis()}"/></span></div></c:forEach></div>
                    <form:errors path="recipientIds" cssClass="field-error"/>
                    <dialog id="recipientPickerDialog" class="recipient-picker-dialog" data-options-url="${pageContext.request.contextPath}/admin/requests/recipient-options">
                        <div class="recipient-dialog-shell">
                            <header><div><span>RECIPIENT SELECTOR</span><h2>동의 수신자 선택</h2><p>검색 결과는 20명씩 표시되며 페이지를 이동해도 선택은 유지됩니다.</p></div><button id="closeRecipientPicker" class="dialog-close" type="button" aria-label="닫기">×</button></header>
                            <div class="recipient-dialog-search"><input id="recipientSearch" type="search" placeholder="소속기관, 직책, 이름, 이메일 또는 등록 근거 검색" aria-label="동의 수신자 검색"><button id="searchRecipients" class="secondary-button" type="button">검색</button></div>
                            <div class="recipient-dialog-actions"><button id="selectCurrentPage" type="button">현재 페이지 전체 선택</button><button id="selectAllResults" type="button">검색 결과 선택 (최대 1,000명)</button><span id="recipientResultStatus" aria-live="polite"></span></div>
                            <div class="recipient-dialog-body">
                                <section class="recipient-results-panel" aria-labelledby="recipientResultsTitle"><h3 id="recipientResultsTitle">검색 결과</h3><div id="recipientResults" class="recipient-results"><p class="recipient-empty">수신자를 불러오는 중입니다.</p></div><nav class="recipient-pagination" aria-label="수신자 검색 결과 페이지"><button id="previousRecipientPage" type="button">이전</button><span id="recipientPageLabel">1페이지</span><button id="nextRecipientPage" type="button">다음</button></nav></section>
                                <aside class="recipient-selected-panel"><div><h3>선택한 수신자</h3><strong id="dialogSelectedCount">0명</strong></div><div id="dialogSelectedRecipients" class="dialog-selected-recipients"><p class="recipient-empty">선택된 수신자가 없습니다.</p></div></aside>
                            </div>
                            <footer><button id="cancelRecipientSelection" class="secondary-button" type="button">취소</button><button id="applyRecipientSelection" class="primary-button" type="button">선택 완료</button></footer>
                        </div>
                    </dialog>
                </div>
            </c:otherwise>
        </c:choose>
        <div class="form-field"><form:label path="templateVersionId">동의서 양식</form:label><form:select path="templateVersionId"><form:option value="" label="선택"/><c:forEach var="template" items="${templates}"><form:option value="${template.templateVersionId()}" label="${template.templateCode()} · ${template.templateName()} v${template.versionNumber()}"/></c:forEach></form:select><form:errors path="templateVersionId" cssClass="field-error"/></div>
        <div class="form-field expiry-field"><label for="expiryDate">응답 만료일시</label><form:hidden path="expiresAt" id="expiresAt"/><div class="expiry-picker-controls"><input id="expiryDate" name="expiryDate" type="date" required aria-label="응답 만료 날짜"><select id="expiryHour" required aria-label="응답 만료 시"><option value="">시</option><c:forEach begin="0" end="23" var="hour"><option value="${hour}">${hour}시</option></c:forEach></select><select id="expiryMinute" required aria-label="응답 만료 분"><option value="">분</option><option value="00">00분</option><option value="10">10분</option><option value="20">20분</option><option value="30">30분</option><option value="40">40분</option><option value="50">50분</option></select></div><div class="expiry-quick-actions" aria-label="빠른 만료일시 선택"><span>빠른 선택</span><button type="button" data-expiry-days="7">7일 후</button><button type="button" data-expiry-days="14">14일 후</button><button type="button" data-expiry-days="30">30일 후</button><small>선택 시 오후 6시로 설정됩니다.</small></div><form:errors path="expiresAt" cssClass="field-error"/></div>
        <div class="form-actions"><a class="secondary-button" href="${pageContext.request.contextPath}/admin/requests">취소</a><button class="primary-button">${editing ? '초안 저장' : '초안 생성'}</button></div>
    </form:form></section>
</main></div>
</body></html>
