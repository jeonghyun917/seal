<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="seal" uri="https://arte.or.kr/seal/functions" %>
<!doctype html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Excel 반입 검증 결과</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/app.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/admin-data.css?v=20260908-2">
</head>
<body class="admin-body">
<jsp:include page="/WEB-INF/jsp/fragments/admin/header.jsp"/>
<div class="admin-layout">
    <jsp:include page="/WEB-INF/jsp/fragments/admin/sidebar.jsp"/>
    <main class="admin-main">
        <div class="page-heading-row">
            <div><p class="section-kicker">IMPORT PREVIEW</p><h1>Excel 반입 검증 결과</h1><p>개인정보는 마스킹되어 표시되며 원본 파일은 저장하지 않습니다.</p></div>
            <a class="secondary-button" href="${pageContext.request.contextPath}/admin/recipients">수신자 목록</a>
        </div>
        <c:if test="${param.completed != null}"><p class="alert alert-success">정상 행 반입을 완료하고 임시 암호문을 파기했습니다.</p></c:if>
        <section class="work-panel">
            <div class="summary-grid">
                <div><strong>${preview.totalCount()}</strong><span>전체</span></div>
                <div><strong>${preview.validCount()}</strong><span>정상</span></div>
                <div><strong>${preview.duplicateCount()}</strong><span>중복</span></div>
                <div><strong>${preview.errorCount()}</strong><span>오류</span></div>
            </div>
            <p>악성코드 검사: <c:out value="${seal:statusLabel('malware', preview.malwareScanStatus())}"/> · 상태: <c:out value="${seal:statusLabel('import', preview.status())}"/></p>
            <p class="field-help">오류가 있는 행은 원본 Excel 파일에서 수정한 뒤 다시 등록해 주세요. 정상 행만 반입되며 중복 행은 제외됩니다.</p><a class="secondary-button excel-upload-button" href="${pageContext.request.contextPath}/admin/recipients/import"><svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false"><path d="M14 3H5a1 1 0 0 0-1 1v16a1 1 0 0 0 1 1h10M14 3l5 5h-5V3Z"/><path d="M7 10h4M7 14h4M7 18h4M19 20v-8m-3 3 3-3 3 3"/></svg><span>Excel 다시 등록</span></a><div class="table-scroll">
                <table class="data-table">
                    <thead><tr><th>행</th><th>수신자 소속기관</th><th>직책</th><th>이름</th><th>이메일</th><th>수신자 정보 등록 근거</th><th>결과</th><th>확인할 내용</th></tr></thead>
                    <tbody>
                    <c:forEach var="row" items="${preview.rows()}"><c:set var="importDataPurged" value="${row.status() == 'PURGED' || preview.status() == 'IMPORTED' || preview.status() == 'EXPIRED'}"/>
                        <tr><td>${row.rowNumber()}</td><td><c:out value="${empty row.affiliationName() ? '미입력' : row.affiliationName()}"/></td><td><c:out value="${empty row.jobTitle() ? '미입력' : row.jobTitle()}"/></td><td><c:out value="${row.maskedName()}"/></td><td><c:out value="${row.maskedEmail()}"/></td><td><c:out value="${empty row.collectionBasis() ? (importDataPurged ? '임시정보 파기 완료' : '미입력') : row.collectionBasis()}"/></td><td><c:out value="${seal:statusLabel('import-row', row.status())}"/></td><td><c:forTokens items="${row.errorCodes()}" delims="," var="errorCode"><div><c:out value="${seal:statusLabel('import-error', errorCode)}"/></div></c:forTokens><c:if test="${empty row.errorCodes()}">-</c:if></td></tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
            <c:if test="${preview.status() == 'VALIDATED'}"><form method="post" action="${pageContext.request.contextPath}/admin/recipients/import/${preview.batchId()}/confirm"><input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"><button class="primary-button">정상 ${preview.validCount()}건 반입 확정</button></form></c:if>
        </section>
    </main>
</div>
</body>
</html>
