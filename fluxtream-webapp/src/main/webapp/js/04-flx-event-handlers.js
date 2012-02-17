function handleClicks() {
	handleNext();
	$(".menuPrevButton").click(function(e) {
		$.ajax({ url: "/home/decrementTimespan.json?force="+e.shiftKey, dataType: "json",
			success: function(model) { updateTimeBrowser(model); }}); });
	$(".menuTodayButton").click(function(e) {
		$.ajax({ url: "/home/setToToday.json?force="+e.shiftKey, dataType: "json",
			success: function(model) { updateTimeBrowser(model); }}); });
	$(".menuDayButton").click(function() {
		$.ajax({ url: "/home/setDayTimeUnit.json", dataType: "json",
			success: function(model) { updateTimeBrowser(model); }}); });
	$(".menuWeekButton").click(function() {
		$.ajax({ url: "/home/setWeekTimeUnit.json", dataType: "json",
			success: function(model) { updateTimeBrowser(model); }}); });
	$(".menuMonthButton").click(function() {
		$.ajax({ url: "/home/setMonthTimeUnit.json", dataType: "json",
			success: function(model) { updateTimeBrowser(model); }}); });
	$(".menuYearButton").click(function() {
		$.ajax({ url: "/home/setYearTimeUnit.json", dataType: "json",
			success: function(model) { updateTimeBrowser(model); }}); });
	
	$(".btnLeft").click(function() {
		$.ajax({ url: "/home/setVisualizationType.json?visualizationType=CLOCK", dataType: "json",
			success: function(model) { updateTimeBrowser(model); }}); });
	$(".btnRight").click(function() {
		$.ajax({ url: "/home/setVisualizationType.json?visualizationType=LIST", dataType: "json",
			success: function(model) { updateTimeBrowser(model); }}); });
	$(".btnTools").click(function() {
		$.ajax({ url: "/home/setVisualizationType.json?visualizationType=TOOLS", dataType: "json",
			success: function(model) { updateTimeBrowser(model); }}); });
}

function showClockHelp() {
	$.ajax({url:"/help/clock", success:function(html) {
		$.facebox(html);
	}});
}

function initSequenceExit() {
	var confirmed = confirm("Are you sure you want to exit this sequence. This will log you out.")
	if (confirmed) {
		window.location = "/logout";
	}
}

function geocode(address) {
    geocoder = new google.maps.Geocoder();
    geocoder.geocode({ 'address': address}, function(results, status) {
    	if (status == google.maps.GeocoderStatus.OK) {
			$(".geocode_addresses").empty();
			$(".geocode_addresses").append("<p>" + results[0].formatted_address + "</p>");
			$("#geocode_address").val(results[0].formatted_address);
			$("#geocode_latitude").val(results[0].geometry.location.lat());
			$("#geocode_longitude").val(results[0].geometry.location.lng());
    	} else {
    		$(".geocode_addresses").append("We couldn't find this address (" + status + ")")
    	}
    });
}

function setAddress(change) {
	$.ajax({
		url:(typeof(change)=="undefined")?"/settings/setAddressForm":"/settings/setAddressForm?change=true",
		success: function(html) {
			jQuery.facebox(html);
			$('#setAddressForm').ajaxForm(function(status) {
				if (status.result=="OK") {
					$.facebox.close();
				} else {
					$("#setAddress_errorMessage").html(status.message);
					$("#setAddress_errorMessage").show();
				}
			});
			$('#sinceDate').DatePicker({
				format:'m/d/Y',
				date: $('#sinceDate').val(),
				current: $('#sinceDate').val(),
				starts: 1,
				position: 'r',
				onBeforeShow: function(){
					$('#sinceDate').DatePickerSetDate($('#sinceDate').val(), true);
				},
				onChange: function(formated, dates){
					$('#sinceDate').val(formated);
//					$('#sinceDate').DatePickerHide();
				}
			});
		}
	});
}

function handleNext() {
	$(".menuNextButton").click(function(e) {
		$.ajax({ url: "/home/incrementTimespan.json?force="+e.shiftKey, dataType: "json",
			success: function(model) { updateTimeBrowser(model); }}); });
}

function addConnector(url) {
	if (startsWith(url, "ajax:")) {
		savedConnectorContent = $(".addConnectorsMain").html();
		$.ajax({ url: url.substring(5), success: function(html) {
				$(".addConnectorsMain").html(html);
				$(".focushere").focus();
			}
		});
	} else {
		var loading = $("#loading").clone().show();
		$(".addConnectorsMain").empty();
		$(".addConnectorsMain").append(loading);
		setTimeout("window.location='"+url + "'", 500);
	}
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
	var chosenUser = $("input#withings-chosenUser").val();
	$.ajax({
		url:"/withings/chooseWithingsUser",
		type: "POST",
		data: {chosenUser: chosenUser},
		success: function(html) {
			$(".addConnectorsMain").html(html);
		}
	});
}

