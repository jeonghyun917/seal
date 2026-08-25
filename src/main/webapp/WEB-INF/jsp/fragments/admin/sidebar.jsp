<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %><%@ taglib prefix="c" uri="jakarta.tags.core" %><%@ taglib prefix="fn" uri="jakarta.tags.functions" %><%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<c:set var="adminPath" value="${requestScope['jakarta.servlet.forward.request_uri']}"/>
<c:if test="${empty adminPath}">
    <c:set var="adminPath" value="${pageContext.request.requestURI}"/>
</c:if>
<nav class="admin-nav" aria-label="관리자 주 메뉴">
    <a class="${fn:endsWith(adminPath, '/admin') ? 'active' : ''}" href="${pageContext.request.contextPath}/admin">대시보드</a>
    <span class="nav-group">동의 업무</span>
<sec:authorize access="hasAuthority('RECIPIENT_VIEW')">    <a class="${fn:contains(adminPath, '/admin/recipients') ? 'active' : ''}" href="${pageContext.request.contextPath}/admin/recipients">동의 대상자</a></sec:authorize>
<sec:authorize access="hasAuthority('CONSENT_REQUEST_VIEW')">    <a class="${fn:contains(adminPath, '/admin/requests') ? 'active' : ''}" href="${pageContext.request.contextPath}/admin/requests">동의 요청</a></sec:authorize>
<sec:authorize access="hasAuthority('MAIL_VIEW')">    <a class="${fn:contains(adminPath, '/admin/mail-outbox') ? 'active' : ''}" href="${pageContext.request.contextPath}/admin/mail-outbox">메일 발송함</a></sec:authorize>
<sec:authorize access="hasAuthority('EVIDENCE_VIEW')">    <a class="${fn:contains(adminPath, '/admin/evidence') ? 'active' : ''}" href="${pageContext.request.contextPath}/admin/evidence">동의 증적</a></sec:authorize>
<sec:authorize access="hasAuthority('RIGHTS_VIEW')">    <a class="${fn:contains(adminPath, '/admin/rights') ? 'active' : ''}" href="${pageContext.request.contextPath}/admin/rights">권리요청</a></sec:authorize>
<sec:authorize access="hasAuthority('RETENTION_VIEW')">    <a class="${fn:contains(adminPath, '/admin/retention') && !fn:contains(adminPath, '/admin/retention-policies') ? 'active' : ''}" href="${pageContext.request.contextPath}/admin/retention">보유기간·파기</a></sec:authorize>
<sec:authorize access="hasAuthority('STATISTICS_VIEW')">    <a class="${fn:contains(adminPath, '/admin/statistics') ? 'active' : ''}" href="${pageContext.request.contextPath}/admin/statistics">운영 통계</a></sec:authorize>
    <span class="nav-group">기준정보</span>
<sec:authorize access="hasAuthority('CONSENT_TEMPLATE_VIEW')">    <a class="${fn:contains(adminPath, '/admin/templates') ? 'active' : ''}" href="${pageContext.request.contextPath}/admin/templates">동의서 템플릿</a></sec:authorize>
<sec:authorize access="hasAuthority('PROCESSING_ACTIVITY_VIEW')">    <a class="${fn:contains(adminPath, '/admin/processing-activities') ? 'active' : ''}" href="${pageContext.request.contextPath}/admin/processing-activities">처리업무</a></sec:authorize>
<sec:authorize access="hasAuthority('RETENTION_POLICY_VIEW')">    <a class="${fn:contains(adminPath, '/admin/retention-policies') ? 'active' : ''}" href="${pageContext.request.contextPath}/admin/retention-policies">보유정책</a></sec:authorize>
    <span class="nav-group">시스템 관리</span>
<sec:authorize access="hasAuthority('ORGANIZATION_VIEW')">    <a class="${fn:contains(adminPath, '/admin/organizations') ? 'active' : ''}" href="${pageContext.request.contextPath}/admin/organizations">조직 관리</a></sec:authorize>
<sec:authorize access="hasAuthority('USER_VIEW')">    <a class="${fn:contains(adminPath, '/admin/users') ? 'active' : ''}" href="${pageContext.request.contextPath}/admin/users">관리자 계정</a></sec:authorize>
<sec:authorize access="hasAuthority('ROLE_VIEW')">    <a class="${fn:contains(adminPath, '/admin/roles') ? 'active' : ''}" href="${pageContext.request.contextPath}/admin/roles">역할·권한</a></sec:authorize>
<sec:authorize access="hasAuthority('AUDIT_VIEW')">    <a class="${fn:contains(adminPath, '/admin/audit-events') ? 'active' : ''}" href="${pageContext.request.contextPath}/admin/audit-events">감사 이력</a></sec:authorize>
</nav>
