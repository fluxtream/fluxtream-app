function handleNotifications(digestInfo) {
	$(".notification").remove();
	$("#notificationIds").empty();
	if (typeof(digestInfo.notifications)!="undefined") {
		$(".alert-message").addClass("success");
		for (n=0; n<digestInfo.notifications.length; n++) {
			var notification = digestInfo.notifications[n];
			$(".alert-message").append("<p class=\"notification\">" + notification.message + "</p>");
			if (n>0)
				$("#notificationIds").append(",");
			$("#notificationIds").append(notification.id);
		}
		$(".alert-message").show();
	}
}

function discardNotifications() {
	var ids = $("#notificationIds").html();
	$.ajax({url:"/notifications/discard?ids=" + ids});
	$(".alert-message").hide();
}
