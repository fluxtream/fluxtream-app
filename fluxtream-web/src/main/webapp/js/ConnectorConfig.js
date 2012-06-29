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
                    list: true
                },
                logged_activity:{
                    list: true
                },
                sleep:{
                    list: true
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
                    list:true
                },
                loved_track:{
                    list:true
                }
            }
        },
        withings:{
            color: "#16924e",
            mapicon: "/static/images/mapicons/medicalstore.png",
            mapshadow: null,
            facets:{
                blood_pressure:{
                    list: true
                },
                weight:{
                    list: true
                }
            }
        },
        picasa:{
            color: "#8c2ebb",
            mapicon: "/static/images/mapicons/photo.png",
            mapshadow: null,
            facets:{
                photo:{
                    list:true
                }
            }
        },
        twitter:{
            color: "rgb(146, 51, 239)",
            mapicon: "/static/images/mapicons/tweet.png",
            mapshadow: null,
            facets:{
                tweet:{
                    list: true
                },
                dm:{
                    list: true
                },
                mention:{
                    list: true
                }
            }
        },
        google_calendar:{
            color: "rgb(146, 218, 70)",
            mapicon: "/static/images/mapicons/calendar-3.png",
            mapshadow: null,
            facets:{
                entry:{
                    list: true
                }
            }
        },
        google_latitude:{
            color: "rgb(92, 174, 92)",
            mapicon : new google.maps.MarkerImage("https://www.google.com/latitude/apps/static/red_measle.png",null,null,new google.maps.Point(5,5),null),
            mapshadow: null,
            facets:{
                location:{
                    list: false
                }
            }
        }

    }

});