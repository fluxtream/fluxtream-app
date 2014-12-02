(function(){

    function loadScript(src,onload) {
        var script = document.createElement("script");
        script.src = src;
        script.type = "text/javascript";
        if (typeof onload == "function"){
            script.onload = onload;
        }
        window.document.head.appendChild(script);
        return script;
    }

    console.log("sandbox initializing");

    //load jquery
    loadScript("/static/js/jquery-1.10.2.min.js");

    function jQueryAvailable(){
        $("body").append("<b>IM ALIVE</b>");

    }

    var jQueryCheck = setInterval(function(){
        if ($ != undefined) {
            clearInterval(jQueryCheck);
            jQueryAvailable();
        }
    },10);


})();