<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %><%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:if test="${not empty pageResult && (pageResult.hasPrevious() || pageResult.hasNext())}">
<c:set var="paginationPath" value="${requestScope['jakarta.servlet.forward.servlet_path']}"/>
<c:if test="${empty paginationPath}"><c:set var="paginationPath" value="${pageContext.request.servletPath}"/></c:if>
<nav class="pagination" aria-label="목록 페이지">
<c:if test="${pageResult.hasPrevious()}"><c:url var="previousPageUrl" value="${paginationPath}"><c:param name="page" value="${pageResult.page()-1}"/><c:param name="status" value="${selectedStatus}"/><c:param name="email" value="${email}"/></c:url><a class="secondary-button" href="${previousPageUrl}">이전</a></c:if>
<span><c:out value="${pageResult.page()}"/>페이지</span>
<c:if test="${pageResult.hasNext()}"><c:url var="nextPageUrl" value="${paginationPath}"><c:param name="page" value="${pageResult.page()+1}"/><c:param name="status" value="${selectedStatus}"/><c:param name="email" value="${email}"/></c:url><a class="secondary-button" href="${nextPageUrl}">다음</a></c:if>
</nav>
</c:if>
