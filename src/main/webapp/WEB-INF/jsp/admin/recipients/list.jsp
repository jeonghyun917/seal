<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="seal" uri="https://arte.or.kr/seal/functions" %>
<!doctype html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>동의 수신자 | 개인정보 전자동의 관리시스템</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/app.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/admin-data.css?v=20260908-2">
</head>
<body class="admin-body">
<jsp:include page="/WEB-INF/jsp/fragments/admin/header.jsp"/>
<div class="admin-layout">
    <jsp:include page="/WEB-INF/jsp/fragments/admin/sidebar.jsp"/>
    <main class="admin-main">
        <div class="page-heading-row">
            <div>
                <p class="section-kicker">RECIPIENT</p>
                <h1>동의 수신자</h1>
                <p>개인정보 동의 요청 이메일을 받을 내부·외부 수신자를 관리합니다. 이름과 이메일은 마스킹하여 표시합니다.</p>
            </div>
            <div class="form-actions">
                <a class="secondary-button excel-upload-button" href="${pageContext.request.contextPath}/admin/recipients/import"><svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false"><path d="M14 3H5a1 1 0 0 0-1 1v16a1 1 0 0 0 1 1h10M14 3l5 5h-5V3Z"/><path d="M7 10h4M7 14h4M7 18h4M19 20v-8m-3 3 3-3 3 3"/></svg><span>수신자 Excel 등록</span></a>
                <a class="primary-button" href="${pageContext.request.contextPath}/admin/recipients/new">수신자 등록</a>
            </div>
        </div>
        <section class="work-panel">
            <form class="list-filter" method="get">
                <label for="email"><span>이메일 검색</span>
                    <input id="email" type="search" name="email" value="<c:out value='${email}'/>" placeholder="이메일 일부 입력">
                </label>
                <label for="collectionBasis"><span>수신자 정보 등록 근거 검색</span>
                    <input id="collectionBasis" type="search" name="collectionBasis" value="<c:out value='${collectionBasis}'/>" placeholder="등록 근거 입력">
                </label>
                <label for="status"><span>상태</span>
                    <select id="status" name="status">
                        <option value="">전체</option>
                        <option value="ACTIVE" ${selectedStatus == 'ACTIVE' ? 'selected' : ''}>활성</option>
                        <option value="INACTIVE" ${selectedStatus == 'INACTIVE' ? 'selected' : ''}>비활성</option>
                        <option value="DESTROYED" ${selectedStatus == 'DESTROYED' ? 'selected' : ''}>파기 완료</option>
                    </select>
                </label>
                <button class="secondary-button" type="submit">조회</button>
            </form>
            <div class="table-scroll">
                <table class="data-table">
                    <thead><tr><th>수신자 소속기관</th><th>직책</th><th>이름</th><th>이메일</th><th class="recipient-basis-heading">수신자 정보 등록 근거</th><th>상태</th></tr></thead>
                    <tbody>
                    <c:forEach var="recipient" items="${recipients}">
                        <tr>
                            <td><c:out value="${empty recipient.affiliationName() ? '미입력' : recipient.affiliationName()}"/></td>
                            <td><c:out value="${empty recipient.jobTitle() ? '미입력' : recipient.jobTitle()}"/></td>
                            <td><a href="${pageContext.request.contextPath}/admin/recipients/${recipient.recipientId()}"><c:out value="${recipient.maskedName()}"/></a></td>
                            <td><c:out value="${recipient.maskedEmail()}"/></td>
                            <td><c:out value="${recipient.collectionBasis()}"/></td>
                            <td><c:out value="${seal:statusLabel('recipient', recipient.status())}"/></td>
                        </tr>
                    </c:forEach>
                    <c:if test="${empty recipients}"><tr><td colspan="6">조건에 맞는 수신자가 없습니다.</td></tr></c:if>
                    </tbody>
                </table>
            </div>
            <jsp:include page="/WEB-INF/jsp/fragments/admin/pagination.jsp"/>
        </section>
    </main>
</div>
</body>
</html>
