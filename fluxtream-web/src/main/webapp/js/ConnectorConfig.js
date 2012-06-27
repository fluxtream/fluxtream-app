define([],function(){


    //TODO: configure default channels for each connector
    return {
        default:{
            color: "",
            mapicon: "http://maps.google.com/mapfiles/marker.png",
            mapshadow: new google.maps.MarkerImage("http://maps.google.com/mapfiles/shadow50.png",null,null,new google.maps.Point(11,34),null),
            defaultChannels: []
        },
        fitbit:{
            color: "#21b5cf",
            mapicon: "/static/images/mapicons/aed-2.png",
            mapshadow: null,
            defaultChannels: ["Fitbit.steps","Fitbit.caloriesOut"]
        },
        bodyTrack:{
            color: "#c79409",
            mapicon: null,
            mapshadow: null,
            defaultChannels: []
        },
        lastfm:{
            color: "rgb(253, 73, 56)",
            mapicon: "/static/images/mapicons/music.png",
            mapshadow: null,
            defaultChannels: []
        },
        withings:{
            color: "#16924e",
            mapicon: "/static/images/mapicons/medicalstore.png",
            mapshadow: null,
            defaultChannels: []
        },
        picasa:{
            color: "#8c2ebb",
            mapicon: "/static/images/mapicons/photo.png",
            mapshadow: null,
            defaultChannels: ["All.photos"]
        },
        twitter:{
            color: "rgb(146, 51, 239)",
            mapicon: "/static/images/mapicons/tweet.png",
            mapshadow: null,
            defaultChannels: []
        },
        google_calendar:{
            color: "rgb(146, 218, 70)",
            mapicon: "/static/images/mapicons/calendar-3.png",
            mapshadow: null,
            defaultChannels: []
        },
        google_latitude:{
            color: "rgb(92, 174, 92)",
            mapicon : new google.maps.MarkerImage("https://www.google.com/latitude/apps/static/red_measle.png",null,null,new google.maps.Point(5,5),null),
            mapshadow: null,
            defaultChannels: []
        }

    }

});