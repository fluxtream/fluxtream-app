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

    function getDateHTML(date){
        var html = "<hr><div>";
        switch (date.getMonth()){
            case 0:
                html += "January";
                break;
            case 1:
                html + "February";
                break;
            case 2:
                html += "March";
                break;
            case 3:
                html += "April";
                break;
            case 4:
                html += "May";
                break;
            case 5:
                html += "June";
                break;
            case 6:
                html += "July";
                break;
            case 7:
                html += "August";
                break;
            case 8:
                html += "September";
                break;
            case 9:
                html += "October";
                break;
            case 10:
                html += "November";
                break;
            case 11:
                html += "December";
                break;
        }
        html += " " + date.getDate();
        html += ", " + date.getFullYear();
        html += "</div>";
        return html;
    }

    function onDataRecieved(data){
        $("#photoTab").empty();
        if (data.length == 0){
            showNoPhotos();
            return;
        }
        var carouselHTML = buildCarouselHTML(data);
        var currentPhotosList = $('<div class="thumbnailGroup"></div>');
        var currentDate = null;
        for (var i = 0; i < data.length; i++){
            var date = new Date(data[i].timeTaken);
            if (currentDate == null){
                currentDate = date;
            }
            else if (currentDate.getMonth() != date.getMonth() || currentDate.getYear() != date.getYear() || currentDate.getDate() != date.getDate()){
                $("#photoTab").append(getDateHTML(currentDate));
                $("#photoTab").append(currentPhotosList);
                currentPhotosList = $("<div class='thumbnailGroup'></div>");
            }
            currentDate = date;
            var photo = $(getPhotoHTML(data[i]));
            photo.click({i:i},function(event){
                App.makeModal(carouselHTML);
                App.carousel(event.data.i);
            });
            currentPhotosList.append(photo);
        }
        if (currentDate != null){
            $("#photoTab").append(getDateHTML(currentDate));
            $("#photoTab").append(currentPhotosList);
        }

    }

    var photosTab = new Tab("photos", "Candide Kemmler", "icon-camera", true);
    photosTab.render = render;
    return photosTab;

});

