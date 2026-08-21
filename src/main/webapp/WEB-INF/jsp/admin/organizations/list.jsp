<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>조직 관리 | 개인정보 전자동의·증적 관리시스템</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/app.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/admin-data.css">
</head>
<body class="admin-body">
<header class="admin-header">
    <a class="admin-brand" href="${pageContext.request.contextPath}/admin"><span>ARTE</span><strong>개인정보 전자동의·증적 관리시스템</strong></a>
    <div class="user-actions"><span><strong><c:out value="${administrator.displayName()}"/></strong> 님</span></div>
</header>
<div class="admin-layout">
    <nav class="admin-nav" aria-label="관리자 주 메뉴">
        <a href="${pageContext.request.contextPath}/admin">대시보드</a>
        <span class="nav-group">기준정보</span>
        <a class="active" aria-current="page" href="${pageContext.request.contextPath}/admin/organizations">조직 관리</a>
        <a href="${pageContext.request.contextPath}/admin/retention-policies">보유정책</a>
        <a href="${pageContext.request.contextPath}/admin/processing-activities">처리업무</a>
        <span class="nav-group">시스템 관리</span>
        <a href="${pageContext.request.contextPath}/admin/users">관리자·권한</a>
        <a href="${pageContext.request.contextPath}/admin/audit-events">감사 이력</a>
    </nav>
    <main class="admin-main">
        <div class="page-heading-row">
            <div><p class="section-kicker">ORGANIZATION</p><h1>조직 관리</h1><p>접근 가능한 조직과 활성 상태를 조회합니다.</p></div><a class="primary-button" href="${pageContext.request.contextPath}/admin/organizations/new">조직 등록</a>
        </div>
        <section class="work-panel" aria-labelledby="organization-list-title">
            <h2 id="organization-list-title">조직 목록</h2>
            <div class="table-scroll">
                <table class="data-table">
                    <thead><tr><th scope="col">조직코드</th><th scope="col">조직명</th><th scope="col">상위 조직</th><th scope="col">상태</th></tr></thead>
                    <tbody>
                    <c:forEach var="organization" items="${organizations}">
                        <tr>
                            <td><c:out value="${organization.organizationCode()}"/></td>
                            <td><a href="${pageContext.request.contextPath}/admin/organizations/${organization.organizationUnitId()}"><c:out value="${organization.organizationName()}"/></a></td>
                            <td><c:out value="${organization.parentOrganizationName()}" default="-"/></td>
                            <td><c:choose><c:when test="${organization.active()}"><span class="state-text ready">활성</span></c:when><c:otherwise><span class="state-text pending">비활성</span></c:otherwise></c:choose></td>
                        </tr>
                    </c:forEach>
                    <c:if test="${empty organizations}"><tr><td colspan="4">조회 가능한 조직이 없습니다.</td></tr></c:if>
                    </tbody>
                </table>
            </div>
        </section>
    </main>
</div>
</body>
</html>
