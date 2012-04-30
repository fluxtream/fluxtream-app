define([], function() {

	return {

		getConfig : function(edgeWidth, start, end) {
			var ORBIT_RATIO = edgeWidth / 600;

			return {
				start : start,
				end : end,
				START_AT : 90,
				STROKE_WIDTH : 16,
				HOUR : 60,
				RATIO : (60 * 24) / 360,
				CLOCK_WIDTH : edgeWidth,
				CLOCK_HEIGHT : edgeWidth,
				locationHistory: null,
				traveling: false,
				CLOCK_CENTER : [ edgeWidth / 2, edgeWidth / 2 ],

				NO_CATEGORY : {
					orbit : 91 * ORBIT_RATIO,
					color : "#ee80cc"
				},
				BODY_CATEGORY : {
					orbit : 91 * ORBIT_RATIO,
					color : "#9233ef"
				},
				AT_HOME_CATEGORY : {
					orbit : 135 * ORBIT_RATIO,
					color : "#4c99c5"
				},
				IN_TRANSIT_CATEGORY : {
					orbit : 158 * ORBIT_RATIO,
					color : "#22b2b9"
				},
				OUTSIDE_CATEGORY : {
					orbit : 184 * ORBIT_RATIO,
					color : "#5cae5c"
				},
				MIND_CATEGORY : {
					orbit : 208 * ORBIT_RATIO,
					color : "#92da46"
				},
				SOCIAL_CATEGORY : {
					orbit : 231 * ORBIT_RATIO,
					color : "#9233ef"
				},
				MEDIA_CATEGORY : {
					orbit : 255 * ORBIT_RATIO,
					color : "#fd4938"
				},

				clockCircles : null
			}
		}

	}

})