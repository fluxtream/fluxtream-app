define(function() {
    var HoursSlept = {};
    HoursSlept.load = function(manifest, digest) {
        require(["text!" + manifest.WidgetRepositoryURL + "/"
                     + manifest.WidgetName + "/hoursSlept-day.mustache"], function(template) {
            var html = Hogan.compile(template);
            $("#hoursSlept-widget .flx-body").empty();
            $("#hoursSlept-widget .flx-body").append(html.render({"manifest" : manifest}));
        });
    }
    return HoursSlept;
})