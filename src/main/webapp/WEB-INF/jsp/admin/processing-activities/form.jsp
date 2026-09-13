<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<!doctype html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>개인정보 이용 업무 등록</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/app.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/admin-data.css?v=20260906-3">
    <script defer src="${pageContext.request.contextPath}/resources/js/processing-activity-items.js?v=20260906-2"></script>
</head>
<body class="admin-body">
<jsp:include page="/WEB-INF/jsp/fragments/admin/header.jsp"/>
<div class="admin-layout">
    <jsp:include page="/WEB-INF/jsp/fragments/admin/sidebar.jsp"/>
    <main class="admin-main">
        <div class="page-heading-row">
            <div>
                <p class="section-kicker">NEW PROCESSING ACTIVITY</p>
                <h1>개인정보 이용 업무 등록</h1>
                <p>개인정보를 사용하는 업무와 이용 목적, 이용 근거, 보유기간 및 필요한 개인정보 항목을 등록합니다.</p>
            </div>
        </div>

        <section class="work-panel">
            <form:form method="post" action="${pageContext.request.contextPath}/admin/processing-activities"
                       modelAttribute="activityForm" cssClass="data-form">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">

                <div class="form-field full-field">
                    <form:label path="activityName">업무 이름</form:label>
                    <form:input path="activityName" maxlength="100"/>
                    <form:errors path="activityName" cssClass="field-error"/>
                </div>
                <div class="form-field">
                    <form:label path="ownerOrganizationUnitId">소유 조직</form:label>
                    <form:select path="ownerOrganizationUnitId">
                        <form:option value="" label="선택"/>
                        <c:forEach var="o" items="${organizations}">
                            <form:option value="${o.organizationUnitId()}" label="${o.organizationName()}"/>
                        </c:forEach>
                    </form:select>
                    <form:errors path="ownerOrganizationUnitId" cssClass="field-error"/>
                </div>
                <div class="form-field">
                    <form:label path="retentionPolicyVersionId">승인 보유정책</form:label>
                    <form:select path="retentionPolicyVersionId">
                        <form:option value="" label="선택"/>
                        <c:forEach var="p" items="${retentionPolicies}">
                            <form:option value="${p.retentionPolicyVersionId()}" label="${p.policyName()} v${p.versionNumber()}"/>
                        </c:forEach>
                    </form:select>
                    <form:errors path="retentionPolicyVersionId" cssClass="field-error"/>
                </div>
                <div class="form-field">
                    <form:label path="legalBasisType">개인정보 이용 근거</form:label>
                    <form:select path="legalBasisType">
                        <c:forEach var="basisType" items="${legalBasisTypes}">
                            <form:option value="${basisType}" label="${basisType.displayName}"/>
                        </c:forEach>
                    </form:select>
                </div>
                <div class="form-field">
                    <form:label path="legalBasisDetail">근거 상세 설명</form:label>
                    <form:textarea path="legalBasisDetail" maxlength="1000"/>
                    <form:errors path="legalBasisDetail" cssClass="field-error"/>
                </div>
                <div class="form-field full-field">
                    <form:label path="processingPurpose">개인정보 이용 목적</form:label>
                    <form:textarea path="processingPurpose" maxlength="1000"/>
                    <form:errors path="processingPurpose" cssClass="field-error"/>
                </div>

                <fieldset class="choice-field processing-data-items">
                    <legend>수집·이용할 개인정보 항목</legend>
                    <div class="processing-data-items-heading">
                        <p>이 업무에서 수집·이용할 개인정보 항목을 한 개씩 등록하세요. 최대 50개까지 추가할 수 있습니다.</p>
                        <button class="secondary-button compact-button" type="button" data-data-item-add>항목 추가</button>
                    </div>
                    <form:errors path="items" cssClass="field-error"/>
                    <div class="processing-data-item-list" data-data-item-list data-max-items="50">
                    <c:forEach var="item" items="${activityForm.items}" varStatus="status">
                        <div class="data-item-row" data-data-item-row>
                            <label>
                                항목명
                                <form:input path="items[${status.index}].itemName" placeholder="항목명"/>
                                <form:errors path="items[${status.index}].itemName" cssClass="field-error"/>
                            </label>
                            <label>
                                이 항목이 필요한 이유
                                <form:input path="items[${status.index}].necessityReason" placeholder="이 항목이 필요한 이유"/>
                                <form:errors path="items[${status.index}].necessityReason" cssClass="field-error"/>
                            </label>
                            <label class="check-label"><form:checkbox path="items[${status.index}].required"/> 필수</label>
                            <button class="secondary-button data-item-remove" type="button" data-data-item-remove>삭제</button>
                        </div>
                    </c:forEach>
                    </div>
                    <p class="data-item-count"><strong data-data-item-count>${activityForm.items.size()}</strong>/50개 등록</p>
                </fieldset>

                <div class="form-actions">
                    <a class="secondary-button" href="${pageContext.request.contextPath}/admin/processing-activities">취소</a>
                    <button class="primary-button" type="submit">초안 생성</button>
                </div>
            </form:form>
        </section>
    </main>
</div>
</body>
</html>
