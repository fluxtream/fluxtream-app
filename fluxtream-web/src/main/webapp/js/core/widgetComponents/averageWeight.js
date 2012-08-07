define([],function(){

    var AverageWeightComponent = function(parentWidget,parentElement,dataArray,weightParamName){
        this.parent = parentWidget;
        App.loadMustacheTemplate("core/widgetComponents/widgetComponentTemplates.html","averageWeight",function(template){
            var weight = 0;
            var averageWeight = 0;
            var measures = 0;
            for (var i=0; dataArray != null && i<dataArray.length; i++) {
                if (dataArray[i][weightParamName]>0) {
                    measures++;
                    averageWeight+=dataArray[i][weightParamName];
                }
            }
            if (averageWeight != 0)
                weight = Math.round((averageWeight/measures)*100)/100;
            var unit = parentWidget.digest.settings.weightMeasureUnit;
            if (unit==="SI")
                unit = "kg";
            else unit = unit.toLowerCase();
            $(parentElement).append(
                template.render({"manifest" : parentWidget.manifest, "weight" : weight>0?weight:null, "unit" : unit, "noMeasure" : weight===0})
            );
        });
    }

    return AverageWeightComponent;

});