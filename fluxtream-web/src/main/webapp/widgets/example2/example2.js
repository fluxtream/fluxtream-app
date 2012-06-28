define(["App"], function() {
    var Example2 = {};
    Example2.load = function(manifest, digest) {
        require(["text!" + manifest.WidgetRepositoryURL + "/"
                + manifest.WidgetName + "/example2.mustache"], function(template) {
            var html = Hogan.compile(template);
            $("#example2-widget .flx-body").empty();
            $("#example2-widget .flx-body").append(html.render({"manifest" : manifest}));
        });
    }
    return Example2;
})