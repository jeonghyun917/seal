<%@ taglib prefix="seal" uri="https://arte.or.kr/seal/functions" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<!doctype html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>운영 통계</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/app.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/admin-data.css?v=20260908-1">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/statistics.css?v=20260908-3">
    <script defer src="${pageContext.request.contextPath}/resources/js/statistics.js?v=20260828-2"></script>
</head>
<body class="admin-body">
<jsp:include page="/WEB-INF/jsp/fragments/admin/header.jsp"/>
<div class="admin-layout">
    <jsp:include page="/WEB-INF/jsp/fragments/admin/sidebar.jsp"/>
    <main class="admin-main">
        <div class="page-heading-row">
            <div>
                <p class="section-kicker">OPERATION STATISTICS</p>
                <h1>운영 통계</h1>
                <p>조회 기간의 상호배타적 업무 상태와 별도 경고 지표를 확인합니다. 개인 식별정보는 포함하지 않습니다.</p>
            </div>
        </div>
        <section class="work-panel statistics-panel">
            <form method="get" class="list-filter statistics-filter">
                <label for="from"><span>시작일</span><input id="from" type="date" name="from" value="${from}"></label>
                <label for="to"><span>종료일</span><input id="to" type="date" name="to" value="${to}"></label>
                <button class="secondary-button">조회</button>
                <sec:authorize access="hasAuthority('STATISTICS_VIEW') and hasAuthority('CONSENT_REQUEST_VIEW') and hasAuthority('RECIPIENT_VIEW') and hasAuthority('MAIL_VIEW') and hasAuthority('EVIDENCE_VIEW') and hasAuthority('RIGHTS_VIEW') and hasAuthority('RETENTION_VIEW')">
                    <button class="secondary-button excel-download-button statistics-export-button" type="submit" data-download formaction="${pageContext.request.contextPath}/admin/statistics/export">
                        <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false">
                            <path d="M14 3H5a1 1 0 0 0-1 1v16a1 1 0 0 0 1 1h10M14 3l5 5h-5V3Z"/>
                            <path d="M7 10h4M7 14h4M7 18h4M19 12v8m-3-3 3 3 3-3"/>
                        </svg>
                        <span>엑셀 다운로드</span>
                    </button>
                </sec:authorize>
            </form>
            <p class="statistics-period-note"><strong>기간 기준</strong> 요청·메일·증적·권리요청·파기는 각 업무의 생성 또는 요청 시각,
                응답은 제출 시각을 기준으로 집계합니다. 철회·증적 검증은 해당 사건 발생 시각 기준이며, 종료일 전체를 포함합니다.</p>
            <c:set var="groups" value="${[statistics.requests(),statistics.responses(),statistics.mail(),statistics.evidence(),statistics.rights(),statistics.retention()]}"/>
            <c:set var="titles" value="${['동의 요청','동의 응답·철회','메일','증적','권리요청','파기·보존 보류']}"/>
            <c:set var="descriptions" value="${['요청 생성부터 완료까지의 현재 상태','제출·철회된 동의 응답 현황','발송 대기와 SMTP 처리 현황','증적 패키지 생성 및 검증 현황','정보주체 권리요청 처리 현황','파기 작업과 보존 보류 현황']}"/>
            <c:set var="statusDomains" value="${['request','response','mail','evidence','rights','retention']}"/>
            <c:set var="allStatusGroups" value="${[
                ['DRAFT','READY','ACTIVE','COMPLETED','EXPIRED','CANCELLED'],
                ['SUBMITTED','DECLINED_ALL'],
                ['QUEUED','PROCESSING','SMTP_ACCEPTED','RETRY_SCHEDULED','SEND_FAILED','CANCELLED','DESTROYED'],
                ['QUEUED','PROCESSING','FINALIZED','FAILED','DESTROYED'],
                ['RECEIVED','IDENTITY_VERIFIED','IN_REVIEW','APPROVED','REJECTED','COMPLETED','CANCELLED'],
                ['DUE','PENDING_APPROVAL','APPROVED','EXECUTING','EXECUTED','CONFIRMED','FAILED','HELD','CANCELLED']
            ]}"/>
            <div class="statistics-grid">
                <c:forEach var="group" items="${groups}" varStatus="loop">
                    <c:set var="groupTotal" value="0"/>
                    <c:forEach var="row" items="${group}"><c:set var="groupTotal" value="${groupTotal + row.count()}"/></c:forEach>
                    <article class="statistics-card">
                        <div class="statistics-card-heading">
                            <div><h2><c:out value="${titles[loop.index]}"/></h2><p><c:out value="${descriptions[loop.index]}"/></p></div>
                            <span class="statistics-total-badge"><c:out value="${groupTotal}"/>건</span>
                        </div>
                        <div class="statistics-chart-layout" data-statistics-chart data-chart-title="${titles[loop.index]}">
                            <div class="statistics-donut-wrap">
                                <svg class="statistics-donut" viewBox="0 0 120 120" role="img" aria-label="${titles[loop.index]} 상태 분포"><circle class="statistics-donut-base" cx="60" cy="60" r="44" pathLength="100"/></svg>
                                <div class="statistics-donut-total"><strong><c:out value="${groupTotal}"/></strong><span>총 건수</span></div>
                            </div>
                            <ul class="statistics-legend">
                                <c:forEach var="statusCode" items="${allStatusGroups[loop.index]}" varStatus="rowLoop">
                                    <c:set var="statusCount" value="0"/>
                                    <c:forEach var="row" items="${group}">
                                        <c:if test="${row.status() == statusCode or (loop.index == 4 and fn:endsWith(row.status(), statusCode))}"><c:set var="statusCount" value="${statusCount + row.count()}"/></c:if>
                                    </c:forEach>
                                    <li data-count="${statusCount}" data-label="${seal:statusLabel(statusDomains[loop.index], statusCode)}">
                                        <span class="statistics-swatch chart-color-${rowLoop.index % 10}"></span>
                                        <span class="statistics-legend-label"><c:out value="${seal:statusLabel(statusDomains[loop.index], statusCode)}"/></span>
                                        <strong><c:out value="${statusCount}"/>건</strong>
                                    </li>
                                </c:forEach>
                            </ul>
                        </div>
                    </article>
                </c:forEach>
            </div>
            <c:set var="warningGroups" value="${[statistics.responseWarnings(),statistics.evidenceWarnings(),statistics.rightsWarnings(),statistics.retentionWarnings()]}"/>
            <c:set var="warningTitles" value="${['응답 철회','증적 검증 경고','권리요청 기한 경고','보존 보류']}"/>
            <c:set var="warningDomains" value="${['response','evidence','rights','retention']}"/>
            <section class="statistics-warning-section" aria-labelledby="statistics-warning-title">
                <div class="statistics-section-heading">
                    <h2 id="statistics-warning-title">중복 가능한 경고 지표</h2>
                    <p>아래 건수는 상태별 총계에 더하지 않습니다. 같은 업무 건이 현재 상태와 경고에 각각 포함될 수 있습니다.</p>
                </div>
                <div class="statistics-warning-grid">
                    <c:forEach var="warningGroup" items="${warningGroups}" varStatus="warningLoop">
                        <c:set var="warningTotal" value="0"/>
                        <c:forEach var="row" items="${warningGroup}"><c:set var="warningTotal" value="${warningTotal + row.count()}"/></c:forEach>
                        <article class="statistics-warning-card">
                            <div><h3><c:out value="${warningTitles[warningLoop.index]}"/></h3><strong><c:out value="${warningTotal}"/>건</strong></div>
                            <ul>
                                <c:forEach var="row" items="${warningGroup}">
                                    <li><span><c:out value="${seal:statusLabel(warningDomains[warningLoop.index], row.status())}"/></span><strong><c:out value="${row.count()}"/>건</strong></li>
                                </c:forEach>
                                <c:if test="${empty warningGroup}"><li class="statistics-empty">해당 경고 없음</li></c:if>
                            </ul>
                        </article>
                    </c:forEach>
                </div>
            </section>
        </section>
    </main>
</div>
</body>
</html>
