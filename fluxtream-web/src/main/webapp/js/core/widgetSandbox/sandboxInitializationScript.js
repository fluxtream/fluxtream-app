(function(){

    var widgetReady = false;
    var Widget = null;
    var runOnWidgetReady = [];

    var Message = function(type,data){
        this.type = type;
        this.data = data;
        this.messagerId = window.messagerId;
        this.widgetName = window.manifest == null ? null : window.manifest.WidgetName;
        this.WidgetRepositoryURL =  window.manifest == null ? null : window.manifest.WidgetRepositoryURL;
    }

    function init(){
        $(".flx-widget")[0].setAttribute("id",window.manifest.WidgetName + "-widget");
        sendMessage("ready");
    }

    setupWindowMessagingListener();
    sendMessage("getManifest");


    function sendMessage(type,data) {
        window.parent.postMessage(new Message(type,data),window.targetOrigin);
    }

    var counter = 0;

    var callbacks = {};

    function ajaxRequest(url,options) {
        var request = ++counter;
        if (options.success == null) {
            options.success = function(){};
        }
        if (options.error == null){
            options.error = function(){};
        }
        callbacks[request] = {
            success: options.success,
            error: options.error
        };
        sendMessage("ajax",{
            url: url,
            id: request,
            dataType: options.dataType,
            type: options.type,
            data: options.data
        });
    }

    $.ajax = function(url,options) {
        if (typeof url != "string"){
            options = url;
        }
        else{
            if (options == null)
                options = {};
            options.url = url;
        }
        ajaxRequest(options.url,{
            success:options.success,
            error: options.error,
            dataType: options.dataType,
            type: options.type,
            data: options.data
        });
        if (options.async == false){
            console.warn("cannot do synchronous ajax in a widget.");
        }
    };

    //a very rough and unfinished wrapper for XMLHttpRequests to go through ajax proxy. Works well enough for requirejs text dependencies
    var oldXMLHttpRequest = window.XMLHttpRequest;
    window.XMLHttpRequest = function(){
    };
    window.XMLHttpRequest.prototype.open = function(method,url,async){
        this.url = rewriteUrl(url);
        this.async = async;
        this.method = method;
    };
    window.XMLHttpRequest.prototype.send = function(data) {
        var that = this;
        $.ajax(this.url,{
            method: this.method,
            async: this.async,
            data: data,
            success: function(data){
                that.responseText = data;
                that.readyState = 4;
                that.onreadystatechange();
            }
        });
    };
    window.XMLHttpRequest.prototype.onreadystatechange = function(){};

    function assureWidgetReady(doFunction) {
        if (widgetReady) {
            doFunction();
        }
        else{
            runOnWidgetReady.push(doFunction);
        }
    }

    function setWidgetReady() {
        widgetReady = true;
        while (runOnWidgetReady.length > 0){
            runOnWidgetReady.shift()();
        }
    }


    Message.prototype.messagerId = window.messagerId;

    function setupWindowMessagingListener(){
        window.addEventListener("message", function(event) {
            var message = event.data;
            switch (message.type) {
                case "digest":
                {
                    var widgetRepository = window.manifest.WidgetRepositoryURL;
                    if (widgetRepository.charAt(0) == "/") {
                        widgetRepository = window.location.origin + widgetRepository;
                    }
                    require([widgetRepository + "/" + window.manifest.WidgetName + "/" + window.manifest.WidgetName + ".js"],function(WidgetObject){
                        Widget = WidgetObject;
                        Widget.load({manifest:window.manifest,settings:window.settings}, message.data.digest, message.data.dashboardId);
                        setWidgetReady();
                    });
                    break;
                }
                case "manifest":
                {
                    if (window.manifest == null) {
                        window.manifest = message.data.manifest;
                        window.settings = message.data.settings;
                        init();
                    }
                    break;
                }
                case "ajaxResponse":
                {
                    var cbs = callbacks[message.data.id];
                    delete callbacks[message.data.id];
                    if (cbs != null) {
                        if (message.data.success) {
                            cbs.success(message.data.data);
                        }
                        else {
                            cbs.error(message.data.error);
                        }
                    }
                    break;
                }
                case "defaultSettings":
                        assureWidgetReady(function() {
                            var widgetSettings = message.data.widgetSettings;
                            Widget.defaultSettings(widgetSettings,function(){
                                sendMessage("defaultSettings",{
                                    callId: message.data.callId,
                                    widgetSettings: widgetSettings
                                });
                            });
                        });
                    break;
                case "bindWidgetSettings":
                    assureWidgetReady(function() {
                        var input = $("<div id='widgetSettings' style='display:none !important;'>" + message.data.inputHtml + "</div>");
                        $("body").append(input);
                        Widget.bindWidgetSettings(message.data.widgetSettings, function(){
                            input.remove();
                            input.find("input").each(function(){
                                this.setAttribute("value",this.value);
                                if (this.checked){
                                    this.setAttribute("checked","checked");
                                }
                                else{
                                    this.removeAttribute("checked");
                                }
                            });
                            sendMessage("bindWidgetSettings",{
                                callId: message.data.callId,
                                htmlOutput: input.html()
                            });
                        });
                    });
                    break;
                case "validateSettings":
                    assureWidgetReady(function() {
                        var input = $("<div id='widgetSettings' style='display:none !important;'>" + message.data.inputHtml + "</div>");
                        $("body").append(input);
                        Widget.validateSettings(function(){
                            input.remove();
                            sendMessage("validateSettings",{
                                callId: message.data.callId
                            });
                        })
                    });
                    break;
                default:
                    console.log("Sandboxed widget received unknown message type: " + message.type);
            }
        }, false);
    }

    App.closeModal = function () {
        sendMessage("App.closeModal");
    }


})();