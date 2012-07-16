define([], function() {
	
	var Builder = {};
	var tabs = {};
	
	var tabs = {
        "DAY":["clock", "dashboards", "map", "photos", "list", "timeline"],
        "WEEK":["dashboards", "map", "photos", "list", "timeline"],
        "MONTH":["dashboards", "map", "photos", "list", "timeline"],
        "YEAR":["dashboards", "photos", "list", "timeline"]
	};
	
	var tab_icons = {
		clock: "icon-time",
        dashboards: "icon-dashboard",
		map: "icon-map-marker",
		diary: "icon-pencil",
		photos: "icon-camera",
		views: "icon-eye-open",
		list: "icon-list",
        timeline: "icon-film"
    };
	
	function capitalizeFirstLetter(string) {
	    return string.charAt(0).toUpperCase() + string.slice(1);
	}
	
	function createTabs(Calendar) {
		$("#calendarTabs").empty();
		for (var i=0; i<tabs[Calendar.timeUnit].length; i++) {
			var tab = "<li style=\"cursor:pointer\">";
			tab += "<a class=\"" + tabs[Calendar.timeUnit][i] + "-tab\" tabname=\"" + tabs[Calendar.timeUnit][i] + "\" data-toggle=\"tab\">"
				+ "<i class=\"" + tab_icons[tabs[Calendar.timeUnit][i]] + "\"></i> " + capitalizeFirstLetter(tabs[Calendar.timeUnit][i]) + "</a></li>";
            tab = $(tab);
			$("#calendarTabs").append(tab);
			$(tab.children()[0]).click(function(event) {
                event.preventDefault();
				var tab = $(event.delegateTarget).attr("tabname");
                if (typeof(tab)==="undefined")
                    return;
				var state = App.state.getState("calendar");
				state = state.substring(state.indexOf("/"));
				Calendar.renderState(tab+state,Calendar.digestTabState != state.substring(1));
			});
		}
		var t = tabExistsForTimeUnit(Calendar.currentTabName, Calendar.timeUnit)?Calendar.currentTabName:tabs[Calendar.timeUnit][0];
		var currentTab = "#calendarTabs a." + t + "-tab";
		$(currentTab).tab("show");
	}
	
	function bindTimeUnitsMenu(Calendar) {
		var timeUnitIds = {"#dayViewBtn":1, "#weekViewBtn":2, "#monthViewBtn":3, "#yearViewBtn":4};
        for (var timeUnitId in timeUnitIds){
            var btn = $(timeUnitId);
            if (btn.attr("unit") == Calendar.timeUnit)
                btn.addClass("active");
            else
                btn.removeClass("active");
            btn.unbind("click");
            btn.click(function(event){
                var timeUnit = $(event.target).attr("unit"),
                    url = "/nav/set" + capitalizeFirstLetter(timeUnit.toLowerCase()) + "TimeUnit.json";
                $.ajax({ url:url,
                   success : function(response) {
                       var t = tabExistsForTimeUnit(Calendar.currentTabName, timeUnit)?Calendar.currentTabName:tabs[timeUnit][0];
                       Calendar.currentTabName = t;
                       Calendar.updateButtonStates();
                       Calendar.renderState(Calendar.currentTabName + "/" + response.state);
                   },
                   error : function() {
                       alert("error");
                   }
               });
            })
        }
		bindTimeNavButtons(Calendar);
	}
	
	function bindTimeNavButtons(Calendar) {
		switch(Calendar.timeUnit) {
		case "DAY":
			nextPrevEnable();
			break;
		case "WEEK":
			nextPrevEnable();
			break;
//		case "MONTH":
//			nextPrevEnable();
//			break;
		case "YEAR":
			nextPrevEnable();
			break;
		}
	};
	
	function nextPrevEnable() {
        $(".menuNextButton").removeClass("disabled");
        $(".menuPrevButton").removeClass("disabled");
    };
	
	function timeNavBtn(Calendar, downOrUp, enabled, targetTimeUnit) {
		var button = $(".menu"+capitalizeFirstLetter(downOrUp)+"Button");
		button.unbind();
		if (!enabled)
			button.addClass("disabled");
		else {
			button.removeClass("disabled");
			button.click(function(event) {
				var timeUnit = $(event.target).attr("class"),
				url = "/nav/set" + capitalizeFirstLetter(targetTimeUnit.toLowerCase()) + "TimeUnit.json";
				$.ajax({ url:url,
					success : function(response) {
						Calendar.renderState(Calendar.currentTabName + "/" + response.state);
					},
					error : function() {
						alert("error");
					}
				});
			});
		}
	}

	function handleNotifications(digestInfo) {
		$(".notification").remove();
		$("#notificationIds").empty();
		if (typeof(digestInfo.notifications)!="undefined") {
			$(".alert-message").addClass("success");
			for (n=0; n<digestInfo.notifications.length; n++) {
				var notification = digestInfo.notifications[n];
				$("#notifications").append("<p class=\"notification\">" + notification.message + "</p>");
				if (n>0)
					$("#notificationIds").append(",");
				$("#notificationIds").append(notification.id);
			}
			$("#notifications").show();
		}
	}
	
	function updateTab(digest, Calendar) {
		handleNotifications(digest);
		if (tabs[Calendar.currentTabName]==null) {
			require([ "applications/calendar/tabs/" + Calendar.currentTabName + "/"
					+ capitalizeFirstLetter(Calendar.currentTabName) + "Tab"],
				function(tab) {
					tabs[Calendar.currentTavName] = tab;
					renderTab(tab, digest, Calendar);
				}
			);
		} else {
			renderTab(tabs[Calendar.currentTabName], digest, Calendar);
		}
	}
	
	function renderTab(tab, digest, Calendar) {
		var currentTab = "#calendarTabs a." + Calendar.currentTabName+"-tab";
		$(currentTab).tab("show");
		tab.render(digest, Calendar.timeUnit, Calendar.tabState, Calendar.connectorEnabled[Calendar.currentTabName]);
        Calendar.currentTab = tab;
        for (var i = 0; i < digest.selectedConnectors.length; i++){
            var button = $("#flx-connector-btn-" + digest.selectedConnectors[i].connectorName);
            if (tab.connectorDisplayable(digest.selectedConnectors[i])){
                button.show();
                if (tab.connectorsAlwaysEnabled()){
                    button.removeClass("flx-disconnected");
                    button.css("border-bottom-color",App.getConnectorConfig(digest.selectedConnectors[i].connectorName).color);
                }
            }
            else
                button.hide();

        }
	}
	
	function tabExistsForTimeUnit(tab, unit) {
		var tabExistsForTimeUnit = false;
		for (var i=0; i<tabs[unit].length; i++) {
			if (tabs[unit][i]===tab)
				tabExistsForTimeUnit = true;
		}
		return tabExistsForTimeUnit;
	}

    function isValidTabName(tabName) {
        for (var name in tab_icons) {
            if (name===tabName)
                return true;
        }
        return false;
    }

    function isValidTimeUnit(timeUnit) {
        return timeUnit==="date"||timeUnit==="week"||
               timeUnit==="month"||timeUnit==="year";
    }
	
	Builder.tabExistsForTimeUnit = tabExistsForTimeUnit;
	Builder.tabs = tabs;
	Builder.bindTimeUnitsMenu = bindTimeUnitsMenu;
	Builder.createTabs = createTabs;
	Builder.updateTab = updateTab;
    Builder.isValidTabName = isValidTabName;
    Builder.isValidTimeUnit = isValidTimeUnit;

    return Builder;
	
});
