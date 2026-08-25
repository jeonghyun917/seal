<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %><%@ taglib prefix="c" uri="jakarta.tags.core" %><%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<sec:authentication property="principal.displayName" var="currentAdminName"/>
<header class="admin-header">
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
