define(["App"], function() {
    var StepsTaken = {};
    var digest;

    StepsTaken.load = function(manifest, dgst) {
        digest = dgst;
        require(["text!" + manifest.WidgetRepositoryURL + "/"
                + manifest.WidgetName + "/averageSteps.mustache"], function(template) {
            var html = Hogan.compile(template);
            $("#stepsTaken-widget .flx-body").empty();
            var steps = 0;
            if (typeof(digest.cachedData["fitbit-activity_summary"])!="undefined" &&
                typeof(digest.cachedData["fitbit-activity_summary"][0])!="undefined") {
                var averageSteps = 0;
                var measures = 0;
                for (var i=0; i<digest.cachedData["fitbit-activity_summary"].length; i++) {
                    if (digest.cachedData["fitbit-activity_summary"][i].steps>0) {
                        measures++;
                        averageSteps+=digest.cachedData["fitbit-activity_summary"][i].steps;
                    }
                }
                steps = addCommas(Math.round((averageSteps/measures)*1)/1);
            }
            $("#averageSteps-widget .flx-body").append(
                html.render({"manifest" : manifest, "digest" : digest, "steps" : steps})
            );
        });
    }

    function addCommas(nStr) {
        nStr += '';
        x = nStr.split('.');
        x1 = x[0];
        x2 = x.length > 1 ? '.' + x[1] : '';
        var rgx = /(\d+)(\d{3})/;
        while (rgx.test(x1)) {
            x1 = x1.replace(rgx, '$1' + ',' + '$2');
        }
        return x1 + x2;
    }

    function getTimeUnit() {
        var ONE_DAY = 3600000*24;
        if (digest.end-digest.start===ONE_DAY)
            return "DAY";
        else if (digest.end-digest.start===7*ONE_DAY)
            return "WEEK";
        else return "YEAR";
    }

    return StepsTaken;
})