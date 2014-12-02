define(["core/DashboardWidget"],function(DashboardWidget){

    var SandboxedWidget = function(){

    };

    for (var member in DashboardWidget.prototype) {
        SandboxedWidget.prototype[member] = DashboardWidget.prototype[member];
    }

    SandboxedWidget.prototype.init = function() {
        /*console.log("SANDBOX:" + this.manifest.WidgetRepositoryURL + "/"
            + this.manifest.WidgetName + "/"
            + this.manifest.WidgetName + ".js");                                                   */
        this.messagerId = ++App.sandboxCounter;
        this.iframe = $("<iframe class='widgetSandboxIframe' sandbox='allow-scripts'></iframe>");
        var that = this;
        this.iframe[0].onload = function(){
            $(that.iframe[0].contentDocument.body).addClass("flx-body");
            var script = that.iframe[0].contentDocument.createElement("script");
            script.type = "text/javascript";
            script.innerText = "window.FLX_RELEASE_NUMBER=" + JSON.stringify(window.FLX_RELEASE_NUMBER) + ";";
            script.innerText += "window.manifest=" + JSON.stringify(that.manifest) + ";";
            script.innerText += "window.messagerId=" + JSON.stringify(that.messagerId) + ";";
            script.innerText += "window.targetOrigin=" + JSON.stringify(window.location.origin) + ";";
            that.iframe[0].contentDocument.head.appendChild(script);
            script = that.iframe[0].contentDocument.createElement("script");
            script.type = "text/javascript";
            script.src = "/" + window.FLX_RELEASE_NUMBER + "/js/core/widgetSandbox/sandboxInitializationScript.js";
            that.iframe[0].contentDocument.head.appendChild(script);
        };
        this.iframe.addClass("flx-body");
        App.addSandboxMessageListener(this.messageListener);
        $("#" + this.manifest.WidgetName + "-widget .flx-body").replaceWith(this.iframe);

    }

    SandboxedWidget.prototype.messageListener = function(event) {
        var message = event.data;
        if (message.messagerId != this.messagerId)
            return;
        if (message.widgetName != this.manifest.WidgetName)
            return;
        if (message.WidgetRepositoryURL != this.manifest.WidgetRepositoryURL)
            return;
        if (message.data == "ready") {
            var digestStrippedDown = {};
            this.iframe[0].contentWindow.postMessage(new Message("digest",digestStrippedDown),"*");

        }
    }

    function Message(type,data) {
        this.type = type;
        this.data = data;

    }


    return SandboxedWidget;
});