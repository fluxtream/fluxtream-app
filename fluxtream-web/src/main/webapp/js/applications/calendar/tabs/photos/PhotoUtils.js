define([],function(){

    var carouselTemplate;

    App.loadMustacheTemplate("applications/calendar/tabs/photos/photosTemplate.html","carousel",function(template){
        carouselTemplate = template;
    });

    function getCarouselHTML(digest){
        if (digest.cachedData["picasa-photo"] == null)
            return null;
        var data = digest.cachedData["picasa-photo"];
        var data = [];
        for (var i = 0; i < digest.cachedData["picasa-photo"].length; i++){
            data[i] = digest.cachedData["picasa-photo"][i];
            data[i].active = i == 0;
            data[i].id = i;
        }
        return carouselTemplate.render({photos:data,includeNav:data.length > 1});
    }


    var PhotoUtils = {};
    PhotoUtils.getCarouselHTML = getCarouselHTML;
    return PhotoUtils;
});