<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %><%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>동의 요청 확정 내용</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/app.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/recipient-request.css?v=20260827-3">
</head>
<body class="admin-body">
<jsp:include page="/WEB-INF/jsp/fragments/admin/header.jsp"/>
<div class="admin-layout">
    <jsp:include page="/WEB-INF/jsp/fragments/admin/sidebar.jsp"/>
    <main class="admin-main snapshot-page">
        <div class="page-heading-row">
            <div>
                <p class="section-kicker">IMMUTABLE SNAPSHOT</p>
                <h1>동의 요청 확정 내용</h1>
                <p>확정 시점에 정보주체에게 제공되는 동의 내용을 확인합니다.</p>
            </div>
            <a class="secondary-button" href="${pageContext.request.contextPath}/admin/requests/${requestId}">동의 요청 상세</a>
        </div>

        <section class="work-panel snapshot-document">
            <header class="snapshot-document-head">
                <span><c:out value="${snapshotView.institution()}"/></span>
                <h2><c:out value="${snapshotView.title()}"/></h2>
                <p>요청번호 <strong><c:out value="${snapshotView.requestCode()}"/></strong></p>
            </header>

            <div class="snapshot-introduction"><c:out value="${snapshotView.introductionHtml()}" escapeXml="false"/></div>

            <dl class="snapshot-summary">
                <div><dt>개인정보 이용 업무</dt><dd><c:out value="${snapshotView.processingActivityName()}"/></dd></div>
                <div><dt>응답 만료일시</dt><dd><c:out value="${snapshotView.expiresAtDisplay()}"/></dd></div>
                <div><dt>개인정보 이용 목적</dt><dd><c:out value="${snapshotView.processingPurpose()}"/></dd></div>
                <div><dt>보유기간</dt><dd><c:out value="${snapshotView.retentionDisplayText()}"/></dd></div>
            </dl>

            <div class="snapshot-section-heading">
                <h3>동의 결정 항목</h3>
                <p>각 항목은 확정 이후 변경되지 않습니다.</p>
            </div>
            <div class="snapshot-items">
                <c:forEach var="item" items="${snapshotView.items()}">
                    <article class="snapshot-item">
                        <header>
                            <div class="snapshot-badges">
                                <span class="snapshot-type"><c:out value="${item.typeLabel()}"/></span>
                                <span class="snapshot-required ${item.required() ? 'is-required' : ''}">${item.required() ? '필수' : '선택'}</span>
                            </div>
                            <h4><c:out value="${item.title()}"/></h4>
                            <small>항목 코드 <c:out value="${item.code()}"/></small>
                        </header>
                        <dl>
                            <div><dt>개인정보 항목</dt><dd><c:forEach var="name" items="${item.dataItemNames()}" varStatus="status"><c:if test="${!status.first}">, </c:if><c:out value="${name}"/></c:forEach></dd></div>
                            <div><dt>개인정보 이용 목적</dt><dd><c:out value="${item.purpose()}"/></dd></div>
                            <div><dt>보유기간</dt><dd><c:out value="${item.retentionText()}"/></dd></div>
                            <c:if test="${not empty item.thirdPartyRecipient()}"><div><dt>제공받는 자</dt><dd><c:out value="${item.thirdPartyRecipient()}"/></dd></div></c:if>
                            <div><dt>동의 거부 안내</dt><dd><c:out value="${item.refusalNotice()}"/></dd></div>
                        </dl>
                    </article>
                </c:forEach>
            </div>

            <div class="snapshot-withdrawal">
                <strong>동의 철회 안내</strong>
                <p><c:out value="${snapshotView.withdrawalNotice()}"/></p>
            </div>
        </section>

        <details class="work-panel snapshot-technical">
            <summary>기술정보 및 원본 JSON 보기</summary>
            <dl>
                <div><dt>스냅샷 SHA-256</dt><dd><c:out value="${snapshotView.snapshotHash()}"/></dd></div>
                <div><dt>동의서 양식 내용 해시</dt><dd><c:out value="${snapshotView.templateContentHash()}"/></dd></div>
                <div><dt>동의서 양식 버전 ID</dt><dd><c:out value="${snapshotView.templateVersionId()}"/></dd></div>
                <div><dt>개인정보 이용 업무 버전 ID</dt><dd><c:out value="${snapshotView.processingActivityVersionId()}"/></dd></div>
                <div><dt>보유정책 버전 ID</dt><dd><c:out value="${snapshotView.retentionPolicyVersionId()}"/></dd></div>
            </dl>
            <h3>원본 JSON</h3>
            <pre><c:out value="${snapshotJson}"/></pre>
        </details>
    </main>
</div>
</body>
</html>
