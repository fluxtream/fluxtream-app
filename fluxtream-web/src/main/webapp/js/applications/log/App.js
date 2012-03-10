define(["core/Application",
        "core/FlxState",
        "libs/jquery.qtip"],
        function(Application, FlxState)
{
	
	Date.prototype.format=function(format){var returnStr='';var replace=Date.replaceChars;for(var i=0;i<format.length;i++){var curChar=format.charAt(i);if(i-1>=0&&format.charAt(i-1)=="\\"){returnStr+=curChar}else if(replace[curChar]){returnStr+=replace[curChar].call(this)}else if(curChar!="\\"){returnStr+=curChar}}return returnStr};Date.replaceChars={shortMonths:['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'],longMonths:['January','February','March','April','May','June','July','August','September','October','November','December'],shortDays:['Sun','Mon','Tue','Wed','Thu','Fri','Sat'],longDays:['Sunday','Monday','Tuesday','Wednesday','Thursday','Friday','Saturday'],d:function(){return(this.getDate()<10?'0':'')+this.getDate()},D:function(){return Date.replaceChars.shortDays[this.getDay()]},j:function(){return this.getDate()},l:function(){return Date.replaceChars.longDays[this.getDay()]},N:function(){return this.getDay()+1},S:function(){return(this.getDate()%10==1&&this.getDate()!=11?'st':(this.getDate()%10==2&&this.getDate()!=12?'nd':(this.getDate()%10==3&&this.getDate()!=13?'rd':'th')))},w:function(){return this.getDay()},z:function(){var d=new Date(this.getFullYear(),0,1);return Math.ceil((this-d)/86400000)}, W:function(){var d=new Date(this.getFullYear(),0,1);return Math.ceil((((this-d)/86400000)+d.getDay()+1)/7)},F:function(){return Date.replaceChars.longMonths[this.getMonth()]},m:function(){return(this.getMonth()<9?'0':'')+(this.getMonth()+1)},M:function(){return Date.replaceChars.shortMonths[this.getMonth()]},n:function(){return this.getMonth()+1},t:function(){var d=new Date();return new Date(d.getFullYear(),d.getMonth(),0).getDate()},L:function(){var year=this.getFullYear();return(year%400==0||(year%100!=0&&year%4==0))},o:function(){var d=new Date(this.valueOf());d.setDate(d.getDate()-((this.getDay()+6)%7)+3);return d.getFullYear()},Y:function(){return this.getFullYear()},y:function(){return(''+this.getFullYear()).substr(2)},a:function(){return this.getHours()<12?'am':'pm'},A:function(){return this.getHours()<12?'AM':'PM'},B:function(){return Math.floor((((this.getUTCHours()+1)%24)+this.getUTCMinutes()/60+this.getUTCSeconds()/ 3600) * 1000/24)}, g:function(){return this.getHours()%12||12},G:function(){return this.getHours()},h:function(){return((this.getHours()%12||12)<10?'0':'')+(this.getHours()%12||12)},H:function(){return(this.getHours()<10?'0':'')+this.getHours()},i:function(){return(this.getMinutes()<10?'0':'')+this.getMinutes()},s:function(){return(this.getSeconds()<10?'0':'')+this.getSeconds()},u:function(){var m=this.getMilliseconds();return(m<10?'00':(m<100?'0':''))+m},e:function(){return"Not Yet Supported"},I:function(){return"Not Yet Supported"},O:function(){return(-this.getTimezoneOffset()<0?'-':'+')+(Math.abs(this.getTimezoneOffset()/60)<10?'0':'')+(Math.abs(this.getTimezoneOffset()/60))+'00'},P:function(){return(-this.getTimezoneOffset()<0?'-':'+')+(Math.abs(this.getTimezoneOffset()/60)<10?'0':'')+(Math.abs(this.getTimezoneOffset()/60))+':00'},T:function(){var m=this.getMonth();this.setMonth(0);var result=this.toTimeString().replace(/^.+ \(?([^\)]+)\)?$/,'$1');this.setMonth(m);return result},Z:function(){return-this.getTimezoneOffset()*60},c:function(){return this.format("Y-m-d\\TH:i:sP")},r:function(){return this.toString()},U:function(){return this.getTime()/1000}};
	
	var widgets = {
		"DAY":["clock", "summary", "map", "diary", "photos"],
		"WEEK":["summary", "map", "diary", "photos"],
		"MONTH":["summary", "map", "diary", "photos"],
		"YEAR":["summary", "map", "diary", "photos"]
	};
	
	var Log = new Application("log", "Candide Kemmler", "icon-calendar");
	
	Log.currentWidget = widgets["DAY"][0];
	Log.widgets = widgets;
	Log.initialize = initialize;
	Log.timeUnit = "DAY";
	
	Log.renderState = function(state) {
		if (state==null||state==="") {
			gotoToday();
		}
		else {
			var splits = state.split("/");
			this.currentWidget = splits[0];
			if ("date"===splits[1]) {
				gotoDate(splits[2]);
			} else if ("week"===splits[1]) {
				gotoWeek(splits[2]);
			} else if ("month"===splits[1]) {
				gotoMonth(splits[2]);
			} else if ("year"===splits[1]) {
				gotoYear(splits[2]);
			}
		}
	}
	
	Log.setupTimeUnit = function() {
		console.log("creating time unit widgets...");
		$("#time-menu").remove();
		$("#widgetsTab").empty();
		createTimeUnitsMenu();
		createWidgetTabs();
	}
	
	function toTimeUnit(urlTimeUnit) {
		if (urlTimeUnit==="date") return "DAY";
		return urlTimeUnit.toUpperCase();
	}
	
	Log.setup = function() {
		bindNavigationEvents();
		Log.setupTimeUnit();
	}
	
	function createTimeUnitsMenu() {
		var timeUnits = {DAY:1, WEEK:2, MONTH: 3, YEAR:4};
		delete timeUnits[Log.timeUnit];
		console.log("Log.timeUnit:" + Log.timeUnit);
		var markup = "<div class=\"btn-group\" id=\"time-menu\">\
		<a class=\"btn\" href=\"#\">"
				+ capitalizeFirstLetter(Log.timeUnit.toLowerCase()) + " View</a> <a class=\"btn dropdown-toggle\"\
			data-toggle=\"dropdown\" href=\"#\"> <span class=\"caret\"></span>\
		</a>\
		<ul class=\"dropdown-menu\" id=\"timeUnits\">";
		for (name in timeUnits) {
			markup += "<li><a href=\"#\" class=\"" + name + "\">"
				+ capitalizeFirstLetter(name.toLowerCase())
				+" View</a></li>";
		}
		markup += "</ul></div>"
		$("#calendar-menubar").append(markup);
		for (name in timeUnits) {
			$("#time-menu a." + name).click(function(event) {
				var timeUnit = $(event.target).attr("class");
				Log.timeUnit = timeUnit;
				Log.setupTimeUnit();
				fetchState("/nav/set" + capitalizeFirstLetter(timeUnit.toLowerCase()) + "TimeUnit.json");
			});
		}
	}
	
	function createWidgetTabs() {
		for (var i=0; i<widgets[Log.timeUnit].length; i++) {
			var tab = "<li>";
			tab += "<a class=\"" + widgets[Log.timeUnit][i] + "-tab\" widget=" + widgets[Log.timeUnit][i] + " data-toggle=\"tab\">"
				+ capitalizeFirstLetter(widgets[Log.timeUnit][i])
				+ "</a></li>";
			$("#widgetsTab").append(tab);
			var currentWidgetTab = "#widgetsTab a." + widgets[Log.timeUnit][i] +"-tab";
			$(currentWidgetTab).css("cursor", "pointer");
			$(currentWidgetTab).click(function(event) {
				var widget = $(event.target).attr("widget");
				var state = App.state.getState("log");
				console.log("clicking a tab, state is " + state);
				state = state.substring(state.indexOf("/"));
				Log.renderState(widget+state);
			});
		}
		$("."+this.currentWidget + "-tab").parent().addClass("active");
	}
	
	function fetchState(url) {
		$(".calendar-navigation-button").toggleClass("disabled");
		$(".loading").show();
		$("#widgets").css("opacity", ".3");
		$.ajax({ url:url,
			success : function(response) {
				FlxState.router.navigate("app/log/" + Log.currentWidget + "/" + response.state, {trigger: false});
				FlxState.saveState("log", Log.currentWidget + "/" + response.state);
				$("#currentTimespanLabel").html(response.currentTimespanLabel);
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
				updateWidget(response);
				$("#widgets").css("opacity", "1");
				$(".calendar-navigation-button").toggleClass("disabled");
				$(".loading").hide();
			},
			error: function() {
				alert("error fetching log");
			}
		});
	}
	
	function updateWidget(digest) {
		$("#widgets").empty();
		require([ "applications/log/widgets/" + Log.currentWidget + "/"
				+ capitalizeFirstLetter(Log.currentWidget) + "Widget"],
				function(widget) {
			var currentWidgetTab = "#widgetsTab a." + Log.currentWidget+"-tab";
			$(currentWidgetTab).tab("show");
			widget.render(digest, Log.timeUnit);
		});
	}
	
	function capitalizeFirstLetter(string) {
	    return string.charAt(0).toUpperCase() + string.slice(1);
	}

	function initialize() {
		_.bindAll(this);
		for (var i=0; i<widgets[Log.timeUnit].length; i++) {
			FlxState.router.route("app/log/:widget/date/:date", "", function(widget, date) {
				Log.render(widget + "/date/" + date);
			})
			FlxState.router.route("app/log/:widget/year/:year", "", function(widget, year) {
				Log.render(widget + "/year/" + year);
			})
			FlxState.router.route("app/log/:widget/month/:year/:month", "", function(widget, year, month) {
				Log.render(widget + "/month/" + year + "/" + month);
			})
			FlxState.router.route("app/log/:widget/week/:year/:week", "", function(widget, year, week) {
				Log.render(widget + "/week/" + year + "/" + week);
			})
		}
	}
	
	function incrementTimespan() {
		fetchState("/nav/incrementTimespan.json");
	}
	
	function decrementTimespan() {
		fetchState("/nav/decrementTimespan.json");
	}
	
	function gotoToday() {
		fetchState("/nav/setToToday.json");
	}
	
	function gotoDate(date) {
		console.log("gotoDate");
		fetchState("/nav/setDate.json?date=" + date);
	}
	
	function gotoWeek(year, week) {
		console.log("gotoWeek");
//		fetchState("/nav/setWeek.json?year=" + year + "&week=" + week);
	}
	
	function gotoMonth(year, month) {
		console.log("gotoMonth");
//		fetchState("/nav/setMonth.json?year=" + year + "&month=" + month);
	}
	
	function gotoYear(year) {
		console.log("gotoYear");
//		fetchState("/nav/setYear.json?year=" + date);
	}
	
	function bindNavigationEvents() {
		$(".menuNextButton").click(function(e) {
			incrementTimespan(); });
		$(".menuPrevButton").click(function(e) {
			decrementTimespan(); });
		$(".menuTodayButton").click(function(e) {
			gotoToday(); });
	}
	
	return Log;

});