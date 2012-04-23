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
	};

	/**
	 * This lets the widget retrieve its last saved state
	 */
	Widget.prototype.getSavedState = function() {
		return FlxState.getWidgetState(this.name);
	};
	
	
	Widget.prototype.getTemplate = function(templatePath, domReady) {
		console.log("getting template: " + templatePath);
		
		var nextWidgetId = this.name + "-widget",
		nextWidgetDiv = $("#"+nextWidgetId);
		var noWidget = $(".widget").length==0;
		var widgetChanged = $(".widget").length>0
			&& $(".widget.active").length>0
			&& $(".widget.active").attr("id")!=nextWidgetId;
		if ( noWidget || widgetChanged) {
			if (widgetChanged) {
				var currentWidgetDiv = $(".widget.active");
				currentWidgetDiv.removeClass("active");
				currentWidgetDiv.addClass("dormant");
			}
			if (nextWidgetDiv.length==0) {
				console.log(nextWidgetId + " is not yet loaded...");
				console.log("-> loading widget " + this.name);
				require([templatePath], function(template) {
					console.log("loaded widget template");
					template = $.mustache(template, {release: window.FLX_RELEASE_NUMBER});
					template = "<div class=\"widget active\" id=\"" + nextWidgetId + "\">"
						 + template + "</div>";
					$("#widgets").append(template);
					domReady();
				});
			} else {
				console.log("using cached widget dom");
				nextWidgetDiv.removeClass("dormant");
				nextWidgetDiv.addClass("active");
				domReady();
			}
		} else {
			console.log("Widget.getTemplate: widget template is already loaded");
			this.renderState(state, true);
			domReady();
		}		
		
	};

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