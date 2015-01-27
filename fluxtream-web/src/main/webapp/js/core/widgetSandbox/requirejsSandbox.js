//A custom implementation of requirejs that allows us to operate properly sandboxed
(function($){

    window.rewriteUrl = function(url){
        if (url.indexOf("./") == 0){
            url = url.substring(2);
            var prefix = "/" + window.FLX_RELEASE_NUMBER + "/js";
            if (url.indexOf("widgets") == 0 || (url.indexOf("widgets") == 1 && url.charAt(0) == '/')){
                prefix = "";
            }
            url = prefix + (url.charAt(0) == '/' ? "" : "/") + url;
        }
        return url;


    }

    //modified version of requirejs.attach that will rewrite the url and send the query through the sandbox proxy
    /**
     * Attaches the script represented by the URL to the current
     * environment. Right now only supports browser loading,
     * but can be redefined in other environments to do the right thing.
     * @param {String} url the url of the script to attach.
     * @param {Object} context the context that wants the script.
     * @param {moduleName} the name of the module that is associated with the script.
     * @param {Function} [callback] optional callback, defaults to require.onScriptLoad
     * @param {String} [type] optional type, defaults to text/javascript
     * @param {Function} [fetchOnlyFunction] optional function to indicate the script node
     * should be set up to fetch the script but do not attach it to the DOM
     * so that it can later be attached to execute it. This is a way for the
     * order plugin to support ordered loading in IE. Once the script is fetched,
     * but not executed, the fetchOnlyFunction will be called.
     */
    var oldAttach = window.requirejs.attach;
    window.requirejs.attach = function (url, context, moduleName, callback, type, fetchOnlyFunction) {
        //rewrite urls to use proper prefixes if they are local
        url = rewriteUrl(url);
        oldAttach.apply(this,[url,context,moduleName,callback,type,fetchOnlyFunction]);
    };
    window.requireJsModsDone();

})(jQuery);