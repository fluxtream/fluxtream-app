define([],function(){

    var AverageCaloriesBurnedComponent = function(parentWidget,parentElement,dataArray,weightParamName){
        this.parent = parentWidget;
        App.loadMustacheTemplate("core/widgetComponents/widgetComponentTemplates.html","averageCaloriesBurned",function(template){
            var averageCaloriesBurned = 0;
            var measures = 0;
            for (var i=0; dataArray != null && i<dataArray.length; i++) {
                if (dataArray[i][weightParamName]>0) {
                    measures++;
                    averageCaloriesBurned+=dataArray[i][weightParamName];
                }
            }
            if (measures > 0)
                averageCaloriesBurned = Math.round((averageCaloriesBurned/measures)*100)/100;
            $(parentElement).append(
                template.render({"manifest" : parentWidget.manifest, "calories" : averageCaloriesBurned>0?averageCaloriesBurned:null, "noMeasure" : averageCaloriesBurned===0})
            );
        });
    }

    return AverageCaloriesBurnedComponent;

});