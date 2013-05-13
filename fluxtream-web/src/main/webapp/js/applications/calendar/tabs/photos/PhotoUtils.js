define([],function(){

    var carouselTemplate;

    App.loadMustacheTemplate("applications/calendar/tabs/photos/photosTemplate.html","carousel",function(template){
        carouselTemplate = template;
    });

    var orientationStyles = ["none",//1
                             "scale(-1,1)",//2
                             "rotate(180deg)",//3
                             "rotate(180deg) scale(-1,1)",//4
                             "rotate(90deg) scale(1,-1)",//5
                             "rotate(90deg)",//6
                             "rotate(270deg) scale(1,-1)",//7
                             "rotate(270deg)"//8
    ];

    function getCarouselHTML(digest,connectorNames){
        if (connectorNames == null){//if the parameter is discluded, show all images
            connectorNames = [];
            for (connectorName in digest.cachedData){
                if (digest.cachedData[connectorName].hasImages)
                    connectorNames[connectorNames.length] = connectorName;
            }
        }
        var noPhotos = true;
        for (var i = 0; i < connectorNames.length && noPhotos; i++){
            noPhotos = digest.cachedData[connectorNames[i]] == null || !digest.cachedData[connectorNames[i]].hasImages;
        }
        if (noPhotos)
            return null;
        var data = [];
        for (var i = 0; i < connectorNames.length; i++){
            for (var j = 0; digest.cachedData[connectorNames[i]] != null && digest.cachedData[connectorNames[i]].hasImages && j < digest.cachedData[connectorNames[i]].length; j++){
                for (var k = 0; digest.cachedData[connectorNames[i]][j].hasImage && k <= data.length; k++){
                    if (k == data.length || data[k].start > digest.cachedData[connectorNames[i]][j].start || digest.cachedData[connectorNames[i]][j].start == null){
                        data.splice(k,0,digest.cachedData[connectorNames[i]][j]);
                        if (data[k].orientation != null){
                            data[k].transformations = orientationStyles[data[k].orientation-1];
                        }
                        if (data[k].transformations == null || data[k].transformations == "")
                            data[k].transformations = "none";

                        break;
                    }
                }
            }
        }
        for (var i = 0; i < data.length; i++){
            data[i].active = i == 0;
            data[i].id = i;
        }
        return carouselTemplate.render({photos:data,includeNav:data.length > 1});
    }


    var PhotoUtils = {};
    PhotoUtils.getCarouselHTML = getCarouselHTML;
    return PhotoUtils;
});