define(["applications/calendar/tabs/Tab",
        "applications/calendar/App"], function(Tab, Calendar) {

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
        if (digest.cachedData["picasa-photo"] == null){
            showNoPhotos();
            return;
        }
        onDataRecieved(digest.cachedData["picasa-photo"]);
    }

    function showNoPhotos(){
        $("#photoTab").empty();
        $("#photoTab").append("<div class=\"emptyList\">(no photos)</div>");
    }

    function onDataRecieved(photos){
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

