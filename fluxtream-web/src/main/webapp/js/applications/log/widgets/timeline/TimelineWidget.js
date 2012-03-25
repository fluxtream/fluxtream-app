define(
		[],
		function() {
			var viewModel, dateAxis, dataSource;
			var yAxisIndex = 0;
						
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
							if (failure_callack) {
								failure_callback(errorThrown);
							}
						}
						catch (ex) {};
					};
					$.ajax({
						url : urlPrefix + level + "." + offset + ".json",
						success : function(data, textStatus, jqXHR) {
							try {
								if (success_callback) {
									success_callback(typeof data === 'string' ? data : JSON.stringify(data));
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

				console.log(viewModel);
				var i, l;
				var yAxes;
				
				// Create x-axis
				dateAxis = new DateAxis("timeline_channel_dateaxis", "horizontal", {
					"min" : viewModel["v2"]["x_axis"]["min"],
					"max" : viewModel["v2"]["x_axis"]["max"]
				});

				// Create y-axes
				yAxes = viewModel["v2"]["y_axes"];
				l = yAxes.length;
				for (i = 0; i < l; i++) {
					renderTimelineChannel(yAxes[i]);
				}
			}
			
			function renderTimelineChannel(channel) {
				var axis, plot, container;
				var index = yAxisIndex;
				yAxisIndex += 1;
				
				channel["index"] = index;
				$("#timeline_channels").append($.mustache($("#timeline_channel_tmpl").html(), channel));
				
				axis = new NumberAxis("timeline_channel_yaxis_" + index, "vertical", {
					"min" : channel["min"],
					"max" : channel["max"]
				});
				
				plot = new DataSeriesPlot(createTimelineDatasource(3, channel["device_name"], channel["channel_name"]),
						dateAxis, axis, channel["style"]);
				
				container = new PlotContainer("timeline_channel_plot_" + index, [plot]);
			}
			
			var widget = {};
			widget.render = render;
			return widget;
		});
