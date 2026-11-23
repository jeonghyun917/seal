<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %><%@ taglib prefix="c" uri="jakarta.tags.core" %><%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="guide" tagdir="/WEB-INF/tags" %>
<guide:fieldGuides/>
<sec:authentication property="principal.displayName" var="currentAdminName"/>
<a class="skip-link" href="#admin-main">본문 바로가기</a>
<header class="admin-header">
    <button class="admin-menu-button" type="button" aria-controls="adminPrimaryNav" aria-expanded="false">
        <span class="admin-menu-icon" aria-hidden="true"></span>
        <span>메뉴</span>
    </button>
    <a class="admin-brand" href="${pageContext.request.contextPath}/admin">
        <span>ARTE</span><strong>개인정보 전자동의·증적 관리시스템</strong>
    </a>
    <div class="user-actions">
        <span><strong><c:out value="${currentAdminName}"/></strong> 님</span>
        <form method="post" action="${pageContext.request.contextPath}/logout">
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
            <button class="text-button" type="submit">로그아웃</button>
        </form>
    </div>
</header>
<script defer src="${pageContext.request.contextPath}/resources/js/admin-ui.js?v=20260929-1"></script>
