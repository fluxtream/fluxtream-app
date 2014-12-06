define(["core/DashboardWidget"],function(DashboardWidget){

    var SandboxedWidget = function(){
        this.communicationLinkReady = false;
        this.messageBacklog = [];
        this.defaultSettingsCalls = 0;
        this.bindWidgetSettingsCalls = 0;
        this.validateSettingsCalls = 0;
        this.callbacks = {};
        this.defaultSettingsObjects = {};
    };

    for (var member in DashboardWidget.prototype) {
        SandboxedWidget.prototype[member] = DashboardWidget.prototype[member];
    }

    SandboxedWidget.prototype.init = function() {
        this.messagerId = ++App.sandboxCounter;
        this.iframe = $("<iframe src= '" + window.location.origin + "/" + window.FLX_RELEASE_NUMBER + "/js/core/widgetSandbox/sandboxHost.html?id=" + this.messagerId + "' class='widgetSandboxIframe' sandbox='allow-scripts' scrolling='no'></iframe>");
        var that = this;
        this.iframe.addClass("flx-body");
        App.addSandboxMessageListener(this.messageListener);
        $("#" + this.manifest.WidgetName + "-widget .flx-body").replaceWith(this.iframe);
    };

    SandboxedWidget.prototype.isUrlBlocked = function(url) {
        var parser = document.createElement("a");
        parser.href = url;
        if (parser.origin == window.location.origin) {
            if (parser.pathname.toLowerCase().startsWith("/api/")) {
                if (parser.pathname.toLowerCase() == this.getSaveSettingsUrl().toLowerCase()) {
                    //whitelist this so the widget is free to save settings.
                    return false;
                }
                return true;
            }

        }
        return false;
    };

    SandboxedWidget.prototype.cloneTransportableObject = function(object) {
        if (typeof object == "object") {
            if (Object.prototype.toString.call(object) === '[object Array]'){
                var newObject = [];
                for (var i = 0; i < object.length; i++){
                    newObject[i] = this.cloneTransportableObject(object[i]);
                }
            }
            else{
                var newObject = {};
                for (var member in object) {
                    if (typeof object[member] == "function") {
                        continue;
                    }
                    newObject[member] = this.cloneTransportableObject(object[member]);
                }
            }
            return newObject;
        }
        else{
            return object;
        }
    };

    SandboxedWidget.prototype.messageListener = function(event) {
        var message = event.data;
        if (message.messagerId != this.messagerId)
            return;
        if (message.type == "getManifest") {
            this.sendMessage("manifest",{
                manifest: this.manifest,
                settings: this.settings
            },true);
            this.enableCommunicationLink();
            return;
        }
        if (message.widgetName != this.manifest.WidgetName)
            return;
        if (message.WidgetRepositoryURL != this.manifest.WidgetRepositoryURL)
            return;
        if (this.sendInfo != null){
            clearInterval(this.sendInfo);
            this.sendInfo = null;
        }
        switch (message.type){
            case "ready":
            {
                var digestStrippedDown = {
                    facets: {},
                    settings: this.digest.settings,
                    metadata: this.digest.metadata
                };
                if (this.manifest.RequiredConnectors != null) {
                    for (var i = 0, li = this.manifest.RequiredConnectors.length; i < li; i++){
                        var connectorName = this.manifest.RequiredConnectors[i];
                        for (var facetType in this.digest.facets) {
                            if (digestStrippedDown.facets[facetType] != null)
                                continue;
                            if (facetType.split("-")[0] == connectorName){
                                digestStrippedDown.facets[facetType] = [];
                                for (var j = 0, lj = this.digest.facets[facetType].length; j < lj; j++){
                                    digestStrippedDown.facets[facetType].push(this.cloneTransportableObject(this.digest.facets[facetType][j]));
                                }
                            }
                        }
                    }
                }
                this.sendMessage("digest",{
                    digest: digestStrippedDown,
                    dashboardId: this.dashboardId
                });
                break;
            }
            case "defaultSettings":
            {
                var callId = message.data.callId;
                var callback = this.callbacks["defaultSettings" + callId];
                delete this.callbacks["defaultSettings" + callId];
                var widgetSettingsObject = this.defaultSettingsObjects[callId];
                delete this.defaultSettingsObjects[callId];
                for (var member in widgetSettingsObject) {
                    delete widgetSettingsObject[member];
                }
                for (var member in message.data.widgetSettings) {
                    widgetSettingsObject[member] = message.data.widgetSettings[member];
                }
                callback();
                break;
            }
            case "bindWidgetSettings":
            {
                var callId = message.data.callId;
                var callback = this.callbacks["bindWidgetSettings" + callId];
                delete this.callbacks["bindWidgetSettings" + callId];
                //TODO: sanitize html of any scripts
                $("#widgetSettings").html(message.data.htmlOutput);
                callback();
                break;
            }
            case "validateSettings":
            {
                var callId = message.data.callId;
                var callback = this.callbacks["validateSettings" + callId];
                delete this.callbacks["validateSettings" + callId];
                callback();
                break;
            }
            case "App.closeModal":
            {
                App.closeModal();
                break;
            }
            case "ajax":
            {
                var that = this;
                var ajaxOptions = message.data;
                var requestId = ajaxOptions.id;
                if (this.isUrlBlocked(ajaxOptions.url)) {
                    this.sendMessage("ajaxResponse",{
                        id: requestId,
                        success: false
                    });
                    console.log("Blocked request to " + ajaxOptions.url);
                }
                if (ajaxOptions.dataType == "script")
                    ajaxOptions.dataType = "text";
                delete ajaxOptions.id;
                ajaxOptions.success = function(data){
                    that.sendMessage("ajaxResponse",{
                        id: requestId,
                        success: true,
                        data: data
                    });

                };
                ajaxOptions.error = function(){
                    that.sendMessage("ajaxResponse",{
                        id: requestId,
                        success: false
                    });

                };
                $.ajax(ajaxOptions);
                break;
            }
            default:
                console.log("Sandbox host received unknown message type: " + message.type);
        }
    };

    SandboxedWidget.prototype.enableCommunicationLink = function(){
        this.communicationLinkReady = true;
        while (this.messageBacklog.length > 0) {
            this.iframe[0].contentWindow.postMessage(this.messageBacklog.shift(),"*");
        }
    }

    SandboxedWidget.prototype.sendMessage = function(type,data,force) {
        if (typeof force == "undefined")
            force = false;
        var message = new Message(type,data);
        if (!this.communicationLinkReady && !force){
            this.messageBacklog.push(message);
        }
        else {
            this.iframe[0].contentWindow.postMessage(new Message(type,data),"*");
        }

    };

    SandboxedWidget.prototype.defaultSettings = function(widgetSettings,onDone) {
        this.callbacks["defaultSettings" + this.defaultSettingsCalls] = onDone;
        this.defaultSettingsObjects[this.defaultSettingsCalls] = widgetSettings;
        this.sendMessage("defaultSettings",{
            callId: this.defaultSettingsCalls++,
            widgetSettings: widgetSettings
        });
    };

    SandboxedWidget.prototype.bindWidgetSettings = function(widgetSettings,onDone) {
        var inputHtml = $("#widgetSettings").html();
        this.callbacks["bindWidgetSettings" + this.bindWidgetSettingsCalls] = onDone;
        this.sendMessage("bindWidgetSettings",{
            callId: this.bindWidgetSettingsCalls++,
            inputHtml: inputHtml,
            widgetSettings: widgetSettings
        });
    };

    SandboxedWidget.prototype.validateSettings = function(onDone) {
        var widgetSettings = $("#widgetSettings");
        widgetSettings.find("input").each(function(){
            this.setAttribute("value",this.value);
        });
        var inputHtml = widgetSettings.html();
        this.callbacks["validateSettings" + this.validateSettingsCalls] = onDone;
        this.sendMessage("validateSettings",{
            callId: this.validateSettingsCalls++,
            inputHtml: inputHtml
        })
    }

    function Message(type,data) {
        this.type = type;
        this.data = data;

    }


    return SandboxedWidget;
});