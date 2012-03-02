define(["core/FlxState"], function(FlxState) {

	var App = {};
	var toLoad = 0, loaded = 0;
	var apps = {};
	
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
		console.log("loading apps " + apps);
		for (var i=0; i<FlxState.apps.length; i++) {
			require([ "applications/"+ FlxState.apps[i] + "/App"], function(app) {
				apps[app.name] = app;
				app.initialize();
				appLoaded(app.name);
			});
		}
	}
	
	/**
	 * Add the buttons to the top apps menu
	 */
	function createAppsMenu(appName, appIcon) {
		for (var i=0; i<FlxState.apps.length; i++) {
			var app = apps[FlxState.apps[i]];
			$("#apps-menu").append("<button id=\"" + app.name + "MenuButton\" class=\"btn\" "
				+ "onclick=\"javascript:App.renderApp('" + app.name + "')\">"
				+ "<i class=\"" + app.icon + "\"></i></button>")
		}
	}
	
	/**
	 * Application-is-loaded callback
	 */
	function appLoaded(appName) {
		console.log("app loaded: " + appName + "/" + apps);
		// we keep track of how many apps have been loaded
		loaded++;
		// when all apps are loaded...
		if (loaded===toLoad) {
			App.apps = apps;
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
		console.log("renderMainApp " + apps);
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
			App.activeApp = apps[appName];
			App.activeApp.render(appState);
		} else {
			App.activeApp = apps[FlxState.defaultApp];
			apps[FlxState.defaultApp].render("");
		}
	}
	
	function renderApp(appName) {
		App.apps[appName].render("last");
	}
	
	App.settings = function() {
		$.ajax({
			url:"/settings/main",
			success: function(html) {
				$("#modal").empty();
				$("#modal").append(html);
				$("#modal").css("display", "block");
				var dialog = $(".modal").modal({show: false});
				dialog.modal("show");
			}
		});
	}
	
	App.eraseEverything = function() {
		var confirmed = confirm("Are you sure?");
	}
	
	App.connectors = function() {
		$.ajax({
			url:"/connectors/main",
			success: function(html) {
				$("#modal").empty();
				$("#modal").append(html);
				$("#modal").css("display", "block");
				var dialog = $(".modal").modal({show: false});
				dialog.modal("show");
			}
		});
	}
		
	App.initialize = initialize;
	App.renderApp = renderApp;
	window.App = App;
	console.log("RETURNING GLOBAL APP OBJECT")
	return App;

});
