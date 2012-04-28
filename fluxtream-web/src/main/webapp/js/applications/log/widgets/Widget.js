define([ "core/FlxState" ], function(FlxState) {

	function Widget(aname, anauthor, anicon) {
		this.name = aname;
		this.author = anauthor;
		this.icon = anicon;
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

	Widget.prototype.getUrl = function(url, id, domReady, forceLoad, widgetData) {
		this.getWidgetContents(url, id, typeof(domReady)==="undefined"?null:domReady, false, forceLoad, widgetData);
	};
	
	Widget.prototype.getTemplate = function(templatePath, id, domReady) {
		this.getWidgetContents(templatePath, id, typeof(domReady)==="undefined"?null:domReady, true, false);
	};

	Widget.prototype.getWidgetContents = function(uri, id, domReady, isResource, forceLoad, widgetData) {
		var nextWidgetId = id + "-widget";

		nextWidgetDiv = $("#"+nextWidgetId);
		var noWidget = $(".widget").length==0;
		var widgetChanged = $(".widget").length>0
			&& $(".widget.active").length>0
			&& $(".widget.active").attr("id")!=nextWidgetId;
		if ( noWidget || widgetChanged || forceLoad) {
			if (widgetChanged) {
				var currentWidgetDiv = $(".widget.active");
				currentWidgetDiv.removeClass("active");
				currentWidgetDiv.addClass("dormant");
			}
			if (nextWidgetDiv.length==0 || forceLoad) {
				if (isResource)
					require([uri], function(template) {
						insertWidgetContents(template, nextWidgetId, domReady, forceLoad, widgetData);
					});
				else
					$.ajax({
						url : uri,
						success: function(html) {
							insertWidgetContents(html, nextWidgetId, domReady, forceLoad, widgetData);
						}
					});
			} else {
				nextWidgetDiv.removeClass("dormant");
				nextWidgetDiv.addClass("active");
				if (domReady!=null)
					domReady();
			}
		} else {
			if (domReady!=null)
				domReady();
		}		
		
	};
	
	function insertWidgetContents(template, nextWidgetId, domReady, forceLoad, widgetData) {
        if (typeof(widgetData)!="undefined" && widgetData!=null)
            template = $.mustache(template, widgetData);
        else
            template = $.mustache(template, {release: window.FLX_RELEASE_NUMBER});
        template = "<div class=\"widget active\" id=\"" + nextWidgetId + "\">"
			 + template + "</div>";
		if (forceLoad && $("#"+nextWidgetId).length>0) {
			$("#"+nextWidgetId).replaceWith(template);
		} else {
			$("#widgets").append(template);
		}
		if (domReady!=null)
			domReady(template);
	}

	/**
	 * This needs to be overridden in "subclasses"
	 */
	function getCurrentState() {
		return {};
	}

	Widget.prototype.getCurrentState = getCurrentState;
	
	return Widget;

});