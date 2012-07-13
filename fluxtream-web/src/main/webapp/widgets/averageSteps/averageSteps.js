define(["core/DashboardWidget"], function(DashboardWidget) {

    var StepsTaken = new DashboardWidget();

    StepsTaken.init = function() {
        require(["text!" + this.manifest.WidgetRepositoryURL + "/"
                + this.manifest.WidgetName + "/averageSteps.mustache"], function(template) {
            StepsTaken.postLoad(template);
        });
    };

    StepsTaken.postLoad = function(template) {
        var html = Hogan.compile(template);
        $("#stepsTaken-widget .flx-body").empty();
        var steps = 0, averageSteps = 0, measures = 0;
        if (typeof(this.digest.cachedData["fitbit-activity_summary"])!="undefined" &&
            typeof(this.digest.cachedData["fitbit-activity_summary"][0])!="undefined") {
            for (var i=0; i<this.digest.cachedData["fitbit-activity_summary"].length; i++) {
                if (this.digest.cachedData["fitbit-activity_summary"][i].steps>0) {
                    measures++;
                    averageSteps+=this.digest.cachedData["fitbit-activity_summary"][i].steps;
                }
            }
            if (averageSteps>0)
                steps = this.addCommas(Math.round((averageSteps/measures)*1)/1);
        }
        var params = {"manifest" : this.manifest,
            "steps" : averageSteps>0?steps:null,
            "noMeasure" : averageSteps===0,
            "noMeasureMsg" : StepsTaken.noSteps()};
        $("#averageSteps-widget .flx-body").append(
            html.render(params)
        );
    };

    StepsTaken.validateSettings = function() {
        var dailyGoal = $("#averageStepsDailyGoal").val();
        this.saveSettings({"dailyGoal" : dailyGoal});
    };

    StepsTaken.bindWidgetSettings = function(widgetSettings) {
        $("#averageStepsDailyGoal").val(widgetSettings.dailyGoal);
    }

    StepsTaken.defaultSettings = function(widgetSettings) {
        if (typeof(widgetSettings.dailyGoal)=="undefined")
            widgetSettings.dailyGoal = 0;
    }

    StepsTaken.noSteps = function() {
        return "No Steps Recorded " + this.getPrettyTimeUnit();
    };

    return StepsTaken;
})