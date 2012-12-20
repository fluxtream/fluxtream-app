define([], function() {

    var FlxState = {};
    FlxState.router = new Backbone.Router();
    FlxState.apps = ["calendar","bodytrack"];
    FlxState.defaultApp = "calendar";

	var storage = {};
	var tabStateStorage = {};

	FlxState.saveState = function(appName, urlState) {
		if (typeof(storage[appName])=="undefined")
			storage[appName] = {};
		storage[appName] = urlState;
	};

	FlxState.getState = function(appName) {
		if (typeof(storage[appName])=="undefined")
			return null;
		return storage[appName];
	};
	
	FlxState.saveTabState = function(appName, tabName, tabState) {
		if (_.isUndefined(tabStateStorage[appName])) {
			tabStateStorage[appName] = {};
        }
		tabStateStorage[appName][tabName] = tabState;
	};

	FlxState.getTabState = function(appName, tabName) {
        var appTabStateStorage = tabStateStorage[appName];
        if (_.isUndefined(appTabStateStorage)) {
            return null;
        }
        if (_.isUndefined(appTabStateStorage[tabName])) {
			return null;
        }
		return appTabStateStorage[tabName];
	};

    return FlxState;
});
