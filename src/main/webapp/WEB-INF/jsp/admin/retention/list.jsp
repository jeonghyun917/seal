<%@ taglib prefix="seal" uri="https://arte.or.kr/seal/functions" %>
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
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/admin-data.css?v=20260906-3">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/retention.css?v=20260901-1">
<script defer src="${pageContext.request.contextPath}/resources/js/request-options.js?v=20260906-1"></script></head>
<body class="admin-body">
<jsp:include page="/WEB-INF/jsp/fragments/admin/header.jsp"/>
<div class="admin-layout">
    <jsp:include page="/WEB-INF/jsp/fragments/admin/sidebar.jsp"/>
    <main class="admin-main">
        <div class="page-heading-row">
            <div>
                <p class="section-kicker">RETENTION &amp; DESTRUCTION</p>
                <h1>보유기간·파기 작업</h1>
                <p>파기 예정일이 도래한 작업을 승인하고 실행 결과를 관리합니다.</p>
            </div>
            <a class="secondary-button" href="${pageContext.request.contextPath}/admin">대시보드</a>
        </div>

        <section class="work-panel retention-jobs-panel">
            <div class="retention-section-heading">
                <div>
                    <h2>수신자별 파기 작업 목록</h2>
                    <p>수신자별 파기 예정일이 도래하면 시스템이 자동으로 파기 대상을 등록합니다.</p>
                </div>
                <form method="get" class="list-filter">
                    <label>
                        <span>상태</span>
                        <select name="status">
                            <option value="">전체</option>
                            <c:forEach var="s" items="${['DUE','PENDING_APPROVAL','APPROVED','EXECUTING','EXECUTED','CONFIRMED','FAILED','HELD','CANCELLED']}">
                                <option value="${s}" ${selectedStatus == s ? 'selected' : ''}>
                                    <c:out value="${seal:statusLabel('retention', s)}"/>
                                </option>
                            </c:forEach>
                        </select>
                    </label>
                    <button class="secondary-button">조회</button>
                </form>
            </div>
            <div class="table-scroll">
                <table class="data-table">
                    <thead>
                    <tr><th>수신자</th><th>동의 요청</th><th>시작 기준일</th><th>파기 예정일</th><th>상태</th><th>파기 방식</th></tr>
                    </thead>
                    <tbody>
                    <c:forEach var="item" items="${items}">
                        <tr>
                            <td><a href="${pageContext.request.contextPath}/admin/recipients/${item.recipientId()}"><c:out value="${recipientLabels[item.recipientId()]}"/></a></td>
                            <td><a href="${pageContext.request.contextPath}/admin/retention/${item.id()}"><c:out value="${item.requestCode()}"/></a></td>
                            <td><c:out value="${item.anchorDate()}"/></td>
                            <td><c:out value="${item.dueDate()}"/></td>
                            <td><c:out value="${seal:statusLabel('retention', item.status())}"/></td>
                            <td><c:out value="${seal:statusLabel('destruction', item.destructionMethod())}"/></td>
                        </tr>
                    </c:forEach>
                    <c:if test="${empty items}">
                        <tr><td colspan="6" class="retention-empty"><c:choose><c:when test="${not empty selectedStatus}">선택한 상태의 파기 작업이 없습니다.</c:when><c:otherwise>아직 생성된 파기 작업이 없습니다. 파기 예정일이 도래하면 시스템이 자동으로 등록합니다.</c:otherwise></c:choose></td></tr>
                    </c:if>
                    </tbody>
                </table>
            </div>
            <jsp:include page="/WEB-INF/jsp/fragments/admin/pagination.jsp"/>
        </section>

        <details class="work-panel retention-advanced">
            <summary>
                <span class="retention-advanced-icon" aria-hidden="true"></span>
                <span>
                    <strong>고급 관리</strong>
                    <small>기존 정책의 기준일 보정과 감사·분쟁에 따른 보존 보류를 관리합니다.</small>
                </span>
            </summary>
            <div class="retention-advanced-content">
                <sec:authorize access="hasAuthority('RETENTION_MANAGE')">
                    <section class="admin-grid">
                        <article class="retention-subpanel">
                            <h2>기존 정책 기준일 보정</h2>
                            <p>과거 정책에서 프로그램 종료일 또는 개인정보 처리 목적 달성일을 사용하는 경우에만 입력합니다. 새 정책에는 사용하지 않습니다.</p>
                            <form class="retention-form retention-anchor-form" method="post" action="${pageContext.request.contextPath}/admin/retention/anchors">
                                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                                <div class="retention-field retention-field-wide" data-request-picker data-options-url="${pageContext.request.contextPath}/admin/retention/request-options">
  <label>동의 요청 검색<input type="search" placeholder="요청번호 또는 동의서명 입력" aria-label="동의 요청 검색"></label>
  <div class="form-actions"><button type="button" class="secondary-button" data-request-search>검색</button><button type="button" class="text-button" data-request-prev>이전</button><button type="button" class="text-button" data-request-next>다음</button></div>
  <label>동의 요청<select name="consentRequestId" required><option value="">검색 후 동의 요청을 선택해 주세요</option></select></label>
  <p class="field-help" data-request-status aria-live="polite">조회 가능한 조직의 요청을 20건씩 표시합니다.</p>
