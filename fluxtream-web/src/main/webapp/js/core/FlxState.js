define([], function() {
	
	var storage = {};
	var tabStateStorage = {};
	
	function store(appName, urlState) {
		if (typeof(storage[appName])=="undefined")
			storage[appName] = {};
		storage[appName] = urlState;
	}

	function restore(appName) {
		if (typeof(storage[appName])=="undefined")
			return null;
		return storage[appName];
	}
	
	function storeTab(appName, tabName, tabState) {
		if (_.isUndefined(tabStateStorage[appName])) {
			tabStateStorage[appName] = {};
        }
		tabStateStorage[appName][tabName] = tabState;
	}

	function restoreTab(appName, tabName) {
        var appTabStateStorage = tabStateStorage[appName];
        if (_.isUndefined(appTabStateStorage)) {
            return null;
        }
        if (_.isUndefined(appTabStateStorage[tabName])) {
			return null;
        }
		return appTabStateStorage[tabName];
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
