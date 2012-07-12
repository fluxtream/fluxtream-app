define(function() {

    function DashboardWidget() {}

    DashboardWidget.prototype.load = function(mnfst, dgst) {
        this.manifest = mnfst;
        this.digest = dgst;
        _.bindAll(this);
        this.init();
        var that = this;
        $("#" + mnfst.WidgetName + "-widget-settings").click(function () {
            console.log("widget settings...");
            that.settings();
        });
    }

    DashboardWidget.prototype.settings = function() {
        var that = this;
        App.loadMustacheTemplate("applications/calendar/tabs/dashboards/dashboardsTabTemplates.html","widgetSettings",function(template) {
            var html = template.render({"manifest" : that.manifest});
            App.makeModal(html);
            that.loadWidgetSettings();
        });
    }

    DashboardWidget.prototype.loadWidgetSettings = function() {
        var that = this;
        require(["text!" + this.manifest.WidgetRepositoryURL + "/"
                     + this.manifest.WidgetName + "/settings.mustache"], function(html) {
            var selector = "#" + that.manifest.WidgetName + "-widgetSettings";
            $(selector).replaceWith(html);
        });
    }

    DashboardWidget.prototype.addCommas = function(nStr) {
        nStr += '';
        x = nStr.split('.');
        x1 = x[0];
        x2 = x.length > 1 ? '.' + x[1] : '';
        var rgx = /(\d+)(\d{3})/;
        while (rgx.test(x1)) {
            x1 = x1.replace(rgx, '$1' + ',' + '$2');
        }
        return x1 + x2;
    };

    DashboardWidget.prototype.getPrettyTimeUnit = function() {
        var unit = this.getTimeUnit();
        switch (unit) {
            case "DAY":
                return "Today";
            case "WEEK":
                return "This Week";
            default:
                return "This Year";
        }
    }

    DashboardWidget.prototype.getTimeUnit=function() {
        var ONE_DAY = 3600000*24;
        if (this.digest.tbounds.end-this.digest.tbounds.start===ONE_DAY-1)
            return "DAY";
        else if (this.digest.tbounds.end-this.digest.tbounds.start===7*ONE_DAY-1)
            return "WEEK";
        else return "YEAR";
    }

    return DashboardWidget;

});