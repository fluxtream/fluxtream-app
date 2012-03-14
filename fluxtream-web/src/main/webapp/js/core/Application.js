define(["core/FlxState"], function(FlxState) {
	
	function Application(name, author, icon) {
		this.name = name;
		this.author = author;
		this.icon = icon;
	}
	
	Application.prototype.destroy = function() {
		console.log("WARNING: DESTROY IS NOT IMPLEMENTED!!!")
	}
	
	/**
	 * Retrieve and dom-insert the html fragment that's associated
	 * with this app, destroy (unwire) the
	 * previous app and call renderState() on the new one
	 */
	Application.prototype.render = function(state) {
		console.log("Application.render: showing up application " + this.name);
		$("#"+this.name+"MenuButton").button('toggle');
		if (state==="last")
			state = FlxState.getState(this.name);
		that = this;
		console.log("Application.render: state=" + state);
		if ($(".application").attr("id")!=this.name) {
			console.log("Application.Render: loading app " + this.name);
			require([ "text!applications/"+ this.name + "/template.html"], function(html) {
				console.log("loaded application template");
				$(".application").attr("id", that.name);
				$(".application").empty();
				$(".application").append(html);
				if (typeof(App.activeApp)!="undefined")
					App.activeApp.destroy();
				App.activeApp = App.apps[that.name];
				App.activeApp.setup();
				App.activeApp.renderState(state, true);
			});
		} else {
			console.log("Application.Render: app is already loaded");
			this.renderState(state, true);
		}
	}

	/**
	 * Bind controls for intra-app interaction. This is called by the
	 * render function, when the app's template is loaded
	 * and dom-inserted
	 */
	Application.prototype.setup = function() {
		console.log("WARNING: SETUP IS NOT IMPLEMENTED")
	}
	
	Application.prototype.renderState = function(state) {
		console.log("WARNING: RENDERSTATE IS NOT IMPLEMENTED!!!")
	}

	return Application;
	
});