<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!doctype html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>${serviceName}</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/app.css">
</head>
<body>
<main class="page-shell">
    <section class="hero-card" aria-labelledby="page-title">
        <p class="eyebrow">ARTE · DEVELOPMENT BASELINE</p>
        <h1 id="page-title">${serviceName}</h1>
        <p class="lead">이메일 안내와 기관 웹페이지의 일회용 보안 링크로 동의 결과를 관리하는 기반 프로젝트입니다.</p>
        <div class="actions">
            <a class="primary-button" href="${pageContext.request.contextPath}/system/status">환경 구성 확인</a>
            <a class="secondary-button" href="${pageContext.request.contextPath}/login">관리자 로그인</a>
        </div>
    </section>
</main>
</body>
</html>
