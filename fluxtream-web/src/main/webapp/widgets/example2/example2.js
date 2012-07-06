define(["core/DashboardWidget"], function(DashboardWidget) {

    var Example2 = new DashboardWidget();

    Example2.init = function() {
        var that = this;
        require(["text!" + this.manifest.WidgetRepositoryURL + "/"
                + this.manifest.WidgetName + "/example2.mustache"], function(template) {
            var html = Hogan.compile(template);
            $("#example2-widget .flx-body").empty();
            $("#example2-widget .flx-body").append(html.render({"manifest" : that.manifest}));
        });
    }
    return Example2;
})