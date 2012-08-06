define([],function(){

    var AverageWeightComponent = function(parentWidget,parentElement,dataArray,systolicParam,diastolicParam){
        this.parent = parentWidget;
        App.loadMustacheTemplate("core/widgetComponents/widgetComponentTemplates.html","averageBP",function(template){
            var averageSys = 0;
            var averageDia = 0;
            var measures = 0;
            for (var i=0; dataArray != null && i<dataArray.length; i++) {
                if (dataArray[i][systolicParam]>0) {
                    measures++;
                    averageSys+=dataArray[i][systolicParam];
                    averageDia+=dataArray[i][diastolicParam];
                }
            }
            if (measures > 0){
                averageSys = Math.round((averageSys/measures)*100)/100;
                averageDia = Math.round((averageDia/measures)*100)/100;
            }
            $(parentElement).append(
                template.render({"manifest" : parentWidget.manifest, "systolic" : averageSys>0?averageSys:null, "diastolic": averageDia>0?averageDia:null, "noMeasure" : averageSys===0})
            );
        });
    }

    return AverageWeightComponent;

});