define(["core/DashboardWidget","core/widgetComponents/averageSteps"], function(DashboardWidget,averageStepsComponent) {

    var StepsTaken = new DashboardWidget();

    StepsTaken.postLoad = function() {
        $("#stepsTaken-widget .flx-body").empty();

        var stepsArray = null;
        var stepsName = ""
        if (typeof(this.digest.facets["bodymedia-steps"])!="undefined" &&
            typeof(this.digest.facets["bodymedia-steps"][0])!="undefined"){
            stepsArray = this.digest.facets["bodymedia-steps"];
            stepsName = "totalSteps";
        }
        else if (typeof(this.digest.facets["fitbit-activity_summary"])!="undefined" &&
            typeof(this.digest.facets["fitbit-activity_summary"][0])!="undefined"){
            stepsArray = this.digest.facets["fitbit-activity_summary"];
            stepsName = "steps";
        }

        $("#averageSteps-widget .flx-body").empty();
        $("#averageSteps-widget .flx-body").append(
            this.template.render({})
        );
        new averageStepsComponent(this,$("#averageStepsWidget"),stepsArray,stepsName);
    };

    StepsTaken.validateSettings = function(onDone) {
        var dailyThreshold = $("#averageStepsHappyDailyThreshold").val();
        if(isNaN(dailyThreshold)) {
            alert("You need to enter a valid integer here...");
            onDone();
        } else {
            this.saveSettings({"dailyThreshold" : dailyThreshold},onDone);
        }
    };

    StepsTaken.bindWidgetSettings = function(widgetSettings,onDone) {
        $("#averageStepsHappyDailyThreshold").val(widgetSettings.dailyThreshold);
        onDone();
    }

    StepsTaken.defaultSettings = function(widgetSettings,onDone) {
        if (typeof(widgetSettings.dailyThreshold)=="undefined")
            widgetSettings.dailyThreshold = 0;
        onDone();
    }

    return StepsTaken;
})