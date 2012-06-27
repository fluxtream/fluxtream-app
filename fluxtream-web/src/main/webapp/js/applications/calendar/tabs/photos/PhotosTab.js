define(["applications/calendar/tabs/Tab",
        "applications/calendar/App"], function(Tab, Calendar) {

    var maxWidth = 200;
    var maxHeight = 200;
    var ratio = maxWidth / maxHeight;

    function render(dgest, timeUnit, calendarState, cEn) {
        this.getTemplate("text!applications/calendar/tabs/photos/photos.html", "photos", function() {
            digest = dgest;
            connectorEnabled = cEn;
            setup(digest,connectorEnabled);
        });
    }

    /*function render(digest, timeUnit) {
        this.getUrl("/tabs/photos", "photos", null, true);
    }*/

    var connectorEnabled;
    var digest;

    function setup(digest, cEn){
        $("#photoTab").empty();
        if (digest.cachedData["picasa-photo"] == null){
            showNoPhotos();
            return;
        }

        $("#photoTab").append("<div class=\"emptyList\">Loading photos...</div>");

        var count = 0;
        for (var i = 0; i < digest.cachedData["picasa-photo"].length; i++){
            $("<img src='" + digest.cachedData["picasa-photo"][i].photoUrl + "'>").load({i:i},function(event){
                var i = event.data.i;
                digest.cachedData["picasa-photo"][i].width = event.target.width;
                digest.cachedData["picasa-photo"][i].height = event.target.height;
                digest.cachedData["picasa-photo"][i].ratio = event.target.width / event.target.height;
                count++;
                if (count == digest.cachedData["picasa-photo"].length)
                    onDataRecieved(digest.cachedData["picasa-photo"]);

            });

        }
    }

    function showNoPhotos(){
        $("#photoTab").empty();
        $("#photoTab").append("<div class=\"emptyList\">(no photos)</div>");
    }

    function onDataRecieved(photos){
        for (var i = 0; i < photos.length; i++){
            photos[i].targetHeight = photos[i].height;
            photos[i].targetWidth = photos[i].width;
            if (photos[i].ratio < ratio){//check height
                if (photos[i].height > maxHeight){
                    photos[i].targetHeight = maxHeight;
                    photos[i].targetWidth = photos[i].width * photos[i].targetHeight / photos[i].height;
                }
            }
            else if (photos[i].width > maxWidth){
                photos[i].targetWidth = maxWidth;
                photos[i].targetHeight = photos[i].height * photos[i].targetWidth / photos[i].width;

            }
        }
        var data = [];
        $("#photoTab").empty();
        for (var i = 0; i < photos.length; i++){
            for (var j = 0; j < digest.selectedConnectors.length; j++){
                var found = false;
                for (var k = 0; !found &&  k < digest.selectedConnectors[j].facetTypes.length; k++){
                   found = digest.selectedConnectors[j].facetTypes[k] == photos[i].type;
                }
                if (found){
                   if (connectorEnabled[digest.selectedConnectors[j].connectorName])
                        data[data.length] = photos[i];
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
        App.loadMustacheTemplate("applications/calendar/tabs/photos/photosTemplate.html","carousel",function(template){
            var carouselHTML = template.render({photos:data,includeNav:data.length > 1});
            App.loadMustacheTemplate("applications/calendar/tabs/photos/photosTemplate.html","thumbnailGroup", function(template){
                var currentGroup = [];
                var currentDate = null;
                for (var i = 0; i < data.length; i++){
                   var date = new Date(data[i].start);
                   if (currentDate == null){
                       currentDate = date;
                   }
                   else if (currentDate.getMonth() != date.getMonth() || currentDate.getYear() != date.getYear()
                       || currentDate.getDate() != date.getDate()) {
                       $("#photoTab").append(template.render({date:App.formatDate(currentDate),photos:currentGroup}));
                       currentGroup = [];
                       currentDate = date;
                   }
                    currentGroup[currentGroup.length] = data[i];
                }
                if (currentGroup.length != 0){
                    $("#photoTab").append(template.render({date:App.formatDate(currentDate),photos:currentGroup}));
                }
                for (var i = 0; i < data.length; i++){
                    $("#photo-" + i).click({i:i},function(event){
                        App.makeModal(carouselHTML);
                        App.carousel(event.data.i);
                    });
                }
                var groups = $(".thumbnailGroup");
                for (var i = 0; i < groups.length; i++){
                    $(groups[i]).masonry({
                         itemSelector: '.thumbnailContainer',
                         columnWidth:1
                     });
                }
            });

        });

    }

    function connectorToggled(connectorName,objectTypeNames,enabled){
        connectorEnabled[connectorName] = enabled;
        setup(digest,connectorEnabled);
    }

    var photosTab = new Tab("photos", "Candide Kemmler", "icon-camera", true);
    photosTab.render = render;
    photosTab.connectorToggled = connectorToggled;
    return photosTab;

});

