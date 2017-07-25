<%@page import="org.openmrs.module.dhislocation.constant.Dhis2Constants"%>
<%@ include file="/WEB-INF/template/include.jsp"%>
<%@ include file="/WEB-INF/template/header.jsp"%>

<script type="text/javascript">
function startSync(){
	var selVal = jQuery('input[name=syncPage]:checked').val();
	if(selVal){
		var page;
		if(selVal === 'input'){
			var inputValue = jQuery('#pageId').val();
			if(inputValue && !isNaN(inputValue)){
				page = jQuery('#pageId').val();
			}
			else {
				alert('The page number to start from is not correct');
				return false;
			}
		}
		else {
			page = selVal;
		}
		
		if(!confirm('Are you sure to start sync? This may take a while to finish. '+ syncLogMessage(false))){
			return false;
		}
		
		jQuery('#currentPage').val(page);
		
		document.getElementById("startBtn").disabled = true;
		document.getElementById("stopBtn").disabled = false;
		
		jQuery('#progress').html('Import has been started. Do not close browser or page. '+
					'Wait for the result. '+ syncLogMessage(false));
		jQuery('#frm').submit();
	}
	else {
		alert('No sync type specified. You must specify one sync type to start operation');
		return false;
	}
}
function stopSync(){
	if(confirm('Are you sure to stop sync? This would stop syncing progress immediately in the middle. '+ syncLogMessage(false))){
		window.location = "${pageContext.request.contextPath}/module/dhislocation/importDhisLocations/stopSync.form";
	}
}
function syncLogMessage(hyperlinked){
	var msg = 'The log for latest sync process could be found at ';
	if(hyperlinked){
		msg += '<a href="${pageContext.request.contextPath}/module/dhislocation/importDhisLocations/log.form">';
	}
		msg += '<%=Dhis2Constants.DHIS_SYNC_LOG_PATH%>';
	if(hyperlinked){
		msg += '</a>';
	}
	
	return msg;
}
</script>
<h3>DHIS2 Locations Sync</h3> (import may take sometime based on number of locations in DHIS)
<br>

    <div id="permissionError" style="color: red; font-size: large;display: block;">You donot have permissions to carry out requested operations. Contact system administrator</div>

<openmrs:hasPrivilege privilege="DHIS - Run Syncer">
	
<script type="text/javascript">
jQuery('#permissionError').hide();
</script>

	<table>
	<tr>
	<td>
	<div align="center" style="width: 500px; height: 300px; border: thin solid silver; text-align: left; padding: 10px 0px 0px 100px; margin: 30px">
	
	<form method="POST" id="frm">
	
		<input type="hidden" id="currentPage" name="currentPage" value="">
		
		<script type="text/javascript">
		if('${message}' && '${message}'.toLowerCase().indexOf('error') !== -1){
			jQuery('#msgDiv').css('color', 'red');
		}
		</script>
		
		<br><br>
		<b>Select sync type to perform:</b><br><br>
		
		<c:if test="${not empty lastPage}">
		<input type="radio" name="syncPage" value="${lastPage}"> Start from last page synced = ${lastPage}<br>
		</c:if>
		<input type="radio" name="syncPage" value="0"> Start from beginning <br>
		<input type="radio" name="syncPage" value="input"> Start from page <input id="pageId" style="width: 50px;" maxlength="6">
		<br><br>
		<button id="startBtn" type="button" onclick="startSync();">Start Sync</button> - 
		<button id="stopBtn" type="button" onclick="stopSync();">Stop Sync</button>
		<br><br><br><br>
		<span id="progress" style="font-size: smaller;"></span>
		<script type="text/javascript">
		if('${syncUnderProgress}' && '${syncUnderProgress}' === 'true'){
			document.getElementById("startBtn").disabled = true;
			document.getElementById("stopBtn").disabled = false;
			
			jQuery('#progress').html('Seems that sync is under progress. Hit Stop Sync to cancel the action and start activity again if you are not sure what`s going on or wait for the result. '+
					syncLogMessage(true));
		}
		else {
			document.getElementById("startBtn").disabled = false;
			document.getElementById("stopBtn").disabled = true;
		
			jQuery('#progress').html(syncLogMessage(true));
		}
		</script>
	</form>
	</div>
	</td>
	<td id="result">
	<div id="msgDiv">${message}</div>
	</td>
	</tr>
	</table>

    </openmrs:hasPrivilege>
<%@ include file="/WEB-INF/template/footer.jsp"%>