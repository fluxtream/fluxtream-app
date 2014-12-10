define(function() {

    function DashboardWidget() {}

    DashboardWidget.prototype.load = function(widgetInfo, dgst, dashboardId) {
        this.manifest = widgetInfo.manifest;
        this.digest = dgst;
        this.dashboardId = dashboardId;
        _.bindAll(this);
        this.settings = widgetInfo.settings;
        if (this.manifest.HasSettings) {
            var that = this;
            this.defaultSettings(this.settings,function(){
                $("#" + widgetInfo.manifest.WidgetName + "-widget-settings").unbind().click(function () {
                    that.showSettingsDialog(that.settings,function(){
                    });
                });
            });

        }
        this.init();
    }


    DashboardWidget.prototype.init = function() {
        var that = this;
        require(["text!" + this.manifest.WidgetRepositoryURL + "/"
                     + this.manifest.WidgetName + "/" + this.manifest.WidgetName + ".mustache"], function(template) {
            that.template = Hogan.compile(template);
            that.postLoad();
        });
    };

    DashboardWidget.prototype.postLoad = function() {
        alert("WARNING: '" + this.manifest.WidgetName + "' widget's postLoad()  method is not yet implemented!");
    }

    DashboardWidget.prototype.showSettingsDialog = function(settings,onDone) {
        var that = this;
        App.loadMustacheTemplate("applications/calendar/tabs/dashboards/dashboardsTabTemplates.html","widgetSettings",function(template) {
            var html = template.render({"manifest" : that.manifest});
            App.makeModal(html);
            that.loadWidgetSettingsForm(function(){
                $("#save-settings-" + that.manifest.WidgetName).click(function() {
                    that.validateSettings(function(){

                    });
                });
            });
            onDone();
        });
    }

    DashboardWidget.prototype.validateSettings = function(onDone) {
        alert("WARNING: '" + this.manifest.WidgetName + "' widget's validateSettings()  method is not yet implemented!");
        onDone();
    }

    DashboardWidget.prototype.getSaveSettingsUrl = function(){
        return "/api/v1/dashboards/" + this.dashboardId + "/widgets/" + this.manifest.WidgetName + "/settings";
    }

    DashboardWidget.prototype.saveSettings = function(settings,onDone) {
        this.settings = settings;
        var that = this;
        $.ajax({
            url: that.getSaveSettingsUrl(),
            type: "POST",
            data: {settingsJSON : JSON.stringify(settings)},
            success: function() {
                App.closeModal();
                that.postLoad();
                onDone();
            },
            error: function() {
                alert("Oops. We couldn't save your settings. Sorry about that.");
                onDone();
            }
        });
    }

    DashboardWidget.prototype.loadWidgetSettingsForm = function(onDone) {
        var that = this;
        $.ajax({url:this.manifest.WidgetRepositoryURL + "/" + this.manifest.WidgetName + "/settings.mustache",
            success: function(html) {
                var selector = "#" + that.manifest.WidgetName + "-widgetSettings";
                $(selector).replaceWith(html);
                that.loadWidgetSettingsData(onDone);
            }
        });
    }

    DashboardWidget.prototype.loadWidgetSettingsData = function(onDone) {
        var that = this;
        $.ajax({
            url: "/api/v1/dashboards/" + that.dashboardId + "/widgets/" + that.manifest.WidgetName + "/settings",
            type: "GET",
            success: function(widgetSettings) {
                that.defaultSettings(widgetSettings,function(){
                    $("#widgetSettings form").submit(function(evt) {
                        that.validateSettings(function(){

                        });
                        evt.preventDefault();
                    });
                    that.bindWidgetSettings(widgetSettings,onDone);
                });
            },
            error: function() {
                alert("Oops. We couldn't get your settings. Sorry about that.")
            }
        })
    }

    DashboardWidget.prototype.bindWidgetSettings = function(widgetSettings,onDone) {
        alert("WARNING: '" + this.manifest.WidgetName + "' widget's bindWidgetSettings()  method is not yet implemented!");
        onDone();
    }

    DashboardWidget.prototype.defaultSettings = function(widgetSettings,onDone) {
        alert("WARNING: '" + this.manifest.WidgetName + "' widget's defaultSettings()  method is not yet implemented!");
        onDone();
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
        var unit = this.getTimeUnit().toLowerCase();
        switch (unit) {
            case "day":
                return "Today";
            case "week":
                return "This Week";
            case "month":
                return "This Month";
            case "year":
                return "This Year";
        }
        return null;
    }

    DashboardWidget.prototype.getTimeUnit=function() {
        return this.digest.metadata.timeUnit;
    }

    DashboardWidget.prototype.setTitle=function(title){
        $("#" + this.manifest.WidgetName + "-widget-title").text(title);
    }

    return DashboardWidget;

});