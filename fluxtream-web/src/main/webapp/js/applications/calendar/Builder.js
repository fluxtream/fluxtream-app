define(["core/TabInterface"], function(TabInterface) {
	
	var Builder = {};

    var tabsPath = "applications/calendar/tabs/";
    var tabPaths = [tabsPath + "clock/ClockTab", tabsPath + "dashboards/DashboardsTab",
                    tabsPath + "diary/DiaryTab", tabsPath + "list/ListTab",
                    tabsPath + "map/MapTab", tabsPath + "photos/PhotosTab",
                    tabsPath + "timeline/TimelineTab"];

    var tabInterface = new TabInterface(tabPaths);
	
	var tabs = {
        "fullList":["clock","dashboards","map","photos","list","timeline"],
        "date":["clock", "dashboards", "map", "photos", "list", "timeline"],
        "week":["dashboards", "map", "photos", "list", "timeline"],
        "month":["dashboards", "map", "photos", "list", "timeline"],
        "year":["dashboards", "photos", "list", "timeline"]
	};
    tabInterface.setTabVisibility(tabs.fullList,true);

    var timeUnits = ['date', 'week', 'month', 'year'];

    Builder.init = function(Calendar){
        $("#calendarTabs").replaceWith(tabInterface.getNav());
        tabInterface.getNav().addClickListener(function(tabName){
            var state = App.state.getState("calendar");
            state = state.substring(state.indexOf("/"));
            if (Calendar.tabParam != null){
                Calendar.tabParam = null;
                state = state.substring(0,state.lastIndexOf("/"));
            }
            Calendar.render(tabName+state);
        });
        bindTimeUnitsMenu(Calendar);
        bindTimeNavButtons(Calendar);
    }
	
	function timeUnitToURL(timeUnit) {
        if (timeUnit.toLowerCase() === 'date') {
            timeUnit = 'day';
        }
        return "/api/calendar/nav/set" + timeUnit.upperCaseFirst() + "TimeUnit";
	}
	
	function createTabs(Calendar) {
		tabInterface.setTabVisibility(tabs.fullList,false);
        tabInterface.setTabVisibility(tabs[Calendar.timeUnit],true);
	}
	
	function bindTimeUnitsMenu(Calendar) {
		var timeUnitIDs = ["#dayViewBtn", "#weekViewBtn", "#monthViewBtn", "#yearViewBtn"];
        $.each(timeUnitIDs, function(i, timeUnitID) {
            var btn = $(timeUnitID);
            btn.toggleClass("active", btn.attr("unit") == Calendar.timeUnit)
                .unbind("click")
                .click(function(event){
                    var timeUnit = $(event.target).attr("unit");
                    // TODO: do something reasonable with this
                    if (Calendar.currentTab.timeNavigation(timeUnit)) {
                        return;
                    }
                    var url = timeUnitToURL(timeUnit),
                        params = {state: Calendar.tabState};
                    Calendar.fetchState(url, params, function(response) {
                        console.log("fetch from bindTimeUnitsMenu callback!")
                        Calendar.timeUnit = timeUnit;
                        Calendar.tabState = response.state;
                        if (!Builder.tabExistsForTimeUnit(Calendar.currentTabName, Calendar.timeUnit)) {
                            Calendar.currentTabName = Builder.tabs[Calendar.timeUnit][0];
                        }
                        Calendar.updateButtonStates();
                        Calendar.render(Calendar.getState());
                    });
                });
        });
	}
	
	function bindTimeNavButtons(Calendar) {
		switch(Calendar.timeUnit) {
		case "date":
			nextPrevEnable();
			break;
		case "week":
			nextPrevEnable();
			break;
        // TODO: why is this disabled?
//		case "month":
//			nextPrevEnable();
//			break;
		case "year":
			nextPrevEnable();
			break;
		}
	};
	
	function nextPrevEnable() {
        $(".menuNextButton").removeClass("disabled");
        $(".menuPrevButton").removeClass("disabled");
    };

	function handleNotifications(digestInfo) {
		$(".alert").remove();
        $("#notifications").empty();
		if (typeof(digestInfo.notifications)!="undefined") {
			for (var n=0; n<digestInfo.notifications.length; n++) {
                console.log("showing a notification " + n)
                showNotification(digestInfo.notifications[n]);
			}
            $("#notifications").show();
		}
	}

    function showNotification(notification) {
        App.loadMustacheTemplate("notificationTemplates.html",
            notification.type+"Notification",
            function(template) {
                if ($("#notification-" + notification.id).length==0) {
                    var html = template.render(notification), message = notification.message;
                    $("#notifications").append(html);
                    if (notification.repeated>1) message += " (" + notification.repeated + "x)";
                    $("#notification-" + notification.id).append(message);
                }
            });
    }
	
	function updateTab(digest, Calendar) {
        tabInterface.setRenderParamsFunction(function(){
            return {digest:digest,timeUnit:Calendar.timeUnit,calendarState:Calendar.tabState,connectorEnabled:Calendar.connectorEnabled[Calendar.currentTabName],tabParam:Calendar.tabParam,setTabParam:Calendar.setTabParam};
        });
        tabInterface.setActiveTab(Calendar.currentTabName);
        updateCurrentTab(digest, Calendar);
	}

    function updateCurrentTab(digest, Calendar){
        Calendar.currentTab = tabInterface.getActiveTab();
        if (Calendar.currentTab == null){
            $.doTimeout(50,function(){updateCurrentTab(digest, Calendar)});
            return;
        }
        for (var i = 0; i < digest.selectedConnectors.length; i++){
            var button = $("#flx-connector-btn-" + digest.selectedConnectors[i].connectorName);
            if (Calendar.currentTab.connectorDisplayable(digest.selectedConnectors[i])){
                button.show();
                if (Calendar.currentTab.connectorsAlwaysEnabled()){
                    button.removeClass("flx-disconnected");
                    button.css("border-bottom-color",App.getConnectorConfig(digest.selectedConnectors[i].connectorName).color);
                }
            }
            else
                button.hide();
        }

    }
	
	function tabExistsForTimeUnit(tabName, timeUnit) {
        return _.include(tabs[timeUnit], tabName);
	}

    function isValidTabName(tabName) {
        return _.include(tabs.fullList, tabName);
    }

    function isValidTimeUnit(timeUnit) {
        return _.include(timeUnits, timeUnit);
    }
	
	Builder.tabExistsForTimeUnit = tabExistsForTimeUnit;
	Builder.tabs = tabs;
	Builder.createTabs = createTabs;
	Builder.updateTab = updateTab;
    Builder.isValidTabName = isValidTabName;
    Builder.isValidTimeUnit = isValidTimeUnit;
    Builder.handleNotifications = handleNotifications;

    return Builder;
	
});
