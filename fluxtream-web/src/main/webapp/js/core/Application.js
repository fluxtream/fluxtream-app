define(["core/FlxState"], function(FlxState) {
	
	function Application(name, author, icon) {
		this.name = name;
		this.author = author;
		this.icon = icon;
	}
	
	Application.prototype.destroy = function() {
	};
	
	/**
	 * Retrieve and dom-insert the html fragment that's associated
	 * with this app, destroy (unwire) the
	 * previous app and call renderState() on the new one
	 */
	Application.prototype.render = function(state) {
        $(".appMenuBtn.active").removeClass("active");
		$("#"+this.name+"MenuButton").addClass('active');
		if (state==="last")
			state = FlxState.getState(this.name);
		that = this;
		var nextAppId = this.name + "-app",
			nextAppDiv = $("#"+nextAppId);
		var noApp = $(".application").length==0;
		var appChanged = $(".application").length>0
			&& $(".application.active").length>0
			&& $(".application.active").attr("id")!=nextAppId;
		if ( noApp || appChanged) {
			if (typeof(App.activeApp)!="undefined")
				App.activeApp.destroy();
			if (appChanged) {
				var currentAppDiv = $(".application.active");
				currentAppDiv.removeClass("active");
				currentAppDiv.addClass("dormant");
			}
			if (nextAppDiv.length==0) {
				require([ "text!applications/"+ this.name + "/template.html"], function(html) {
					html = "<div class=\"application active\" id=\"" + nextAppId + "\">"
						 + html + "</div>";
					
					$("#applications").append(html);
					
					App.activeApp = App.apps[that.name];
					App.activeApp.renderState(state, true);
					App.activeApp.setup();
				});
			} else {
				nextAppDiv.removeClass("dormant");
				nextAppDiv.addClass("active");
                this.renderState(state, true);
			}
		} else {
			this.renderState(state, true);
		}
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
	
	Application.prototype.renderState = function(state) {
	};

	return Application;
	
});