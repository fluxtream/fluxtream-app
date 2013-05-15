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
                        if (data[k].orientation == null)
                            data[k].orientation = 1;
                        data[k].transformations = orientationStyles[data[k].orientation-1];
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

    function isSideways(orientation){
        return orientation >= 5 && orientation <= 8;
    }

    function getNegationFactor(orientation){
        return orientation >= 7 && orientation <= 8 ? -1 : 1;
    }

    function doResize(){
        var container = $(".photoModalBody");
        var images = container.find(".carousel-inner img");
        var photoHolder = container.find(".photoHolder");
        for (var i = 0, li = images.length; i < li; i++){
            var image = $(images[i]);
            var fullSizeImage = photoHolder.find("." + image.attr("fullsize"));
            var doResize = function(image,fullSizeImage){
                if (fullSizeImage.width() == 0 || fullSizeImage.height() == 0){
                    setTimeout(function(){
                        doResize(image,fullSizeImage);
                    },100);
                    return;
                }
                var container = $(".photoModalBody");
                var cWidth = container.width();
                var cHeight = container.height();
                var ratio = fullSizeImage.width() / fullSizeImage.height();
                var cRatio = cWidth / cHeight;
                var orientation = image.attr("orientation");
                image.css("max-height","none");
                image.css("max-width","none");
                var extraTransformations = "";
                if (isSideways(orientation)){
                    if (ratio < cRatio){
                        image.width(cHeight);
                        image.height(cHeight / ratio);
                        extraTransformations = "translateX(" + getNegationFactor(orientation) * ((cHeight - cHeight / ratio) / 2) + "px)";
                    }
                    else{
                        image.width(cWidth * ratio);
                        image.height(cWidth);
                        extraTransformations = "translateX(" + getNegationFactor(orientation) * ((cWidth * ratio - cWidth) / 2) + "px)";
                        extraTransformations += "translateY(" + getNegationFactor(orientation) * ((cWidth * ratio - cWidth) / 2) + "px)";

                    }

                }
                else{
                    if (ratio < cRatio){
                        image.height(cHeight);
                        image.width(ratio * cHeight);
                    }
                    else{
                        image.width(cWidth);
                        image.height(cWidth / ratio);
                    }
                }

                var fullTransformation = image.attr("transformations") + " " + extraTransformations;
                image.css("transform",fullTransformation);
                image.css("-ms-transform",fullTransformation);
                image.css("-webkit-transform",fullTransformation);
                image.css("-o-transform",fullTransformation);

            }
            doResize(image,fullSizeImage);
        }
    }

    $(window).resize(function(){
        $.doTimeout("photoUtilsResizeTimeout");
        $.doTimeout("photoUtilsResizeTImeout",100,doResize());
    });


    var PhotoUtils = {};
    PhotoUtils.getCarouselHTML = getCarouselHTML;
    return PhotoUtils;
});