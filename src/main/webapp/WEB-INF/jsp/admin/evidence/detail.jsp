<%@ taglib prefix="seal" uri="https://arte.or.kr/seal/functions" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<!doctype html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>증적 상세</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/app.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/admin-data.css?v=20260908-1">
</head>
<body class="admin-body">
<jsp:include page="/WEB-INF/jsp/fragments/admin/header.jsp"/>
<div class="admin-layout">
    <jsp:include page="/WEB-INF/jsp/fragments/admin/sidebar.jsp"/>
    <main class="admin-main">
        <div class="page-heading-row">
            <div>
                <p class="section-kicker">EVIDENCE DETAIL</p>
                <h1>증적 패키지 상세</h1>
                <p>완료된 개인정보 동의의 생성 파일과 무결성 정보를 확인합니다.</p>
            </div>
            <a class="secondary-button" href="${pageContext.request.contextPath}/admin/evidence">목록</a>
        </div>

        <c:if test="${not empty verificationResult}">
            <p class="alert evidence-verification-result">무결성 검증 결과: <strong><c:out value="${verificationResult}"/></strong></p>
        </c:if>

        <section class="work-panel evidence-detail-panel">
            <div class="evidence-panel-heading">
                <div>
                    <p class="evidence-panel-kicker">PACKAGE INFORMATION</p>
                    <h2>패키지 정보</h2>
                    <p><c:out value="${item.templateTitle()}"/></p>
                </div>
                <span class="evidence-state state-${item.status()}"><c:out value="${seal:statusLabel('evidence', item.status())}"/></span>
            </div>

            <div class="evidence-user-summary">
                <h2>증적 핵심 정보</h2>
                <p><strong><c:out value="${item.templateTitle()}"/></strong> 동의의 증적입니다. 요청번호 <a href="${pageContext.request.contextPath}/admin/requests/${item.requestId()}"><c:out value="${item.requestCode()}"/></a>, 상태는 <c:out value="${seal:statusLabel('evidence', item.status())}"/>입니다.</p>
            </div>
            <dl class="evidence-summary-grid">
                <div><dt>동의서명</dt><dd><c:out value="${item.templateTitle()}"/></dd></div>
                <div><dt>동의 요청번호</dt><dd><c:out value="${item.requestCode()}"/></dd></div>
                <div><dt>증적 상태</dt><dd><c:out value="${seal:statusLabel('evidence', item.status())}"/></dd></div>
                <div><dt>생성 요청 일시</dt><dd><c:out value="${seal:dateTime(item.queuedAt())}"/></dd></div>
                <div><dt>생성 완료 일시</dt><dd><c:out value="${empty item.finalizedAt() ? '-' : seal:dateTime(item.finalizedAt())}"/></dd></div>
                <div><dt>포함 파일</dt><dd><c:out value="${item.files().size()}"/>개</dd></div>
            </dl>
            <details class="evidence-technical">
                <summary>기술 정보 보기</summary>
                <dl class="evidence-metadata"><div><dt>패키지 식별자</dt><dd class="evidence-code"><c:out value="${item.packageId()}"/></dd></div>
                <div>
                    <dt>동의 요청</dt>
                    <dd class="evidence-request-summary">
                        <a href="${pageContext.request.contextPath}/admin/requests/${item.requestId()}"><c:out value="${item.requestCode()}"/></a>
                        <span><c:out value="${item.templateTitle()}"/></span>
                    </dd>
                </div>
                <div>
                    <dt>생성 대기 시각</dt>
                    <dd><c:out value="${seal:dateTime(item.queuedAt())}"/></dd>
                </div>
                <div>
                    <dt>생성 완료 시각</dt>
                    <dd><c:out value="${empty item.finalizedAt() ? '-' : seal:dateTime(item.finalizedAt())}"/></dd>
                </div>
                <div>
                    <dt>HMAC 키 ID</dt>
                    <dd><c:out value="${empty item.hmacKeyId() ? '-' : item.hmacKeyId()}"/></dd>
                </div>
                <div class="evidence-meta-wide">
                    <dt>매니페스트 SHA-256</dt>
                    <dd class="evidence-code"><c:out value="${empty item.manifestSha256() ? '-' : item.manifestSha256()}"/></dd>
                </div>
                <div class="evidence-meta-wide">
                    <dt>저장 경로</dt>
                    <dd class="evidence-code"><c:out value="${empty item.relativePath() ? '-' : item.relativePath()}"/></dd>
                </div>
            </dl>

            <div class="evidence-files-heading">
                <div>
                    <h2>생성 파일</h2>
                    <p>증적 패키지에 포함된 파일과 파일별 해시값입니다.</p>
                </div>
                <span><c:out value="${item.files().size()}"/>개 파일</span>
            </div>
            <div class="table-scroll evidence-file-table">
                <table class="data-table">
                    <thead><tr><th>종류</th><th>파일명</th><th>크기</th><th>SHA-256</th></tr></thead>
                    <tbody>
                    <c:forEach var="file" items="${item.files()}">
                        <tr>
                            <td><span class="evidence-file-type"><c:out value="${file.type()}"/></span></td>
                            <td><c:out value="${file.fileName()}"/></td>
                            <td><c:out value="${file.size()}"/> bytes</td>
                            <td class="evidence-file-hash"><c:out value="${file.sha256()}"/></td>
                        </tr>
                    </c:forEach>
                    <c:if test="${empty item.files()}"><tr><td colspan="4" class="evidence-empty">아직 생성된 파일이 없습니다.</td></tr></c:if>
                    </tbody>
                </table>
            </div>

            </details>
            <div class="form-actions evidence-actions">
                <sec:authorize access="hasAuthority('EVIDENCE_VERIFY')">
                    <form method="post" action="${pageContext.request.contextPath}/admin/evidence/${item.packageId()}/verify">
                        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                        <button class="secondary-button">무결성 검증</button>
                    </form>
                </sec:authorize>
                <sec:authorize access="hasAuthority('EVIDENCE_DOWNLOAD')">
                    <a class="primary-button" href="${pageContext.request.contextPath}/admin/evidence/${item.packageId()}/download">ZIP 다운로드</a>
                </sec:authorize>
                <c:if test="${item.status() == 'FAILED'}">
                    <sec:authorize access="hasAuthority('EVIDENCE_GENERATE')">
                        <form method="post" action="${pageContext.request.contextPath}/admin/evidence/${item.packageId()}/retry">
                            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                            <button class="secondary-button">재생성</button>
                        </form>
                    </sec:authorize>
                </c:if>
            </div>
        </section>
    </main>
</div>
</body>
</html>
