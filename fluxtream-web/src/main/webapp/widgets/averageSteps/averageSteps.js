define(["core/DashboardWidget"], function(DashboardWidget) {

    var StepsTaken = new DashboardWidget();

    StepsTaken.postLoad = function() {
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
            if (averageSteps>0) {
                averageSteps/=measures;
                steps = this.addCommas(Math.round((averageSteps)*1)/1);
            }
        }
        var color = "rgb(0,0,0)";
        if (averageSteps>0) {
            if (typeof this.settings.dailyThreshold != "undefined") {
                if (averageSteps>=this.settings.dailyThreshold)
                    color = "rgb(0,255,0)";
                else
                    color = "rgb(255,0,0)";
            }
        }
        var params = {"manifest" : this.manifest,
            "settings" : this.settings,
            "color" : color,
            "steps" : averageSteps>0?steps:null,
            "noMeasure" : averageSteps===0,
            "noMeasureMsg" : StepsTaken.noSteps()};
        $("#averageSteps-widget .flx-body").empty();
        $("#averageSteps-widget .flx-body").append(
            this.template.render(params)
        );
    };

    StepsTaken.validateSettings = function() {
        var dailyThreshold = $("#averageStepsHappyDailyThreshold").val();
        if(isNaN(dailyThreshold)) {
            alert("You need to enter a valid integer here...");
        } else {
            this.saveSettings({"dailyThreshold" : dailyThreshold});
        }
    };

    StepsTaken.bindWidgetSettings = function(widgetSettings) {
        $("#averageStepsHappyDailyThreshold").val(widgetSettings.dailyThreshold);
    }

    StepsTaken.defaultSettings = function(widgetSettings) {
        if (typeof(widgetSettings.dailyThreshold)=="undefined")
            widgetSettings.dailyThreshold = 0;
    }

    StepsTaken.noSteps = function() {
        return "No Steps Recorded " + this.getPrettyTimeUnit();
    };

    return StepsTaken;
})