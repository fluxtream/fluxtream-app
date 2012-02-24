define(["applications/calendar/Router"], function(Router) {

	var application;
	
	function initialize() {
		Router.initialize();
		loadApplication("calendar");
	}
	
	function loadApplication(appName) {
		console.log("loading application: " + appName);
		require([ "applications/"+ appName + "/App",
				"text!applications/" + appName + "/template.html" ], function(
				app, html) {
			$(".application").empty();
			$(".application").append(html);
//			application.destroy();
			application = app;
			app.initialize();
		});
	}

	var App = {};
	App.initialize = initialize;
	App.loadApplication = loadApplication;
	App.gotoDate = function(date) {
		console.log("application.name: " + application.name);
		if (application.name=="calendar")
			application.gotoDate(date);
	}
	window.App = App;
	return App;

});
