<%@ taglib prefix="seal" uri="https://arte.or.kr/seal/functions" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>동의서 양식 버전 상세 | 개인정보 전자동의 관리시스템</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/app.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/admin-data.css?v=20260906-3">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/template.css?v=20260906-3">
</head>
<body class="admin-body">
<jsp:include page="/WEB-INF/jsp/fragments/admin/header.jsp"/>
<div class="admin-layout">
    <jsp:include page="/WEB-INF/jsp/fragments/admin/sidebar.jsp"/>
    <main class="admin-main">
        <div class="page-heading-row">
            <div>
                <p class="section-kicker">TEMPLATE VERSION DETAIL</p>
                <h1><c:out value="${detail.version().title()}"/></h1>
                <p><c:out value="${detail.templateCode()}"/> · v<c:out value="${detail.version().versionNumber()}"/></p>
            </div>
            <div class="form-actions">
                <a class="secondary-button" href="${pageContext.request.contextPath}/admin/templates/${detail.consentTemplateId()}">버전 이력</a>
                <a class="primary-button" href="${pageContext.request.contextPath}/admin/templates/${detail.consentTemplateId()}/versions/${detail.version().consentTemplateVersionId()}/preview">화면 미리보기</a>
            </div>
        </div>

        <section class="work-panel">
            <div class="version-detail-heading">
                <div>
                    <h2>버전 정보</h2>
                    <p>발행된 버전은 변경되지 않으며 실제 동의 요청에 이 내용이 사용됩니다.</p>
                </div>
                <div class="version-badges">
                    <span class="status-badge"><c:out value="${seal:statusLabel('workflow', detail.version().workflowStatus())}"/></span>
                    <c:if test="${detail.currentPublishedVersionId() == detail.version().consentTemplateVersionId()}">
                        <span class="current-version-badge">현재 사용 버전</span>
                    </c:if>
                </div>
            </div>
            <dl class="version-metadata">
                <div><dt>관리 명칭</dt><dd><c:out value="${detail.templateName()}"/></dd></div>
                <div><dt>개인정보 이용 업무</dt><dd><c:out value="${detail.processingActivityName()}"/></dd></div>
                <div><dt>동의서 양식 상태</dt><dd><c:choose><c:when test="${detail.templateStatus() == 'ACTIVE'}">사용</c:when><c:otherwise>중지</c:otherwise></c:choose></dd></div>
                <div><dt>동의 항목 수</dt><dd><c:out value="${preview.items().size()}"/>개</dd></div>
            </dl>
        </section>

        <section class="work-panel">
            <h2>안내문</h2>
            <div class="version-introduction">${preview.introductionHtml()}</div>
        </section>

        <section class="work-panel">
            <div class="panel-title-row">
                <div><h2>동의 결정 항목</h2><p>정보주체에게 표시되는 항목별 세부내용입니다.</p></div>
            </div>
            <div class="version-item-list">
                <c:forEach var="item" items="${preview.items()}">
                    <article class="version-item-card">
                        <div class="version-item-head">
                            <div>
                                <span class="item-order">항목 <c:out value="${item.order()}"/></span>
                                <h3><c:out value="${item.title()}"/></h3>
                                <p><c:out value="${item.code()}"/></p>
                            </div>
                            <div class="version-badges">
                                <span class="type-badge"><c:choose><c:when test="${item.type().name() == 'COLLECTION_USE'}">개인정보 수집·이용</c:when><c:when test="${item.type().name() == 'THIRD_PARTY_PROVISION'}">제3자 제공</c:when><c:otherwise>기타 선택 동의</c:otherwise></c:choose></span>
                                <span class="required-badge ${item.required() ? 'is-required' : ''}">${item.required() ? '필수' : '선택'}</span>
                            </div>
                        </div>
                        <dl class="version-item-details">
                            <div><dt>개인정보 항목</dt><dd><c:forEach var="name" items="${item.dataItemNames()}" varStatus="status"><c:if test="${!status.first}">, </c:if><c:out value="${name}"/></c:forEach></dd></div>
                            <div><dt>개인정보 이용 목적</dt><dd><c:out value="${item.purpose()}"/></dd></div>
                            <div><dt>보유기간</dt><dd><c:out value="${item.retentionText()}"/></dd></div>
                            <c:if test="${not empty item.thirdPartyRecipient()}"><div><dt>제공받는 자</dt><dd><c:out value="${item.thirdPartyRecipient()}"/></dd></div></c:if>
                            <div><dt>거부권 안내</dt><dd><c:out value="${item.refusalNotice()}"/></dd></div>
                        </dl>
                    </article>
                </c:forEach>
            </div>
        </section>
    </main>
</div>
</body>
</html>
