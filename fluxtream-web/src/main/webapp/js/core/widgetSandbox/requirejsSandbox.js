(function($){

    window.define = function(modules,newModule){
        if (typeof modules == "function"){
            newModule = modules;
            modules = [];
        }
        var args = [];

        var onDone = {
            onDefineDone: function(returnVal){
                console.warn("Define done missed!!");
            },
            isDefineObject: true
        };

        (function getNextScript(i){
            if (i >= modules.length){
                onAllScriptsReady();
                return;
            }
            var url = modules[i];
            if (url.substring(0,4) != "http"){
                var prefix = "/" + window.FLX_RELEASE_NUMBER + "/js";
                url = prefix + (modules[i].charAt(0) == '/' ? "" : "/") + url;
            }
            url = url + ".js";
            $.ajax(url,{
                dataType: "text",
                success: function(result){
                    var ret = eval(result);
                    if (typeof ret == "object" && ret.isDefineObject){
                        ret.onDefineDone = function(returnVal){
                            args.push(returnVal);
                            getNextScript(i+1);
                        }

                    }
                    else{
                        args.push(ret);
                        getNextScript(i+1);
                    }
                },
                error: function(){
                    console.log(args);
                }
            });
        })(0);

        function onAllScriptsReady(){
            var ret = newModule.apply(window,args);
            if (typeof ret == "object" && ret.isDefineObject){
                ret.onDefineDone = function(returnVal){
                    doDone(returnVal);
                }
            }
            else{
                doDone(ret);
            }
        }

        function doDone(returnVal){
            setTimeout(function(){
                onDone.onDefineDone(returnVal);
            },10);
        }
        return onDone;
    }

    window.require = function(modules,newModule) {
        window.define(modules,newModule).onDefineDone = function(){};
    }
})(jQuery);