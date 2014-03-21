require.config({
    waitSeconds: 0
});

require(['App', 'Connectors'], function(App, Connectors) {
	App.initialize();
    setAvatarImage();
});

function setAvatarImage() {
    $.ajax({
        url: "/api/guest/avatarImage",
        success: function(result) {
            if (result.type!="none") {
                $("#profileIcon").replaceWith("<img src=\"" + result.url + "\" style=\"display:inline;width:27px;margin: 0 1px 0 4px;\" width=27 height=27>");
                $("#profileIconCaret").css("margin-top", "10px");
                $("#helpDropdownToggle").css("margin-top", "3px");
                $("#connectorsDropdownToggle").css("margin-top", "3px");
                $("#appsMenuWrapper").css("margin-top", "4px");
                $(".brand").css("margin-top", "3px");
            } else {
                $("#profileIcon").replaceWith("<i class=\"icon-user icon-large\"></i>");
            }
        },
        error: function() {
            $("#profileIcon").replaceWith("<i class=\"icon-user icon-large\"></i>");
        }
    });
}


//below are require statements for all dynamically required files.
//this allows for the optimizer to properly package all the files together

//templates loaded via App.loadMustacheTemplate

var aggressiveLoading = false;

if (aggressiveLoading) {
    require([
        "text!notificationTemplates.html",
        "text!connectorMgmtTemplates.html",
        "text!addressesTemplate.html",
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