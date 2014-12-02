define(["core/DashboardWidget"],function(DashboardWidget){
    var SandboxedWidget = new DashboardWidget();



    SandboxedWidget.init = function(){
        /*console.log("SANDBOX:" + this.manifest.WidgetRepositoryURL + "/"
            + this.manifest.WidgetName + "/"
            + this.manifest.WidgetName + ".js");                                                   */
        this.iframe = $("<iframe class='widgetSandboxIframe' sandbox='allow-scripts'></iframe>");
        var that = this;
        this.iframe[0].onload = function(){
            $(that.iframe[0].contentDocument.body).addClass("flx-body");
            var script = that.iframe[0].contentDocument.createElement("script");
            script.type = "text/javascript";
            script.src = "/" + window.FLX_RELEASE_NUMBER + "/js/core/widgetSandbox/sandboxInitializationScript.js";
            that.iframe[0].contentDocument.head.appendChild(script);
        }
        $("#" + this.manifest.WidgetName + "-widget .flx-body").replaceWith(this.iframe);
    }

    return SandboxedWidget;
});