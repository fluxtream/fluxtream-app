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


    function getPhotoHTML(photoData){
        var html = "<div class='thumbnailContainer'>";
        html += "<div class='photoThumbnail' style='background-image: url(";
        html += photoData.photoUrl;
        html += ")'></div></div>";
       return html;
    }

    function buildCarouselHTML(data){
        var html =  '<div class="modal fade" id="modal"><div class="modal-header">';
        html += '<h3>&nbsp;<a href="#" class="close" data-dismiss="modal">&times;</a></h3></div>'
        html += '<div class="modal-body" style="overflow:hidden"><div id="photosCarousel" class="carousel">';
        html += '<div class="carousel-inner">';
        for (var i = 0; i < data.length; i++){
            html += '<div style="text-align:center;" id="photo-' + i + '" class="item';
            if (i == 0)
                html += " active";
            html += '" style="overflow:none">';
            html += '<img style="display:inline-block" src="' + data[i].photoUrl + '" /></div>';
        }
        html += "</div>";
        if (data.length > 1){
          html += '<a class="carousel-control left" href="#photosCarousel" data-slide="prev">&lsaquo;</a>';
          html += '<a class="carousel-control right" href="#photosCarousel" data-slide="next">&rsaquo;</a>';
        }
        html += '</div><div class="modal-footer"></div></div>';
        return html;
    }

    function formatDate(date){
        var value = "";
        switch (date.getMonth()){
            case 0:
                value += "January";
                break;
            case 1:
                value + "February";
                break;
            case 2:
                value += "March";
                break;
            case 3:
                value += "April";
                break;
            case 4:
                value += "May";
                break;
            case 5:
                value += "June";
                break;
            case 6:
                value += "July";
                break;
            case 7:
                value += "August";
                break;
            case 8:
                value += "September";
                break;
            case 9:
                value += "October";
                break;
            case 10:
                value += "November";
                break;
            case 11:
                value += "December";
                break;
        }
        value += " " + date.getDate();
        value += ", " + date.getFullYear();
        return value;
    }

    function buildCarouselPhotos(photos,onDone){
        var photosHTML = ""
        var i = 0;
        var loadNextPhoto = function (){
            App.loadHTMLTemplate("applications/calendar/tabs/photos/photosTemplate.html","carouselPhoto",{
                active:" active",
                photoURL:photos[i].photoURL
            }, function(html){
                photosHTML += html;
                i++;
                if (i == photos.length)
                    onDone(photosHTML);
                else
                    loadNextPhoto();
            });
        }
        loadNextPhoto();

    }

    function buildMainHTML(photos,onDone){
        var i = 0;
        var photosHTML = "";
        var groupHTML = "";
        var currentDate = new Date(photos[i].timeTaken);
        var loadNextPhoto = function(){
            App.loadHTMLTemplate("applications/calendar/tabs/photos/photosTemplate.html","photoThumbnail",{
                id:i,
                photoURL:photos[i].photoUrl
            }, function(html){
                var date = new Date(photos[i].timeTaken);
                photosHTML += html;
                i++;
                var mainCallback = this;
                if (i == photos.length || currentDate.getMonth() != date.getMonth() || currentDate.getYear() != date.getYear()
                    || currentDate.getDate() != date.getDate()){
                    App.loadHTMLTemplate("applications/calendar/tabs/photos/photosTemplate.html","thumbnailGroup",{
                        date:formatDate(currentDate),
                        thumbnails:photosHTML
                    }, function(group){
                        groupHTML += group;
                        if (i == photos.length){
                            onDone(groupHTML);
                        }
                        else{
                            currentDate = date;
                            loadNextPhoto();
                        }
                    });
                }
                else{
                    loadNextPhoto();
                }
            });
        }
        loadNextPhoto();
    }



    function onDataRecieved(data){
        $("#photoTab").empty();
        if (data.length == 0){
            showNoPhotos();
            return;
        }
        buildCarouselPhotos(data,function (photoHTML){
            App.loadHTMLTemplate("applications/calendar/tabs/photos/photosTemplate.html","carouselWith" + (data.length == 1 ? "out" : "") + "Nav",{
                itemsHTML:photoHTML
            },function(carouselHTML){
                buildMainHTML(data,function(html){
                    $("#photoTab").append(html);
                    for (var i = 0; i < data.length; i++){
                        $("#photo-" + i).click({i:i},function(event){
                            App.makeModal(carouselHTML);
                            App.carousel(event.data.i);
                        });
                    }
                })

            });

        });

    }

    var photosTab = new Tab("photos", "Candide Kemmler", "icon-camera", true);
    photosTab.render = render;
    return photosTab;

});

