<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %><%@ taglib prefix="c" uri="jakarta.tags.core" %><%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<!doctype html>
<html lang="ko">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width,initial-scale=1">
  <title>파기 작업 상세</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/app.css">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/admin-data.css">
</head>
<body class="admin-body">
<jsp:include page="/WEB-INF/jsp/fragments/admin/header.jsp"/>
<div class="admin-layout">
  <jsp:include page="/WEB-INF/jsp/fragments/admin/sidebar.jsp"/>
  <main class="admin-main">
    <div class="page-heading-row">
      <div><p class="section-kicker">RETENTION JOB</p><h1>파기 작업 상세</h1><p><c:out value="${item.id()}"/></p></div>
      <a class="secondary-button" href="${pageContext.request.contextPath}/admin/retention">목록</a>
    </div>
    <p class="alert">파기 범위: 운영 주 DB의 메일 본문·연락처·완료 세션 흐름값·종료 권리요청 메모와 주 증적 저장소. 백업·물리매체 파기는 운영 절차에서 별도 수행합니다.</p>
    <section class="work-panel">
      <dl>
        <dt>상태</dt><dd><c:out value="${item.status()}"/></dd>
        <dt>기산일 / 예정일</dt><dd><c:out value="${item.anchorDate()}"/> / <c:out value="${item.dueDate()}"/></dd>
        <dt>요청자 / 승인자</dt><dd><c:out value="${item.requestedBy()}"/> / <c:out value="${item.approvedBy()}"/></dd>
        <dt>실행자 / 확인자</dt><dd><c:out value="${item.executedBy()}"/> / <c:out value="${item.confirmedBy()}"/></dd>
        <dt>처리 건수</dt><dd>메일 <c:out value="${item.destroyedMailCount()}"/> · 파일 <c:out value="${item.destroyedFileCount()}"/> · 세션 <c:out value="${item.destroyedSessionCount()}"/> · 권리요청 <c:out value="${item.destroyedRightsCount()}"/> · 대상자 PII <c:out value="${item.recipientPiiDestroyed()}"/></dd>
      </dl>
      <c:if test="${item.status() == 'DUE'}"><sec:authorize access="hasAuthority('RETENTION_MANAGE')"><form method="post" action="${pageContext.request.contextPath}/admin/retention/${item.id()}/submit"><input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"><button class="primary-button">승인 요청</button></form></sec:authorize></c:if>
      <c:if test="${item.status() == 'PENDING_APPROVAL'}"><sec:authorize access="hasAuthority('RETENTION_APPROVE')"><form method="post" action="${pageContext.request.contextPath}/admin/retention/${item.id()}/approve"><input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"><button class="primary-button">파기 승인</button></form></sec:authorize></c:if>
      <c:if test="${item.executionAvailable()}"><sec:authorize access="hasAuthority('RETENTION_EXECUTE')"><form method="post" action="${pageContext.request.contextPath}/admin/retention/${item.id()}/execute"><input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"><input type="hidden" name="idempotencyKey" value="${item.executionKey()}"><button class="primary-button"><c:choose><c:when test="${item.status() == 'EXECUTING'}">중단된 주 저장소 파기 재개</c:when><c:otherwise>주 저장소 파기 실행</c:otherwise></c:choose></button></form></sec:authorize></c:if>
      <c:if test="${item.status() == 'EXECUTED'}"><sec:authorize access="hasAuthority('RETENTION_CONFIRM')"><form method="post" action="${pageContext.request.contextPath}/admin/retention/${item.id()}/confirm"><input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"><button class="primary-button">결과 확인</button></form></sec:authorize></c:if>
    </section>
  </main>
</div>
</body>
</html>
