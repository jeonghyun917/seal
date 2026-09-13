<%@ taglib prefix="seal" uri="https://arte.or.kr/seal/functions" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<!doctype html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>동의서 관리 | 개인정보 전자동의 관리시스템</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/app.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/admin-data.css?v=20260906-3">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/template.css?v=20260906-3">
    <script defer src="${pageContext.request.contextPath}/resources/js/template-bulk-actions.js?v=20260826-2"></script>
</head>
<body class="admin-body">
<jsp:include page="/WEB-INF/jsp/fragments/admin/header.jsp"/>
<div class="admin-layout">
    <jsp:include page="/WEB-INF/jsp/fragments/admin/sidebar.jsp"/>
    <main class="admin-main">
        <div class="page-heading-row">
            <div>
                <p class="section-kicker">CONSENT TEMPLATE</p>
                <h1>동의서 관리</h1>
                <p>승인된 개인정보 이용 업무를 기준으로 이메일과 웹에서 공유할 동의 내용을 관리합니다.</p>
            </div>
            <a class="primary-button" href="${pageContext.request.contextPath}/admin/templates/new">동의서 양식 등록</a>
        </div>
        <c:if test="${param.deleted != null}">
            <p class="alert alert-success">선택한 동의서 양식을 삭제 목록으로 이동했습니다.</p>
        </c:if>
        <section class="work-panel">
            <form class="list-filter" method="get">
                <label>검색
                    <input name="keyword" value="<c:out value='${keyword}'/>" placeholder="코드 또는 명칭">
                </label>
                <label class="status-filter">상태
                    <select name="status">
                        <option value="">전체</option>
                        <option value="ACTIVE" ${selectedStatus == 'ACTIVE' ? 'selected' : ''}>사용</option>
                        <option value="INACTIVE" ${selectedStatus == 'INACTIVE' ? 'selected' : ''}>중지</option>
                    </select>
                </label>
                <button class="secondary-button" type="submit">조회</button>
            </form>

            <sec:authorize access="hasAuthority('CONSENT_TEMPLATE_EDIT')" var="canEditTemplate"/>
            <c:set var="columnCount" value="${canEditTemplate ? 7 : 6}"/>
            <form method="post" action="${pageContext.request.contextPath}/admin/templates/delete"
                  data-bulk-form data-delete-bulk-form>
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                <div class="bulk-toolbar">
                    <a class="secondary-button" href="${pageContext.request.contextPath}/admin/templates/deleted">삭제된 양식</a>
                    <c:if test="${canEditTemplate}">
                        <span class="selection-count" data-selection-count>0개 선택</span>
                        <button class="danger-button" type="button" data-bulk-trigger disabled>선택 삭제</button>
                    </c:if>
                </div>
                <div class="table-scroll">
                    <table class="data-table">
                        <thead>
                        <tr>
                            <c:if test="${canEditTemplate}">
                                <th class="selection-cell"><input type="checkbox" data-select-all aria-label="전체 동의서 양식 선택"></th>
                            </c:if>
                            <th>코드</th><th>양식 관리 명칭</th><th>개인정보 이용 업무</th><th>발행 버전</th><th>최근 상태</th><th>상태</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach var="template" items="${templates}">
                            <tr>
                                <c:if test="${canEditTemplate}">
                                    <td class="selection-cell"><input type="checkbox" name="templateIds"
                                            value="${template.consentTemplateId()}" data-row-select
                                            aria-label="<c:out value='${template.templateName()}'/> 선택"></td>
                                </c:if>
                                <td><a href="${pageContext.request.contextPath}/admin/templates/${template.consentTemplateId()}"><c:out value="${template.templateCode()}"/></a></td>
                                <td><c:out value="${template.templateName()}"/></td>
                                <td><c:out value="${template.processingActivityName()}"/></td>
                                <td><c:out value="${template.publishedVersionNumber()}" default="미발행"/></td>
                                <td><span class="template-state"><c:out value="${seal:statusLabel('workflow', template.latestWorkflowStatus())}"/></span></td>
                                <td><c:out value="${seal:statusLabel('template', template.templateStatus())}"/></td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty templates}"><tr><td colspan="${columnCount}">조건에 맞는 동의서 양식이 없습니다.</td></tr></c:if>
                        </tbody>
                    </table>
                </div>
                <c:if test="${canEditTemplate}">
                    <dialog class="delete-dialog" data-delete-dialog aria-labelledby="delete-dialog-title">
                        <div class="delete-dialog-panel">
                            <div class="delete-dialog-head">
                                <div>
                                    <p class="section-kicker">DELETE TEMPLATE</p>
                                    <h2 id="delete-dialog-title">선택한 동의서 양식 삭제</h2>
                                </div>
                                <button class="dialog-close" type="button" data-dialog-close aria-label="삭제 창 닫기">×</button>
                            </div>
                            <p><strong data-selection-count>0개 선택</strong> 항목을 일반 목록에서 숨기고 삭제 이력으로 보관합니다.</p>
                            <label class="delete-dialog-reason" for="deleteReason">삭제 사유 <span aria-hidden="true">*</span>
                                <textarea id="deleteReason" name="deleteReason" rows="4" maxlength="500" required
                                          placeholder="예) 중복 등록된 동의서 양식 정리"></textarea>
                                <small>삭제 목록에 삭제자·삭제 일시와 함께 기록됩니다.</small>
                            </label>
                            <div class="delete-dialog-actions">
                                <button class="secondary-button" type="button" data-dialog-close>취소</button>
                                <button class="danger-button" type="submit" data-bulk-submit disabled>삭제 확인</button>
                            </div>
                        </div>
                    </dialog>
                </c:if>
            </form>
        </section>
    </main>
</div>
</body>
</html>
