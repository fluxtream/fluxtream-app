define(["App"], function() {
    var CaloriesBurned = {};
    CaloriesBurned.load = function(manifest, digest) {
        require(["text!" + manifest.WidgetRepositoryURL + "/"
                + manifest.WidgetName + "/caloriesBurned-day.mustache"], function(template) {
            var html = Hogan.compile(template);
            $("#caloriesBurned-widget .flx-body").empty();
            $("#caloriesBurned-widget .flx-body").append(html.render({"manifest" : manifest}));
        });
    }
    return CaloriesBurned;
})