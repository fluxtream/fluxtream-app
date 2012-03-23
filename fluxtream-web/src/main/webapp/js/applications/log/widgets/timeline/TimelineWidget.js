define([], function() {
	
	var viewModel, dateAxis;
	
	function render(digest, timeUnit) {
		require(["text!applications/log/widgets/timeline/timeline.html"], function(template) {
			console.log("got timeline template");
			that = this;
			$("#widgets").append(template);
			$.ajax({
				url: "/bodytrack/users/3/views/get?name=zeo-armband1",
				success: function(data, textStatus, jqXHR) {
					renderTimelineWidget(data);
				}
			});
		});
	}
	
	function renderTimelineWidget(data) {
		viewModel = JSON.parse(data);
		console.log(viewModel.v2);
		  // Create x-axis
		dateAxis = new DateAxis("_timeline_dateAxis", "horizontal", {
			"min" : viewModel["v2"]["x_axis"]["min"],
			"max" : viewModel["v2"]["x_axis"]["max"]
		});
	}
	
	var widget = {};
	widget.render = render;
	return widget;
	
});
