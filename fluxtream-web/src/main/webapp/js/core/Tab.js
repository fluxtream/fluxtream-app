define([ "core/FlxState" ], function(FlxState) {

	function Tab(appname, aname, anauthor, anicon, needsTemplating) {
        this.appname = appname;
		this.name = aname;
		this.author = anauthor;
		this.icon = anicon;
        this.needsTemplating = needsTemplating;
	}

	/**
	 * This method is called every time the user is selecting a different view
	 * or tab
	 */
    Tab.prototype.saveState = function() {
		FlxState.saveTabState(this.appname, this.name, this.getCurrentState());
	};

	/**
	 * This lets the tab retrieve its last saved state
	 */
    Tab.prototype.getSavedState = function() {
		return FlxState.getTabState(this.appname, this.name);
	};

    Tab.prototype.getUrl = function(url, id, domReady, forceLoad, tabData) {
		this.getTabContents(url, id, typeof(domReady)==="undefined"?null:domReady, false, forceLoad, tabData);
	};

    Tab.prototype.getTemplate = function(templatePath, id, domReady, tabData) {
		this.getTabContents(templatePath, id, typeof(domReady)==="undefined"?null:domReady, true, false, tabData);
	};

    Tab.prototype.getTabContents = function(uri, id, domReady, isResource, forceLoad, tabData) {
        var that = this;
        var onContentsRetrieved = function(html){
            var nextTabId = that.appname + "-" + id + "-tab",
                nextTabDiv = $("#"+nextTabId);
            var noTab = $(".tab").length==0;
            var tabChanged = $(".tab").length>0
                                 && $(".tab.active").length>0
                && $(".tab.active").attr("id")!=nextTabId;
            if ( noTab || tabChanged || forceLoad) {
                if (tabChanged) {
                    var currentTabDiv = $(".tab.active");
                    currentTabDiv.removeClass("active");
                    currentTabDiv.addClass("dormant");
                }
                if (nextTabDiv.length==0 || forceLoad) {
                    that.insertTabContents(html, nextTabId, domReady, forceLoad, tabData);

                } else {
                    nextTabDiv.removeClass("dormant");
                    nextTabDiv.addClass("active");
                    if (domReady!=null)
                        domReady();
                }
            } else {
                if (domReady!=null)
                    domReady();
            }

        }
        if (isResource)
            require([uri], onContentsRetrieved);
        else
            $.ajax({
                url : uri,
                success: onContentsRetrieved
            });
		
	};
	
	Tab.prototype.insertTabContents = function(template, nextTabId, domReady, forceLoad, tabData) {
        var templateJs = Hogan.compile(template);
        if (typeof(tabData)!="undefined" && tabData!=null) {
            tabData.release = window.FLX_RELEASE_NUMBER;
            template = templateJs.render(tabData);
        } else if (this.needsTemplating){
            template = templateJs.render({release: window.FLX_RELEASE_NUMBER});
        }
        template = "<div class=\"tab active\" id=\"" + nextTabId + "\">"
			 + template + "</div>";
		if (forceLoad && $("#"+nextTabId).length>0) {
			$("#"+nextTabId).replaceWith(template);
		} else {
			$("#" + this.appname + "-app #tabs").append(template);
		}
		if (domReady!=null)
			domReady(template);
	};

	/**
	 * This needs to be overridden in "subclasses"
	 */
	function getCurrentState() {
		return {};
	}

    Tab.prototype.getCurrentState = getCurrentState;

    Tab.prototype.connectorToggled = function(){};
    Tab.prototype.connectorDisplayable = function(){return true};
    Tab.prototype.connectorsAlwaysEnabled = function(){return false;};

	return Tab;

});