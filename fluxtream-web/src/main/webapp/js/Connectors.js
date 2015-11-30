define(function() {
	
	var Connectors = {};

    function submitMymeeAuthInfo() {
				$("#submitMymeeInfo").attr("disabled", "disabled");
        var username = $("input#mymee-username").val();
        var password = $("input#mymee-password").val();
        var activationCode = $("input#mymee-activationCode").val();
        $.ajax({
            url:"/mymee/setAuthInfo",
            type: "POST",
            data: {username : username, password : password, activationCode : activationCode},
            success: function(html) {
                $(".addConnectorsMain").html(html);
                $("#mymee-username").focus();
            }
        });
    }

	function submitMymeeFetchURL() {
		var input = $("input#mymee-fetchURL");
        var fetchURL = input.val();
		$.ajax({
			url:"/mymee/setFetchURL",
			type: "POST",
			data: {url: fetchURL},
			success: function(html) {
				$(".mymee-loading").hide();
				$(".addConnectorsMain").html(html);
				$("#mymee-fetchURL").focus();
				$(".mymee-send").prop("disabled", false);
				input.prop("disabled", false);
			}
		});
		$(".mymee-loading").show();
		$(".mymee-send").prop("disabled", true);
		input.prop("disabled", true);
	}

    function submitBedditAuthInfo(form) {
        var emailField = form.find(".email");
        var passwordField = form.find(".password");
        var submitButton = form.find(".btn");
        var email = emailField.val();
        var password = passwordField.val();
        emailField.attr("disabled", true);
        passwordField.attr("disabled", true);
        submitButton.attr("disabled", true);
        $.ajax({
            url: "/beddit/setAuthInfo",
            type: "POST",
            data: {email: email, password: password},
            success: function() {
                $(form.closest(".modal")).modal("hide");
            },
            error: function() {
                emailField.attr("disabled", false);
                passwordField.attr("disabled", false);
                submitButton.attr("disabled", false);
                // we should let the user that there was an error and that they can try again
            }
        })
    }

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

    function getQuantifiedMindToken() {
        $.ajax({
            url: "http://quantifiedmind.com/authenticate/get_token",
            success: function(data) {
                try {
                    var jsonToken = JSON.parse(data);
                    setQuantifiedMindToken(jsonToken.token);
                } catch (err) {
                    quantifiedMindLogin();
                }
            }
        })
    }

    function setQuantifiedMindToken(token) {
        $.ajax({
            url:"/quantifiedmind/setToken",
            data: { token: token },
            type: "POST",
            success: function(html) {
                $(".addConnectorsMain").html(html);
            },
            error: function() {
                alert("There was an error");
            }
        });
    }

    function quantifiedMindLogin() {
        $.ajax({
            url:"/quantifiedmind/login",
            success: function(html) {
                $(".addConnectorsMain").html(html);
            },
            error: function() {
                alert("There was an error");
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

	function chooseWithingsUser() {
        var chosenUser = $("input#withings-chosenUser:checked").val();
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
	Connectors.submitWithingsUsernameAndPassword = submitWithingsUsernameAndPassword;
	Connectors.submitZeoCredentials = submitZeoCredentials;
	Connectors.submitToodledoCredentials = submitToodledoCredentials;
	Connectors.submitNikePlusCredentials = submitNikePlusCredentials;
	Connectors.submitMymeeFetchURL = submitMymeeFetchURL;
	Connectors.submitMymeeAuthInfo = submitMymeeAuthInfo;
	Connectors.submitOpenPathKeypair = submitOpenPathKeypair;
	Connectors.getQuantifiedMindToken = getQuantifiedMindToken;
	Connectors.submitBedditAuthInfo = submitBedditAuthInfo;

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