define(["App"], function() {
    var StepsTaken = {};
    StepsTaken.load = function(manifest, digest) {
        require(["text!" + manifest.WidgetRepositoryURL + "/"
                + manifest.WidgetName + "/stepsTaken.mustache"], function(template) {
            var html = Hogan.compile(template);
            $("#stepsTaken-widget .flx-body").empty();
            var weight = digest.cachedData["withings-weight"][0].description;
            console.log(digest);
            $("#stepsTaken-widget .flx-body").append(
                html.render({"manifest" : manifest, "digest" : digest, "weight" : weight})
            );
        });
    }
    return StepsTaken;
})