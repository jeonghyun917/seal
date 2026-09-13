<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %><%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<!doctype html><html lang="ko"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>보유정책 등록</title><link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/app.css"><link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/admin-data.css?v=20260907-2"></head>
<body class="admin-body"><jsp:include page="/WEB-INF/jsp/fragments/admin/header.jsp"/><div class="admin-layout"><jsp:include page="/WEB-INF/jsp/fragments/admin/sidebar.jsp"/><main class="admin-main"><div class="page-heading-row"><div><p class="section-kicker">NEW RETENTION POLICY</p><h1>보유정책 등록</h1><p>정책 기본정보와 첫 번째 초안 버전을 함께 생성합니다.</p></div></div>
<section class="work-panel"><form:form method="post" action="${pageContext.request.contextPath}/admin/retention-policies" modelAttribute="policyForm" cssClass="data-form"><input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
<div class="form-field"><form:label path="policyName">정책명</form:label><form:input path="policyName" maxlength="100"/><form:errors path="policyName" cssClass="field-error"/></div>
<div class="form-field"><form:label path="ownerOrganizationUnitId">소유 조직</form:label><form:select path="ownerOrganizationUnitId"><form:option value="" label="소유 조직 선택"/><c:forEach var="organization" items="${organizations}"><form:option value="${organization.organizationUnitId()}" label="${organization.organizationName()}"/></c:forEach></form:select><form:errors path="ownerOrganizationUnitId" cssClass="field-error"/></div>
<div class="form-field"><form:label path="startEvent">보유기간 시작 기준</form:label><form:select path="startEvent"><form:option value="" label="시작 기준 선택"/><form:options items="${startEvents}" itemLabel="label"/></form:select><form:errors path="startEvent" cssClass="field-error"/></div>
<div class="retention-period-fields">
    <div class="form-field"><form:label path="periodValue">보유기간</form:label><form:input path="periodValue" type="number" min="1" max="100"/><form:errors path="periodValue" cssClass="field-error"/></div>
    <div class="form-field"><form:label path="periodUnit">보유기간 단위</form:label><form:select path="periodUnit"><form:options items="${periodUnits}" itemLabel="label"/></form:select></div>
</div>
<aside class="retention-scope-note full-field" aria-label="수신자 개인정보 보유 및 파기 안내">
    <strong>‘수신자 응답 제출일’ 선택 시 계산 방법</strong>
    <p class="retention-calculation-note"><b>응답한 수신자</b>는 실제 응답 제출일부터, <b>미응답 수신자</b>는 동의 요청 만료일부터 보유기간을 계산합니다.</p>
    <div class="retention-disposal-grid">
        <section>
            <h2>응답한 수신자 파기 항목</h2>
            <ul>
                <li>수신자 이름·이메일·소속·직책</li>
                <li>저장된 발송 이메일 주소와 메일 본문</li>
                <li>동의 링크 세션과 관련 상세 개인정보</li>
                <li>생성된 동의 증적 파일</li>
            </ul>
        </section>
        <section>
            <h2>미응답 수신자 파기 항목</h2>
            <ul>
                <li>수신자 이름·이메일·소속·직책</li>
                <li>저장된 발송 이메일 주소와 메일 본문</li>
                <li>만료된 동의 링크 세션과 관련 상세 개인정보</li>
                <li>응답·증적 파일은 생성되지 않으므로 파기 대상 없음</li>
            </ul>
        </section>
    </div>
    <p>수신자 개인정보는 연결된 다른 동의 요청의 보유기간도 모두 끝난 뒤 최종 제거됩니다. 요청번호·상태·발송/만료/파기 시각과 응답·감사기록은 관련 기준에 따라 별도로 보존될 수 있습니다.</p>
</aside>
<div class="form-field"><form:label path="gracePeriodDays">파기 유예기간(일)</form:label><form:input path="gracePeriodDays" type="number" min="0" max="365"/><form:errors path="gracePeriodDays" cssClass="field-error"/></div>
<div class="form-field"><form:label path="destructionMethod">파기 방식</form:label><form:select path="destructionMethod"><form:options items="${destructionMethods}" itemLabel="label"/></form:select></div>
<aside class="destruction-method-note full-field" aria-label="파기 방식 상세 안내">
    <strong>파기 방식 선택 가이드</strong>
    <p>선택한 방식은 파기 작업과 감사기록에 남습니다. 현재 파기 실행 시에는 어떤 방식을 선택해도 이름·이메일 등 개인정보, 저장된 메일 본문, 동의 링크 세션 식별값과 관련 상세 개인정보를 공통으로 제거합니다.</p>
    <div class="destruction-method-grid">
        <section>
            <h2>개인정보 삭제 <span>일반적인 선택</span></h2>
            <p>수신자 이름·이메일·소속·직책 등 개인을 식별하는 값을 제거합니다. 동의 요청 관련 개인정보를 더 이상 사용할 필요가 없을 때 선택합니다.</p>
        </section>
        <section>
            <h2>익명 처리 <span>통계 이력 유지</span></h2>
            <p>개인을 다시 알아볼 수 없도록 식별값을 제거하고, 요청 상태·처리 건수처럼 개인을 식별하지 않는 통계·감사 이력만 남길 때 선택합니다.</p>
        </section>
        <section>
            <h2>파일 완전 삭제 <span>증적 파일 포함</span></h2>
            <p>PDF·JSON 등 생성된 동의 증적 파일을 시스템의 운영 저장소에서 제거해야 할 때 선택합니다. 데이터베이스에 저장된 개인정보도 함께 제거되며, 별도 백업의 만료는 기관의 백업 정책을 따릅니다.</p>
        </section>
    </div>
    <p class="destruction-method-caution">요청번호·상태·발송/만료/파기 시각 등 최소 감사기록은 파기 증명과 이력 추적을 위해 남을 수 있습니다.</p>
</aside>
<div class="form-actions"><a class="secondary-button" href="${pageContext.request.contextPath}/admin/retention-policies">취소</a><button class="primary-button" type="submit">초안 생성</button></div></form:form></section></main></div></body></html>
