define(["core/FlxState"], function(FlxState) {
	
	function Application(name, author, icon, prettyName) {
		this.name = name;
		this.author = author;
		this.icon = icon;
        this.prettyName = prettyName;
	}
	
	Application.prototype.destroy = function() {
	};
	
	/**
	 * Retrieve and dom-insert the html fragment that's associated
	 * with this app, destroy (unwire) the
	 * previous app and call renderState() on the new one
	 */
	Application.prototype.render = function(state,params) {
        this.renderState(state, true, params);
	};
	
	Application.prototype.saveState = function() {
	};

	/**
	 * Bind controls for intra-app interaction. This is called by the
	 * render function, when the app's template is loaded
	 * and dom-inserted
	 */
	Application.prototype.setup = function() {
	};

    /**
     * Parses the URL state fragment and returns a "state object" that
     * packages all state represented in that fragment. This object is passed to
     * renderState(). If parsing fails, returns null.
     *
     * @param state  URL fragment to be parsed into a state object
     */
    Application.prototype.parseState = function(state) {
        return state;
    };

    Application.prototype.renderDefaultState = function() {
        return this.renderState();
    };

	Application.prototype.renderState = function(state) {
	};

    Application.prototype.getParams = function(){
        return this.params;
    }

    Application.prototype.navigateState = function(state,params) {
        this.params = params;
        if (this.params == null)
            this.params = {};
        var url = "app/" + this.name;
        if (state) {
            url += "/" + state;
        }

        FlxState.router.navigate(url, {trigger: true});
        if (typeof(ga)!="undefined") {
            ga("send", "pageview", url);
        }
        FlxState.saveState(this.name, state);
    }

	return Application;
	
});