define(["core/DashboardWidget"], function(DashboardWidget) {

    var StepsHistory = new DashboardWidget();

    StepsHistory.init = function() {
        require(["text!" + this.manifest.WidgetRepositoryURL + "/"
                + this.manifest.WidgetName + "/stepsHistory.mustache"], function(template) {
            StepsHistory.postLoad(template);
        });
    };

    StepsHistory.postLoad = function(template) {
        var html = Hogan.compile(template);
        $("#stepsHistory-widget .flx-body").empty();
        $("#stepsHistory-widget .flx-body").append(
            html.render({"manifest" : this.manifest})
        );
    }

    return StepsHistory;
})