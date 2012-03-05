define(["core/Application",
        "core/FlxState",
        "libs/jquery.qtip"],
        function(Application, FlxState)
{
	
	Date.prototype.format=function(format){var returnStr='';var replace=Date.replaceChars;for(var i=0;i<format.length;i++){var curChar=format.charAt(i);if(i-1>=0&&format.charAt(i-1)=="\\"){returnStr+=curChar}else if(replace[curChar]){returnStr+=replace[curChar].call(this)}else if(curChar!="\\"){returnStr+=curChar}}return returnStr};Date.replaceChars={shortMonths:['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'],longMonths:['January','February','March','April','May','June','July','August','September','October','November','December'],shortDays:['Sun','Mon','Tue','Wed','Thu','Fri','Sat'],longDays:['Sunday','Monday','Tuesday','Wednesday','Thursday','Friday','Saturday'],d:function(){return(this.getDate()<10?'0':'')+this.getDate()},D:function(){return Date.replaceChars.shortDays[this.getDay()]},j:function(){return this.getDate()},l:function(){return Date.replaceChars.longDays[this.getDay()]},N:function(){return this.getDay()+1},S:function(){return(this.getDate()%10==1&&this.getDate()!=11?'st':(this.getDate()%10==2&&this.getDate()!=12?'nd':(this.getDate()%10==3&&this.getDate()!=13?'rd':'th')))},w:function(){return this.getDay()},z:function(){var d=new Date(this.getFullYear(),0,1);return Math.ceil((this-d)/86400000)}, W:function(){var d=new Date(this.getFullYear(),0,1);return Math.ceil((((this-d)/86400000)+d.getDay()+1)/7)},F:function(){return Date.replaceChars.longMonths[this.getMonth()]},m:function(){return(this.getMonth()<9?'0':'')+(this.getMonth()+1)},M:function(){return Date.replaceChars.shortMonths[this.getMonth()]},n:function(){return this.getMonth()+1},t:function(){var d=new Date();return new Date(d.getFullYear(),d.getMonth(),0).getDate()},L:function(){var year=this.getFullYear();return(year%400==0||(year%100!=0&&year%4==0))},o:function(){var d=new Date(this.valueOf());d.setDate(d.getDate()-((this.getDay()+6)%7)+3);return d.getFullYear()},Y:function(){return this.getFullYear()},y:function(){return(''+this.getFullYear()).substr(2)},a:function(){return this.getHours()<12?'am':'pm'},A:function(){return this.getHours()<12?'AM':'PM'},B:function(){return Math.floor((((this.getUTCHours()+1)%24)+this.getUTCMinutes()/60+this.getUTCSeconds()/ 3600) * 1000/24)}, g:function(){return this.getHours()%12||12},G:function(){return this.getHours()},h:function(){return((this.getHours()%12||12)<10?'0':'')+(this.getHours()%12||12)},H:function(){return(this.getHours()<10?'0':'')+this.getHours()},i:function(){return(this.getMinutes()<10?'0':'')+this.getMinutes()},s:function(){return(this.getSeconds()<10?'0':'')+this.getSeconds()},u:function(){var m=this.getMilliseconds();return(m<10?'00':(m<100?'0':''))+m},e:function(){return"Not Yet Supported"},I:function(){return"Not Yet Supported"},O:function(){return(-this.getTimezoneOffset()<0?'-':'+')+(Math.abs(this.getTimezoneOffset()/60)<10?'0':'')+(Math.abs(this.getTimezoneOffset()/60))+'00'},P:function(){return(-this.getTimezoneOffset()<0?'-':'+')+(Math.abs(this.getTimezoneOffset()/60)<10?'0':'')+(Math.abs(this.getTimezoneOffset()/60))+':00'},T:function(){var m=this.getMonth();this.setMonth(0);var result=this.toTimeString().replace(/^.+ \(?([^\)]+)\)?$/,'$1');this.setMonth(m);return result},Z:function(){return-this.getTimezoneOffset()*60},c:function(){return this.format("Y-m-d\\TH:i:sP")},r:function(){return this.toString()},U:function(){return this.getTime()/1000}};
	
	var widgets = ["clock", "summary", "map", "diary"];
	
	var Log = new Application("log", "Candide Kemmler", "icon-calendar");
	
	Log.currentWidget = widgets[0];
	Log.initialize = initialize;
	
	Log.renderState = function(state) {
		console.log("log app: renderState");
		if (state==null||state==="") {
			console.log("log app: no state -> gotoToday()");
			gotoToday();
		}
		else {
			console.log("log app: we have state [" + state + "] thus going to specified date");
			var splits = state.split("/");
			console.log("log app: " + splits[0]);
			if (splits[0]!=this.currentWidget)
				this.currentWidget = splits[0];
			if ("date"===splits[1]) {
				gotoDate(splits[2]);
			}
		}
	}
	
	Log.setup = function() {
		nav = new NavModel();
		var now = new Date(),
			format = 'Y-m-d',
			today = now.format(format);
		var options = {
				format: format,
				date:today,
				current:today,
				position:'r',
				onChange:function(){
					$('#currentTimespanLabel').DatePickerHide();
					gotoDate($('#currentTimespanLabel').DatePickerGetDate(true));
				}
		};
		bindNavigationEvents();
		for (var i=0; i<widgets.length; i++) {
			var tab = "<li>";
			tab += "<a class=\"" + widgets[i] + "-tab\" widget=" + widgets[i] + " data-toggle=\"tab\">"
				+ capitalizeFirstLetter(widgets[i])
				+ "</a></li>";
			$("#widgetsTab").append(tab);
			var currentWidgetTab = "#widgetsTab a." + widgets[i] +"-tab";
			$(currentWidgetTab).css("cursor", "pointer");
			$(currentWidgetTab).click(function(event) {
				var widget = $(event.target).attr("widget");
				var state = App.state.getState("log");
				state = state.substring(state.indexOf("/"));
				Log.renderState(widget+state);
			});
		}
		console.log("showing tab");
	}
		
	var nav, NavModel = Backbone.Model.extend({
		url : "/nav/model.json",
		lastUrl: null,
		fetchState: function() {
			$(".calendar-navigation-button").toggleClass("disabled");
			$(".loading").show();
			$("#widgets").css("opacity", ".3");
			nav.fetch({
				success : function(model, response) {
					FlxState.router.navigate("app/log/" + Log.currentWidget + "/" + response.state);
					FlxState.saveState("log", Log.currentWidget + "/" + response.state);
					$("#currentTimespanLabel").html(response.currentTimespanLabel);
					log.url = "/api/log/all/" + response.state;
					log.fetchLog();
				},
				error : function() {
					alert("error");
				}
			});
		}
	});
	
	var log, LogModel = Backbone.Model.extend({
		url : "",
		fetchLog: function() {
			log.fetch({
				success : function(model, response) {
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
	});
	
	function updateWidget(digest) {
		$("#widgets").empty();
		require([ "applications/log/widgets/" + Log.currentWidget + "/"
				+ capitalizeFirstLetter(Log.currentWidget) + "Widget",
				"text!applications/log/widgets/" + Log.currentWidget + "/"
				+ Log.currentWidget + ".html"], function(widget, template) {
			$("#widgets").append(template);
			var currentWidgetTab = "#widgetsTab a." + Log.currentWidget+"-tab";
			$(currentWidgetTab).tab("show");
			widget.render(digest);
		});
	}
	
	function capitalizeFirstLetter(string) {
	    return string.charAt(0).toUpperCase() + string.slice(1);
	}

	function initialize() {
		_.bindAll(this);
		nav = new NavModel();
		log = new LogModel();
		for (var i=0; i<widgets.length; i++) {
			var route = "app/log/:widget/date/:date";
			FlxState.router.route(route, "", function(widget, date) {
				Log.render(widget + "/" + "date/" + date);
			})
		}
	}
	
	function incrementTimespan() {
		nav.url = "/nav/incrementTimespan.json";
		nav.fetchState(true);
	}
	
	function decrementTimespan() {
		nav.url = "/nav/decrementTimespan.json";
		nav.fetchState(true);
	}
	
	function gotoToday() {
		nav.url = "/nav/setToToday.json";
		nav.fetchState(true);
	}
	
	function gotoDate(date) {
		nav.url = "/nav/setDate.json?date=" + date;
		nav.fetchState();
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