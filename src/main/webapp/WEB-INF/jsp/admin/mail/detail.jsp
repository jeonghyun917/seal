<%@ taglib prefix="seal" uri="https://arte.or.kr/seal/functions" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>메일 발송 상세</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/app.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/admin-data.css?v=20260906-3">
</head>
<body class="admin-body">
<jsp:include page="/WEB-INF/jsp/fragments/admin/header.jsp"/>
<div class="admin-layout">
    <jsp:include page="/WEB-INF/jsp/fragments/admin/sidebar.jsp"/>
    <main class="admin-main">
        <div class="page-heading-row">
            <div>
                <p class="section-kicker">MAIL DETAIL</p>
                <h1><c:out value="${mailItem.subject()}"/></h1>
                <p><c:out value="${seal:statusLabel('mail', mailItem.status())}"/> · 시도 <c:out value="${mailItem.attemptCount()}"/>회</p>
            </div>
            <a class="secondary-button" href="${pageContext.request.contextPath}/admin/mail-outbox">목록</a>
        </div>

        <section class="work-panel">
            <dl class="detail-list">
                <div>
                    <dt>동의 요청</dt>
                    <dd>
                        <a href="${pageContext.request.contextPath}/admin/requests/${mailItem.requestId()}">
                            <c:out value="${requestCode}"/>
                        </a>
                    </dd>
                </div>
                <div>
                    <dt>수신자</dt>
                    <dd class="mail-recipient-summary">
                        <strong><c:out value="${recipient.maskedName()}"/> · <c:out value="${recipient.maskedEmail()}"/></strong>
                        <span>
                            <c:out value="${recipient.affiliationName()}"/>
                            <c:if test="${not empty recipient.jobTitle()}"> · <c:out value="${recipient.jobTitle()}"/></c:if>
                        </span>
                    </dd>
                </div>
                <c:if test="${not empty scheduleView}">
                    <div>
                        <dt><c:out value="${scheduleView.label()}"/></dt>
                        <dd><c:out value="${seal:dateTime(scheduleView.at())}"/></dd>
                    </div>
                </c:if>
            </dl>

            <c:choose>
                <c:when test="${mailItem.status() != 'DESTROYED'}">
                    <p><a class="primary-button" target="_blank" rel="noopener noreferrer" href="${pageContext.request.contextPath}/admin/mail-outbox/${mailItem.outboxId()}/preview">HTML 이메일 미리보기</a></p>
                </c:when>
                <c:otherwise>
                    <p class="alert">개인정보와 이메일 본문이 파기되었습니다.</p>
                </c:otherwise>
            </c:choose>

            <c:if test="${mailItem.status() == 'SEND_FAILED'}">
                <form method="post" action="${pageContext.request.contextPath}/admin/mail-outbox/${mailItem.outboxId()}/retry">
                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                    <button class="secondary-button">재시도 대기열 등록</button>
                </form>
            </c:if>
        </section>
    </main>
</div>
</body>
</html>
