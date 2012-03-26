define([ "core/FlxState" ], function(FlxState) {

	function Widget(name, author, icon) {
		this.name = name;
		this.author = author;
		this.icon = icon;
	}

	/**
	 * This method is called every time the user is selecting a different view
	 * or widget
	 */
	Widget.prototype.saveState = function() {
		FlxState.saveWidgetState(this.getCurrentState());
	}

	/**
	 * This lets the widget retrieve its last saved state
	 */
	Widget.prototype.getSavedState = function() {
		return FlxState.getWidgetState(name);
	}

	/**
	 * This needs to be overridden in "subclasses"
	 */
	function getCurrentState() {
		console.log("getCurrentState: not yet implemented");
		return {};
	}

	Widget.prototype.getCurrentState = getCurrentState;

	return Widget;

});