<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<!doctype html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>보유기간·파기</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/app.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/admin-data.css">
</head>
<body class="admin-body"><jsp:include page="/WEB-INF/jsp/fragments/admin/header.jsp"/>
<div class="admin-layout"><jsp:include page="/WEB-INF/jsp/fragments/admin/sidebar.jsp"/><main class="admin-main">
    <div class="page-heading-row">
        <div><p class="section-kicker">RETENTION &amp; DESTRUCTION</p><h1>보유기간·파기 작업</h1>
            <p>보존 보류와 이중 통제를 확인한 뒤 주 저장소 범위만 파기합니다.</p></div>
        <a class="secondary-button" href="${pageContext.request.contextPath}/admin">대시보드</a>
    </div>

    <sec:authorize access="hasAuthority('RETENTION_MANAGE')">
        <section class="admin-grid">
            <article class="work-panel">
                <h2>수동 기산일 등록·정정</h2>
                <p>기존 기산일은 삭제하지 않고 비활성화한 뒤 새 버전으로 보존합니다.</p>
                <form method="post" action="${pageContext.request.contextPath}/admin/retention/anchors">
                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                    <label>동의요청 UUID<input required name="consentRequestId" autocomplete="off"></label>
                    <label>기산 이벤트<select name="anchorType"><option value="PROGRAM_END_AT">프로그램 종료일</option><option value="PURPOSE_ACHIEVED_AT">목적 달성일</option></select></label>
                    <label>기산일<input required type="date" name="anchorDate"></label>
                    <button class="primary-button">기산일 저장</button>
                </form>
            </article>
            <article class="work-panel">
                <h2>보존 보류 설정</h2>
                <p>감사·분쟁 등 파기를 중단해야 하는 근거와 만료일을 기록합니다.</p>
                <form method="post" action="${pageContext.request.contextPath}/admin/retention/holds">
                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                    <label>동의요청 UUID<input required name="consentRequestId" autocomplete="off"></label>
                    <label>사유 코드<input required maxlength="100" name="reasonCode" placeholder="AUDIT"></label>
                    <label>근거 설명<textarea required maxlength="1000" name="reasonText"></textarea></label>
                    <label>만료일(선택)<input type="date" name="expiresOn"></label>
                    <button class="primary-button">보존 보류 설정</button>
                </form>
            </article>
        </section>
    </sec:authorize>

    <section class="work-panel">
        <h2>활성 보존 보류</h2>
        <div class="table-scroll"><table class="data-table"><thead><tr><th>동의요청</th><th>사유</th><th>만료일</th><th>설정시각</th><th>관리</th></tr></thead><tbody>
        <c:forEach var="hold" items="${activeHolds}"><tr><td><c:out value="${hold.consentRequestId()}"/></td><td><c:out value="${hold.reasonCode()}"/></td><td><c:out value="${hold.expiresOn()}"/></td><td><c:out value="${hold.heldAt()}"/></td><td><sec:authorize access="hasAuthority('RETENTION_MANAGE')"><form method="post" action="${pageContext.request.contextPath}/admin/retention/holds/${hold.id()}/release"><input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"><button class="secondary-button">해제</button></form></sec:authorize></td></tr></c:forEach>
        <c:if test="${empty activeHolds}"><tr><td colspan="5">활성 보존 보류가 없습니다.</td></tr></c:if>
        </tbody></table></div>
    </section>

    <section class="work-panel">
        <form method="get"><label>상태<select name="status"><option value="">전체</option><c:forEach var="s" items="${['DUE','PENDING_APPROVAL','APPROVED','EXECUTING','EXECUTED','CONFIRMED','FAILED','HELD','CANCELLED']}"><option value="${s}" ${selectedStatus == s ? 'selected' : ''}>${s}</option></c:forEach></select></label><button class="secondary-button">조회</button></form>
        <div class="table-scroll"><table class="data-table"><thead><tr><th>요청 ID</th><th>상태</th><th>예정일</th><th>방식</th><th>등록시각</th></tr></thead><tbody>
        <c:forEach var="item" items="${items}"><tr><td><a href="${pageContext.request.contextPath}/admin/retention/${item.id()}"><c:out value="${item.consentRequestId()}"/></a></td><td><c:out value="${item.status()}"/></td><td><c:out value="${item.dueDate()}"/></td><td><c:out value="${item.destructionMethod()}"/></td><td><c:out value="${item.requestedAt()}"/></td></tr></c:forEach>
        <c:if test="${empty items}"><tr><td colspan="5">파기 작업이 없습니다.</td></tr></c:if>
        </tbody></table></div>
    <jsp:include page="/WEB-INF/jsp/fragments/admin/pagination.jsp"/></section>
</main></div></body>
</html>
