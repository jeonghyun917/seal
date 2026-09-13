<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="seal" uri="https://arte.or.kr/seal/functions" %>
<!doctype html><html lang="ko"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>감사 이력 | 개인정보 전자동의·증적 관리시스템</title><link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/app.css"><link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/admin-data.css?v=20260906-3"></head>
<body class="admin-body"><jsp:include page="/WEB-INF/jsp/fragments/admin/header.jsp"/>

<div class="admin-layout"><jsp:include page="/WEB-INF/jsp/fragments/admin/sidebar.jsp"/><main class="admin-main"><div class="page-heading-row"><div><p class="section-kicker">AUDIT TRAIL</p><h1>감사 이력</h1><p>조직 범위 안에서 최근 7일 동안 발생한 중요 이벤트를 조회합니다.</p></div><span class="status-badge">최대 200건</span></div>
<section class="work-panel" aria-labelledby="audit-list-title"><h2 id="audit-list-title">최근 이벤트</h2><div class="table-scroll"><table class="data-table"><thead><tr><th>발생시각</th><th>행위자</th><th>작업</th><th>대상</th><th>결과</th><th>사유</th></tr></thead><tbody>
<c:forEach var="event" items="${events}"><tr><td><c:out value="${seal:dateTime(event.occurredAt())}"/></td><td><c:out value="${event.actorDisplayName()}" default="${event.actorType()}"/></td><td><c:out value="${event.actionCode()}"/></td><td><c:out value="${event.targetType()}"/> <c:out value="${event.targetId()}"/></td><td><span class="state-text ${event.result() == 'SUCCESS' ? 'ready' : 'pending'}"><c:out value="${event.result()}"/></span></td><td><c:out value="${event.reasonCode()}" default="-"/></td></tr></c:forEach>
<c:if test="${empty events}"><tr><td colspan="6">조회 기간에 감사 이벤트가 없습니다.</td></tr></c:if></tbody></table></div></section></main></div></body></html>
