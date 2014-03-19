define(["core/Tab",
        "applications/calendar/App", "applications/calendar/tabs/photos/PhotoUtils"], function(Tab, Calendar, PhotoUtils) {

    var maxWidth = 200;
    var maxHeight = 200;
    var ratio = maxWidth / maxHeight;

    var lastTimestamp = null;


    function render(params) {
        params.setTabParam(null);
        this.getTemplate("text!applications/calendar/tabs/photos/photos.html", "photos", function() {
            //$(window).resize(); //masonry reorganizes pictures when the window is resized and some tabs can braek the layout, this fixes it
            if (lastTimestamp == params.digest.generationTimestamp && !params.forceReload){
                params.doneLoading();
                return;
            }
            else
                lastTimestamp = params.digest.generationTimestamp;
            digest = params.digest;
            connectorEnabled = params.connectorEnabled;
            setup(digest,connectorEnabled,params.doneLoading);

        });
    }

    /*function render(digest, timeUnit) {
        this.getUrl("/tabs/photos", "photos", null, true);
    }*/

    var connectorEnabled;
    var digest;

    var doneLoading;
    var thumbnailGroupTemplate;

    function setup(digest, cEn, dl){
        App.loadMustacheTemplate("applications/calendar/tabs/photos/photosTemplate.html","thumbnailGroup", function(tTemplate){
            thumbnailGroupTemplate = tTemplate;
            doneLoading = dl;
            $("#photoTab").empty();
            var noPhotos = true;
            var photos = [];
            for (var connectorName in digest.facets){
                if (digest.facets[connectorName].hasImages){
                    noPhotos = false;
                    photos = _.union(photos, digest.facets[connectorName]);
                }
            }
            if (noPhotos){
                showNoPhotos();
            } else{
                photos.sort(function(a,b) {return a.start- b.start;});
                onDataReceived(photos);
            }
            if (doneLoading != null)
                doneLoading();
        });
    }

    function showNoPhotos(){
        $("#photoTab").empty();
        $("#photoTab").append("<div class=\"emptyList\">(no photos)</div>");
    }

    function onDataReceived(photos){
        var data = [];
        $("#photoTab").empty();
        for (var i = 0; i < photos.length; i++){
            if (!photos[i].hasImage)
                continue;
            for (var j = 0; j < digest.selectedConnectors.length; j++){
                var found = false;
                for (var k = 0; !found &&  k < digest.selectedConnectors[j].facetTypes.length; k++){
                   found = digest.selectedConnectors[j].facetTypes[k] == photos[i].type;
                }
                if (found){
                   if (connectorEnabled[digest.selectedConnectors[j].connectorName]){
                       data[data.length] = photos[i];
                       break;//to avoid duplicates
                   }
                }
            }
        }
        for (var i = 0; i < data.length; i++){
            data[i].active = i == 0;
            data[i].id = i;
        }
        $("#photoTab").empty();
        if (data.length == 0){
            showNoPhotos();
            return;
        }
        var currentGroup = [];
        var currentDate = null;
        var facetCity = null;
        var currentCity = null;
        for (var i = 0; i < data.length; i++){
           facetCity = App.getFacetCity(data[i], digest.getConsensusCitiesList());
           if (facetCity==null)
               continue;
           var date = App.formatDate(data[i].start + facetCity.tzOffset,false,true);
           if (currentDate == null){
               currentDate = facetCity.dateWithTimezone;
               currentCity = facetCity;
           }
           else if (currentDate != facetCity.dateWithTimezone) {
               $("#photoTab").append(thumbnailGroupTemplate.render({date:App.prettyDateFormat(currentDate),city:currentCity.name,timezone:currentCity.shortTimezone,state:"photos/date/"+currentDate.split(" ")[0],photos:currentGroup}));
               currentGroup = [];
               currentDate = facetCity.dateWithTimezone;
               currentCity = facetCity;
           }
            var photoUrl = data[i].photoUrl;
            if (typeof(data[i].thumbnailUrl)!="undefined")
                photoUrl = data[i].thumbnailUrl;
            if (data[i].thumbnailSizes != null){
                var closest = null;
                var closestValue = 100000;
                for (var j in data[i].thumbnailSizes){
                    var difference = Math.pow(data[i].thumbnailSizes[j].height - 200,2) + Math.pow(data[i].thumbnailSizes[j].width - 200,2);
                    if (difference < closestValue){
                        closest = j;
                        closestValue = difference;
                    }
                }
                if (closest != null){
                    photoUrl = data[i].thumbnailUrls[closest];
                }
            }
            var time = "", ampm = "";
            if (typeof(data[i].startMinute)!="undefined") {
                time = App.formatMinuteOfDay(data[i].startMinute)[0];
                ampm = App.formatMinuteOfDay(data[i].startMinute)[1];
            }
            currentGroup[currentGroup.length] = {id:data[i].id,photoUrl:photoUrl,time:time,ampm:ampm};
        }
        if (currentGroup.length != 0){
            $("#photoTab").append(thumbnailGroupTemplate.render({
                date: App.prettyDateFormat(currentDate),
                city: currentCity.name,
                timezone: currentCity.shortTimezone,
                state: "photos/date/"+currentDate.split(" ")[0],
                photos: currentGroup
            }));
        }
        for (var i = 0; i < data.length; i++){
            $("#photo-" + data[i].id).click({i:data[i].id,facet:data[i]},function(event){
                var photoFacet = event.data.facet;
                PhotoUtils.showPhotoDialog(photoFacet.deviceName, photoFacet.channelName, photoFacet.UID, photoFacet.start,{minTime:digest.tbounds.start,maxTime:digest.tbounds.end});
            });
        }
        //var groups = $(".thumbnailGroup");
        //for (var i = 0; i < groups.length; i++){
        //    $(groups[i]).imagesLoaded(function(event){
        //        $(this).masonry({
        //             itemSelector: '.photoThumbnail',
        //             columnWidth:1,
        //             hiddenStyle: null,
        //             isResizeBound: false
        //         });
        //    });
        //}

    }

    function connectorToggled(connectorName,objectTypeNames,enabled){
        connectorEnabled[connectorName] = enabled;
        setup(digest,connectorEnabled);
    }

    function connectorDisplayable(connector){
        for (var i = 0; i < connector.facetTypes.length; i++){
            var config = App.getFacetConfig(connector.facetTypes[i]);
            if (config.photos)
                return true;
        }
        return false;
    }

    function onScroll(scrollPosition){
        var listTops = $("#photoTab .dateHeadingGroup");
        for (var i = 0, li = listTops.length; i < li; i++){
            var listTop = $(listTops[i]);
            var hr = listTop.find(".priorRuler");
            var floater = listTop.find(".dateLabel");
            var placeholder = listTop.find(".placeholder");
            var beginFloat = hr.offset().top + hr.outerHeight(false) + parseInt(hr.css("marginBottom"));
            if (beginFloat < 0){
                beginFloat = 0;
            }
            var endFloat = null;
            if (i < li - 1){
                var nextListTop = $(listTops[i+1]);
                var nextHr = nextListTop.find(".priorRuler");
                endFloat = nextHr.offset().top + nextHr.outerHeight(false) + parseInt(nextHr.css("marginBottom"));
            }
            if (scrollPosition < beginFloat){
                placeholder.addClass("hidden");
                floater.removeClass("floating");
                floater.css("marginTop","0px");
            }
            else{
                placeholder.removeClass("hidden");
                floater.addClass("floating");
                floater.css("top",$("#selectedConnectors").height() + "px");
                if (endFloat != null){
                    var temp = scrollPosition +  floater.outerHeight(false);
                    var marginAmount = endFloat - temp;
                    if (marginAmount > 0) marginAmount = 0;
                    floater.css("marginTop",marginAmount + "px");
                }

            }
            placeholder.height(floater.height());

        }
    }

    $(window).scroll(function(){
        if ($("#photoTab").parent().hasClass("active"))
            onScroll($(window).scrollTop() + $("#selectedConnectors").height());
        else
            onScroll(-100);
    });

    var photosTab = new Tab("calendar", "photos", "Candide Kemmler", "icon-camera", true);
    photosTab.render = render;
    photosTab.connectorToggled = connectorToggled;
    photosTab.connectorDisplayable = connectorDisplayable;
    return photosTab;

});

