require.config({
    waitSeconds: 0
});

(function(){
    //Before loading the app we need to load fallbacks for if APIs are innaccessible
    if (window.google == null){//google maps API was blocked
        require(["core/libFallback/GoogleFallback"],function(google){
            window.google = google;
        });
    }

    //Now we can load the app
    require(['App', 'Connectors'], function(App, Connectors) {
        App.initialize();
    });
})();


//below are require statements for all dynamically required files.
//this allows for the optimizer to properly package all the files together

//templates loaded via App.loadMustacheTemplate

var aggressiveLoading = false;

if (aggressiveLoading) {
    require([
        "text!notificationTemplates.html",
        "text!connectorMgmtTemplates.html",
        "text!applications/calendar/facetTemplates.html",
        "text!applications/calendar/tabs/clock/clockTemplate.html",
        "text!applications/calendar/tabs/photos/photosTemplate.html",
        "text!core/grapher/timelineTemplates.html",
        "text!applications/calendar/tabs/dashboards/dashboardsTabTemplates.html"
    ]);

    //Applications
    require([
            "applications/calendar/App",
            "applications/bodytrack/App"
    ]);

    //calendar tabs
    require([
        "applications/calendar/tabs/clock/ClockTab",
        "applications/calendar/tabs/dashboards/DashboardsTab",
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
        "text!applications/calendar/tabs/photos/photos.html"
    ]);

    //bodytrack tabs
    require([
        "applications/bodytrack/tabs/grapher/GrapherTab",
        "applications/bodytrack/tabs/views/ViewsTab"
    ]);

    //bodytrack tabs' main templates
    require([
        "text!applications/bodytrack/tabs/grapher/grapher.html",
        "text!applications/bodytrack/tabs/views/views.html"
    ]);

    //widgets
    require([
        "../widgets/averageBloodPressure/averageBloodPressure",
        "../widgets/averageSteps/averageSteps",
        "../widgets/averageWeight/averageWeight",
        "../widgets/grapherWidget/grapherWidget",
    ]);
}