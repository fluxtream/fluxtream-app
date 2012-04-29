define(["core/Application", "core/FlxState", "applications/calendar/Builder", "libs/bootstrap-datepicker"], function(Application, FlxState, Builder) {

	var Calendar = new Application("calendar", "Candide Kemmler", "icon-calendar");

    Calendar.currentTabName = Builder.tabs["DAY"][0];
    Calendar.currentTab = null;
    Calendar.tabState = null;
    Calendar.digest = null;
    Calendar.timeUnit = "DAY";

	var start, end;
	
	Calendar.setup = function() {
		$(".menuNextButton").click(function(e) {
			fetchState("/nav/incrementTimespan.json"); });
		$(".menuPrevButton").click(function(e) {
			fetchState("/nav/decrementTimespan.json"); });
		$(".menuTodayButton").click(function(e) {
			Calendar.timeUnit = "DAY";
			var t = Builder.tabExistsForTimeUnit(Calendar.currentTabName, Calendar.timeUnit)?Calendar.currentTabName:Builder.tabs[Calendar.timeUnit][0];
			Calendar.currentTabName = t;
			Builder.createTimeUnitsMenu(Calendar);
			Builder.createTabs(Calendar);
			fetchState("/nav/setToToday.json");
		});
	};
	
	Calendar.initialize = function () {
		_.bindAll(this);
		for (var i=0; i<Builder.tabs[Calendar.timeUnit].length; i++) {
			FlxState.router.route("app/calendar/:tab/date/:date", "", function(tab, date) {
				var w = Builder.tabExistsForTimeUnit(tab, Calendar.timeUnit)?tab:Builder.tabs[Calendar.timeUnit][0];
				Calendar.render(w + "/date/" + date);
			});
			FlxState.router.route("app/calendar/:tab/year/:year", "", function(tab, year) {
				var w = Builder.tabExistsForTimeUnit(tab, Calendar.timeUnit)?tab:Builder.tabs[Calendar.timeUnit][0];
				Calendar.render(w + "/year/" + year);
			});
			FlxState.router.route("app/calendar/:tab/month/:year/:month", "", function(tab, year, month) {
				var w = Builder.tabExistsForTimeUnit(tab, Calendar.timeUnit)?tab:Builder.tabs[Calendar.timeUnit][0];
				Calendar.render(w + "/month/" + year + "/" + month);
			});
			FlxState.router.route("app/calendar/:tab/week/:year/:week", "", function(tab, year, week) {
				var w = Builder.tabExistsForTimeUnit(tab, Calendar.timeUnit)?tab:Builder.tabs[Calendar.timeUnit][0];
				Calendar.render(w + "/week/" + year + "/" + week);
			});
		}
	};
		
	Calendar.renderState = function(state, force) {
		if (!force&&FlxState.getState("calendar")===state) {
			return;
		}
		if (state==null||state==="") {
			Builder.createTimeUnitsMenu(Calendar);
			Builder.createTabs(Calendar);
			fetchState("/nav/setToToday.json");
            return;
		}
		var splits = state.split("/");
		Calendar.currentTabName = splits[0];
		Calendar.timeUnit = toTimeUnit(splits[1]);
		var nextTabState = state.substring(splits[0].length+1);
		if (Calendar.tabState==nextTabState) {
			// time didn't change
			var w = Builder.tabExistsForTimeUnit(Calendar.currentTabName, Calendar.timeUnit)?Calendar.currentTabName:Builder.tabs[Calendar.timeUnit][0];
			Calendar.currentTabName = w;
			Builder.createTabs(Calendar);
			Builder.updateTab(Calendar.digest, Calendar);
			FlxState.router.navigate("app/calendar/" + state);
			FlxState.saveState("calendar", state);
			return;
		} else {
			var w = Builder.tabExistsForTimeUnit(Calendar.currentTabName, Calendar.timeUnit)?Calendar.currentTabName:Builder.tabs[Calendar.timeUnit][0];
			Calendar.currentTabName = w;
			Builder.createTimeUnitsMenu(Calendar);
			Builder.createTabs(Calendar);
			if ("DAY"===Calendar.timeUnit) {
				fetchState("/nav/setDate.json?date=" + splits[2]);
			} else if ("WEEK"===Calendar.timeUnit) {
				fetchState("/nav/setWeek.json?year=" + splits[2] + "&week=" + splits[3]);
			} else if ("MONTH"===Calendar.timeUnit) {
				fetchState("/nav/setMonth.json?year=" + splits[2] + "&month=" + splits[3]);
			} else if ("YEAR"===Calendar.timeUnit) {
				fetchState("/nav/setYear.json?year=" + splits[2]);
			}
		}
	};
	
	function fetchState(url) {
		$(".calendar-navigation-button").toggleClass("disabled");
		$(".loading").show();
		$("#tabs").css("opacity", ".3");
		$.ajax({ url:url,
			success : function(response) {
				if (Calendar.currentTab) {
					Calendar.currentTab.saveState();
				}
				Calendar.tabState = response.state;
				FlxState.router.navigate("app/calendar/" + Calendar.currentTabName + "/" + response.state);
				FlxState.saveState("calendar", Calendar.currentTabName + "/" + response.state);
				$("#currentTimespanLabel span").html(response.currentTimespanLabel);
				if (Calendar.timeUnit==="DAY") {
					setDatepicker(response.state.split("/")[1]);
				}
				fetchCalendar("/api/calendar/all/" + response.state);
			},
			error : function() {
				alert("error");
			}
		});
	}
	
	function setDatepicker(currentDate) {
		$("#datepicker").attr("data-date", currentDate);
		$("#datepicker").unbind("changeDate");
		$("#datepicker").datepicker().on(
			"changeDate", function(event) {
				var curr_date = event.date.getDate();
				var curr_month = event.date.getMonth() + 1;
				var curr_year = event.date.getFullYear();
				var formatted = curr_year + "-" + curr_month + "-" + curr_date;
				fetchState("/nav/setDate.json?date=" + formatted);
				$(".datepicker").hide();
			}
		);
	}
	
	function fetchCalendar(url) {
		$.ajax({ url: url,
			success : function(response) {
				$("#modal").empty();
                Calendar.digest = response;
				Builder.updateTab(response, Calendar);
				$("#tabs").css("opacity", "1");
				$(".calendar-navigation-button").toggleClass("disabled");
				$(".loading").hide();
			},
			error: function() {
				alert("error fetching calendar");
			}
		});
	}

	function toTimeUnit(urlTimeUnit) {
		if (urlTimeUnit==="date") return "DAY";
		return urlTimeUnit.toUpperCase();
	}
	
	return Calendar;
	
});