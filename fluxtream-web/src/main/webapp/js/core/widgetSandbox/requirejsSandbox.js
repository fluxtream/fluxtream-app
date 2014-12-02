(function($){
    window.define = function(modules,newModule){
        if (typeof modules == "function"){
            newModule = modules;
            modules = [];
        }
        var args = [];
        for (var i = 0, li = modules.length; i < li; i++) {
            if (modules[i].substring(0,4) != "http"){
                var prefix = "/" + window.FLX_RELEASE_NUMBER + "/js";
                modules[i] = prefix + (modules[i].charAt(0) == '/' ? "" : "/") + modules[i];
            }
            modules[i] = modules[i] + ".js";
            $.ajax(modules[i],{
                dataType: "text",
                async: false,
                success: function(result){
                    args.push(eval(result));
                },
                errror: function(){
                    console.log(args);
                }
            })
        }
        return newModule.apply(window,args);
    }

    window.require = function(modules,newModule) {
        window.define(modules,newModule);
    }
})(jQuery);