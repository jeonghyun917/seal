<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %><%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>관리자 홈 | 개인정보 전자동의·증적 관리시스템</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/app.css">
</head>
<body class="admin-body"><jsp:include page="/WEB-INF/jsp/fragments/admin/header.jsp"/>





    <div class="admin-layout"><jsp:include page="/WEB-INF/jsp/fragments/admin/sidebar.jsp"/><main class="admin-main">
        <div class="page-heading-row">
            <div>
                <p class="section-kicker">ADMIN DASHBOARD</p>
                <h1>관리자 홈</h1>
                <p><c:out value="${administrator.displayName()}"/>님, 개인정보 동의 업무 관리 화면입니다.</p>
            </div>
        </div>

        <section class="work-panel" aria-labelledby="work-item-title">
            <div>
                <p class="section-kicker">WORK QUEUE</p>
                <h2 id="work-item-title">현재 처리할 일</h2>
                <c:if test="${not empty workItemTo}"><p><c:out value="${workItemTo}"/> 현재 미처리 상태 · 전체 기간 · 내 조직 조회 범위 기준</p></c:if>
            </div>
            <c:choose>
                <c:when test="${not empty workItems}">
                    <div class="summary-grid">
                        <c:forEach var="workItem" items="${workItems}">
                            <article class="summary-card">
                                <strong class="card-number"><c:out value="${workItem.count()}"/>건</strong>
                                <h3><c:out value="${workItem.label()}"/></h3>
                                <a href="${pageContext.request.contextPath}${workItem.href()}">목록에서 확인</a>
                            </article>
                        </c:forEach>
                    </div>
                </c:when>
                <c:otherwise><p class="empty-state">현재 표시할 처리 항목이 없거나 조회 권한이 없습니다.</p></c:otherwise>
            </c:choose>
        </section>
    </main></div></body>
</html>
