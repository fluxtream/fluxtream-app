$(document).ready(function() {
});

function cleanupRunningUpdateTasks() {
	$.ajax({
		url : "/admin/cleanupRunningUpdateTasks",
		dataType : "json",
		success : function(response) {
			alert("Done");
		}
	});
}

function selectGuest() {
	window.location = "/admin/guests/" + $("#guestSelect").val();
}

function hideMessageBox() {
	$('#messageBox').hide();
}

function initLuceneIndex() {
	$.ajax({
		url : "/admin/initLuceneIndex",
		dataType : "json",
		success : handleStatus
	});
}

function handleStatus(status) {
	if (status.result == "KO")
		handleFailure(status);
	else
		handleSuccess(status);
}

function handleSuccess(status) {
	alert(status.message);
}

function handleFailure(status) {
	alert("There was a problem: " + status.message);
	if (status.stackTrace && typeof (console) != "undefined")
		console.log(status.stackTrace);
}

function searchComments(guestId, terms) {
	if (typeof(guestId)=="undefined"||guestId==null||guestId=='') {
		alert("please choose a guest");
		return;
	}
	if (typeof(terms)=="undefined"||terms=='') {
		alert("please enter one or more search terms: " + terms);
		return;
	}
	$.ajax({
		url : "/admin/searchComments",
		data: {guestId: guestId, terms: terms},
		dataType : "json",
		success : function(status) {
			if (status.result == "KO")
				handleFailure(status);
			else {
				console.log(status.payload);
			}
		}
	});
}


function searchFacets(guestId, terms) {
	if (typeof(guestId)=="undefined"||guestId==null||guestId=='') {
		alert("please choose a guest");
		return;
	}
	if (typeof(terms)=="undefined"||terms=='') {
		alert("please enter one or more search terms: " + terms);
		return;
	}
	$.ajax({
		url : "/admin/searchFacets",
		data: {guestId: guestId, terms: terms},
		dataType : "html",
		success : function(html) {
			$("#searchResults").html(html);
		}
	});
}
