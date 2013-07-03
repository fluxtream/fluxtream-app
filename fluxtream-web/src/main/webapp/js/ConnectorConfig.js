define(["applications/calendar/tabs/clock/ClockConfig"],function(ClockConfig){
    ClockConfig = ClockConfig.getConfig();


    //TODO: configure default channels for each connector
    return {
        default:{
            color: "",
            mapicon: "http://maps.google.com/mapfiles/marker.png",
            mapshadow: new google.maps.MarkerImage("http://maps.google.com/mapfiles/shadow50.png",null,null,new google.maps.Point(11,34),null),
            facets: {}
        },
        fitbit:{
            device_name: "Fitbit",
            color: "#21b5cf",
            mapicon: "/" + FLX_RELEASE_NUMBER + "/images/mapicons/aed-2.png",
            mapshadow: null,
            facets:{
                activity_summary:{
                    list: true,
                    photos: false,
                    map: false,
                    gps: false,
                    clock: null
                },
                logged_activity:{
                    list: true,
                    photos: false,
                    map: false,
                    gps: false,
                    clock: null
                },
                weight:{
                    list: true,
                    photos: false,
                    map: false,
                    gps: false,
                    clock: ClockConfig.BODY_CATEGORY
                },
                sleep:{
                    list: true,
                    photos: false,
                    map: false,
                    gps: false,
                    clock: ClockConfig.BODY_CATEGORY
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
            mapicon: "/" + FLX_RELEASE_NUMBER + "/images/mapicons/music.png",
            mapshadow: null,
            facets:{
                recent_track:{
                    list:true,
                    photos: false,
                    map: true,
                    gps: false,
                    clock: ClockConfig.MEDIA_CATEGORY
                },
                loved_track:{
                    list:true,
                    photos: false,
                    map: true,
                    gps: false,
                    clock: ClockConfig.MEDIA_CATEGORY
                }
            }
        },
        withings:{
            device_name:"Withings",
            color: "#16924e",
            mapicon: "/" + FLX_RELEASE_NUMBER + "/images/mapicons/medicalstore.png",
            mapshadow: null,
            facets:{
                blood_pressure:{
                    list: true,
                    photos: false,
                    map: true,
                    gps: false,
                    clock: null,
                    clock: ClockConfig.BODY_CATEGORY
                },
                weight:{
                    channel_name: "weight",
                    list: true,
                    photos: false,
                    map: true,
                    gps: false,
                    clock: ClockConfig.BODY_CATEGORY
                },
                heart_pulse:{
                    channel_name: "scaleHeartRate",
                    list: true,
                    photos: false,
                    map: true,
                    gps: false,
                    clock: ClockConfig.BODY_CATEGORY
                }
            }
        },
        picasa:{
            color: "#8c2ebb",
            mapicon: "/" + FLX_RELEASE_NUMBER + "/images/mapicons/photo.png",
            mapshadow: null,
            facets:{
                photo:{
                    list:true,
                    photos: true ,
                    map: true,
                    gps: false,
                    clock: ClockConfig.SOCIAL_CATEGORY
                }
            }
        },
        runkeeper:{
            color: "#8c2ebb",
            mapshadow: null,
            facets:{
                "fitnessActivity":{
                    mapicon: "/" + FLX_RELEASE_NUMBER + "/images/mapicons/cup.png",
                    list:true,
                    map: true,
                    clock: ClockConfig.BODY_CATEGORY
                },
                location:{
                    mapicon : new google.maps.MarkerImage("/" + FLX_RELEASE_NUMBER + "/images/mapicons/transparentdot.png",null,null,new google.maps.Point(5,5),null),
                    list: false,
                    photos: false,
                    map: true,
                    gps: true,
                    clock: true //just has to be nonnull, gps data has special treatment
                }
            }
        },
        twitter:{
            color: "rgb(146, 51, 239)",
            mapicon: "/" + FLX_RELEASE_NUMBER + "/images/mapicons/tweet.png",
            mapshadow: null,
            facets:{
                tweet:{
                    list: true,
                    photos: false,
                    map: true,
                    gps: false,
                    clock: ClockConfig.SOCIAL_CATEGORY
                },
                dm:{
                    list: true,
                    photos: false,
                    map: true,
                    gps: false,
                    clock: ClockConfig.SOCIAL_CATEGORY
                },
                mention:{
                    list: true,
                    photos: false,
                    map: true,
                    gps: false,
                    clock: ClockConfig.SOCIAL_CATEGORY
                }
            }
        },
        google_calendar:{
            color: "rgb(146, 218, 70)",
            mapicon: "/" + FLX_RELEASE_NUMBER + "/images/mapicons/calendar-3.png",
            mapshadow: null,
            facets:{
                entry:{
                    list: true,
                    photos: false,
                    map: true,
                    gps: false,
                    clock: ClockConfig.MIND_CATEGORY
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
                    photos: false,
                    map: true,
                    gps: true,
                    clock: true //just has to be nonnull, gps data has special treatment
                }
            }
        },
        bodymedia:{
            device_name:"BodyMedia",
            color: "rgb(160, 67, 175)",
            mapicon: null,
            mapshadow: null,
            facets: {
                burn:{
                    list: true,
                    photos: false,
                    map: false,
                    gps: false,
                    clock: null
                },
                sleep:{
                    list: true,
                    photos: false,
                    map: false,
                    gps: false,
                    clock: null
                },
                steps:{
                    list: true,
                    photos: false,
                    map: false,
                    gps: false,
                    clock: null
                }
            }
        },
        flickr:{
            color: "rgb(255, 0, 132)",
            mapicon: "/" + FLX_RELEASE_NUMBER + "/images/mapicons/photo.png",
            mapshadow: new google.maps.MarkerImage("http://maps.google.com/mapfiles/shadow50.png",null,null,new google.maps.Point(11,34),null),
            facets: {
                photo:{
                    list:true,
                    photos: true,
                    map: true,
                    gps: false,
                    clock: ClockConfig.SOCIAL_CATEGORY
                }
            }
        },
        sms_backup:{
            color: "",
            mapicon: "http://maps.google.com/mapfiles/marker.png",
            mapshadow: new google.maps.MarkerImage("http://maps.google.com/mapfiles/shadow50.png",null,null,new google.maps.Point(11,34),null),
            facets: {
                call_log:{
                    list: true,
                    photos: false,
                    map: true,
                    gps: false,
                    clock: ClockConfig.SOCIAL_CATEGORY
                },
                sms:{
                    list: true,
                    photos: false,
                    map: true,
                    gps: false,
                    clock: ClockConfig.SOCIAL_CATEGORY
                }
            }
        },
        zeo:{
            device_name:"Zeo",
            color: "rgb(255, 178, 0)",
            mapicon :"/" + FLX_RELEASE_NUMBER + "/images/mapicons/zeo.png",
            mapshadow: new google.maps.MarkerImage("http://maps.google.com/mapfiles/shadow50.png",null,null,new google.maps.Point(11,34),null),
            facets: {
                sleep:{
                    list: true,
                    photos: false,
                    map: true,
                    gps: false,
                    clock: ClockConfig.BODY_CATEGORY
                }
            }
        },
        toodledo:{
            color: "",
            mapicon: "http://maps.google.com/mapfiles/marker.png",
            mapshadow: new google.maps.MarkerImage("http://maps.google.com/mapfiles/shadow50.png",null,null,new google.maps.Point(11,34),null),
            facets: {
                task:{
                    list: true,
                    photos: false,
                    map: true,
                    gps: false,
                    clock: ClockConfig.MIND_CATEGORY
                },
                goal:{
                    list: true,
                    photos: false,
                    map: true,
                    gps: false,
                    clock: null
                }
            }
        },
        openpath:{
            color: "",
            mapicon : new google.maps.MarkerImage("https://www.google.com/latitude/apps/static/red_measle.png",null,null,new google.maps.Point(5,5),null),
            mapshadow: null,
            facets: {
                location:{
                    list: false,
                    photos: false,
                    map: true,
                    gps: true,
                    clock: true //just has to be nonnull, gps data has special treatment
                }
            }
        },
        quantifiedmind: {
            filterLabel: "QuantMind",
            color : "rgb(20,20,20)",
            mapicon :new google.maps.MarkerImage("https://www.google.com/latitude/apps/static/red_measle.png",null,null,new google.maps.Point(5,5),null),
            mapshadow: new google.maps.MarkerImage("http://maps.google.com/mapfiles/shadow50.png",null,null,new google.maps.Point(11,34),null),
            facets : {
                test : {
                    list : true,
                    photos : false,
                    map : true,
                    gps : false,
                    clock : ClockConfig.BODY_CATEGORY
                }
            }
        },
        github: {
            color : "rgb(5,5,5)",
            mapicon :new google.maps.MarkerImage("https://www.google.com/latitude/apps/static/red_measle.png",null,null,new google.maps.Point(5,5),null),
            mapshadow: new google.maps.MarkerImage("http://maps.google.com/mapfiles/shadow50.png",null,null,new google.maps.Point(11,34),null),
            facets : {
                push : {
                    list : true,
                    photos : false,
                    map : true,
                    gps : false,
                    clock : ClockConfig.SOCIAL_CATEGORY
                }
            }
        },
        mymee: {
            device_name: "Mymee",
            filterLabel: "Mymee",
            color : "rgb(5,5,5)",
            mapicon :"/" + FLX_RELEASE_NUMBER + "/images/mapicons/mymee.png",
            mapshadow: new google.maps.MarkerImage("http://maps.google.com/mapfiles/shadow50.png",null,null,new google.maps.Point(11,34),null),
            facets : {
                observation : {
                    list : true,
                    photos : true,
                    map : true,
                    gps : false,
                    clock : ClockConfig.BODY_CATEGORY
                }
            }
        },
        fluxtream_capture:{
            device_name:"FluxtreamCapture",
            filterLabel: "FluxtreamCap",
            color: "rgb(204, 204, 204)",
            facets: {
                photo:{
                    list:true,
                    photos: true,
                    map: true,
                    gps: false,
                    clock: ClockConfig.SOCIAL_CATEGORY,
                    mapicon :"/" + FLX_RELEASE_NUMBER + "/images/mapicons/fluxcap.png",
                    mapshadow: new google.maps.MarkerImage("http://maps.google.com/mapfiles/shadow50.png",null,null,new google.maps.Point(11,34),null)
                },
                location:{
                    color: "rgb(30, 30, 30)",
                    list:false,
                    photos:false,
                    map:true,
                    gps: true,
                    mapicon : new google.maps.MarkerImage("/" + FLX_RELEASE_NUMBER + "/images/mapicons/transparentdot.png",null,null,new google.maps.Point(5,5),null),
                    mapshadow: null
                }
            }
        },
        moves:{
            filterLabel: "Moves",
            color: "rgb(36, 77, 187)",
            mapicon :"/" + FLX_RELEASE_NUMBER + "/images/mapicons/glasses.png",
            mapshadow: new google.maps.MarkerImage("http://maps.google.com/mapfiles/shadow50.png",null,null,new google.maps.Point(11,34),null),
            facets: {
                "place":{
                    mapicon: "/" + FLX_RELEASE_NUMBER + "/images/mapicons/moves-place.png",
                    list:true,
                    map: true,
                    clock: ClockConfig.OUTSIDE_CATEGORY
                },
                "move":{
                    mapicon: "/" + FLX_RELEASE_NUMBER + "/images/mapicons/transparentdot.png",
                    list:true,
                    map: true,
                    clock: ClockConfig.OUTSIDE_CATEGORY
                },
                "location":{
                    color: "#000",
                    wlkColor: "#23ee70",
                    trpColor: "#8f8f8d",
                    cycColor: "#68abef",
                    runColor: "#e674ec",
                    mapicon : new google.maps.MarkerImage("/" + FLX_RELEASE_NUMBER + "/images/mapicons/transparentdot.png",null,null,new google.maps.Point(5,5),null),
                    list: false,
                    photos: false,
                    map: true,
                    gps: true,
                    clock: true //just has to be nonnull, gps data has special treatment
                },
                "move-activity":{
                    mapicon : new google.maps.MarkerImage("/" + FLX_RELEASE_NUMBER + "/images/mapicons/transparentdot.png",null,null,new google.maps.Point(5,5),null)
                }
            }
        }
    }

});