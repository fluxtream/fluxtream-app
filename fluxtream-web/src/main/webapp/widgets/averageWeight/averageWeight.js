define(["core/DashboardWidget","core/widgetComponents/averageWeight"], function(DashboardWidget,AverageWeightComponent) {

    var AverageWeight = new DashboardWidget();

    AverageWeight.init = function() {
        require(["text!" + this.manifest.WidgetRepositoryURL + "/"
                + this.manifest.WidgetName + "/averageWeight.mustache"], function(template) {
            AverageWeight.postLoad(template);
        });
    }

    AverageWeight.postLoad = function(template) {
        var html = Hogan.compile(template);
        $("#averageWeight-widget .flx-body").empty();
        $("#averageWeight-widget .flx-body").append(html.render({}))
        var weightArray = null;
        var weightParam = ""
        if (typeof(this.digest.cachedData["withings-weight"])!="undefined" &&
            typeof(this.digest.cachedData["withings-weight"][0])!="undefined") {
            weightArray = this.digest.cachedData["withings-weight"];
            weightParam = "weight"
        }
        new AverageWeightComponent(this,$("#averageWeightWidget"),weightArray,weightParam);
    }

    return AverageWeight;
})