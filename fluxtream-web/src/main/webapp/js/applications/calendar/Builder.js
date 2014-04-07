define(["core/TabInterface", "core/DateUtils"], function(TabInterface, DateUtils) {
	
	var Builder = {};

    var tabsPath = "applications/calendar/tabs/";
    var tabPaths = [tabsPath + "clock/ClockTab", tabsPath + "dashboards/DashboardsTab",
                    tabsPath + "list/ListTab",
                    tabsPath + "map/MapTab", tabsPath + "photos/PhotosTab",
                    tabsPath + "timeline/TimelineTab"];

    var tabInterface = new TabInterface(tabPaths);
	
	var tabs = {
        "fullList":["clock","dashboards","map","photos","list","timeline"],
        "date":["clock", "dashboards", "map", "photos", "list", "timeline"],
        "week":["list", "map", "photos", "dashboards", "timeline"],
        "month":["list", "map", "photos", "dashboards", "timeline"],
        "year":["list", "photos", "dashboards", "timeline"]
	};
    tabInterface.setTabVisibility(tabs.fullList,true);

    var timeUnits = ['date', 'week', 'month', 'year'];

    var connectorNames = [];

    Builder.init = function(App, Calendar){
        bindDatepicker(App, Calendar);
        bindTabInterface(Calendar);
        bindConnectorButtons(App, Calendar);
        bindTimeUnitsMenu(Calendar);
        bindTimeNavButtons(Calendar);
    }

    function bindDatepicker(App, Calendar) {
        $("#datepicker").datepicker().on("changeDate", function(event) {
            if (Calendar.timeUnit == "date"){
                var formatted = App._formatDateAsDatePicker(event.date.getUTCFullYear(),
                    event.date.getUTCMonth(),
                    event.date.getUTCDate());
                Calendar.navigateState(Calendar.currentTab.name + "/date/" + formatted);
            }
            else if (Calendar.timeUnit == "week"){
                var formatted = App._formatDateAsDatePicker(event.date.getUTCFullYear(),
                    event.date.getUTCMonth(),
                    event.date.getUTCDate());
                var date = moment(formatted, "YYYY-MM-DD");
                Calendar.navigateState(Calendar.currentTab.name + "/week/" + date.year() + "/" + date.week());
            }
            $(".datepicker").hide();
        });
        $("#datepicker").click(function(){
            if (Calendar.timeUnit == "month" || Calendar.timeUnit == "year"){
                $(".datepicker-days .switch").click();
            }
            if (Calendar.timeUnit == "year"){
                $(".datepicker-months .switch").click();
            }
        });
        $(".datepicker-months td").click(function(event){
            if (Calendar.timeUnit == "month" && $(event.target).hasClass("month")){
                var year = $(".datepicker-months .switch").text();
                var month = DateUtils.getMonthFromName($(event.target).text()) + 1;
                Calendar.navigateState(Calendar.currentTab.name + "/month/" + year + "/" + month);
                $(".datepicker").hide();
            }
        });
    }

    function bindTabInterface(Calendar) {
        $("#calendarTabs").replaceWith(tabInterface.getNav());
        tabInterface.getNav().addClickListener(function(tabName){
            Calendar.navigateState(tabName + "/" + Calendar.tabState);
        });
    }

    Builder.getConnectorButton = function(connector,Calendar) {
        if (typeof connector === "string"){
            return $("#flx-connector-btn-" + connector);
        }
        if ($("#flx-connector-btn-" + connector.connectorName).length == 0){
            createConnectorButton(null,Calendar,connector);
            connectorNames.push(connector.connectorName)
        }
        return $("#flx-connector-btn-" + connector.connectorName);
    };

    Builder.getConnectorNames = function() {
        return connectorNames;
    };

    function connectorClicked(Calendar, connector) {
        var connectorName = connector.connectorName,
            button = Builder.getConnectorButton(connectorName);
        //if (button.is(".flx-disconnected")) {
        //    return;
        //}
        var enabled = !Calendar.connectorEnabled[Calendar.currentTabName][connectorName];
        Calendar.connectorEnabled[Calendar.currentTabName][connectorName] = enabled;
        button.toggleClass("flx-active", enabled);
        button.toggleClass("flx-inactive", !enabled);
        Calendar.currentTab.connectorToggled(connectorName, connector.facetTypes, enabled);
    }

    function createConnectorButton(App, Calendar, connector) {
        //var configFilterLabel = App.getConnectorConfig(connector.connectorName).filterLabel,
        //    filterLabel = configFilterLabel || connector.name;
        var button = $('<li/>');
        $('<a/>', {
            href: "#",
            id: "flx-connector-btn-" + connector.connectorName,
            class: "flx-active"
        }).click(function(event){
            event.preventDefault();
            $(document).click(); //needed for click away to work on tooltips in clock tab
            connectorClicked(Calendar, connector);
            var uploadData = {};
            for (var member in Calendar.connectorEnabled){
                if (member != "default")
                    uploadData[member] = Calendar.connectorEnabled[member];
            }
            $.ajax("/api/connectors/filters",{
                type:"POST",
                data:{filterState:JSON.stringify(uploadData)}
            });
            for (var member in uploadData){
                Calendar.connectorEnabled[member] = uploadData[member];
            }
        }).appendTo(button);
        button.hide();
        $("#selectedConnectors").append(button);
        return button;
    }

    function bindConnectorButtons(App, Calendar) {
        $.ajax({
            url: "/api/connectors/installed",
            async: false,
            success: function(response) {
                $.each(response, function(i, connector) {
                    createConnectorButton(App, Calendar, connector);
                    connectorNames.push(connector.connectorName);
                });
            }
        });

        $(window).scroll(function(event){
            var beginFloat = $("#calendar-app .nav-tabs").offset().top + $("#calendar-app .nav-tabs").height();
            if (beginFloat <= 0){
                beginFloat = $(window).height() * 5000;
            }
            var scrollTop = $(window).scrollTop();
            if (scrollTop < beginFloat){
                $("#filtersContainer").removeClass("floating");
                $("#filterPlaceHolderElement").addClass("hidden");

            }
            else{
                $("#filtersContainer").addClass("floating");
                $("#filterPlaceHolderElement").removeClass("hidden");
                $("#filterPlaceHolderElement").width($("#filtersContainer").width());
                $("#filterPlaceHolderElement").height($("#filtersContainer").height());
            }
        });

        $(window).scroll();
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

                    if (Calendar.dateAxisCursorPosition == null || Calendar.currentTabName != "timeline"){
                        var newState = updateTimeUnit(timeUnit, Calendar.tabState);
                        Calendar.navigateState(Calendar.currentTab.name + "/" + newState);
                    }
                    else{
                        var state = Calendar.toState(Calendar.currentTabName,timeUnit,new Date(Calendar.dateAxisCursorPosition * 1000));
                        Calendar.renderState(state);
                    }

                });
        });
	}
	
	function bindTimeNavButtons(Calendar) {
        $(".menuNextButton").click(function(event) {
            if($(event.delegateTarget).hasClass('disabled'))
                return;
            var nextTimespan = computeNextTimespan(Calendar.timespanState);
            Calendar.navigateState(Calendar.currentTab.name+"/"+nextTimespan);
        });
        $(".menuPrevButton").click(function(event) {
            if($(event.delegateTarget).hasClass('disabled'))
                return;
            var prevTimespan = computePreviousTimespan(Calendar.timespanState);
            Calendar.navigateState(Calendar.currentTab.name+"/"+prevTimespan);
        });
        $(".menuTodayButton").click(function(event) {
            if($(event.delegateTarget).hasClass('disabled'))
                return;
            Calendar.navigateState(Calendar.currentTab.name+"/date/"+moment().format("YYYY-MM-DD"));
        });
        nextPrevEnable();//removed a switch statement here that did the same thing for every possible timeunit
	};

    function updateTimeUnit(timeUnit, state) {
        var splits = state.split("/");
        var date;
        if (splits[0]==="date"){
            date = moment(splits[1], "YYYY-MM-DD");
        } else if (splits[0]==="week") {
            var year = Number(splits[1]);
            var week = Number(splits[2]);
            date = moment().year(year).isoWeek(week).day(0);
        } else if (splits[0]==="month") {
            var year = Number(splits[1]);
            var month = Number(splits[2])-1;
            date = moment().year(year).month(month).date(1);
        }
        switch(timeUnit){
            case "date":
                return "date/" + date.format("YYYY-MM-DD");
            case "week":
                return "week/" + date.year() + "/" + date.week();
            case "month":
                var month = date.month()+1;
                return "month/" + date.year() + "/" + month;
        }
    }

    function computePreviousTimespan(state){
        var splits = state.split("/");
        if (splits[0]==="date"){
            var date = moment(splits[1], "YYYY-MM-DD");
            date.subtract("days", 1);
            var result = date.format("YYYY-MM-DD");
            return "date/" + result;
        } else if (splits[0]==="week") {
            var year = Number(splits[1]);
            var week = Number(splits[2]);
            var startDay = moment().year(year).isoWeek(week).day("Thursday").subtract("weeks", 1);
            var result = "week/" + startDay.year() + "/" + startDay.isoWeek();
            return result;
        } else if (splits[0]==="month") {
            var year = Number(splits[1]);
            var month = Number(splits[2])-1;
            var startDay = moment().year(year).month(month).date(1).subtract("months", 1);
            var result = "month/" + startDay.year() + "/" + (startDay.month()+1);
            return result;
        }
    }

    function computeNextTimespan(state){
        var splits = state.split("/");
        if (splits[0]==="date"){
            var date = moment(splits[1], "YYYY-MM-DD");
            date.add("days", 1);
            var result = date.format("YYYY-MM-DD");
            return "date/" + result;
        } else if (splits[0]==="week") {
            var year = Number(splits[1]);
            var week = Number(splits[2]);
            var endDay = moment().year(year).isoWeek(week).day("Thursday").add("weeks", 1);
            var result = "week/" + endDay.year() + "/" + endDay.isoWeek();
            return result;
        } else if (splits[0]==="month") {
            var year = Number(splits[1]);
            var month = Number(splits[2])-1;
            var startDay = moment().year(year).month(month).date(1).add("months", 1);
            var result = "month/" + startDay.year() + "/" + (startDay.month()+1);
            return result;
        }
    }

	function nextPrevEnable() {
        $(".menuNextButton").removeClass("disabled");
        $(".menuPrevButton").removeClass("disabled");
    };

	function updateTab(digest, Calendar, force) {
        if (App.activeApp.name != "calendar")
            return;
        tabInterface.setRenderParamsFunction(function(){
            return $.extend({
                digest:digest,
                timeUnit:Calendar.timeUnit,
                calendarState:Calendar.tabState,
                connectorEnabled:Calendar.connectorEnabled[Calendar.currentTabName],
                tabParam:Calendar.tabParam,
                setTabParam:Calendar.setTabParam,
                doneLoading:Calendar.stopLoading,
                forceReload: force}, Calendar.params);
        });
        tabInterface.setActiveTab(Calendar.currentTabName);

        updateCurrentTab(digest, Calendar);
	}

    /**
     * determines what filter buttons to display in the current tab depending on the ConnectorConfig js
     * @param digest
     * @param Calendar
     */
    function updateCurrentTab(digest, Calendar){
        Calendar.currentTab = tabInterface.getActiveTab();
        if (Calendar.currentTab == null){
            $.doTimeout(50,function(){updateCurrentTab(digest, Calendar)});
            return;
        }
        for (var i = 0; i < digest.selectedConnectors.length; i++){
            var button = $("#flx-connector-btn-" + digest.selectedConnectors[i].connectorName);
            if (button.length == 0){
                button = createConnectorButton(App,Calendar,digest.selectedConnectors[i]);
            }
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

    return Builder;
	
});
