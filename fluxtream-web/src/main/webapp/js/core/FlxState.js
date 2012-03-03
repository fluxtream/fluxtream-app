define([], function() {
	
	var storage = {};
	
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
	
	var state = {};
	state.saveState = store;
	state.getState = restore;
	state.router = new Backbone.Router();
	state.apps = ["bodytrack", "log", "diary"];
	state.defaultApp = "bodytrack";
	
	return state;
});
