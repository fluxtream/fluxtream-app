define([], function() {
	
	var Builder = {};
	var tabs = {};
	
	var tabs = {
		"DAY":["clock", "dashboard", "map", "diary", "photos", "list", "timeline"],
		"WEEK":["dashboard", "map", "diary", "photos", "list", "timeline"]
	};
	
	var tab_icons = {
		clock: "icon-time",
		dashboard: "icon-bar-chart",
		map: "icon-map-marker",
		diary: "icon-pencil",
		photos: "icon-camera",
		views: "icon-eye-open",
		list: "icon-list",
        timeline: "icon-camera"
    };
	
	function capitalizeFirstLetter(string) {
	    return string.charAt(0).toUpperCase() + string.slice(1);
	}
	
	function createTabs(Calendar) {
		$("#calendarTabs").empty();
		for (var i=0; i<tabs[Calendar.timeUnit].length; i++) {
			var tab = "<li>";
			tab += "<a class=\"" + tabs[Calendar.timeUnit][i] + "-tab\" tab=" + tabs[Calendar.timeUnit][i] + " data-toggle=\"tab\">"
				+ "<i class=\"" + tab_icons[tabs[Calendar.timeUnit][i]] + "\"></i> "
				+ capitalizeFirstLetter(tabs[Calendar.timeUnit][i])
				+ "</a></li>";
			$("#calendarTabs").append(tab);
			var currentTab = "#calendarTabs a." + tabs[Calendar.timeUnit][i] +"-tab";
			$(currentTab).css("cursor", "pointer");
			$(currentTab).click(function(event) {
				var tab = $(event.target).attr("tab");
				var state = App.state.getState("calendar");
				state = state.substring(state.indexOf("/"));
				Calendar.renderState(tab+state);
			});
		}
		var t = tabExistsForTimeUnit(Calendar.currentTabName, Calendar.timeUnit)?Calendar.currentTabName:tabs[Calendar.timeUnit][0];
		var currentTab = "#calendarTabs a." + t+"-tab";
		$(currentTab).tab("show");
	}
	
	function createTimeUnitsMenu(Calendar) {
		$("#time-menu").remove();
		var timeUnits = {"DAY":1, "WEEK":2 /*,"MONTH": 3, "YEAR":4*/};
		delete timeUnits[Calendar.timeUnit];
		$(".loading").remove();
		var markup = "<div class=\"btn-group\" id=\"time-menu\">\
		<a class=\"btn\">"
				+ capitalizeFirstLetter(Calendar.timeUnit.toLowerCase()) + " View</a> <a class=\"btn dropdown-toggle\"\
			data-toggle=\"dropdown\" href=\"#\"> <span class=\"caret\"></span>\
		</a>\
		<ul class=\"dropdown-menu\" id=\"timeUnits\">";
		for (var timeUnitName in timeUnits) {
			markup += "<li><a href=\"#\" class=\"" + timeUnitName + "\">"
				+ capitalizeFirstLetter(timeUnitName.toLowerCase())
				+" View</a></li>";
		}
		markup += "</ul></div>";
		markup += "<span class=\"loading\"><img src=\"/static/img/loading.gif\"/></span>";
		$("#calendar-menubar").append(markup);
		for (timeUnitName in timeUnits) {
			$("#time-menu a." + timeUnitName).click(function(event) {
				var timeUnit = $(event.target).attr("class"),
					url = "/nav/set" + capitalizeFirstLetter(timeUnit.toLowerCase()) + "TimeUnit.json";
				$.ajax({ url:url,
					success : function(response) {
						var t = tabExistsForTimeUnit(Calendar.currentTabName, timeUnit)?Calendar.currentTabName:tabs[timeUnit][0];
						Calendar.currentTabName = t;
						Calendar.renderState(Calendar.currentTabName + "/" + response.state);
					},
					error : function() {
						alert("error");
					}
				});
			});
		}
		bindTimeNavButtons(Calendar);
	}
	
	function bindTimeNavButtons(Calendar) {
		switch(Calendar.timeUnit) {
		case "DAY":
			nextPrevEnable(true);
			timeNavBtn(Calendar, "down", false);
			timeNavBtn(Calendar, "up", true, "WEEK");
			break;
		case "WEEK":
			nextPrevEnable(true);
			timeNavBtn(Calendar, "down", true, "DAY");
			timeNavBtn(Calendar, "up", false, "MONTH");
			break;
//		case "MONTH":
//			nextPrevEnable(true);
//			timeNavBtn(Calendar, "down", true, "WEEK");
//			timeNavBtn(Calendar, "up", true, "YEAR");
//			break;
//		case "YEAR":
//			nextPrevEnable(true);
//			timeNavBtn(Calendar, "down", true, "MONTH");
//			timeNavBtn(Calendar, "up", false, "CONTINUOUS");
//			break;
		}
	};
	
	function nextPrevEnable(b) {
		if (b) {
			$(".menuNextButton").removeClass("disabled");
			$(".menuPrevButton").removeClass("disabled");
		} else {
			$(".menuNextButton").addClass("disabled");
			$(".menuPrevButton").addClass("disabled");
		}
	};
	
	function timeNavBtn(Calendar, downOrUp, enabled, targetTimeUnit) {
		var button = $(".menu"+capitalizeFirstLetter(downOrUp)+"Button");
		button.unbind();
		if (!enabled)
			button.addClass("disabled");
		else {
			button.removeClass("disabled");
			button.click(function(event) {
				var timeUnit = $(event.target).attr("class"),
				url = "/nav/set" + capitalizeFirstLetter(targetTimeUnit.toLowerCase()) + "TimeUnit.json";
				$.ajax({ url:url,
					success : function(response) {
						Calendar.renderState(Calendar.currentTabName + "/" + response.state);
					},
					error : function() {
						alert("error");
					}
				});
			});
		}
	}

	function handleNotifications(digestInfo) {
		$(".notification").remove();
		$("#notificationIds").empty();
		if (typeof(digestInfo.notifications)!="undefined") {
			$(".alert-message").addClass("success");
			for (n=0; n<digestInfo.notifications.length; n++) {
				var notification = digestInfo.notifications[n];
				$("#notifications").append("<p class=\"notification\">" + notification.message + "</p>");
				if (n>0)
					$("#notificationIds").append(",");
				$("#notificationIds").append(notification.id);
			}
			$("#notifications").show();
		}
	}
	
	function updateTab(digest, Calendar) {
		handleNotifications(digest);
		if (tabs[Calendar.currentTabName]==null) {
			require([ "applications/calendar/tabs/" + Calendar.currentTabName + "/"
					+ capitalizeFirstLetter(Calendar.currentTabName) + "tab"],
				function(tab) {
					tabs[Calendar.currentTavName] = tab;
					renderTab(tab, digest, Calendar);
				}
			);
		} else {
			renderTab(tabs[Calendar.currentTabName], digest, Calendar);
		}
	}
	
	function renderTab(tab, digest, Calendar) {
		Calendar.currentTab = tab;
		var currentTab = "#calendarTabs a." + Calendar.currentTabName+"-tab";
		$(currentTab).tab("show");
		tab.render(digest, Calendar.timeUnit, Calendar.tabState);
	}
	
	function tabExistsForTimeUnit(tab, unit) {
		var tabExistsForTimeUnit = false;
		for (var i=0; i<tabs[unit].length; i++) {
			if (tabs[unit][i]===tab)
				tabExistsForTimeUnit = true;
		}
		return tabExistsForTimeUnit;
	}
	
	Builder.tabExistsForTimeUnit = tabExistsForTimeUnit;
	Builder.tabs = tabs;
	Builder.createTimeUnitsMenu = createTimeUnitsMenu;
	Builder.createTabs = createTabs;
	Builder.updateTab= updateTab;
	
	return Builder;
	
});
