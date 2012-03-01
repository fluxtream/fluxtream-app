define(["core/FlxState"], function(FlxState) {

	var toLoad = 0, loaded = 0;
	
	function initialize() {
		_.bindAll(this);
		// start loading all applications
		loadApps();
	}
	
	/**
	 * Preload all applications dynamically; we do this because
	 * a) the applications list can/will be user-dependent
	 * b) we need to preload in order to setup the (global) router
	 *    object and (in the future) to let every application
	 *    know of the existence of others
	 */
	function loadApps() {
		toLoad = FlxState.apps.length;
		for (var i=0; i<FlxState.apps.length; i++) {
			require([ "applications/"+ FlxState.apps[i] + "/App"], function(app) {
				console.log("pre-loading app...");
				App.apps[app.name] = app;
				app.initialize();
				appLoaded();
			});
		}
	}
	
	/**
	 * Add the buttons to the top apps menu
	 */
	function createAppsMenu(appName, appIcon) {
		for (var i=0; i<FlxState.apps.length; i++) {
			var app = App.apps[FlxState.apps[i]];
			$("#apps-menu").append("<a class=\"btn\" "
				+ "href=\"javascript:App.renderApp('" + app.name + "')\">"
				+ "<i class=\"" + app.icon + "\"></i></a>")
		}
	}
	
	/**
	 * Application-is-loaded callback
	 */
	function appLoaded() {
		// we keep track of how many apps have been loaded
		loaded++;
		// when all apps are loaded...
		if (loaded===toLoad) {
			// we create the top apps menu
			createAppsMenu();
			console.log("Backbone.history.start");
			// we start the history
			Backbone.history.start({pushState: true});
			// finally we render the default - or url-specified - app
			renderMainApp();
		}
	}
	
	/**
	 * Render main app or the one that's specified in the location bar's contents
	 */
	function renderMainApp() {
		var parse_url = /^(?:([A-Za-z]+):)?(\/{0,3})([0-9.\-A-Za-z]+)(?::(\d+))?(?:\/([^?#]*))?(?:\?([^#]*))?(?:#(.*))?$/;
		var result = parse_url.exec(window.location.href);
		var names = ['url', 'scheme', 'slash', 'host', 'port', 'path', 'query', 'hash'];
		var blanks = ' ';
		var i;
		var parts = {};
		for (i = 0; i < names.length; i += 1)
			parts[names[i]] = result[i];
		var splits = parts.path.split("/");
		if (splits[0]==="app"&&typeof(splits[1])!="undefined") {
			console.log("loading " + splits[1]);
			var appState = parts.path.substring("app/".length+splits[1].length+1);
			var appName = splits[1];
			console.log("appState: " + appState);
			FlxState.saveState(appName, appState);
			renderApp(appName);
		} else
			renderApp(FlxState.defaultApp);
	}
	
	/**
	 * Retrieve and dom-insert the html fragment that's associated
	 * with this app (the one called 'appName'), destroy (unwire) the
	 * previous app and call render() on the new one
	 */
	function renderApp(appName) {
		console.log("showing up application " + appName);
		require([ "text!applications/"+ appName + "/template.html"], function(html) {
			$(".application").empty();
			$(".application").append(html);
			if (typeof(App.activeApp)!="undefined")
				App.activeApp.destroy();
			App.activeApp = App.apps[appName];
			App.activeApp.render();
		});
	}
	
	var App = {};
	App.initialize = initialize;
	App.renderApp = renderApp;
	App.apps = {};
	App.activeApp;
	window.App = App;
	return App;

});
