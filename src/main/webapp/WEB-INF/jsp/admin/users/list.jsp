<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html><html lang="ko"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>관리자 계정 | 개인정보 전자동의·증적 관리시스템</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/app.css"><link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/admin-data.css"></head>
<body class="admin-body"><jsp:include page="/WEB-INF/jsp/fragments/admin/header.jsp"/>

<div class="admin-layout"><jsp:include page="/WEB-INF/jsp/fragments/admin/sidebar.jsp"/><main class="admin-main"><div class="page-heading-row"><div><p class="section-kicker">ADMINISTRATORS</p><h1>관리자 계정</h1><p>허용된 조직 범위의 관리자 상태와 로그인 현황을 조회합니다.</p></div><a class="primary-button" href="${pageContext.request.contextPath}/admin/users/new">관리자 등록</a></div>
<section class="work-panel" aria-labelledby="user-list-title"><h2 id="user-list-title">관리자 목록</h2><div class="table-scroll"><table class="data-table"><thead><tr><th>로그인 ID</th><th>이름</th><th>소속</th><th>상태</th><th>마지막 로그인</th></tr></thead><tbody>
<c:forEach var="user" items="${users}"><tr><td><c:out value="${user.loginId()}"/></td><td><a href="${pageContext.request.contextPath}/admin/users/${user.adminUserId()}"><c:out value="${user.displayName()}"/></a></td><td><c:out value="${user.organizationName()}"/></td><td><span class="state-text ${user.accountStatus() == 'ACTIVE' ? 'ready' : 'pending'}"><c:out value="${user.accountStatus()}"/></span></td><td><c:out value="${user.lastLoginAt()}" default="-"/></td></tr></c:forEach>
<c:if test="${empty users}"><tr><td colspan="5">조회 가능한 관리자가 없습니다.</td></tr></c:if></tbody></table></div></section></main></div></body></html>
