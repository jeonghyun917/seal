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
<body class="admin-body"><jsp:include page="/WEB-INF/jsp/fragments/admin/header.jsp"/>



    <div class="admin-layout"><jsp:include page="/WEB-INF/jsp/fragments/admin/sidebar.jsp"/><main class="admin-main">
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
    </main></div></body>
</html>
