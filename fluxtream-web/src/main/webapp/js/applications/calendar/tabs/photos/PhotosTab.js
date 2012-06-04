define(["applications/calendar/tabs/Tab",
        "applications/calendar/App"], function(Tab, Calendar) {

    function render(digest, timeUnit, calendarState) {
        this.getTemplate("text!applications/calendar/tabs/photos/photos.html", "photos", function() {
            setup(digest, timeUnit, calendarState);
        });
    }

    /*function render(digest, timeUnit) {
        this.getUrl("/tabs/photos", "photos", null, true);
    }*/

    function setup(digest, timeUnit, calendarState){
        $("#photoTab").empty();
        $("#photoTab").append("<div style='text-align:center'>Retrieving photos...</div>")
        $.ajax("/api/guest/" + App.getUsername() + "/photo/" + calendarState,{
            success: function(data, textStatus, jqXHR){
                onDataRecieved(data);
            },
            error: function(jqXHR, textStatus){
                $("#photoTab").empty();
                showNoPhotos();
                $("#photoTab").append('<div class="alert alert-error"><button class="close" data-dismiss="alert">×</button><strong>Error!</strong> Photos could not be retrieved!</div>');
            }

        });

    }

    function showNoPhotos(){
        $("#photoTab").append("<div class=\"emptyList\">(no photos)</div>");
    }

    function onDataRecieved(data){
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
                   var date = new Date(data[i].timeTaken);
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

    var photosTab = new Tab("photos", "Candide Kemmler", "icon-camera", true);
    photosTab.render = render;
    return photosTab;

});

