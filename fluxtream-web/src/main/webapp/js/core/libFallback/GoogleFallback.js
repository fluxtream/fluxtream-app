define([],function(){
    var google = {};
    google.fallback = true;

    //maps stub
    google.maps = {};
    google.maps.Circle = function(){};
    google.maps.Circle.prototype.setCenter = function(){};
    google.maps.Circle.prototype.setMap = function(){};
    google.maps.Circle.prototype.setRadius = function(){};
    google.maps.Geocoder = function(){};
    google.maps.Geocoder.prototype.geocode = function(input,callback){
        callback([],google.maps.GeocoderStatus.ZERO_RESULTS);
    }
    google.maps.InfoWindow = function(){};
    google.maps.Size = function(){};
    google.maps.LatLng = function(){};
    google.maps.LatLng.prototype.lat = function(){};
    google.maps.LatLng.prototype.lng = function(){};
    google.maps.LatLngBounds = function(){};
    google.maps.LatLngBounds.prototype.getNorthEast = function(){return new google.maps.LatLng();};
    google.maps.LatLngBounds.prototype.getSouthWest = function(){return new google.maps.LatLng();};
    google.maps.Marker = function(){};
    google.maps.Marker.prototype.getPosition = function(){};
    google.maps.Marker.prototype.setPosition = function(){};
    google.maps.Marker.prototype.setIcon = function(){};
    google.maps.Marker.prototype.setMap = function(){};
    google.maps.Marker.MAX_ZINDEX = 0;
    google.maps.MarkerImage = function(){};
    google.maps.Map = function(container){
        this.container = $(container);
        this.controlsContainer = $("<div style='display:none;'></div>");
        this.container.append(this.controlsContainer);
        this.controls = [];
        this.controls[google.maps.ControlPosition.RIGHT_TOP] = [];
        this.controls[google.maps.ControlPosition.TOP] = [];
        this.controls[google.maps.ControlPosition.RIGHT_TOP].oldpush = this.controls[google.maps.ControlPosition.RIGHT_TOP].push;
        this.controls[google.maps.ControlPosition.TOP].oldpush = this.controls[google.maps.ControlPosition.TOP].push;
        var that = this;
        this.controls[google.maps.ControlPosition.TOP].push = this.controls[google.maps.ControlPosition.RIGHT_TOP].push = function(element){
            that.controlsContainer.append(element);
        }

        this.container.append("<b>Map unavailable!</b>");
    };
    google.maps.Map.prototype.fitBounds = function(){};
    google.maps.Map.prototype.getZoom = function(){};
    google.maps.Map.prototype.getProjection = function(){return new google.maps.Projection();};
    google.maps.Map.prototype.setCenter = function(){};
    google.maps.Map.prototype.setZoom = function(){};
    google.maps.Point = function(){};
    google.maps.Polyline = function(){};
    google.maps.Polyline.prototype.getMap = function(){};
    google.maps.Polyline.prototype.setMap = function(){};
    google.maps.Polyline.prototype.setOptions = function(){};
    google.maps.Projection = function(){};
    google.maps.Projection.prototype.fromLatLngToPoint = function(){return new google.maps.Point();};
    google.maps.Projection.prototype.fromPointToLatLng = function(){return new google.maps.LatLng();};

    google.maps.MapTypeId = {
        ROADMAP: 0
    };
    google.maps.GeocoderStatus = {
        OK: 0,
        ZERO_RESULTS: 1
    }
    google.maps.ControlPosition = {
        RIGHT_TOP: 0,
        TOP: 1
    }

    google.maps.event = {};
    google.maps.event.addListener = function(){};
    google.maps.event.trigger = function(){};

    return google;
});