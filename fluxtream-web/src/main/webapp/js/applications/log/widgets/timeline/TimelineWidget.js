define([], function() {
	
        var viewModel, dateAxis, dataSource;
        var yAxis;
       
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
	        yAxis = new NumberAxis("_timeline_yAxis", "vertical", {
	        	"min" : 0,
	    		"max" : 1
	        });
                dataSource = function(level, offset, success_callback, failure_callback) {
		  var urlPrefix = "/bodytrack/tiles/3/" + deviceName + "." + channelName + "/";
                  var onerr = function(jqXHR, textStatus, errorThrown) {
		    try {
		      if (failure_callback) {
			failure_callback(errorThrown);
		      }
		    }
		    catch (ex) {
		    }
		  };
		  $.ajax({
		    url     : urlPrefix + level + "." + offset + ".json",
                    success : function(data, textStatus, jqXHR) {
		      try {
			if (success_callback) {
			  success_callback(typeof data === 'string'
					   ? data : JSON.stringify(data));
			}
		      }
		      catch (ex) {
			onerr(jqXHR, "JSON parse error", ex);
		      }
		    },
		    failure : onerr
		  });
		};
	  
	       var plot = new DataSeriesPlot(dataSource, dateAxis, yAxis, {});
               new PlotContainer("pc", [plot]);
	}
  
	var widget = {};
	widget.render = render;
	return widget;
	
});
