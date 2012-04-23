define([], function() {
	
	var Builder = {};
	
	var widgets = {
		"DAY":["clock", "dashboard", "map", "diary", "photos", "list"],
		"WEEK":["dashboard", "map", "diary", "photos", "list"]
	};
	
	var widget_icons = {
		clock: "icon-time",
		dashboard: "icon-bar-chart",
		map: "icon-map-marker",
		diary: "icon-pencil",
		photos: "icon-camera",
		views: "icon-eye-open",
		list: "icon-list"
	};
	
	function capitalizeFirstLetter(string) {
	    return string.charAt(0).toUpperCase() + string.slice(1);
	}
	
	function createWidgetTabs(Log) {
		$("#widgetsTab").empty();
		for (var i=0; i<widgets[Log.timeUnit].length; i++) {
			var tab = "<li>";
			tab += "<a class=\"" + widgets[Log.timeUnit][i] + "-tab\" widget=" + widgets[Log.timeUnit][i] + " data-toggle=\"tab\">"
				+ "<i class=\"" + widget_icons[widgets[Log.timeUnit][i]] + "\"></i> "
				+ capitalizeFirstLetter(widgets[Log.timeUnit][i])
				+ "</a></li>";
			$("#widgetsTab").append(tab);
			var currentWidgetTab = "#widgetsTab a." + widgets[Log.timeUnit][i] +"-tab";
			$(currentWidgetTab).css("cursor", "pointer");
			$(currentWidgetTab).click(function(event) {
				var widget = $(event.target).attr("widget");
				var state = App.state.getState("log");
				state = state.substring(state.indexOf("/"));
				Log.renderState(widget+state);
			});
		}
		var w = widgetExistsForTimeUnit(Log.currentWidgetName, Log.timeUnit)?Log.currentWidgetName:widgets[Log.timeUnit][0];
		var currentWidgetTab = "#widgetsTab a." + w+"-tab";
		$(currentWidgetTab).tab("show");
	}
	
	function createTimeUnitsMenu(Log) {
		$("#time-menu").remove();
		var timeUnits = {"DAY":1, "WEEK":2, /*"MONTH": 3, "YEAR":4*/};
		delete timeUnits[Log.timeUnit];
		$(".loading").remove();
		var markup = "<div class=\"btn-group\" id=\"time-menu\">\
		<a class=\"btn\">"
				+ capitalizeFirstLetter(Log.timeUnit.toLowerCase()) + " View</a> <a class=\"btn dropdown-toggle\"\
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
						var w = widgetExistsForTimeUnit(Log.currentWidgetName, timeUnit)?Log.currentWidgetName:widgets[timeUnit][0];
						Log.currentWidgetName = w;
						Log.renderState(Log.currentWidgetName + "/" + response.state);
					},
					error : function() {
						alert("error");
					}
				});
			});
		}
		bindTimeNavButtons(Log);
	}
	
	function bindTimeNavButtons(Log) {
		switch(Log.timeUnit) {
		case "DAY":
			nextPrevEnable(true);
			timeNavBtn(Log, "down", false);
			timeNavBtn(Log, "up", true, "WEEK");
			break;
		case "WEEK":
			nextPrevEnable(true);
			timeNavBtn(Log, "down", true, "DAY");
			timeNavBtn(Log, "up", false, "MONTH");
			break;
//		case "MONTH":
//			nextPrevEnable(true);
//			timeNavBtn(Log, "down", true, "WEEK");
//			timeNavBtn(Log, "up", true, "YEAR");
//			break;
//		case "YEAR":
//			nextPrevEnable(true);
//			timeNavBtn(Log, "down", true, "MONTH");
//			timeNavBtn(Log, "up", false, "CONTINUOUS");
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
	
	function timeNavBtn(Log, downOrUp, enabled, targetTimeUnit) {
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
						Log.renderState(Log.currentWidgetName + "/" + response.state);
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
	
	function updateWidget(digest, Log) {
		handleNotifications(digest);
		$("#widgets").empty();
		require([ "applications/log/widgets/" + Log.currentWidgetName + "/"
				+ capitalizeFirstLetter(Log.currentWidgetName) + "Widget"],
				function(widget) {
			Log.currentWidget = widget;
			var currentWidgetTab = "#widgetsTab a." + Log.currentWidgetName+"-tab";
			$(currentWidgetTab).tab("show");
			widget.render(digest, Log.timeUnit, Log.widgetState);
		});
	}
	
	function widgetExistsForTimeUnit(widget, unit) {
		var widgetExistsForTimeUnit = false;
		for (var i=0; i<widgets[unit].length; i++) {
			if (widgets[unit][i]===widget)
				widgetExistsForTimeUnit = true;
		}
		return widgetExistsForTimeUnit;
	}
	
	Builder.widgetExistsForTimeUnit = widgetExistsForTimeUnit;
	Builder.widgets = widgets;
	Builder.createTimeUnitsMenu = createTimeUnitsMenu;
	Builder.createWidgetTabs = createWidgetTabs;
	Builder.updateWidget = updateWidget;
	
	return Builder;
	
});
