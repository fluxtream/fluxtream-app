define([], function() {

	var application;
	
	function initialize() {
		require([ "applications/calendar/Calendar",
				"text!applications/calendar/template.html" ], function(
				Calendar, html) {
			$(".application").empty();
			$(".application").append(html);
			application = Calendar;
			Calendar.initialize();
		});
	}

	var App = {};
	App.initialize = initialize;
	App.gotoDate = function(date) {
		console.log("application.name: " + application.name);
		if (application.name=="calendar")
			application.gotoDate(date);
	}
	window.App = App;
	return App;

});
