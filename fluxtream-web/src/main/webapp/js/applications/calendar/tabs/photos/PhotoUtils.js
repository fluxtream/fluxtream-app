define([],function(){

    var carouselTemplate;

    App.loadMustacheTemplate("applications/calendar/tabs/photos/photosTemplate.html","carousel",function(template){
        carouselTemplate = template;
    });

    var handleResize = function(){

    };

    var handleKeyEvent = handleResize;


    var orientationStyles = ["none",//1
                             "scale(-1,1)",//2
                             "rotate(180deg)",//3
                             "rotate(180deg) scale(-1,1)",//4
                             "rotate(90deg) scale(1,-1)",//5
                             "rotate(90deg)",//6
                             "rotate(270deg) scale(1,-1)",//7
                             "rotate(270deg)"//8
    ];

    function getCarouselHTML(digest,connectorNames){  //TODO: make this work without a digest
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
        }
        return carouselTemplate.render({photos:data,includeNav:data.length > 1});
    }

    function isSideways(orientation){
        return orientation >= 5 && orientation <= 8;
    }

    function getNegationFactor(orientation){
        return orientation >= 7 && orientation <= 8 ? -1 : 1;
    }

    function getNegationFactorHorizontal(orientation){
        return  (orientation == 5 || orientation == 8) ? -1 : 1;
    }

    function doResize(imageId){
        var container = $("#photoViewContainer");
        var photoHolder = container.find(".photoHolder");
        var image = container.find("#photo-" + imageId).find("img");
        var fullSizeImage = photoHolder.find("." + image.attr("fullsize"));
        var doResize = function(image,fullSizeImage){
            if (fullSizeImage.width() == 0 || fullSizeImage.height() == 0){
                setTimeout(function(){
                    doResize(image,fullSizeImage);
                },100);
                return;
            }
            var cWidth = container.width();
            var cHeight = container.height();
            var ratio = fullSizeImage.width() / fullSizeImage.height();
            var cRatio = cWidth / cHeight;
            var orientation = image.attr("orientation");
            image.css("max-height","none");
            image.css("max-width","none");
            var extraTransformations = "";
            var height, width;
            var marginTop;
            var forceFullSize = container.find(".carousel-inner").hasClass("fullSizeImages");
            if (isSideways(orientation)){
                if (ratio < cRatio){
                    if (forceFullSize || Height < fullSizeImage.width()){
                        width = cHeight;
                        height = cHeight / ratio;
                    }
                    else{
                        width = fullSizeImage.width();
                        height = fullSizeImage.height();
                    }
                    extraTransformations = "translateX(" + getNegationFactor(orientation) * ((width - height) / 2) + "px)";
                }
                else{
                    var height, width;
                    if (forceFullSize || cWidth < fullSizeImage.height()){
                        width = cWidth * ratio;
                        height = cWidth;
                    }
                    else{
                        width = fullSizeImage.width();
                        height = fullSizeImage.height();
                    }
                    extraTransformations = "translateX(" + getNegationFactor(orientation) * ((width - height) / 2) + "px)";
                    extraTransformations += "translateY(" + getNegationFactorHorizontal(orientation) * ((width - height) / 2) + "px)";

                }
                marginTop = (cHeight - width) / 2;
            }
            else{
                if (ratio < cRatio){
                    var height, width;
                    if (forceFullSize || cHeight < fullSizeImage.height()){
                        height = cHeight;
                        width = ratio * cHeight;
                    }
                    else{
                        width = fullSizeImage.width();
                        height = fullSizeImage.height();
                    }
                }
                else{
                    if (forceFullSize || cWidth < fullSizeImage.width()){
                        height = cWidth / ratio;
                        width = cWidth;
                    }
                    else{
                        width = fullSizeImage.width();
                        height = fullSizeImage.height();
                    }
                }
                marginTop = (cHeight - height) / 2;
            }
            image.width(width);
            image.height(height);
            var fullTransformation = image.attr("transformations") + " " + extraTransformations;
            image.css("transform",fullTransformation);
            image.css("-ms-transform",fullTransformation);
            image.css("-webkit-transform",fullTransformation);
            image.css("-o-transform",fullTransformation);
            image.css("margin-top",marginTop + "px");

        }
        doResize(image,fullSizeImage);
    }

    function showCarouselHTML(html,defaultImage){
        var widget = $(html);
        var items = widget.find(".carousel-inner .item");
        var photoIdToIndex = {};
        var indexToPhotoId = [];
        for (var i = 0, li = items.length; i < li; i++){
            var photoId = $(items[i]).attr("photoId");
            photoIdToIndex[photoId] = i;
            indexToPhotoId[i] = photoId;
        }
        $("body").append(widget);
        var oldTop = $('body').scrollTop();
        $("body").addClass("photoCarouselViewing");

        if (defaultImage == null)
            defaultImage = 0;
        else
            defaultImage = photoIdToIndex[defaultImage];

        function showImage(){
            doResize(indexToPhotoId[defaultImage]);
            widget.find(".carousel-inner div.item.active").removeClass("active");
            widget.find(".carousel-inner #photo-"+indexToPhotoId[defaultImage]).addClass("active");
        }
        showImage();

        widget.find(".carousel-control.left").click(function(){
            defaultImage--;
            if (defaultImage < 0)
                defaultImage += indexToPhotoId.length;
            showImage();
            return false;
        });

        widget.find(".carousel-control.right").click(function(){
            defaultImage++;
            if (defaultImage >= indexToPhotoId.length)
                defaultImage -= indexToPhotoId.length;
            showImage();
            return false;
        });

        widget.find("#closePhotoViewer").click(function(){
            widget.remove();
            handleKeyEvent = handleResize = function(){};
            $("body").removeClass("photoCarouselViewing");
            $("body").scrollTop(oldTop);
            return false;
        });

        handleResize = function(){
            doResize(indexToPhotoId[defaultImage]);
        }

        handleKeyEvent = function(event){
            switch (event.keyCode){
                case 32://space
                case 39://right
                    widget.find(".carousel-control.right").click();
                    break;
                case 37://left
                    widget.find(".carousel-control.left").click();
                    break;
                case 27://escape
                    widget.find("#closePhotoViewer").click();
                    break;
            }
            console.log(event.keyCode);
        }

        widget.find(".carousel-inner img").click(function(){
            if (widget.find(".carousel-inner").hasClass("fullSizeImages"))
                widget.find(".carousel-inner").removeClass("fullSizeImages");
            else
                widget.find(".carousel-inner").addClass("fullSizeImages");
            handleResize();
        });
    }

    $(window).resize(function(){
        handleResize();
    });

    $(window).keydown(function(event){
        handleKeyEvent(event);
    });


    var PhotoUtils = {};
    PhotoUtils.getCarouselHTML = getCarouselHTML;
    PhotoUtils.showCarouselHTML = showCarouselHTML;
    return PhotoUtils;
});