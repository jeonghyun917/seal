<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!doctype html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>관리자 홈 | 개인정보 전자동의·증적 관리시스템</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/app.css">
</head>
<body class="admin-body">
<header class="admin-header">
    <a class="admin-brand" href="${pageContext.request.contextPath}/admin">
        <span>ARTE</span>
        <strong>개인정보 전자동의·증적 관리시스템</strong>
    </a>
    <div class="user-actions">
        <span><strong>${administrator.displayName()}</strong> 님</span>
        <form method="post" action="${pageContext.request.contextPath}/logout">
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
            <button class="text-button" type="submit">로그아웃</button>
        </form>
    </div>
</header>

<div class="admin-layout">
    <nav class="admin-nav" aria-label="관리자 주 메뉴">
        <a class="active" href="${pageContext.request.contextPath}/admin" aria-current="page">대시보드</a>
        <span class="nav-group">기준정보</span>
        <a href="${pageContext.request.contextPath}/admin/organizations">조직 관리</a>
        <a href="${pageContext.request.contextPath}/admin/retention-policies">보유정책</a>
        <a href="${pageContext.request.contextPath}/admin/processing-activities">처리업무</a>
        <a href="${pageContext.request.contextPath}/admin/templates">동의서 템플릿</a>
        <span class="nav-group">시스템 관리</span>
        <a href="${pageContext.request.contextPath}/admin/users">관리자 계정</a>
        <a href="${pageContext.request.contextPath}/admin/roles">역할·권한</a>
        <a href="${pageContext.request.contextPath}/admin/audit-events">감사 이력</a>
    </nav>

    <main class="admin-main">
        <div class="page-heading-row">
            <div>
                <p class="section-kicker">ADMIN DASHBOARD</p>
                <h1>관리자 홈</h1>
                <p>${administrator.displayName()}님, 개인정보 동의 업무 관리 화면입니다.</p>
            </div>
            <span class="status-badge">정상 운영</span>
        </div>

        <section class="summary-grid" aria-label="1단계 구축 현황">
            <article class="summary-card">
                <span class="card-number">01</span>
                <h2>관리자 인증</h2>
                <p>DB 계정, 임시 잠금, BCrypt 12가 적용됐습니다.</p>
                <span class="state-text ready">구축 완료</span>
            </article>
            <article class="summary-card" id="pending-policy">
                <span class="card-number">02</span>
                <h2>보유정책 관리</h2>
                <p>불변 승인 버전과 작성·승인 분리 흐름을 구현합니다.</p>
                <span class="state-text pending">다음 개발 단위</span>
            </article>
            <article class="summary-card" id="pending-activity">
                <span class="card-number">03</span>
                <h2>처리업무 관리</h2>
                <p>목적·근거·개인정보 항목과 승인 정책을 연결합니다.</p>
                <span class="state-text pending">다음 개발 단위</span>
            </article>
            <article class="summary-card">
                <span class="card-number">04</span>
                <h2>동의서 템플릿</h2>
                <p>승인 처리업무 기반의 구조화 동의서와 발행 버전을 관리합니다.</p>
                <a href="${pageContext.request.contextPath}/admin/templates">템플릿 관리</a>
            </article>
        </section>

        <section class="work-panel" aria-labelledby="security-title">
            <div>
                <p class="section-kicker">SECURITY BASELINE</p>
                <h2 id="security-title">현재 적용된 보안 기준</h2>
            </div>
            <dl class="detail-list">
                <div><dt>인증 방식</dt><dd>PostgreSQL 관리자 계정</dd></div>
                <div><dt>실패 대응</dt><dd>기본 5회 실패 시 15분 임시 잠금</dd></div>
                <div><dt>권한 기준</dt><dd>역할 + 조직 범위 + 작업 권한</dd></div>
                <div><dt>감사 기록</dt><dd>로그인·비밀번호 변경 결과 추가 전용 저장</dd></div>
            </dl>
        </section>
    </main>
</div>
</body>
</html>
