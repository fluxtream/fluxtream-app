define([], function() {
	
	var storage = {};
	var tabStateStorage = {};
	
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
	
	function storeTab(tabName, tabState) {
		if (typeof(tabStateStorage[tabName])=="undefined")
			tabStateStorage[tabName] = {};
		tabStateStorage[tabName].state = tabState;
	}

	function restoreTab(tabName) {
		if (typeof(tabStateStorage[tabName])=="undefined")
			return null;
		return tabStateStorage[tabName].state;
	}
	
	var state = {};
	state.saveState = store;
	state.getState = restore;
	state.saveTabState = storeTab;
	state.getTabState = restoreTab;
	state.router = new Backbone.Router();
	state.apps = ["calendar","bodytrack"];
	state.defaultApp = "calendar";
	
	return state;
});
