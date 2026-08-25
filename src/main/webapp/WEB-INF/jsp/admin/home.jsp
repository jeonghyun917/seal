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
            <span class="status-badge">정상 운영</span>
        </div>

        <section class="summary-grid" aria-label="구축 현황">
            <article class="summary-card">
                <span class="card-number">01</span>
                <h2>관리자 인증</h2>
                <p>DB 계정, 임시 잠금, BCrypt 12가 적용됐습니다.</p>
                <span class="state-text ready">구축 완료</span>
            </article>
            <article class="summary-card" id="pending-policy">
                <span class="card-number">02</span>
                <h2>보유정책 관리</h2>
                <p>불변 승인 버전과 작성·승인 분리 흐름이 적용됐습니다.</p>
                <span class="state-text ready">구축 완료</span>
            </article>
            <article class="summary-card" id="pending-activity">
                <span class="card-number">03</span>
                <h2>처리업무 관리</h2>
                <p>목적·근거·개인정보 항목과 승인 정책을 연결했습니다.</p>
                <span class="state-text ready">구축 완료</span>
            </article>
            <article class="summary-card">
                <span class="card-number">04</span>
                <h2>동의서 템플릿</h2>
                <p>승인 처리업무 기반의 구조화 동의서와 발행 버전을 관리합니다.</p>
                <a href="${pageContext.request.contextPath}/admin/templates">템플릿 관리</a>
            </article>
            <article class="summary-card">
                <span class="card-number">05</span>
                <h2>동의 대상자</h2>
                <p>암호화 저장과 정확 이메일 검색, 마스킹 조회로 대상자를 관리합니다.</p>
                <a href="${pageContext.request.contextPath}/admin/recipients">대상자 관리</a>
            </article>
            <article class="summary-card"><span class="card-number">06</span><h2>동의 증적</h2><p>한글 PDF·JSON·SHA-256·HMAC 패키지와 검증 이력을 관리합니다.</p><a href="${pageContext.request.contextPath}/admin/evidence">증적 관리</a></article>
            <article class="summary-card"><span class="card-number">07</span><h2>권리요청·철회</h2><p>외부 본인확인, 작성·승인 분리와 원본 불변 철회를 처리합니다.</p><a href="${pageContext.request.contextPath}/admin/rights">권리요청 관리</a></article>
            <article class="summary-card"><span class="card-number">08</span><h2>보유기간·파기</h2><p>기산일, 보존 보류, 이중 통제와 주 저장소 파기를 관리합니다.</p><a href="${pageContext.request.contextPath}/admin/retention">파기 작업</a></article>
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
    </main></div></body>
</html>
