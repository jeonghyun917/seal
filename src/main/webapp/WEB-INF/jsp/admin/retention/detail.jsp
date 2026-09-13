<%@ taglib prefix="seal" uri="https://arte.or.kr/seal/functions" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<!doctype html><html lang="ko"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>파기 작업 상세</title><link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/app.css"><link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/admin-data.css?v=20260906-3"></head>
<body class="admin-body"><jsp:include page="/WEB-INF/jsp/fragments/admin/header.jsp"/><div class="admin-layout"><jsp:include page="/WEB-INF/jsp/fragments/admin/sidebar.jsp"/><main class="admin-main">
<div class="page-heading-row"><div><p class="section-kicker">RETENTION JOB</p><h1>파기 작업 상세</h1></div><a class="secondary-button" href="${pageContext.request.contextPath}/admin/retention">목록</a></div>
<p class="alert">파기 범위: 운영 주 DB의 메일 본문·연락처·완료 세션 흐름값·종료 권리요청 메모와 주 증적 저장소. 백업·물리매체 파기는 운영 절차에서 별도 수행합니다.</p>
<section class="work-panel">
<dl class="detail-list">
<div><dt>파기 대상</dt><dd><c:out value="${recipient.maskedName()}"/> · <c:out value="${recipient.maskedEmail()}"/> / <a href="${pageContext.request.contextPath}/admin/requests/${consentRequest.requestId()}"><c:out value="${consentRequest.requestCode()}"/></a></dd></div>
<div><dt>동의서</dt><dd><c:out value="${empty requestTemplate ? '동의서 정보 확인 불가' : requestTemplate.templateName()}"/></dd></div>
<div><dt>상태</dt><dd><c:out value="${seal:statusLabel('retention', item.status())}"/></dd></div>
<div><dt>파기 방법</dt><dd><c:out value="${seal:statusLabel('destruction', item.destructionMethod())}"/></dd></div>
<div><dt>기산일 / 예정일</dt><dd><c:out value="${item.anchorDate()}"/> / <c:out value="${item.dueDate()}"/></dd></div>
<div><dt>요청자 / 승인자</dt><dd><c:out value="${empty actorNames.requestedBy() ? '미지정' : actorNames.requestedBy()}"/> / <c:out value="${empty actorNames.approvedBy() ? '미지정' : actorNames.approvedBy()}"/></dd></div>
<div><dt>실행자 / 확인자</dt><dd><c:out value="${empty actorNames.executedBy() ? '미지정' : actorNames.executedBy()}"/> / <c:out value="${empty actorNames.confirmedBy() ? '미지정' : actorNames.confirmedBy()}"/></dd></div>
<div><dt>파기 결과</dt><dd><c:choose><c:when test="${item.status() == 'EXECUTED' or item.status() == 'CONFIRMED'}">메일 <c:out value="${item.destroyedMailCount()}"/>건 · 파일 <c:out value="${item.destroyedFileCount()}"/>건 · 공개 세션 <c:out value="${item.destroyedSessionCount()}"/>건 · 종료 권리요청 <c:out value="${item.destroyedRightsCount()}"/>건 · 대상자 개인정보 <c:out value="${item.recipientPiiDestroyed() ? '파기됨' : '공유 요청으로 보존'}"/></c:when><c:otherwise>아직 실행되지 않았습니다.</c:otherwise></c:choose></dd></div>
</dl>
<details><summary>작업 식별자 보기</summary><p><c:out value="${item.id()}"/></p></details>
<c:set var="confirmMessage">대상자 <c:out value="${recipient.maskedName()}"/> (<c:out value="${recipient.maskedEmail()}"/>), 요청 <c:out value="${consentRequest.requestCode()}"/>의 개인정보와 증적 파일을 되돌릴 수 없게 파기합니다. 계속하시겠습니까?</c:set>
<c:if test="${item.status() == 'DUE'}"><sec:authorize access="hasAuthority('RETENTION_MANAGE')"><form method="post" action="${pageContext.request.contextPath}/admin/retention/${item.id()}/submit"><input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"><button class="primary-button">승인 요청</button></form></sec:authorize></c:if>
<c:if test="${item.status() == 'PENDING_APPROVAL'}"><sec:authorize access="hasAuthority('RETENTION_APPROVE')"><form method="post" action="${pageContext.request.contextPath}/admin/retention/${item.id()}/approve"><input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"><button class="primary-button">파기 승인</button></form></sec:authorize></c:if>
<c:if test="${item.executionAvailable()}"><sec:authorize access="hasAuthority('RETENTION_EXECUTE')"><form method="post" data-confirm-title="주 저장소 파기 실행" data-confirm-message="${confirmMessage}" action="${pageContext.request.contextPath}/admin/retention/${item.id()}/execute"><input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"><input type="hidden" name="idempotencyKey" value="${item.executionKey()}"><button class="primary-button"><c:choose><c:when test="${item.status() == 'EXECUTING'}">중단된 주 저장소 파기 재개</c:when><c:otherwise>주 저장소 파기 실행</c:otherwise></c:choose></button></form></sec:authorize></c:if>
<c:if test="${item.status() == 'EXECUTED'}"><sec:authorize access="hasAuthority('RETENTION_CONFIRM')"><form method="post" data-confirm-title="파기 결과 확인" data-confirm-message="파기 결과와 담당자를 확인했습니까? 확인 후 작업이 최종 완료됩니다." action="${pageContext.request.contextPath}/admin/retention/${item.id()}/confirm"><input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"><button class="primary-button">결과 확인</button></form></sec:authorize></c:if>
</section></main></div></body></html>
