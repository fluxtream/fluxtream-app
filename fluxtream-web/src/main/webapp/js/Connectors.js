define(function() {
	
	var Connectors = {};
	
	function submitNikePlusCredentials() {
		var username = $("input#nikeplus-username").val();
		$.ajax({
			url:"/nikeplus/setUsername",
			type: "POST",
			data: {username: username},
			success: function(html) {
				$(".addConnectorsMain").html(html);
				$(".focushere").focus();
			}
		});
	}

	function submitToodledoCredentials() {
		var username = $("input#toodledo-username").val(),
			password = $("input#toodledo-password").val();
		$.ajax({
			url:"/toodledo/submitCredentials",
			type: "POST",
			data: {username: username, password: password},
			success: function(html) {
				$(".addConnectorsMain").html(html);
				$(".focushere").focus();
			}
		});
	}

	function submitZeoCredentials() {
		var username = $("input#zeo-username").val(),
			password = $("input#zeo-password").val();
		$.ajax({
			url:"/zeo/submitCredentials",
			type: "POST",
			data: {username: username, password: password},
			success: function(html) {
				$(".addConnectorsMain").html(html);
				$(".focushere").focus();
			}
		});
	}

	function submitWithingsUsernameAndPassword() {
		var username = $("input#withings-username").val(),
			password = $("input#withings-password").val();
		$.ajax({
			url:"/withings/setupWithings",
			type: "POST",
			data: {username: username, password: password},
			success: function(html) {
				$(".addConnectorsMain").html(html);
				$(".focushere").focus();
			}
		});
	}

	function submitSmsBackupFolderNames() {
		var smsFolderName = $("input#smsBackup-smsFolderName").val(),
		callLogFolderName = $("input#smsBackup-callLogFolderName").val();
		$.ajax({
			url:"/smsBackup/setFolderNames",
			type: "POST",
			data: {smsFolderName: smsFolderName, callLogFolderName: callLogFolderName},
			success: function(html) {
				$(".addConnectorsMain").html(html);
				$(".focushere").focus();
			}
		});
	}

	function submitSmsBackupUsernameAndPassword() {
		var username = $("input#smsBackup-username").val(),
			password = $("input#smsBackup-password").val();
		$.ajax({
			url:"/smsBackup/check",
			type: "POST",
			data: {username: username, password: password},
			success: function(html) {
				$(".addConnectorsMain").html(html);
				$(".focushere").focus();
			}
		});
	}

	function chooseWithingsUser() {
        var radioButtons = $("input#withings-chosenUser");
        var chosenUser;
        for (var i = 0; i < radioButtons.length; i++){
            if (radioButtons[i].checked)
                chosenUser = radioButtons[i].value;
        }
		$.ajax({
			url:"/withings/chooseWithingsUser",
			type: "POST",
			data: {chosenUser: chosenUser},
			success: function(html) {
				$(".addConnectorsMain").html(html);
			}
		});
	}
	
	function submitOpenPathKeypair() {
		var accessKey = $("input#openpath-accessKey").val(),
			secretKey = $("input#openpath-secretKey").val();
		$.ajax({
			url:"/openPath/check",
			type: "POST",
			data: {accessKey: accessKey, secretKey: secretKey},
			success: function(html) {
				$(".addConnectorsMain").html(html);
				$(".focushere").focus();
			}
		});
	}
	
	Connectors.chooseWithingsUser = chooseWithingsUser;
	Connectors.submitSmsBackupUsernameAndPassword = submitSmsBackupUsernameAndPassword;
	Connectors.submitSmsBackupFolderNames = submitSmsBackupFolderNames;
	Connectors.submitWithingsUsernameAndPassword = submitWithingsUsernameAndPassword;
	Connectors.submitZeoCredentials = submitZeoCredentials;
	Connectors.submitToodledoCredentials = submitToodledoCredentials;
	Connectors.submitNikePlusCredentials = submitNikePlusCredentials;
	Connectors.submitOpenPathKeypair = submitOpenPathKeypair;
	
	Connectors.submitBodytrackCredentials = function() {
		var username = $("input#bodytrack-username").val(),
		password = $("input#bodytrack-password").val(),
		host = $("input#bodytrack-host").val();
		$.ajax({
			url:"/bodytrack/submitCredentials",
			type: "POST",
			data: {username: username, password: password, host: host},
			success: function(html) {
				$(".addConnectorsMain").html(html);
				$(".focushere").focus();
			}
		});
	};
	
	window.Connectors = Connectors;
	
	return Connectors;
});