define(["core/Application", "core/FlxState", "applications/log/Builder"], function(Application, FlxState, Builder) {

	var Log = new Application("log", "Candide Kemmler", "icon-calendar");
	
	Log.currentWidget = Builder.widgets["DAY"][0];
	Log.timeUnit = "DAY";
	
	Log.setup = function() {
		$(".menuNextButton").click(function(e) {
			fetchState("/nav/incrementTimespan.json"); });
		$(".menuPrevButton").click(function(e) {
			fetchState("/nav/decrementTimespan.json"); });
		$(".menuTodayButton").click(function(e) {
			Log.timeUnit = "DAY";
			var w = Builder.widgetExistsForTimeUnit(Log.currentWidget, Log.timeUnit)?Log.currentWidget:Builder.widgets[Log.timeUnit][0];
			Log.currentWidget = w;
			Builder.createTimeUnitsMenu(Log);
			Builder.createWidgetTabs(Log);
			fetchState("/nav/setToToday.json");
		});
	}
	
	Log.initialize = function () {
		_.bindAll(this);
		for (var i=0; i<Builder.widgets[Log.timeUnit].length; i++) {
			FlxState.router.route("app/log/:widget/date/:date", "", function(widget, date) {
				var w = Builder.widgetExistsForTimeUnit(widget, Log.timeUnit)?widget:Builder.widgets[Log.timeUnit][0];
				Log.render(w + "/date/" + date);
			});
			FlxState.router.route("app/log/:widget/year/:year", "", function(widget, year) {
				var w = Builder.widgetExistsForTimeUnit(widget, Log.timeUnit)?widget:Builder.widgets[Log.timeUnit][0];
				Log.render(w + "/year/" + year);
			});
			FlxState.router.route("app/log/:widget/month/:year/:month", "", function(widget, year, month) {
				var w = Builder.widgetExistsForTimeUnit(widget, Log.timeUnit)?widget:Builder.widgets[Log.timeUnit][0];
				Log.render(w + "/month/" + year + "/" + month);
			});
			FlxState.router.route("app/log/:widget/week/:year/:week", "", function(widget, year, week) {
				var w = Builder.widgetExistsForTimeUnit(widget, Log.timeUnit)?widget:Builder.widgets[Log.timeUnit][0];
				Log.render(w + "/week/" + year + "/" + week);
			});
			FlxState.router.route("app/log/:widget/continuous", "", function(widget) {
				var w = Builder.widgetExistsForTimeUnit(widget, Log.timeUnit)?widget:Builder.widgets[Log.timeUnit][0];
				Log.render(w + "/continuous");
			});
		}
	}
		
	Log.renderState = function(state, force) {
		if (!force&&FlxState.getState("log")===state) {
			return;
		}
		if (state==null||state==="") {
			Builder.createTimeUnitsMenu(Log);
			Builder.createWidgetTabs(Log);
			fetchState("/nav/setToToday.json");
		}
		else {
			var splits = state.split("/");
			Log.currentWidget = splits[0];
			Log.timeUnit = toTimeUnit(splits[1]);
			var w = Builder.widgetExistsForTimeUnit(Log.currentWidget, Log.timeUnit)?Log.currentWidget:Builder.widgets[Log.timeUnit][0];
			Log.currentWidget = w;
			Builder.createTimeUnitsMenu(Log);
			Builder.createWidgetTabs(Log);
			if ("DAY"===Log.timeUnit) {
				fetchState("/nav/setDate.json?date=" + splits[2]);
			} else if ("WEEK"===Log.timeUnit) {
				fetchState("/nav/setWeek.json?year=" + splits[2] + "&week=" + splits[3]);
			} else if ("MONTH"===Log.timeUnit) {
				fetchState("/nav/setMonth.json?year=" + splits[2] + "&month=" + splits[3]);
			} else if ("YEAR"===Log.timeUnit) {
				fetchState("/nav/setYear.json?year=" + splits[2]);
			} else if ("CONTINUOUS"===Log.timeUnit) {
				fetchState("/nav/setContinuousTimeUnit.json");
			}
		}
	}
	
	function fetchState(url) {
		$(".calendar-navigation-button").toggleClass("disabled");
		$(".loading").show();
		$("#widgets").css("opacity", ".3");
		$.ajax({ url:url,
			success : function(response) {
				FlxState.router.navigate("app/log/" + Log.currentWidget + "/" + response.state);
				FlxState.saveState("log", Log.currentWidget + "/" + response.state);
				$("#currentTimespanLabel span").html(response.currentTimespanLabel);
				fetchLog("/api/log/all/" + response.state);
			},
			error : function() {
				alert("error");
			}
		});
	}
	
	function fetchLog(url) {
		$.ajax({ url: url,
			success : function(response) {
				Builder.updateWidget(response, Log);
				$("#widgets").css("opacity", "1");
				$(".calendar-navigation-button").toggleClass("disabled");
				$(".loading").hide();
			},
			error: function() {
				alert("error fetching log");
			}
		});
	}
	
	function toTimeUnit(urlTimeUnit) {
		if (urlTimeUnit==="date") return "DAY";
		return urlTimeUnit.toUpperCase();
	}
	
	return Log;
	
});