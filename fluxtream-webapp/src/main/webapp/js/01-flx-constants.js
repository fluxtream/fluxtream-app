var FlxState = {

	START_AT : 90,
	STROKE_WIDTH : 16,
	HOUR : 60,
	RATIO : (60 * 24) / 360,
	TIMELINE_WIDTH : 876,
	TIMELINE_HEIGHT : 30,
	CLOCK_WIDTH : 600,
	CLOCK_HEIGHT : 620,
	CLOCK_CENTER : [ 300, 310 ],

	NO_CATEGORY : {
		orbit : 91,
		lane : 15,
		color : "#ee80cc"
	},
	BODY_CATEGORY : {
		orbit : 91,
		lane : 75,
		color : "#9233ef"
	},
	AT_HOME_CATEGORY : {
		orbit : 135,
		lane : 100,
		color : "#4c99c5"
	},
	IN_TRANSIT_CATEGORY : {
		orbit : 158,
		lane : 125,
		color : "#22b2b9"
	},
	OUTSIDE_CATEGORY : {
		orbit : 184,
		lane : 150,
		color : "#5cae5c"
	},
	MIND_CATEGORY : {
		orbit : 208,
		lane : 175,
		color : "#92da46"
	},
	SOCIAL_CATEGORY : {
		orbit : 231,
		lane : 200,
		color : "#9233ef"
	},
	MEDIA_CATEGORY : {
		orbit : 255,
		lane : 225,
		color : "#fd4938"
	},

	map : null,
	positionMarker : null,
	weather : null,
	paper : null,
	clockCircles : null,
	timelineLanes : null,
	start : 0,
	end : 0,
	savedConnectorContent : "",
	timeHash : null,
	previous_vtype : null,
	vtype : null,
	locationHistory : null,
	lastHoveredEvent : null,
	state : null,
	homeAddress : null,
	homeMarker : null,
	hasLatitude : false,
	cities : null,
	traveling : false
}

var flxHomeImage = new google.maps.MarkerImage('/static/images/homeImage.png',
		new google.maps.Size(32, 37), new google.maps.Point(0, 0),
		new google.maps.Point(16, 37));

var flxHomeShadow = new google.maps.MarkerImage('/static/images/homeShadow.png',
		new google.maps.Size(54, 37), new google.maps.Point(0, 0),
		new google.maps.Point(16, 37));

var flxHomeShape = {
	coord : [ 29, 0, 30, 1, 31, 2, 31, 3, 31, 4, 31, 5, 31, 6, 31, 7, 31, 8,
			31, 9, 31, 10, 31, 11, 31, 12, 31, 13, 31, 14, 31, 15, 31, 16, 31,
			17, 31, 18, 31, 19, 31, 20, 31, 21, 31, 22, 31, 23, 31, 24, 31, 25,
			31, 26, 31, 27, 31, 28, 31, 29, 30, 30, 29, 31, 23, 32, 22, 33, 21,
			34, 20, 35, 19, 36, 12, 36, 11, 35, 10, 34, 9, 33, 8, 32, 2, 31, 1,
			30, 0, 29, 0, 28, 0, 27, 0, 26, 0, 25, 0, 24, 0, 23, 0, 22, 0, 21,
			0, 20, 0, 19, 0, 18, 0, 17, 0, 16, 0, 15, 0, 14, 0, 13, 0, 12, 0,
			11, 0, 10, 0, 9, 0, 8, 0, 7, 0, 6, 0, 5, 0, 4, 0, 3, 0, 2, 1, 1, 2,
			0, 29, 0 ],
	type : 'poly'
};
