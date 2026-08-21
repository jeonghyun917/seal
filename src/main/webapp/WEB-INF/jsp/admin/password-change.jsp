<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>비밀번호 변경 | 개인정보 전자동의·증적 관리시스템</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/app.css">
</head>
<body class="auth-body">
<main class="single-card-shell">
    <section class="auth-card password-card" aria-labelledby="change-title">
        <div class="card-heading">
            <p class="section-kicker">ACCOUNT SECURITY</p>
            <h1 id="change-title">비밀번호 변경</h1>
            <p>${administrator.displayName()}님, 관리자 기능을 사용하기 전에 임시 비밀번호를 변경해 주세요.</p>
        </div>

        <div class="policy-note">
            <strong>비밀번호 기준</strong>
            <span>12~128자 · 로그인 아이디와 다른 값 · 입력한 공백을 포함해 그대로 검증</span>
        </div>

        <c:if test="${not empty errorMessage}">
            <div class="alert alert-error" role="alert">${errorMessage}</div>
        </c:if>

        <form class="stack-form" method="post" action="${pageContext.request.contextPath}/account/password/change">
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
            <div class="form-field">
                <label for="currentPassword">현재 비밀번호</label>
                <input id="currentPassword" name="currentPassword" type="password"
                       autocomplete="current-password" maxlength="128" required autofocus>
            </div>
            <div class="form-field">
                <label for="newPassword">새 비밀번호</label>
                <input id="newPassword" name="newPassword" type="password"
                       autocomplete="new-password" minlength="12" maxlength="128"
                       aria-describedby="password-help" required>
                <span id="password-help" class="field-help">12자 이상 128자 이하로 입력해 주세요.</span>
            </div>
            <div class="form-field">
                <label for="newPasswordConfirmation">새 비밀번호 확인</label>
                <input id="newPasswordConfirmation" name="newPasswordConfirmation" type="password"
                       autocomplete="new-password" minlength="12" maxlength="128" required>
            </div>
            <button class="primary-button form-submit" type="submit">변경하고 다시 로그인</button>
        </form>
    </section>
</main>
</body>
</html>
