define([], function() {

	return {

		getConfig : function(edgeWidth, start, end) {

			return {
                ORBIT_RATIO: edgeWidth / 600,
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
					orbit : 91,
					color : "#ee80cc"
				},
				BODY_CATEGORY : {
					orbit : 91,
					color : "#9233ef"
				},
				AT_HOME_CATEGORY : {
					orbit : 135,
					color : "#4c99c5"
				},
                AT_WORK_CATEGORY : {
					orbit : 158,
					color : "#22b2b9"
				},
				OUTSIDE_CATEGORY : {
					orbit : 184,
					color : "#5cae5c"
				},
				MIND_CATEGORY : {
					orbit : 208,
					color : "#92da46"
				},
				SOCIAL_CATEGORY : {
					orbit : 231,
					color : "#9233ef"
				},
				MEDIA_CATEGORY : {
					orbit : 255,
					color : "#fd4938"
				},

				clockCircles : null
			}
		}

	}

})