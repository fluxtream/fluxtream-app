define(["core/DashboardWidget"], function(DashboardWidget) {

    var Typo3 = new DashboardWidget();

    Typo3.init = function() {
        require(["text!" + this.manifest.WidgetRepositoryURL + "/"
                     + this.manifest.WidgetName + "/typo3.mustache"], function(template) {
            Typo3.postLoad(template);
        });
    };

    Typo3.postLoad = function(template) {
        var html = Hogan.compile(template);
        $("#typo3-widget .flx-body").empty();
        $("#typo3-widget .flx-body").append(
            html.render({"manifest" : this.manifest})
        );
    }

    Typo3.noSteps = function() {
        return "No Steps Recorded " + this.getPrettyTimeUnit();
    }

    return Typo3;
})