function connectorDescription(api) {
	$(".addConnectorsMain").load("/connectors/connectorDescription?api="+api);
}

function connectorSettings(api) {
	$(".addConnectorsMain").load("/connectors/connectorSettings?api="+api);
}

function eraseEverything() {
	var sure = confirm("We will erase all your data, " +
			"comments and account information. Forever.\nAre you sure?");
	if (sure) {
		$.ajax({
			url:"/eraseEverything",
			type: "GET",
			success: function(html) {
				document.location="/logout";
			}
		});
	}
}

function removeConnector(api) {
	var c = confirm("If you wrote comments on events related to this connector, " +
			"you will loose them forever.\n" +
			"Are your sure you want to continue?");
	if (c) {
		$.ajax({
			url: "/connectors/removeConnector?api="+api,
			dataType: "json",
			success: function(data) {
				if (data.result=="ok") {
					$("#userConnectors").load("/connectors/userConnectors");
					showConnectorsPage(0);
				}
			}
		});
	}
}

function congrats(api) {
	jQuery.facebox("Congratulations, you have now added a new connector for " + api);
}

function showConnectorsPage(page) {
	$("#availableConnectors").load("/connectors/availableConnectors?page="+page);
}

function viewAllConnectors() {
	$(".addConnectorsMain").load("/connectors/main?all=1");
}

function hideMap() {
	$(".sideBarBlock.mapSbar").hide();
}

function showMap() {
	$(".sideBarBlock.mapSbar").show();
	var myOptions = {
	  zoom: 11,
	  scrollwheel: false,
	  streetViewControl: false,
	  mapTypeControl: false,
	  mapTypeId: google.maps.MapTypeId.ROADMAP
	}
	FlxState.map = new google.maps.Map(document.getElementById("the_map"), myOptions);
}

function setCurrentTimespanLabel(label) {
	var currentTimespanLabel = $("#currentTimespanLabel");
	currentTimespanLabel.text(label);
}

function setToday(b) {
	var menuDayButton = $(".menuTodayButton");
	if (b) {
		menuDayButton.addClass("timeMenuButtonOn");
		$(".menuNextButton").unbind();
		$(".menuNextButton").click(function() {
			$(".menuNextButton").qtip({
			   content: {
			      text: "We cannot browse future dates, yet.<br/>Don't worry though, we are working hard on this issue"
			   },
			   style: {
			      classes: 'ui-tooltip-light ui-tooltip-shadow ui-tooltip-rounded',
			   },
			   position: {
			   	   my: "left middle",
			   	   at: "right middle",
			   	   adjust: { x: 10 }
			   },
		       show: {
		          ready: true, // Show it straight away
		       },
		       hide: {
				  effect: function(offset) {
				      $(this).slideDown(100); // "this" refers to the tooltip
				  },
				  inactive : 3000	
		       }
			});
		});
	} else {
		menuDayButton.removeClass("timeMenuButtonOn");
		$(".menuNextButton").unbind();
		handleNext();
	}
}

function setTimeUnit(timeUnit) {
	$(".menuDayButton").removeClass("timeMenuButtonOn");
	$(".menuWeekButton").removeClass("timeMenuButtonOn");
	$(".menuMonthButton").removeClass("timeMenuButtonOn");
	$(".menuYearButton").removeClass("timeMenuButtonOn");
	switch(timeUnit) {
	case "DAY":
		$(".menuDayButton").addClass("timeMenuButtonOn");
		break;
	case "WEEK":
		$(".menuWeekButton").addClass("timeMenuButtonOn");
		break;
	case "MONTH":
		$(".menuMonthButton").addClass("timeMenuButtonOn");
		break;
	case "YEAR":
		$(".menuYearButton").addClass("timeMenuButtonOn");
		break;
	}
}

function setSelectedDate(formattedDate) {
	$.ajax({ url: "/home/setDate.json?date=" + formattedDate, dataType: "json",
		success: function(model) { updateTimeBrowser(model); }});
}

function settings() {
	$.ajax({ url: "/settings/main", dataType: "html",
		success: function(html) {
			$.facebox(html);
			$('#settingsForm').ajaxForm(function() {$.facebox.close();})
		}
	});
}

function connectors() {
	$.ajax({ url: "/connectors/main", dataType: "html",
		success: function(html) { $.facebox(html); }});
}
