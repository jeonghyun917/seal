<%@ taglib prefix="seal" uri="https://arte.or.kr/seal/functions" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="ko">
<head>
    <meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
    <title>동의 요청</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/app.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/admin-data.css?v=20260906-3">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/recipient-request.css?v=20260831-1">
</head>
<body class="admin-body">
<jsp:include page="/WEB-INF/jsp/fragments/admin/header.jsp"/>
<div class="admin-layout">
    <jsp:include page="/WEB-INF/jsp/fragments/admin/sidebar.jsp"/>
    <main class="admin-main">
        <div class="page-heading-row"><div><p class="section-kicker">CONSENT REQUEST</p><h1>동의 요청</h1><p>대상자와 동의서 양식을 연결하고 발행 동의서의 링크·만료 상태를 관리합니다.</p></div><a class="primary-button" href="${pageContext.request.contextPath}/admin/requests/new">요청 생성</a></div>
        <c:if test="${not empty notice}"><div class="alert alert-success" role="status"><c:out value="${notice}"/></div></c:if>
        <section class="work-panel">
            <form method="get" class="list-filter"><label for="q"><span>요청번호·동의서</span><input id="q" name="q" value="<c:out value='${q}'/>" placeholder="검색어"></label><label for="status"><span>상태</span><select id="status" name="status"><option value="">전체</option><c:forEach var="state" items="${statusOptions}"><option value="${state}" ${selectedStatus == state ? 'selected' : ''}><c:out value="${seal:statusLabel('request', state)}"/></option></c:forEach></select></label><button class="secondary-button">조회</button></form>
            <div class="table-scroll"><table class="data-table request-list-table"><thead><tr><th>요청번호</th><th>대상자</th><th>동의서</th><th>상태</th><th>만료일시</th><th>생성일시</th></tr></thead><tbody><c:forEach var="row" items="${requests}"><c:set var="request" value="${row.summary()}"/><tr><td><a href="${pageContext.request.contextPath}/admin/requests/${request.requestId()}"><c:out value="${request.requestCode()}"/></a></td><td><c:out value="${row.recipient().maskedName()}"/> · <c:out value="${row.recipient().maskedEmail()}"/></td><td><c:out value="${empty row.template() ? '동의서 정보 확인 불가' : row.template().templateName()}"/></td><td><span class="request-state state-${request.status()}"><c:out value="${seal:statusLabel('request', request.status())}"/></span></td><td><c:out value="${seal:dateTime(request.expiresAt())}"/></td><td><c:out value="${seal:dateTime(request.createdAt())}"/></td></tr></c:forEach><c:if test="${empty requests}"><tr><td colspan="6">동의 요청이 없습니다.</td></tr></c:if></tbody></table></div>
            <jsp:include page="/WEB-INF/jsp/fragments/admin/pagination.jsp"/>
        </section>
    </main>
</div>
</body>
</html>
