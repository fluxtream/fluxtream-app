function submitCreateAccountForm() {
	var invitationCode = $("#invitationCode").val(),
		email = $("input#email").val(),
		password = $("input#password1").val(),
		password2 = $("input#password2").val(),
		username = $("input#username").val(),
		firstname = $("input#firstname").val(),
		lastname = $("input#lastname").val()
//		recaptchaChallenge = Recaptcha.get_challenge(),
//		recaptchaResponse = Recaptcha.get_response()
	$.ajax({
		url:"/createAccount",
		type: "POST",
		data: {invitationCode: invitationCode, email: email,
			password: password, password2: password2,
			username: username, firstname: firstname,
			lastname: lastname},
//			recaptchaChallenge : recaptchaChallenge,
//			recaptchaResponse : recaptchaResponse},
		success: function(html) {
			jQuery.facebox(html);
//			Recaptcha.create("6LeXl8QSAAAAAKWevmlwuQ5YoA8nrwx0s93ac8fu", "recaptcha", { theme: "red" } );
		}
	});
}

function createAccount() {
	$.ajax({
		url: "/createAccountForm",
		success: function(html) {
			jQuery.facebox(html);
			Recaptcha.create("6LeXl8QSAAAAAKWevmlwuQ5YoA8nrwx0s93ac8fu", "recaptcha", { theme: "red" } );
		}
	})
}
