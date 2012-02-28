define([], function() {

	var application;
	
	function initialize() {
		var parse_url = /^(?:([A-Za-z]+):)?(\/{0,3})([0-9.\-A-Za-z]+)(?::(\d+))?(?:\/([^?#]*))?(?:\?([^#]*))?(?:#(.*))?$/;
		var result = parse_url.exec(window.location.href);
		var names = ['url', 'scheme', 'slash', 'host', 'port', 'path', 'query', 'hash'];
		var blanks = ' ';
		var i;
		var parts = {};
		for (i = 0; i < names.length; i += 1) {
			parts[names[i]] = result[i];
		}
		console.log("path: " + parts.path)
		var splits = parts.path.split("/");
		if (splits.length>2&&typeof(splits[1])!="undefined") {
			console.log("loading " + splits[0]);
			loadApplication(splits[0]);
		} else
			loadApplication("log");
	}
	
	function loadApplication(appName) {
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
