<%@page import="org.openmrs.module.dhislocation.constant.Dhis2Constants"%>
<%@ include file="/WEB-INF/template/include.jsp"%>
<%@ include file="/WEB-INF/template/header.jsp"%>

<h3>DHIS2 Locations Syncing</h3>

<form method="POST">
    <div>${message}</div>
    <a href="${pageContext.request.contextPath}/module/dhislocation/importDhisLocations.form?currentPage=${currentPage}">Start again from current page</a>
    <a href="${pageContext.request.contextPath}/module/dhislocation/importDhisLocations.form">Start again from beginning</a>
</form>

<%@ include file="/WEB-INF/template/footer.jsp"%>