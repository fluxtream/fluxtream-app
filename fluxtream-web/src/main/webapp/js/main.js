require(['App', 'Connectors'], function(App, Connectors) {
	document.body.onselectstart = function() { return false; };
	document.body.style.MozUserSelect = "none";
	document.body.style.KhtmlUserSelect = "none";
	document.body.unselectable = "on";
	App.initialize();
});


//below are require statements for all dynamically required files.
//this allows for the optimizer to properly package all the files together

//templates loaded via App.loadMustacheTemplate

var aggressiveLoading = false;

if (aggressiveLoading) {
    require([
        "text!connectorMgmtTemplates.html",
        "text!addressesTemplate.html",
        "text!applications/calendar/facetTemplates.html",
        "text!applications/calendar/tabs/clock/clockTemplate.html",
        "text!applications/calendar/tabs/photos/photosTemplate.html",
        "text!applications/calendar/tabs/timeline/timelineTemplates.html",
        "text!applications/calendar/tabs/dashboards/dashboardsTabTemplates.html"
    ]);

    //Applications
    require([
            "applications/calendar/App"
    ]);

    //calendar tabs
    require([
        "applications/calendar/tabs/clock/ClockTab",
        "applications/calendar/tabs/dashboards/DashboardsTab",
        "applications/calendar/tabs/diary/DiaryTab",
        "applications/calendar/tabs/list/ListTab",
        "applications/calendar/tabs/map/MapTab",
        "applications/calendar/tabs/photos/PhotosTab",
        "applications/calendar/tabs/timeline/TimelineTab"
    ]);

    //calendar tabs' main templates
    require([
        "text!applications/calendar/tabs/clock/clock.html",
        "text!applications/calendar/tabs/dashboards/dashboards.html",
        "text!applications/calendar/tabs/diary/dayDiary.html",
        "text!applications/calendar/tabs/diary/weekDiary.html",
        "text!applications/calendar/tabs/diary/monthDiary.html",
        "text!applications/calendar/tabs/diary/yearDiary.html",
        "text!applications/calendar/tabs/list/list.html",
        "text!applications/calendar/tabs/map/map.html",
        "text!applications/calendar/tabs/photos/photos.html",
        "text!applications/calendar/tabs/timeline/template.html"
    ]);
}