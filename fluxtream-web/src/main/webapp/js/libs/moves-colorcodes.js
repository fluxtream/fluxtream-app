define(function() {

    var MovesColors = {};
    MovesColors.activities =
    [
        {
            "activity": "aerobics",
            "geo": false,
            "place": true,
            "color": "bc4fff",
            "units": "duration,calories"
        },
        {
            "activity": "badminton",
            "geo": false,
            "place": true,
            "color": "11d1cb",
            "units": "duration,calories"
        },
        {
            "activity": "baseball",
            "geo": false,
            "place": true,
            "color": "fa7070",
            "units": "duration,calories"
        },
        {
            "activity": "basketball",
            "geo": false,
            "place": true,
            "color": "fc6f0a",
            "units": "duration,calories"
        },
        {
            "activity": "beach_volleyball",
            "geo": false,
            "place": true,
            "color": "ffb938",
            "units": "duration,calories"
        },
        {
            "activity": "bodypump",
            "geo": false,
            "place": true,
            "color": "d1416c",
            "units": "duration,calories"
        },
        {
            "activity": "bowling",
            "geo": false,
            "place": true,
            "color": "d68b00",
            "units": "duration,calories"
        },
        {
            "activity": "boxing",
            "geo": false,
            "place": true,
            "color": "c93838",
            "units": "duration,calories"
        },
        {
            "activity": "circuit_training",
            "geo": false,
            "place": true,
            "color": "eb4db4",
            "units": "duration,calories"
        },
        {
            "activity": "cleaning",
            "geo": false,
            "place": true,
            "color": "96cc00",
            "units": "duration,calories"
        },
        {
            "activity": "climbing",
            "geo": false,
            "place": true,
            "color": "c96a26",
            "units": "duration,calories"
        },
        {
            "activity": "cricket",
            "geo": false,
            "place": true,
            "color": "96cc00",
            "units": "duration,calories"
        },
        {
            "activity": "cross_country_skiing",
            "geo": true,
            "place": true,
            "color": "2183b8",
            "units": "distance,duration,calories"
        },
        {
            "activity": "cycling",
            "group": "cycling",
            "geo": true,
            "place": true,
            "color": "00cdec",
            "units": "distance,duration,calories"
        },
        {
            "activity": "dancing",
            "geo": false,
            "place": true,
            "color": "963fcc",
            "units": "duration,calories"
        },
        {
            "activity": "downhill_skiing",
            "geo": true,
            "place": true,
            "color": "00a6ff",
            "units": "distance,duration,calories"
        },
        {
            "activity": "elliptical_training",
            "geo": false,
            "place": true,
            "color": "bc4fff",
            "units": "duration,calories"
        },
        {
            "activity": "fencing",
            "geo": false,
            "place": true,
            "color": "42bdff",
            "units": "duration,calories"
        },
        {
            "activity": "floorball",
            "geo": false,
            "place": true,
            "color": "22b5b0",
            "units": "duration,calories"
        },
        {
            "activity": "american_football",
            "geo": false,
            "place": true,
            "color": "c93838",
            "units": "duration,calories"
        },
        {
            "activity": "golfing",
            "geo": true,
            "place": true,
            "color": "44c42d",
            "units": "steps,distance,duration,calories"
        },
        {
            "activity": "gym_training",
            "geo": false,
            "place": true,
            "color": "e82c64",
            "units": "duration,calories"
        },
        {
            "activity": "gymnastics",
            "geo": false,
            "place": true,
            "color": "9c27e6",
            "units": "duration,calories"
        },
        {
            "activity": "handball",
            "geo": false,
            "place": true,
            "color": "3e5ec7",
            "units": "duration,calories"
        },
        {
            "activity": "hockey",
            "geo": false,
            "place": true,
            "color": "22b5b0",
            "units": "duration,calories"
        },
        {
            "activity": "indoor_cycling",
            "group": "cycling",
            "geo": false,
            "place": true,
            "color": "00cdec",
            "units": "duration,calories"
        },
        {
            "activity": "kayaking",
            "geo": true,
            "place": true,
            "color": "1390d4",
            "units": "distance,duration,calories"
        },
        {
            "activity": "kettlebell",
            "geo": false,
            "place": true,
            "color": "eb4db4",
            "units": "duration,calories"
        },
        {
            "activity": "kite_surfing",
            "geo": false,
            "place": true,
            "color": "00a6ff",
            "units": "duration,calories"
        },
        {
            "activity": "martial_arts",
            "geo": false,
            "place": true,
            "color": "fa4646",
            "units": "duration,calories"
        },
        {
            "activity": "paddling",
            "geo": true,
            "place": true,
            "color": "1390d4",
            "units": "distance,duration,calories"
        },
        {
            "activity": "paintball",
            "geo": true,
            "place": true,
            "color": "e82c64",
            "units": "distance,duration,calories"
        },
        {
            "activity": "pilates",
            "geo": false,
            "place": true,
            "color": "ff82a8",
            "units": "duration,calories"
        },
        {
            "activity": "polo",
            "geo": false,
            "place": true,
            "color": "fa4646",
            "units": "duration,calories"
        },
        {
            "activity": "riding",
            "geo": true,
            "place": true,
            "color": "995e34",
            "units": "distance,duration,calories"
        },
        {
            "activity": "roller_skiing",
            "geo": true,
            "place": true,
            "color": "2183b8",
            "units": "distance,duration,calories"
        },
        {
            "activity": "rollerblading",
            "geo": true,
            "place": true,
            "color": "e0bb00",
            "units": "distance,duration,calories"
        },
        {
            "activity": "rollerskating",
            "geo": true,
            "place": true,
            "color": "ffa361",
            "units": "distance,duration,calories"
        },
        {
            "activity": "rowing",
            "geo": true,
            "place": true,
            "color": "1390d4",
            "units": "distance,duration,calories"
        },
        {
            "activity": "rugby",
            "geo": false,
            "place": true,
            "color": "238755",
            "units": "duration,calories"
        },
        {
            "activity": "running",
            "group": "running",
            "geo": true,
            "place": true,
            "color": "f660f4",
            "units": "steps,distance,duration,calories"
        },
        {
            "activity": "running_on_treadmill",
            "group": "running",
            "geo": false,
            "place": true,
            "color": "f660f4",
            "units": "steps,duration,calories"
        },
        {
            "activity": "scuba_diving",
            "geo": false,
            "place": true,
            "color": "11d1cb",
            "units": "duration,calories"
        },
        {
            "activity": "skateboarding",
            "geo": true,
            "place": true,
            "color": "ff8c3b",
            "units": "distance,duration,calories"
        },
        {
            "activity": "skating",
            "geo": true,
            "place": true,
            "color": "1390d4",
            "units": "distance,duration,calories"
        },
        {
            "activity": "snowboarding",
            "geo": true,
            "place": true,
            "color": "8765f7",
            "units": "distance,duration,calories"
        },
        {
            "activity": "snowshoeing",
            "geo": true,
            "place": true,
            "color": "3360f2",
            "units": "steps,distance,duration,calories"
        },
        {
            "activity": "soccer",
            "geo": false,
            "place": true,
            "color": "4a963c",
            "units": "duration,calories"
        },
        {
            "activity": "spinning",
            "geo": false,
            "place": true,
            "color": "ffa600",
            "units": "duration,calories"
        },
        {
            "activity": "squash",
            "geo": false,
            "place": true,
            "color": "ab91ff",
            "units": "duration,calories"
        },
        {
            "activity": "stair_climbing",
            "geo": false,
            "place": true,
            "color": "ffa600",
            "units": "duration,calories"
        },
        {
            "activity": "stretching",
            "geo": false,
            "place": true,
            "color": "8ca7ff",
            "units": "duration,calories"
        },
        {
            "activity": "surfing",
            "geo": false,
            "place": true,
            "color": "00a6ff",
            "units": "duration,calories"
        },
        {
            "activity": "swimming",
            "geo": false,
            "place": true,
            "color": "42bdff",
            "units": "duration,calories"
        },
        {
            "activity": "table_tennis",
            "geo": false,
            "place": true,
            "color": "3360f2",
            "units": "duration,calories"
        },
        {
            "activity": "tennis",
            "geo": false,
            "place": true,
            "color": "ffb938",
            "units": "duration,calories"
        },
        {
            "activity": "volleyball",
            "geo": false,
            "place": true,
            "color": "8ca7ff",
            "units": "duration,calories"
        },
        {
            "activity": "walking",
            "group": "walking",
            "geo": true,
            "place": true,
            "color": "00d55a",
            "units": "steps,distance,duration,calories"
        },
        {
            "activity": "walking_on_treadmill",
            "group": "walking",
            "geo": false,
            "place": true,
            "color": "00d55a",
            "units": "steps,duration,calories"
        },
        {
            "activity": "weight_training",
            "geo": false,
            "place": true,
            "color": "cc68c9",
            "units": "duration,calories"
        },
        {
            "activity": "wheel_chair",
            "geo": true,
            "place": true,
            "color": "358f8c",
            "units": "distance,duration,calories"
        },
        {
            "activity": "windsurfing",
            "geo": false,
            "place": true,
            "color": "00a6ff",
            "units": "duration,calories"
        },
        {
            "activity": "yoga",
            "geo": false,
            "place": true,
            "color": "a655a3",
            "units": "duration,calories"
        },
        {
            "activity": "zumba",
            "geo": false,
            "place": true,
            "color": "fa5788",
            "units": "duration,calories"
        },
        {
            "activity": "airplane",
            "group": "transport",
            "geo": true,
            "place": false,
            "color": "848484",
            "units": "distance,duration"
        },
        {
            "activity": "boat",
            "group": "transport",
            "geo": true,
            "place": false,
            "color": "848484",
            "units": "distance,duration"
        },
        {
            "activity": "bus",
            "group": "transport",
            "geo": true,
            "place": false,
            "color": "848484",
            "units": "distance,duration"
        },
        {
            "activity": "car",
            "group": "transport",
            "geo": true,
            "place": false,
            "color": "848484",
            "units": "distance,duration"
        },
        {
            "activity": "escalator",
            "group": "transport",
            "geo": true,
            "place": false,
            "color": "848484",
            "units": "distance,duration"
        },
        {
            "activity": "ferry",
            "group": "transport",
            "geo": true,
            "place": false,
            "color": "848484",
            "units": "distance,duration"
        },
        {
            "activity": "funicular",
            "group": "transport",
            "geo": true,
            "place": false,
            "color": "848484",
            "units": "distance,duration"
        },
        {
            "activity": "motorcycle",
            "group": "transport",
            "geo": true,
            "place": false,
            "color": "848484",
            "units": "distance,duration"
        },
        {
            "activity": "sailing",
            "group": "transport",
            "geo": true,
            "place": false,
            "color": "848484",
            "units": "distance,duration"
        },
        {
            "activity": "scooter",
            "group": "transport",
            "geo": true,
            "place": false,
            "color": "848484",
            "units": "distance,duration"
        },
        {
            "activity": "transport",
            "group": "transport",
            "geo": true,
            "place": false,
            "color": "848484",
            "units": "distance,duration"
        },
        {
            "activity": "train",
            "group": "transport",
            "geo": true,
            "place": false,
            "color": "848484",
            "units": "distance,duration"
        },
        {
            "activity": "tram",
            "group": "transport",
            "geo": true,
            "place": false,
            "color": "848484",
            "units": "distance,duration"
        },
        {
            "activity": "underground",
            "group": "transport",
            "geo": true,
            "place": false,
            "color": "848484",
            "units": "distance,duration"
        },
        {
            "activity": "bandy",
            "geo": false,
            "place": true,
            "color": "22b5b0",
            "units": "duration,calories"
        },
        {
            "activity": "curling",
            "geo": false,
            "place": true,
            "color": "11d1cb",
            "units": "duration,calories"
        },
        {
            "activity": "ballet",
            "geo": false,
            "place": true,
            "color": "ff82a8",
            "units": "duration,calories"
        },
        {
            "activity": "disc_ultimate",
            "geo": false,
            "place": true,
            "color": "4a963c",
            "units": "duration,calories"
        },
        {
            "activity": "lacrosse",
            "geo": false,
            "place": true,
            "color": "4a963c",
            "units": "duration,calories"
        },
        {
            "activity": "parkour",
            "geo": false,
            "place": true,
            "color": "c96a26",
            "units": "duration,calories"
        },
        {
            "activity": "petanque",
            "geo": false,
            "place": true,
            "color": "42bdff",
            "units": "duration,calories"
        },
        {
            "activity": "racquetball",
            "geo": false,
            "place": true,
            "color": "2183b8",
            "units": "duration,calories"
        },
        {
            "activity": "wrestling",
            "geo": false,
            "place": true,
            "color": "fc6f0a",
            "units": "duration,calories"
        },
        {
            "activity": "water_polo",
            "geo": false,
            "place": true,
            "color": "cc68c9",
            "units": "duration,calories"
        }
    ];

    return MovesColors;
});