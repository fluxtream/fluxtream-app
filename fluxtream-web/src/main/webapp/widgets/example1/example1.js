define(["core/DashboardWidget"], function(DashboardWidget) {

    var Example1 = new DashboardWidget();

    Example1.init = function() {
        var that = this;
        require(["text!" + this.manifest.WidgetRepositoryURL + "/"
                + this.manifest.WidgetName + "/example1.mustache"], function(template) {
            var html = Hogan.compile(template);
            $("#example1-widget .flx-body").empty();
            $("#example1-widget .flx-body").append(html.render({"manifest" : that.manifest}));
        });
    }
    return Example1;
})