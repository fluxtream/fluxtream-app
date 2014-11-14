define(["applications/calendar/tabs/clock/ClockConfig", "libs/moves-colorcodes"],function(ClockConfig, MovesColors){
    ClockConfig = ClockConfig.getConfig();


    return {
        default:{
            color: "",
            mapicon: {
                url: "https://mts.googleapis.com/vt/icon/name=icons/spotlight/spotlight-poi.png&scale=1",
                size: new google.maps.Size(22,40)
            },
            mapshadow: {
                url: "http://maps.google.com/mapfiles/shadow50.png",
                anchor: new google.maps.Point(11,34)
            },
            gpsSplittingStrategy: "flatCutoff(3600000)",
            clockOutline: false,
            facets: {}
        },
        evernote:{
            hasGeneralSettings: true,
            hasTimelineSettings: true,
            color: "rgb(130, 182, 82)",
            applySettings: function(facet, connectorSettings) {
                if (typeof(connectorSettings)=="undefined")
                    console.log("warning: no connector settings");
                else if (typeof(facet.apiKeyId)=="undefined")
                    console.log("warning: no apiKeyId associated with this facet: " + facet.type);
                else if (typeof(connectorSettings[facet.apiKeyId])=="undefined")
                    console.log("warning: no connector settings are associated with apiKeyId " + facet.apiKeyId);
                else {
                    var settings = connectorSettings[facet.apiKeyId];
                    if (typeof(settings)!="undefined") {
                        for (var i=0; i<settings.notebooks.length; i++) {
                            if (settings.notebooks[i].guid==facet.notebookGuid) {
                                var notebookSettings = settings.notebooks[i];
                                facet.color = notebookSettings.backgroundColor;
                                facet.notebook = notebookSettings.name;
                            }
                        }
                        if (facet.tagGuids!=null && facet.tagGuids.length>0) {
                            facet.evernoteTags = [];
                            for (var i=0; i<facet.tagGuids.length;i++) {
                                var evernoteTag = settings.tags[facet.tagGuids[i]];
                                facet.evernoteTags.push(evernoteTag);
                            }
                        }
                    }
                }
            },
            isFilteredOut: function(facet, connectorSettings) {
                var settings = connectorSettings[facet.apiKeyId];
                if (typeof(settings)!="undefined") {
                    for (var i=0; i<settings.notebooks.length; i++) {
                        if (settings.notebooks[i].guid==facet.notebookGuid&&settings.notebooks[i].hidden) {
                            return true;
                        }
                    }
                }
                return false;
            },
            mapicon: {
                url: "/" + FLX_RELEASE_NUMBER + "/images/mapicons/evernote.png",
                size: new google.maps.Size(32,37)
            },
            facets:{
                note:{
                    list:true,
                    photos: false,
                    map: true,
                    gps: false,
                    clock: ClockConfig.AT_HOME_CATEGORY
                },
                photo:{
                    list:false,
                    photos: true,
                    map: false,
                    gps: false,
                    clock: null
                },
                notebook:{
                    list:false,
                    photos: false,
                    map: false,
                    gps: false,
                    clock: null
                },
                resource:{
                    list:false,
                    photos: false,
                    map: false,
                    gps: false,
                    clock: null
                },
                tag:{
                    list:false,
                    photos: false,
                    map: false,
                    gps: false,
                    clock: null
                },
                location:{
                    list:false,
                    photos: false,
                    map: true,
                    gps: true,
                    clock: null
                }
            }
        },
        up:{
            hasTimelineSettings: true,
            device_name: "Jawbone_UP",
            color: "#000",
            mapicon: {
                url: "/" + FLX_RELEASE_NUMBER + "/images/mapicons/up.png",
                size: new google.maps.Size(32,37)
            },
            mapshadow: null,
            facets:{
                meal:{
                    list: true,
                    photos: false,
                    map: true,
                    gps: false,
                    clock: ClockConfig.BODY_CATEGORY
                },
                moves:{
                    list: true,
                    photos: false,
                    map: false,
                    gps: false,
                    clock: null
                },
                sleep:{
                    list: true,
                    photos: false,
                    map: true,
                    gps: false,
                    clock: ClockConfig.BODY_CATEGORY
                },
                workout:{
                    list: true,
                    photos: false,
                    map: true,
                    gps: false,
                    clock: ClockConfig.BODY_CATEGORY
                },
                location:{
                    list:false,
                    photos: false,
                    map: true,
                    gps: true,
                    clock: null
                },
                serving:{
                    list:false,
                    photos: true,
                    map: false,
                    gps: false,
                    clock: null
                }
            }
        },
        fitbit:{
            hasTimelineSettings: true,
            device_name: "Fitbit",
            color: "#21b5cf",
            mapicon: {
                url: "/" + FLX_RELEASE_NUMBER + "/images/mapicons/aed-2.png",
                size: new google.maps.Size(32,37)
            },
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
                food_log_summary:{
                    list: true,
                    photos: false,
                    map: false,
                    gps: false,
                    clock: null
                },
                food_log_entry:{
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
            hasTimelineSettings: true,
            color: "rgb(253, 73, 56)",
            mapicon: {
                url: "/" + FLX_RELEASE_NUMBER + "/images/mapicons/music.png",
                size: new google.maps.Size(32,37)
            },
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
            hasTimelineSettings: true,
            device_name:"Withings",
            color: "#16924e",
            mapicon: {
                url: "/" + FLX_RELEASE_NUMBER + "/images/mapicons/medicalstore.png",
                size: new google.maps.Size(32,37)
            },
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
                    list: true,
                    photos: false,
                    map: true,
                    gps: false,
                    clock: ClockConfig.BODY_CATEGORY
                },
                activity:{
                    list: true,
                    photos: false,
                    map: true,
                    gps: false,
                    clock: null
                },
                heart_pulse:{
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
            mapicon: {
                url: "/" + FLX_RELEASE_NUMBER + "/images/mapicons/photo.png",
                size: new google.maps.Size(32,37)
            },
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
            hasTimelineSettings: true,
            facets:{
                fitnessActivity:{
                    mapicon: {
                        url: "/" + FLX_RELEASE_NUMBER + "/images/mapicons/cup.png",
                        size: new google.maps.Size(32,37)
                    },
                    list:true,
                    map: true,
                    clock: ClockConfig.BODY_CATEGORY
                },
                location:{
                    mapicon : {
                        url: "/" + FLX_RELEASE_NUMBER + "/images/mapicons/transparentdot.png",
                        anchor: new google.maps.Point(5,5)
                    },
                    list: false,
                    photos: false,
                    map: true,
                    gps: true,
                    gpsSplittingStrategy: "uriEquality()",
                    clock: true //just has to be nonnull, gps data has special treatment
                }
            }
        },
        twitter:{
            color: "rgb(146, 51, 239)",
            hasTimelineSettings: true,
            mapicon: {
                url: "/" + FLX_RELEASE_NUMBER + "/images/mapicons/tweet.png",
                size: new google.maps.Size(32,37)
            },
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
            hasGeneralSettings: true,
            applySettings: function(facet, connectorSettings) {
                if (typeof(connectorSettings)=="undefined")
                    console.log("warning: no connector settings");
                else if (typeof(facet.apiKeyId)=="undefined")
                    console.log("warning: no apiKeyId associated with this facet: " + facet.type);
                else if (typeof(connectorSettings[facet.apiKeyId])=="undefined")
                    console.log("warning: no connector settings are associated with apiKeyId " + facet.apiKeyId);
                else {
                    var settings = connectorSettings[facet.apiKeyId];
                    for (var i=0; i<settings.calendars.length; i++) {
                        if (settings.calendars[i].id==facet.calendarId) {
                            var calendarSettings = settings.calendars[i];
                            facet.color = calendarSettings.backgroundColor;
                        }
                    }
                }
            },
            isFilteredOut: function(facet, connectorSettings) {
                var settings = connectorSettings[facet.apiKeyId];
                for (var i=0; i<settings.calendars.length; i++) {
                    if (settings.calendars[i].id==facet.calendarId&&settings.calendars[i].hidden) {
                        return true;
                    }
                }
                return false;
            },
            mapicon: {
                url: "/" + FLX_RELEASE_NUMBER + "/images/mapicons/calendar-3.png",
                size: new google.maps.Size(32,37)
            },
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
            mapicon : {
                url: "/" + FLX_RELEASE_NUMBER + "/images/mapicons/greendot.png",
                anchor: new google.maps.Point(5,5)
            },
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
            hasTimelineSettings: true,
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
            mapicon: {
                url: "/" + FLX_RELEASE_NUMBER + "/images/mapicons/photo.png",
                size: new google.maps.Size(32,37)
            },
            mapshadow: null,
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
        sms_backup:{ //TODO: define mapicons
            hasGeneralSettings: true,
            color: "rgb(51,102,153)",
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
                    photos: true,
                    map: true,
                    gps: false,
                    clock: ClockConfig.SOCIAL_CATEGORY
                }
            }
        },
        zeo:{
            hasTimelineSettings: true,
            device_name:"Zeo",
            color: "rgb(255, 178, 0)",
            mapicon : {
                url: "/" + FLX_RELEASE_NUMBER + "/images/mapicons/zeo.png",
                size: new google.maps.Size(32,37)
            },
            mapshadow: null,
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
        toodledo:{//TODO: define mapicons
            color: "",
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
            mapicon : {
                url: "/" + FLX_RELEASE_NUMBER + "/images/mapicons/transparentdot.png",
                anchor: new google.maps.Point(5,5)
            },
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
            hasTimelineSettings: true,
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
            hasTimelineSettings: true,
            device_name: "Mymee",
            filterLabel: "Mymee",
            color : "rgb(5,5,5)",
            mapicon: {
                url: "/" + FLX_RELEASE_NUMBER + "/images/mapicons/mymee.png",
                size: new google.maps.Size(32,37)
            },
            mapshadow: null,
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
            hasTimelineSettings: true,
            device_name:"FluxtreamCapture",
            filterLabel: "Fluxtream",
            color: "rgb(204, 204, 204)",
            facets: {
                photo:{
                    list:true,
                    photos: true,
                    map: true,
                    gps: false,
                    clock: ClockConfig.SOCIAL_CATEGORY,
                    mapicon : {
                        url: "/" + FLX_RELEASE_NUMBER + "/images/mapicons/fluxcap.png",
                        size: new google.maps.Size(32,37)
                    },
                    mapshadow: null
                },
                location:{
                    color: "rgb(90, 90, 90)",
                    list:false,
                    photos:false,
                    map:true,
                    gps:true,
                    mapicon : {
                        url: "/" + FLX_RELEASE_NUMBER + "/images/mapicons/greydot.png",
                        anchor: new google.maps.Point(5,5)
                    },
                    mapshadow: null
                }
            }
        },
        moves:{
            filterLabel: "Moves",
            color: "rgb(36, 77, 187)",
            mapicon : {
                url: "/" + FLX_RELEASE_NUMBER + "/images/mapicons/glasses.png",
                size: new google.maps.Size(32,37)
            },
            mapshadow: null,
            facets: {
                "place":{
                    mapicon: {
                        url: "/" + FLX_RELEASE_NUMBER + "/images/mapicons/moves-place.png",
                        size: new google.maps.Size(32,37)
                    },
                    list:true,
                    map: true,
                    clock: ClockConfig.OUTSIDE_CATEGORY,
                    clockOutline: true
                },
                "move":{
                    mapicon : {
                        url: "/" + FLX_RELEASE_NUMBER + "/images/mapicons/transparentdot.png",
                        anchor: new google.maps.Point(5,5)
                    },
                    list:true,
                    map: true,
                    clock: ClockConfig.OUTSIDE_CATEGORY
                },
                "location":{
                    getCustomColor: function(type) {
                        switch(type){
                            case "wlk":
                                type="walking";
                                break;
                            case "trp":
                                type="transport";
                                break;
                            case "run":
                                type="running";
                                break;
                            case "cyc":
                                type="cycling";
                                break;
                        }
                        for (var i=0;i<MovesColors.activities.length;i++) {
                            if (type.toLowerCase()===MovesColors.activities[i].group||
                                type.toLowerCase()===MovesColors.activities[i].activity){
                                return "#"+MovesColors.activities[i].color;
                            }
                        }
                        return "#000";
                    },
                    color: "rgb(36, 77, 187)",
                    mapicon : {
                        url: "/" + FLX_RELEASE_NUMBER + "/images/mapicons/transparentdot.png",
                        anchor: new google.maps.Point(5,5)
                    },
                    list: false,
                    photos: false,
                    map: true,
                    gps: true,
                    gpsSplittingStrategy: "uriEquality()",
                    clock: true //just has to be nonnull, gps data has special treatment
                },
                "move-activity":{
                    mapicon : {
                        url: "/" + FLX_RELEASE_NUMBER + "/images/mapicons/transparentdot.png",
                        anchor: new google.maps.Point(5,5)
                    }
                }
            }
        }
    }

});