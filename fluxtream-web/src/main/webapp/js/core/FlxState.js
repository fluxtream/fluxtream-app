define([], function() {
	
	var storage = {};
	var widgetStateStorage = {};
	
	function store(appName, urlState) {
		if (typeof(storage[appName])=="undefined")
			storage[appName] = {};
		storage[appName].state = urlState;
	}

	function restore(appName) {
		if (typeof(storage[appName])=="undefined")
			return null;
		return storage[appName].state;
	}
	
	function storeWidget(widgetName, widgetState) {
		if (typeof(widgetStateStorage[widgetName])=="undefined")
			widgetStateStorage[widgetName] = {};
		widgetStateStorage[widgetName].state = widgetState;
	}

	function restoreWidget(widgetName) {
		if (typeof(widgetStateStorage[widgetName])=="undefined")
			return null;
		return widgetStateStorage[widgetName].state;
	}
	
	var state = {};
	state.saveState = store;
	state.getState = restore;
	state.saveWidgetState = storeWidget;
	state.getWidgetState = restoreWidget;
	state.router = new Backbone.Router();
	state.apps = ["timeline", "log"];
	state.defaultApp = "log";
	
	return state;
});
