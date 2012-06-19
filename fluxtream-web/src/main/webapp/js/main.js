require(['App', 'Connectors'], function(App, Connectors) {
	document.body.onselectstart = function() { return false; };
	document.body.style.MozUserSelect = "none";
	document.body.style.KhtmlUserSelect = "none";
	document.body.unselectable = "on";
	App.initialize();
});


//below are require statements for all dynamically required files.
//this allws for the optimizer to properly package all the files together

//templates loaded via App.loadMustacheTemplate
require([
    "text!connectorMgmtTemplates.html",
    "text!addressesTemplate.html",
    "text!applications/calendar/facetTemplates.html",
    "text!applications/calendar/tabs/clock/clockTemplate.html",
    "text!applications/calendar/tabs/photos/photosTemplate.html",
    "text!applications/calendar/tabs/timeline/timelineTemplates.html"
]);

//Applications
require([
        "applications/calendar/App"
]);

//calendar tabs
require([
    "applications/calendar/tabs/clock/ClockTab",
    "applications/calendar/tabs/dashboard/DashboardTab",
    "applications/calendar/tabs/dashboards/DashboardsTab",
    "applications/calendar/tabs/diary/DiaryTab",
    "applications/calendar/tabs/list/ListTab",
    "applications/calendar/tabs/map/MapTab",
    "applications/calendar/tabs/photos/PhotosTab",
    "applications/calendar/tabs/timeline/TimelineTab"
])