<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>관리자 로그인 | 개인정보 전자동의·증적 관리시스템</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/app.css?v=20260907-3">
</head>
<body class="auth-body">
<main class="auth-shell">
    <section class="auth-intro" aria-labelledby="service-title">
        <a class="institution-brand" href="${pageContext.request.contextPath}/" aria-label="한국문화예술교육진흥원 홈">
            <img src="${pageContext.request.contextPath}/resources/images/arte-logo-ko.png"
                 alt="한국문화예술교육진흥원">
        </a>
        <h1 id="service-title">
            <span>개인정보 전자동의</span>
            <strong>증적 관리시스템</strong>
        </h1>
        <p class="lead">기관 담당자 전용 화면입니다. 승인받은 관리자 계정으로 로그인해 주세요.</p>
        <ul class="trust-list">
            <li>DB 기반 관리자 인증</li>
            <li>역할·조직 범위별 접근 통제</li>
            <li>주요 작업 감사 기록</li>
        </ul>
    </section>

    <section class="auth-card" aria-labelledby="login-title">
        <div class="card-heading">
            <p class="section-kicker">ADMINISTRATOR</p>
            <h2 id="login-title">관리자 로그인</h2>
            <p>아이디와 비밀번호를 입력해 주세요.</p>
        </div>

        <c:if test="${not empty errorMessage}">
            <div class="alert alert-error" role="alert">${errorMessage}</div>
        </c:if>
        <c:if test="${not empty noticeMessage}">
            <div class="alert alert-success" role="status">${noticeMessage}</div>
        </c:if>

        <form class="stack-form" method="post" action="${pageContext.request.contextPath}/login">
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
            <div class="form-field">
                <label for="username">관리자 아이디</label>
                <input id="username" name="username" type="text" inputmode="email"
                       autocomplete="username" maxlength="50" required autofocus>
            </div>
            <div class="form-field">
                <label for="password">비밀번호</label>
                <input id="password" name="password" type="password"
                       autocomplete="current-password" maxlength="128" required>
            </div>
            <button class="primary-button form-submit" type="submit">로그인</button>
        </form>

        <p class="support-copy">로그인 문제가 지속되면 시스템 관리자에게 계정 상태를 문의해 주세요.</p>
    </section>
</main>
</body>
</html>
