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
        $("body").append("<div class='flx-widget' id='" + window.manifest.WidgetName + "-widget'><div class='flx-body'></div></div>");
        $("head").append('<link rel="stylesheet" type="text/css" href="/' + window.FLX_RELEASE_NUMBER + '/css/sandbox.css">');
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
            dataType: options.dataType
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
            dataType: options.dataType
        });
        if (options.async == false){
            console.warn("cannot do synchronous ajax in a widget.");
        }
    }

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
                    require([widgetRepository + "/" + window.manifest.WidgetName + "/" + window.manifest.WidgetName],function(WidgetObject){
                        Widget = WidgetObject;
                        Widget.load({manifest:window.manifest,settings:window.settings}, message.data, null);
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
                default:
                    console.log("Sandboxed widget received unknown message type: " + message.type);
            }
        }, false);
    }


})();