</div>
                                <label class="retention-field">시작 기준<select name="anchorType"><option value="PROGRAM_END_AT">프로그램 종료일</option><option value="PURPOSE_ACHIEVED_AT">개인정보 처리 목적 달성일</option></select></label>
                                <label class="retention-field">기준일<input required type="date" name="anchorDate"></label>
                                <div class="retention-actions"><button class="primary-button">기준일 저장</button></div>
                            </form>
                        </article>
                        <article class="retention-subpanel">
                            <h2>보존 보류 설정</h2>
                            <p>감사·분쟁 등 파기를 중단해야 하는 근거와 만료일을 기록합니다.</p>
                            <form class="retention-form retention-hold-form" method="post" action="${pageContext.request.contextPath}/admin/retention/holds">
                                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                                <div class="retention-field retention-field-wide" data-request-picker data-options-url="${pageContext.request.contextPath}/admin/retention/request-options">
  <label>동의 요청 검색<input type="search" placeholder="요청번호 또는 동의서명 입력" aria-label="동의 요청 검색"></label>
  <div class="form-actions"><button type="button" class="secondary-button" data-request-search>검색</button><button type="button" class="text-button" data-request-prev>이전</button><button type="button" class="text-button" data-request-next>다음</button></div>
  <label>동의 요청<select name="consentRequestId" required><option value="">검색 후 동의 요청을 선택해 주세요</option></select></label>
  <p class="field-help" data-request-status aria-live="polite">조회 가능한 조직의 요청을 20건씩 표시합니다.</p>
</div>
                                <label class="retention-field">보존 보류 사유<select name="reasonCode" required><option value="">사유 선택</option><option value="AUDIT">감사</option><option value="DISPUTE">분쟁</option><option value="LITIGATION">소송</option><option value="OTHER">기타</option></select></label>
                                <label class="retention-field retention-field-wide">근거 설명<textarea required maxlength="1000" name="reasonText" rows="3" placeholder="보존 보류가 필요한 구체적인 사유와 근거를 입력해 주세요."></textarea></label>
                                <label class="retention-field">만료일 <span class="optional-text">(선택)</span><input type="date" name="expiresOn"></label>
                                <div class="retention-actions"><button class="primary-button">보존 보류 설정</button></div>
                            </form>
                        </article>
                    </section>
                </sec:authorize>

                <section class="retention-holds">
                    <h2>활성 보존 보류</h2>
                    <div class="table-scroll">
                        <table class="data-table">
                            <thead><tr><th>동의요청</th><th>사유</th><th>만료일</th><th>설정시각</th><th>관리</th></tr></thead>
                            <tbody>
                            <c:forEach var="hold" items="${activeHolds}">
                                <tr>
                                    <td><a href="${pageContext.request.contextPath}/admin/requests/${hold.consentRequestId()}"><c:out value="${holdRequestLabels[hold.consentRequestId()]}"/></a></td>
                                    <td><c:out value="${seal:statusLabel('hold-reason', hold.reasonCode())}"/></td>
                                    <td><c:out value="${empty hold.expiresOn() ? '별도 해제 시까지' : hold.expiresOn()}"/></td>
                                    <td><c:out value="${empty hold.heldAt() ? '-' : seal:dateTime(hold.heldAt())}"/></td>
                                    <td>
                                        <sec:authorize access="hasAuthority('RETENTION_MANAGE')">
                                            <form method="post" data-confirm-title="보존 보류 해제" data-confirm-message="보존 보류를 해제하면 해당 요청이 다시 파기 대상이 될 수 있습니다. 근거를 확인한 후 해제해 주세요." action="${pageContext.request.contextPath}/admin/retention/holds/${hold.id()}/release">
                                                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                                                <button class="secondary-button">해제</button>
                                            </form>
                                        </sec:authorize>
                                    </td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty activeHolds}">
                                <tr><td colspan="5">활성 보존 보류가 없습니다.</td></tr>
                            </c:if>
                            </tbody>
                        </table>
                    </div>
                </section>
            </div>
        </details>
    </main>
</div>
</body>
</html>
