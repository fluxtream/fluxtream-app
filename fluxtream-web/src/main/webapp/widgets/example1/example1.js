define(["App"], function() {
    var Example1 = {};
    Example1.load = function(manifest, digest) {
        require(["text!" + manifest.WidgetRepositoryURL + "/"
                + manifest.WidgetName + "/example1.mustache"], function(template) {
            var html = Hogan.compile(template);
            $("#example1-widget .flx-body").empty();
            $("#example1-widget .flx-body").append(html.render({"manifest" : manifest}));
        });
    }
    return Example1;
})