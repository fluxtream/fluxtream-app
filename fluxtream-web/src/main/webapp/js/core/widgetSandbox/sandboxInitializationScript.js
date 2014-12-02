(function(){

    function loadScript(src) {
        var script = document.createElement("script");
        script.src = src;
        script.type = "text/javascript";
        window.document.head.appendChild(script);
        return script;
    }


    //load jquery
    loadScript("/static/js/jquery-1.10.2.min.js");

    function jQueryAvailable(){
        $("body").append("<div id='" + window.manifest.WidgetName + "-widget'><div class='flx-body'></div></div>");
        $.getScript("/" + window.FLX_RELEASE_NUMBER + "/js/core/widgetSandbox/requirejsSandbox.js",function(){
            $.getScript("/static/js/underscore-1.3.3-min.js",function(){
                $.getScript("/static/js/hogan-2.0.0.js",function(){
                    setupWindowMessagingListener();
                });
            });
        });


    }

    var jQueryCheck = setInterval(function(){
        if (window.$ != undefined) {
            clearInterval(jQueryCheck);
            jQueryAvailable();
        }
    },50);

    function setupWindowMessagingListener(){
        window.addEventListener("message", function(event) {
            var message = event.data;
            if (message.type == "digest"){
                require([window.manifest.WidgetRepositoryURL + "/" + window.manifest.WidgetName + "/" + window.manifest.WidgetName],function(Widget){
                    Widget.load({manifest:window.manifest}, message.data, null);
                });
            }
        }, false);

        sendMessage("ready");
    }

    function sendMessage(data) {
        window.parent.postMessage(new Message(data),window.targetOrigin);
    }

    var Message = function(data){
        this.data = data;
        this.messagerId = window.messagerId;
        this.widgetName = window.manifest.WidgetName;
        this.WidgetRepositoryURL = window.manifest.WidgetRepositoryURL;
    }
    Message.prototype.messagerId = window.messagerId;


})();