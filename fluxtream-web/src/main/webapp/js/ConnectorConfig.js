define([],function(){


    //TODO: configure default channels for each connector
    return {
        default:{
            color: "",
            mapicon: "http://maps.google.com/mapfiles/marker.png",
            mapshadow: new google.maps.MarkerImage("http://maps.google.com/mapfiles/shadow50.png",null,null,new google.maps.Point(11,34),null),
            facets: {}
        },
        fitbit:{
            color: "#21b5cf",
            mapicon: "/static/images/mapicons/aed-2.png",
            mapshadow: null,
            facets:{
                activity_summary:{
                    list: true,
                    photos: false
                },
                logged_activity:{
                    list: true,
                    photos: false
                },
                sleep:{
                    list: true,
                    photos: false
                }
            }
        },
        bodyTrack:{
            color: "#c79409",
            mapicon: null,
            mapshadow: null,
            facets:{}
        },
        lastfm:{
            color: "rgb(253, 73, 56)",
            mapicon: "/static/images/mapicons/music.png",
            mapshadow: null,
            facets:{
                recent_track:{
                    list:true,
                    photos: false
                },
                loved_track:{
                    list:true,
                    photos: false
                }
            }
        },
        withings:{
            color: "#16924e",
            mapicon: "/static/images/mapicons/medicalstore.png",
            mapshadow: null,
            facets:{
                blood_pressure:{
                    list: true,
                    photos: false
                },
                weight:{
                    list: true,
                    photos: false
                }
            }
        },
        picasa:{
            color: "#8c2ebb",
            mapicon: "/static/images/mapicons/photo.png",
            mapshadow: null,
            facets:{
                photo:{
                    list:true,
                    photos: true
                }
            }
        },
        twitter:{
            color: "rgb(146, 51, 239)",
            mapicon: "/static/images/mapicons/tweet.png",
            mapshadow: null,
            facets:{
                tweet:{
                    list: true,
                    photos: false
                },
                dm:{
                    list: true,
                    photos: false
                },
                mention:{
                    list: true,
                    photos: false
                }
            }
        },
        google_calendar:{
            color: "rgb(146, 218, 70)",
            mapicon: "/static/images/mapicons/calendar-3.png",
            mapshadow: null,
            facets:{
                entry:{
                    list: true,
                    photos: false
                }
            }
        },
        google_latitude:{
            color: "rgb(92, 174, 92)",
            mapicon : new google.maps.MarkerImage("https://www.google.com/latitude/apps/static/red_measle.png",null,null,new google.maps.Point(5,5),null),
            mapshadow: null,
            facets:{
                location:{
                    list: false,
                    photos: false
                }
            }
        }

    }

});