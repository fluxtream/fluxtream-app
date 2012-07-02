define(["App"], function() {
    var StepsTaken = {};
    StepsTaken.load = function(manifest, digest) {
        require(["text!" + manifest.WidgetRepositoryURL + "/"
                + manifest.WidgetName + "/averageWeight.mustache"], function(template) {
            var html = Hogan.compile(template);
            $("#stepsTaken-widget .flx-body").empty();
            var weight = "No Measure";
            if (typeof(digest.cachedData["withings-weight"])!="undefined" &&
                typeof(digest.cachedData["withings-weight"][0])!="undefined") {
                var averageWeight = 0;
                var measures = 0;
                for (var i=0; i<digest.cachedData["withings-weight"].length; i++) {
                    if (digest.cachedData["withings-weight"][i].weight>0) {
                        measures++;
                        averageWeight+=digest.cachedData["withings-weight"][i].weight;
                    }
                }
                weight = Math.round((averageWeight/measures)*100)/100;
            }
            $("#averageWeight-widget .flx-body").append(
                html.render({"manifest" : manifest, "digest" : digest, "weight" : weight})
            );
        });
    }
    return StepsTaken;
})