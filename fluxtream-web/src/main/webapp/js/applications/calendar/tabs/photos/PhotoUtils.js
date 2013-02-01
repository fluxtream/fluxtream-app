define([],function(){

    var carouselTemplate;

    App.loadMustacheTemplate("applications/calendar/tabs/photos/photosTemplate.html","carousel",function(template){
        carouselTemplate = template;
    });

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
                    if (k == data.length){
                        data[k] = digest.cachedData[connectorNames[i]][j];
                        break;
                    }
                    if (data[k].start > digest.cachedData[connectorNames[i]][j].start || digest.cachedData[connectorNames[i]][j].start == null){
                        data.splice(k,0,digest.cachedData[connectorNames[i]][j]);
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