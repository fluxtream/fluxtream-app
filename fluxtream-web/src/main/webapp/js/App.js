define([], function() {

	var application;
	
	function initialize() {
		loadApplication("log");
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
	window.App = App;
	return App;

});
