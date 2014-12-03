(function(){
    var App = window.App = {};

    var compiledTemplates = {};
    App.loadMustacheTemplate = function(templatePath,templateId,onLoad){
        App.loadAllMustacheTemplates(templatePath,function(templates){
            onLoad(templates[templateId]);
        });
    };

    var templateRegexp = new RegExp("<template id=\"[A-z0-9\\-.]*\">","ig");

    App.loadAllMustacheTemplates = function(templatePath,onLoad){
        if (compiledTemplates[templatePath] != null){
            onLoad(compiledTemplates[templatePath]);
            return;
        }
        require(["text!" + templatePath], function(template){
            var templateData = {};
            var matches = template.match(templateRegexp);
            for (var i = 0; i < matches.length; i++){
                var curMatch = matches[i];
                var templateName = curMatch.substring(14,curMatch.length - 2);
                var start = template.indexOf(curMatch) + curMatch.length;
                var end = template.indexOf("</template>",start);
                var html = template.substring(start,end);
                templateData[templateName] = Hogan.compile(html);
            }
            compiledTemplates[templatePath] = templateData;
            onLoad(compiledTemplates[templatePath]);
        });
    };
})();