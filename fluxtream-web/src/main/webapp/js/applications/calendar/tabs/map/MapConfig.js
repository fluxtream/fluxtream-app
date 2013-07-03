define([], function() {
    return {
        getConfig : function(){
              return {
                  flatAccuracyCutoff:200,
                  stdAccuracyCutoff:1,
                  maxTimeBetweenGPSPoints: 3600000 //one hour
              };
        }
    };
});
