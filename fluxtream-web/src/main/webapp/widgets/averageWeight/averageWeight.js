define(["core/DashboardWidget"], function(DashboardWidget) {

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
        var weight = 0;
        if (typeof(this.digest.cachedData["withings-weight"])!="undefined" &&
            typeof(this.digest.cachedData["withings-weight"][0])!="undefined") {
            var averageWeight = 0;
            var measures = 0;
            for (var i=0; i<this.digest.cachedData["withings-weight"].length; i++) {
                if (this.digest.cachedData["withings-weight"][i].weight>0) {
                    measures++;
                    averageWeight+=this.digest.cachedData["withings-weight"][i].weight;
                }
            }
            weight = Math.round((averageWeight/measures)*100)/100;
        }
        var unit = this.digest.settings.weightMeasureUnit;
        if (unit==="SI")
            unit = "kg";
        else unit = unit.toLowerCase();
        $("#averageWeight-widget .flx-body").append(
            html.render({"manifest" : this.manifest, "weight" : weight>0
                ?weight + " " + unit
                :"No Measure " + this.getPrettyTimeUnit()})
        );
    }

    return AverageWeight;
})