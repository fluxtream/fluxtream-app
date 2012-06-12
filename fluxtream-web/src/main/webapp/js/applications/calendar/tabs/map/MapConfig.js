define([], function() {
    return {
        getConfig : function(){
              return {
                  NO_CATEGORY : {
                      icon : "http://maps.google.com/mapfiles/marker_white.png",
                      shadow : new google.maps.MarkerImage("http://maps.google.com/mapfiles/shadow50.png",null,null,new google.maps.Point(11,34),null)
                  },
                  BODY_CATEGORY : {
                      icon : "http://maps.google.com/mapfiles/marker_purple.png",
                      shadow : new google.maps.MarkerImage("http://maps.google.com/mapfiles/shadow50.png",null,null,new google.maps.Point(11,34),null)
                  },
                  MIND_CATEGORY : {
                      icon : "http://maps.google.com/mapfiles/marker_green.png",
                      shadow : new google.maps.MarkerImage("http://maps.google.com/mapfiles/shadow50.png",null,null,new google.maps.Point(11,34),null)
                  },
                  SOCIAL_CATEGORY : {
                      icon : "http://maps.google.com/mapfiles/marker_purple.png",
                      shadow : new google.maps.MarkerImage("http://maps.google.com/mapfiles/shadow50.png",null,null,new google.maps.Point(11,34),null)
                  },
                  MEDIA_CATEGORY : {
                      icon : "http://maps.google.com/mapfiles/marker.png",
                      shadow : new google.maps.MarkerImage("http://maps.google.com/mapfiles/shadow50.png",null,null,new google.maps.Point(11,34),null)
                  },
                  flatAccuracyCutoff:200,
                  stdAccuracyCutoff:1
              };
        }
    };
});
