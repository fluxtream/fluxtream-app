define(
		[],
		function() {

			var viewModel, dateAxis, dataSource;
		        var yAxis1, yAxis2, yAxis3;

			function render(digest, timeUnit) {
				require(
						[ "text!applications/log/widgets/timeline/timeline.html" ],
						function(template) {
							that = this;
							$("#widgets").append(template);
							$.ajax({
								url : "/bodytrack/users/3/views/get?name=zeo-armband1",
								success : function(data, textStatus,
										jqXHR) {
									renderTimelineWidget(data);
								}
							});
						});
			}


		  function createTimelineDatasource(userId, deviceName, channelName) {
		    var urlPrefix = "/bodytrack/tiles/" + userId + "/" + deviceName + "." + channelName + "/";
		    return function(level, offset, success_callback, failure_callback) {
					var onerr = function(jqXHR, textStatus, errorThrown) {
						try {
							if (failure_callback) {
								failure_callback(errorThrown);
							}
						} catch (ex) {
						}
					};
					$.ajax({
						url : urlPrefix + level + "." + offset
								+ ".json",
						success : function(data, textStatus, jqXHR) {
							try {
								if (success_callback) {
									success_callback(typeof data === 'string' ? data
											: JSON.stringify(data));
								}
							} catch (ex) {
								onerr(jqXHR, "JSON parse error", ex);
							}
						},
						failure : onerr
					});
				};
		    
		  }
			function renderTimelineWidget(data) {
				viewModel = JSON.parse(data);
				// Create x-axis
				
				dateAxis = new DateAxis("_timeline_dateAxis", "horizontal", {
					"min" : viewModel["v2"]["x_axis"]["min"],
					"max" : viewModel["v2"]["x_axis"]["max"]
				});
			  	yAxis1 = new NumberAxis("_timeline_yAxis_1", "vertical", {
					"min" : viewModel["v2"]["y_axes"][0]["min"],
					"max" : viewModel["v2"]["y_axes"][0]["max"]
				});

				yAxis2 = new NumberAxis("_timeline_yAxis_2", "vertical", {
					"min" : viewModel["v2"]["y_axes"][1]["min"],
					"max" : viewModel["v2"]["y_axes"][1]["max"]
				});

			  	yAxis3 = new NumberAxis("_timeline_yAxis_3", "vertical", {
					"min" : viewModel["v2"]["y_axes"][2]["min"],
					"max" : viewModel["v2"]["y_axes"][2]["max"]
				});

			  var plot1 = new DataSeriesPlot(createTimelineDatasource(3, viewModel["v2"]["y_axes"][0]["device_name"], viewModel["v2"]["y_axes"][0]["channel_name"]),
							dateAxis,
							yAxis1,
							viewModel["v2"]["y_axes"][0].style);

			  var plot2 = new DataSeriesPlot(createTimelineDatasource(3, viewModel["v2"]["y_axes"][1]["device_name"], viewModel["v2"]["y_axes"][1]["channel_name"]),
							dateAxis,
							yAxis2,
							viewModel["v2"]["y_axes"][1].style);

			  var plot3 = new DataSeriesPlot(createTimelineDatasource(3, viewModel["v2"]["y_axes"][2]["device_name"], viewModel["v2"]["y_axes"][2]["channel_name"]),
							dateAxis,
							yAxis3,
							viewModel["v2"]["y_axes"][2].style);

			  var plotContainer1 = new PlotContainer("_timeline_pc_1", [plot1]);
			  var plotContainer2 = new PlotContainer("_timeline_pc_2", [plot2]);
			  var plotContainer3 = new PlotContainer("_timeline_pc_3", [plot3]);
			}

			var widget = {};
			widget.render = render;
			return widget;

		});
