define(["applications/calendar/tabs/Tab",
        "applications/calendar/App",
       "applications/calendar/tabs/map/MapUtils"], function(Tab, Calendar, MapUtils) {

	var map = null;

    function render(digest, timeUnit, calendarState) {
        this.getTemplate("text!applications/calendar/tabs/map/map.html", "map", function(){setup(digest,calendarState);});
    }

    function setup(digest, calendarState) {
        $("#tooltips").load("/calendar/tooltips");
        App.fullHeight();
        $("#the_map").empty();
        $("#selectedConnectors").empty();
        $("#mapFit").unbind("click");

        var bounds = null;
        if (map != null)
            bounds = map.getBounds();

        if (digest.homeAddress != null && digest.homeAddress.isSet){
            map = MapUtils.newMap(new google.maps.LatLng(digest.homeAddress.latitude,digest.homeAddress.longitude),8,"the_map",false);
        }
        else{
            map = MapUtils.newMap(new google.maps.LatLng(0,0),8,"the_map",false);
        }

        if (digest!=null && digest.cachedData!=null &&
            typeof(digest.cachedData.google_latitude)!="undefined"
                && digest.cachedData.google_latitude !=null &&
            digest.cachedData.google_latitude.length>0) { //make sure gps data is available before trying to display it
            map.addGPSData(digest.cachedData.google_latitude);

            $.ajax("/api/guest/" + digest.username + "/photo/" + calendarState,{
                success: function(data, textStatus, jqXHR){
                    data = JSON.parse('[{"photoUrl":"https://lh5.googleusercontent.com/-2hTKJ78KE8w/T7qpvBCqk6I/AAAAAAAADRo/RMDyFsgdm64/IMG_3918.jpg","thumbnailUrl":"https://lh5.googleusercontent.com/-2hTKJ78KE8w/T7qpvBCqk6I/AAAAAAAADRo/RMDyFsgdm64/s72/IMG_3918.jpg","timeTaken":1337464990000},{"photoUrl":"https://lh5.googleusercontent.com/-SY8WGPDTtmo/T7qpt1nTdwI/AAAAAAAADRc/KTgMT8j35h4/IMG_3919.jpg","thumbnailUrl":"https://lh5.googleusercontent.com/-SY8WGPDTtmo/T7qpt1nTdwI/AAAAAAAADRc/KTgMT8j35h4/s72/IMG_3919.jpg","timeTaken":1337505998000},{"photoUrl":"https://lh6.googleusercontent.com/-GpPJ60NlkXM/T7qptOIj2mI/AAAAAAAADRY/Evpd7CjVbVc/IMG_3920.JPG","thumbnailUrl":"https://lh6.googleusercontent.com/-GpPJ60NlkXM/T7qptOIj2mI/AAAAAAAADRY/Evpd7CjVbVc/s72/IMG_3920.JPG","timeTaken":1337506622000},{"photoUrl":"https://lh5.googleusercontent.com/-BDGjyNGniLs/T7qpr8SP4jI/AAAAAAAADRQ/m-M7I2pPfIM/IMG_3921.jpg","thumbnailUrl":"https://lh5.googleusercontent.com/-BDGjyNGniLs/T7qpr8SP4jI/AAAAAAAADRQ/m-M7I2pPfIM/s72/IMG_3921.jpg","timeTaken":1337508036000},{"photoUrl":"https://lh6.googleusercontent.com/-cGnfWFm2WzY/T7qpqkPxzkI/AAAAAAAADRI/Ob1sCE9-054/IMG_3922.JPG","thumbnailUrl":"https://lh6.googleusercontent.com/-cGnfWFm2WzY/T7qpqkPxzkI/AAAAAAAADRI/Ob1sCE9-054/s72/IMG_3922.JPG","timeTaken":1337508469000},{"photoUrl":"https://lh5.googleusercontent.com/-6y3B8wefdqw/T7qppFaOJUI/AAAAAAAADRA/1iRV6g_6ykg/IMG_3923.jpg","thumbnailUrl":"https://lh5.googleusercontent.com/-6y3B8wefdqw/T7qppFaOJUI/AAAAAAAADRA/1iRV6g_6ykg/s72/IMG_3923.jpg","timeTaken":1337513308000},{"photoUrl":"https://lh5.googleusercontent.com/-dHPdRXxAZbE/T7qpnwtj6HI/AAAAAAAADQ4/CIF84sZ5Uf0/IMG_3924.jpg","thumbnailUrl":"https://lh5.googleusercontent.com/-dHPdRXxAZbE/T7qpnwtj6HI/AAAAAAAADQ4/CIF84sZ5Uf0/s72/IMG_3924.jpg","timeTaken":1337513990000},{"photoUrl":"https://lh4.googleusercontent.com/-KmlEu_o1rqw/T7qpmpfT53I/AAAAAAAADQw/9PKjPn5GR3E/IMG_3925.jpg","thumbnailUrl":"https://lh4.googleusercontent.com/-KmlEu_o1rqw/T7qpmpfT53I/AAAAAAAADQw/9PKjPn5GR3E/s72/IMG_3925.jpg","timeTaken":1337518321000},{"photoUrl":"https://lh5.googleusercontent.com/-pELkkDybEV0/T7qplQS9XWI/AAAAAAAADQo/NDdydynB58A/IMG_3926.JPG","thumbnailUrl":"https://lh5.googleusercontent.com/-pELkkDybEV0/T7qplQS9XWI/AAAAAAAADQo/NDdydynB58A/s72/IMG_3926.JPG","timeTaken":1337519043000},{"photoUrl":"https://lh5.googleusercontent.com/-hllCITPeuyo/T7qpjsxk1EI/AAAAAAAADQg/di4XHoF7yf4/IMG_3927.jpg","thumbnailUrl":"https://lh5.googleusercontent.com/-hllCITPeuyo/T7qpjsxk1EI/AAAAAAAADQg/di4XHoF7yf4/s72/IMG_3927.jpg","timeTaken":1337526656000},{"photoUrl":"https://lh3.googleusercontent.com/-31vNbtliAIQ/T7qpiFSQhjI/AAAAAAAADQY/EKQ61WqInpU/IMG_3928.jpg","thumbnailUrl":"https://lh3.googleusercontent.com/-31vNbtliAIQ/T7qpiFSQhjI/AAAAAAAADQY/EKQ61WqInpU/s72/IMG_3928.jpg","timeTaken":1337532140000},{"photoUrl":"https://lh6.googleusercontent.com/-1v17f76b6o0/T7qphZKekKI/AAAAAAAADQQ/4z4kXIXqh00/IMG_3929.JPG","thumbnailUrl":"https://lh6.googleusercontent.com/-1v17f76b6o0/T7qphZKekKI/AAAAAAAADQQ/4z4kXIXqh00/s72/IMG_3929.JPG","timeTaken":1337535034000},{"photoUrl":"https://lh5.googleusercontent.com/-xArETi3rimg/T7qpfkLEAEI/AAAAAAAADQI/J9eT8Z7QeJA/IMG_3930.JPG","thumbnailUrl":"https://lh5.googleusercontent.com/-xArETi3rimg/T7qpfkLEAEI/AAAAAAAADQI/J9eT8Z7QeJA/s72/IMG_3930.JPG","timeTaken":1337535542000},{"photoUrl":"https://lh5.googleusercontent.com/-bHWsPpLiNEI/T7qpeFWu6JI/AAAAAAAADQA/5BRBISDfMck/IMG_3931.JPG","thumbnailUrl":"https://lh5.googleusercontent.com/-bHWsPpLiNEI/T7qpeFWu6JI/AAAAAAAADQA/5BRBISDfMck/s72/IMG_3931.JPG","timeTaken":1337544431000},{"photoUrl":"https://lh5.googleusercontent.com/-RYFIZAql1Do/T7qpceotYtI/AAAAAAAADP4/VqP_EFFvLtc/IMG_3932.JPG","thumbnailUrl":"https://lh5.googleusercontent.com/-RYFIZAql1Do/T7qpceotYtI/AAAAAAAADP4/VqP_EFFvLtc/s72/IMG_3932.JPG","timeTaken":1337545845000}]');
                    data[0].type = "photo";
                    if (data != null && data.length != 0)
                        map.addData(data,data[0].type,true);
                }

            });

            if (!document.getElementById("perserveViewCheckBox").checked)
                bounds = map.gpsBounds;

            var checkedContainer = $("#selectedConnectors");
            for(var objectTypeName in digest.cachedData) {
                if (digest.cachedData[objectTypeName]==null||typeof(digest.cachedData[objectTypeName])=="undefined")
                    continue;
                var dataAdded = map.addData(digest.cachedData[objectTypeName], objectTypeName, true);
                if (dataAdded || objectTypeName == "google_latitude"){
                    var button = $('<button class="btnList btn btnListChecked enabled">' + objectTypeName + '</button>');
                    button.click({button:button,objectTypeName:objectTypeName,isGoogleLatitude:objectTypeName == "google_latitude"},function(event){
                        buttonClicked(event.data.button,event.data.objectTypeName,event.data.isGoogleLatitude);
                    });
                    checkedContainer.append(button);
                    checkedContainer.append("&nbsp;");
                }
            }

            $("#mapFit").show();
            $("#mapFit").click(function(){
                map.fitBounds(map.gpsBounds);
            });

        } else {
            $("#mapFit").hide();
            $("#selectedConnectors").append("<div class=\"emptyList\">(no location data)</div>");
        }
        if (bounds != null)
            map.fitBounds(bounds);
	}

    function buttonClicked(button,connectorName,isGoogleLatitude){
        if (button.hasClass("disabled"))
            return;
        button.removeClass("enabled");
        button.addClass("disabled");
        if (button.hasClass("btnListChecked")){
            button.removeClass("btnListChecked");
            button.addClass("btn-inverse");
            if (isGoogleLatitude){
                map.hideGPSData();
            }
            else{
                map.hideData(connectorName);
            }
        }
        else{
            button.removeClass("btn-inverse");
            button.addClass("btnListChecked");
            if (isGoogleLatitude){
                map.showGPSData();
            }
            else{
                map.showData(connectorName);
            }
        }

        button.removeClass("disabled");
        button.addClass("enabled");
    }

    var mapTab = new Tab("map", "Candide Kemmler", "icon-map-marker", true);
    mapTab.render = render;
    return mapTab;

});
