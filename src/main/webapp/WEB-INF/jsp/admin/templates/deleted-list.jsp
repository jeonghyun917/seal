<%@ taglib prefix="seal" uri="https://arte.or.kr/seal/functions" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<!doctype html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>삭제된 동의서 양식 | 개인정보 전자동의 관리시스템</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/app.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/admin-data.css?v=20260906-3">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/template.css?v=20260906-3">
    <script defer src="${pageContext.request.contextPath}/resources/js/template-bulk-actions.js?v=20260826-2"></script>
</head>
<body class="admin-body">
<jsp:include page="/WEB-INF/jsp/fragments/admin/header.jsp"/>
<div class="admin-layout">
    <jsp:include page="/WEB-INF/jsp/fragments/admin/sidebar.jsp"/>
    <main class="admin-main">
        <div class="page-heading-row">
            <div>
                <p class="section-kicker">DELETED TEMPLATE</p>
                <h1>삭제된 동의서 양식</h1>
                <p>삭제된 동의서 양식의 이력을 확인하고 필요한 항목을 복구합니다.</p>
            </div>
            <a class="secondary-button" href="${pageContext.request.contextPath}/admin/templates">일반 목록</a>
        </div>
        <c:if test="${param.restored != null}">
            <p class="alert alert-success">선택한 동의서 양식을 일반 목록으로 복구했습니다.</p>
        </c:if>
        <section class="work-panel">
            <form class="list-filter" method="get">
                <label>검색
                    <input name="keyword" value="<c:out value='${keyword}'/>" placeholder="코드 또는 명칭">
                </label>
                <button class="secondary-button" type="submit">조회</button>
            </form>

            <sec:authorize access="hasAuthority('CONSENT_TEMPLATE_EDIT')" var="canEditTemplate"/>
            <c:set var="columnCount" value="${canEditTemplate ? 8 : 7}"/>
            <form method="post" action="${pageContext.request.contextPath}/admin/templates/restore"
                  data-bulk-form data-confirm-message="선택한 동의서 양식을 일반 목록으로 복구하시겠습니까?">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                <c:if test="${canEditTemplate}">
                    <div class="bulk-toolbar bulk-toolbar-end">
                        <span class="selection-count" data-selection-count>0개 선택</span>
                        <button class="primary-button" type="submit" data-bulk-submit disabled>선택 복구</button>
                    </div>
                </c:if>
                <div class="table-scroll">
                    <table class="data-table">
                        <thead>
                        <tr>
                            <c:if test="${canEditTemplate}">
                                <th class="selection-cell"><input type="checkbox" data-select-all aria-label="전체 삭제 동의서 양식 선택"></th>
                            </c:if>
                            <th>코드</th><th>양식 관리 명칭</th><th>개인정보 이용 업무</th><th>기존 상태</th><th>삭제자</th><th>삭제 일시</th><th>삭제 사유</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach var="template" items="${deletedTemplates}">
                            <tr>
                                <c:if test="${canEditTemplate}">
                                    <td class="selection-cell"><input type="checkbox" name="templateIds"
                                        value="${template.consentTemplateId()}" data-row-select
                                        aria-label="<c:out value='${template.templateName()}'/> 복구 선택"></td>
                                </c:if>
                                <td><c:out value="${template.templateCode()}"/></td>
                                <td><c:out value="${template.templateName()}"/></td>
                                <td><c:out value="${template.processingActivityName()}"/></td>
                                <td><c:out value="${seal:statusLabel('template', template.templateStatus())}"/></td>
                                <td><c:out value="${template.deletedByName()}"/></td>
                                <td><c:out value="${seal:dateTime(template.deletedAt())}"/></td>
                                <td><c:out value="${template.deleteReason()}"/></td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty deletedTemplates}"><tr><td colspan="${columnCount}">삭제된 동의서 양식이 없습니다.</td></tr></c:if>
                        </tbody>
                    </table>
                </div>
            </form>
        </section>
    </main>
</div>
</body>
</html>
