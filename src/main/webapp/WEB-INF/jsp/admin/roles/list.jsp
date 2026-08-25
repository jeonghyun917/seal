<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html><html lang="ko"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>역할·권한 | 개인정보 전자동의·증적 관리시스템</title><link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/app.css"><link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/admin-data.css"></head>
<body class="admin-body"><jsp:include page="/WEB-INF/jsp/fragments/admin/header.jsp"/>

<div class="admin-layout"><jsp:include page="/WEB-INF/jsp/fragments/admin/sidebar.jsp"/><main class="admin-main"><div class="page-heading-row"><div><p class="section-kicker">ROLES &amp; PERMISSIONS</p><h1>역할·권한</h1><p>시스템 역할별 사용자 수와 부여된 작업 권한을 조회합니다.</p></div><a class="primary-button" href="${pageContext.request.contextPath}/admin/roles/new">역할 등록</a></div>
<section class="work-panel" aria-labelledby="role-list-title"><h2 id="role-list-title">역할 목록</h2><div class="table-scroll"><table class="data-table"><thead><tr><th>역할코드</th><th>역할명</th><th>유형</th><th>사용자</th><th>권한</th><th>상태</th></tr></thead><tbody>
<c:forEach var="role" items="${roles}"><tr><td><a href="${pageContext.request.contextPath}/admin/roles/${role.roleId()}"><c:out value="${role.roleCode()}"/></a></td><td><c:out value="${role.roleName()}"/></td><td>${role.systemRole() ? '시스템' : '사용자 정의'}</td><td><c:out value="${role.userCount()}"/>명</td><td><c:out value="${role.permissionCount()}"/>개</td><td><span class="state-text ${role.active() ? 'ready' : 'pending'}">${role.active() ? '활성' : '비활성'}</span></td></tr></c:forEach>
<c:if test="${empty roles}"><tr><td colspan="6">등록된 역할이 없습니다.</td></tr></c:if></tbody></table></div></section></main></div></body></html>
