define(["core/DashboardWidget","core/widgetComponents/averageBloodPressure"], function(DashboardWidget,AverageBloodPressureComponent) {

    var AverageBloodPressure = new DashboardWidget();

    AverageBloodPressure.init = function() {
        require(["text!" + this.manifest.WidgetRepositoryURL + "/"
                + this.manifest.WidgetName + "/averageBloodPressure.mustache"], function(template) {
            AverageBloodPressure.postLoad(template);
        });
    }

    AverageBloodPressure.postLoad = function(template) {
        var html = Hogan.compile(template);
        $("#averageBloodPressure-widget .flx-body").empty();
        $("#averageBloodPressure-widget .flx-body").append(html.render({}))
        var bloodPressureArray = null;
        var systolicParam = ""
        var diastolicParam = ""
        if (typeof(this.digest.cachedData["withings-blood_pressure"])!="undefined" &&
            typeof(this.digest.cachedData["withings-blood_pressure"][0])!="undefined") {
            bloodPressureArray = this.digest.cachedData["withings-blood_pressure"];
            systolicParam = "systolic";
            diastolicParam = "diastolic";
        }
        new AverageBloodPressureComponent(this,$("#averageBloodPressureWidget"),bloodPressureArray,systolicParam,diastolicParam);
    }

    return AverageBloodPressure;
})