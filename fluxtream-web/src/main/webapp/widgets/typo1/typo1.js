define(["core/DashboardWidget"], function(DashboardWidget) {

    var Typo1 = new DashboardWidget();

    Typo1.init = function() {
        require(["text!" + this.manifest.WidgetRepositoryURL + "/"
                + this.manifest.WidgetName + "/typo1.mustache"], function(template) {
            Typo1.postLoad(template);
        });
    };

    Typo1.postLoad = function(template) {
        var html = Hogan.compile(template);
        $("#typo1-widget .flx-body").empty();
        $("#typo1-widget .flx-body").append(
            html.render({"manifest" : this.manifest})
        );
    }

    return Typo1;
})