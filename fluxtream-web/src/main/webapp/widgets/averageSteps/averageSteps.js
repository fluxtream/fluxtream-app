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
        var steps = 0;
        var stepsText;
        if (typeof(this.digest.cachedData["fitbit-activity_summary"])!="undefined" &&
            typeof(this.digest.cachedData["fitbit-activity_summary"][0])!="undefined") {
            var averageSteps = 0;
            var measures = 0;
            for (var i=0; i<this.digest.cachedData["fitbit-activity_summary"].length; i++) {
                if (this.digest.cachedData["fitbit-activity_summary"][i].steps>0) {
                    measures++;
                    averageSteps+=this.digest.cachedData["fitbit-activity_summary"][i].steps;
                }
            }
            if (averageSteps>0) {
                steps = this.addCommas(Math.round((averageSteps/measures)*1)/1);
                stepsText = steps + " Steps";
            } else
                stepsText = StepsTaken.noSteps();
        } else
            stepsText = StepsTaken.noSteps();
        $("#averageSteps-widget .flx-body").append(
            html.render({"manifest" : this.manifest, "steps" : stepsText})
        );
    }

    StepsTaken.noSteps = function() {
        return "No Steps Recorded " + this.getPrettyTimeUnit();
    }

    return StepsTaken;
})