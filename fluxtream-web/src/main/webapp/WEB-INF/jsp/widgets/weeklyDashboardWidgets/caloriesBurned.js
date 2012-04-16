define(function() {

	function render(digest, dashboardData) {
		var s1 = [ 200, 600, 700, 1000 ];
		var s2 = [ 460, -210, 690, 820 ];
		var s3 = [ -260, -440, 320, 200 ];
		// Can specify a custom tick Array.
		// Ticks should match up one for each y value (category) in the series.
		var ticks = [ 'May', 'June', 'July', 'August' ];

		var plot1 = $.jqplot('chart1', [ s1, s2, s3 ], {
			// The "seriesDefaults" option is an options object that will
			// be applied to all series in the chart.
			seriesDefaults : {
				renderer : $.jqplot.BarRenderer,
				rendererOptions : {
					fillToZero : true
				}
			},
			// Custom labels for the series are specified with the "label"
			// option on the series option. Here a series option object
			// is specified for each series.
			series : [ {
				label : 'Motel'
			}, {
				label : 'Event Regristration'
			}, {
				label : 'Airplane'
			} ],
			axes : {
				// Use a category axis on the x axis and use our custom ticks.
				xaxis : {
					renderer : $.jqplot.CategoryAxisRenderer,
					ticks : ticks
				},
				// Pad the y axis just a little so bars can get close to, but
				// not touch, the grid boundaries. 1.2 is the default padding.
				yaxis : {
					pad : 1.05,
					tickOptions : {
						formatString : '$%d'
					}
				}
			}
		});
	}

	var CaloriesBurned = {};
	CaloriesBurned.render = render;
	return CaloriesBurned;

});