define(["core/TabInterface"], function(TabInterface) {
	
	var Builder = {};

    var tabsPath = "applications/calendar/tabs/";
    var tabPaths = [tabsPath + "clock/ClockTab", tabsPath + "dashboards/DashboardsTab", tabsPath + "diary/DiaryTab",
                    tabsPath + "list/ListTab", tabsPath + "map/MapTab", tabsPath + "photos/PhotosTab", tabsPath + "timeline/TimelineTab"];

    var tabInterface = new TabInterface(tabPaths);

	
	var tabs = {
        "fullList":["clock","dashboards","diary","map","photos","list","timeline"],
        "DAY":["clock", "dashboards", "map", "photos", "list", "timeline"],
        "WEEK":["dashboards", "map", "photos", "list", "timeline"],
        "MONTH":["dashboards", "map", "photos", "list", "timeline"],
        "YEAR":["dashboards", "photos", "list", "timeline"]
	};
    tabInterface.setTabVisibility(tabs.fullList,true);

    Builder.init = function(Calendar){
        $("#calendarTabs").replaceWith(tabInterface.getNav());
        tabInterface.getNav().addClickListener(function(tabName){
            var state = App.state.getState("calendar");
            state = state.substring(state.indexOf("/"));
            Calendar.renderState(tabName+state,Calendar.digestTabState != state.substring(1));
        });
    }
	
	function capitalizeFirstLetter(string) {
	    return string.charAt(0).toUpperCase() + string.slice(1);
	}
	
	function createTabs(Calendar) {
		tabInterface.setTabVisibility(tabs.fullList,false);
        tabInterface.setTabVisibility(tabs[Calendar.timeUnit],true);
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
                if (Calendar.currentTab.timeNavigation(timeUnit))
                    return;
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
        tabInterface.setRenderParamsFunction(function(){
            return {digest:digest,timeUnit:Calendar.timeUnit,calendarState:Calendar.tabState,connectorEnabled:Calendar.connectorEnabled[Calendar.currentTabName]};
        });
        tabInterface.setActiveTab(Calendar.currentTabName);
        Calendar.currentTab = tabInterface.getActiveTab();
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
	
	function tabExistsForTimeUnit(tab, unit) {
		var tabExistsForTimeUnit = false;
		for (var i=0; i<tabs[unit].length; i++) {
			if (tabs[unit][i]===tab)
				tabExistsForTimeUnit = true;
		}
		return tabExistsForTimeUnit;
	}

    function isValidTabName(tabName) {
        for (var i = 0; i < tabs.fullList.length; i++) {
            if (tabs.fullList[i]===tabName)
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
