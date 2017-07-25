<%@page import="org.openmrs.module.dhislocation.constant.Dhis2Constants"%>
<%@ include file="/WEB-INF/template/include.jsp"%>
<%@ include file="/WEB-INF/template/header.jsp"%>

<h3>DHIS2 Server Configuration</h3>

<form method="POST">
    <div id="permissionError" style="color: red; font-size: large;display: block;">You donot have permissions to carry out requested operations. Contact system administrator</div>

	<openmrs:hasPrivilege privilege="DHIS - Configure">
	
<script type="text/javascript">
jQuery('#permissionError').hide();
</script>
	    <table>
	    	<openmrs:portlet url="globalProperties" parameters="title=${title}|propertyPrefix=dhislocation.|excludePrefix=dhislocation.started"/>
	    </table>
    </openmrs:hasPrivilege>
</form>

<%@ include file="/WEB-INF/template/footer.jsp"%>