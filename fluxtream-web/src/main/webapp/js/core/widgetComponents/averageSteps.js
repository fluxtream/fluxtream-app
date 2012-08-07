define([],function(){

    var AverageStepsComponent = function(parentWidget,parentElement,dataArray,stepsParamName){
        this.parent = parentWidget;
        var that = this;
        App.loadMustacheTemplate("core/widgetComponents/widgetComponentTemplates.html","averageSteps",function(template){
            var steps = 0;
            var averageSteps = 0;
            var measures = 0;
            if (dataArray != null) {
                for (var i=0; i<dataArray.length; i++) {
                    if (dataArray[i][stepsParamName]>0) {
                        measures++;
                        averageSteps+=dataArray[i][stepsParamName];
                    }
                }
                if (averageSteps>0) {
                    averageSteps/=measures;
                    steps = parentWidget.addCommas(Math.round((averageSteps)*1)/1);
                }
            }
            var color = "rgb(0,0,0)";
            if (averageSteps>0) {
                if (typeof parentWidget.settings.dailyThreshold != "undefined") {
                    if (averageSteps>=parentWidget.settings.dailyThreshold)
                        color = "rgb(0,255,0)";
                    else
                        color = "rgb(255,0,0)";
                }
            }
            var params = {"manifest" : parentWidget.manifest,
                "settings" : parentWidget.settings,
                "color" : color,
                "steps" : averageSteps>0?steps:null,
                "noMeasure" : averageSteps===0,
                "noMeasureMsg" : that.noSteps()};
            $(parentElement).append($(template.render(params)));
        });
    }

    AverageStepsComponent.prototype.noSteps = function() {
        return "No Steps Recorded " + this.parent.getPrettyTimeUnit();
    };

    return AverageStepsComponent;

});