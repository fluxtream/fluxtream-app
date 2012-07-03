define(["core/DashboardWidget"], function(DashboardWidget) {

    var Typo2 = new DashboardWidget();

    Typo2.init = function() {
        require(["text!" + this.manifest.WidgetRepositoryURL + "/"
                + this.manifest.WidgetName + "/typo2.mustache"], function(template) {
            Typo2.postLoad(template);
        });
    };

    Typo2.postLoad = function(template) {
        var html = Hogan.compile(template);
        $("#typo2-widget .flx-body").empty();
        $("#typo2-widget .flx-body").append(
            html.render({"manifest" : this.manifest})
        );
    }

    return Typo2;
})