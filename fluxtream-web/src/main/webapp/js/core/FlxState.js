define([], function() {
		
	var storage = {};
	var router = new Backbone.Router();
	
	function store(appName, urlState) {
		if (typeof(storage[appName])=="undefined")
			storage[appName] = {};
		console.log("saving state: " + urlState)
		storage[appName].state = urlState;
	}

	function restore(appName) {
		console.log("getting state for " + appName + " -> " + storage[appName])
		if (typeof(storage[appName])=="undefined")
			return null;
		return storage[appName].state;
	}
	
	var state = {};
	state.saveState = store;
	state.getState = restore;
	state.router = router;
	state.historyStarted = false;
	state.defaultApplication = "diary";
	
	return state;
});
