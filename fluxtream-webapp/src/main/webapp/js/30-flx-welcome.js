
jQuery(document).ready(function($) {
	$("#createAccountButton").click(createAccount);
	$("#f_username").focus();
	var timezone = jstz.determine_timezone(),
		d = new Date(),
		currentDate = d.getDate(),
		currentMonth = d.getMonth() + 1,
		currentYear = d.getFullYear();
	setCookie("timeZone", timezone.name(), 1);
	setCookie("date", currentYear + "-" + currentMonth + "-" + currentDate, 1);
})

function setCookie(c_name,value,exdays) {
	var exdate=new Date();
	exdate.setDate(exdate.getDate() + exdays);
	var c_value=escape(value) + ((exdays==null) ? "" : "; expires="+exdate.toUTCString());
	document.cookie=c_name + "=" + c_value;
}