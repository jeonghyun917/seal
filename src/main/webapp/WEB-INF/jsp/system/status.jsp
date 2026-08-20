<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>개발환경 구성 확인</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/app.css">
</head>
<body>
<main class="page-shell">
    <section class="status-card" aria-labelledby="status-title">
        <p class="eyebrow">SYSTEM BASELINE</p>
        <h1 id="status-title">개발환경 구성 확인</h1>
        <dl class="status-grid">
            <div><dt>실행환경</dt><dd><c:out value="${runtimeVersion}" /></dd></div>
            <div><dt>Java</dt><dd><c:out value="${javaVersion}" /></dd></div>
            <div><dt>데이터베이스</dt><dd><c:out value="${databaseVersion}" /></dd></div>
            <div><dt>화면 기술</dt><dd><c:out value="${viewTechnology}" /></dd></div>
            <div><dt>전달 방식</dt><dd><c:out value="${deliveryScope}" /></dd></div>
            <div><dt>제외 범위</dt><dd><c:out value="${excludedScope}" /></dd></div>
        </dl>
        <a class="secondary-button" href="${pageContext.request.contextPath}/">처음으로</a>
    </section>
</main>
</body>
</html>
