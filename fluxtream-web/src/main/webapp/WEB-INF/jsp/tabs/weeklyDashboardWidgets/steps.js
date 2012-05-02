define(function() {

    var digest, dashboardData;

    function render(d, dbd) {
        digest = d;
        dashboardData = dbd;
        setTimeout(renderForGood, 2000);
    }

    function renderForGood() {
        var style = {"comments":{"show":true,"styles":[{"type":"point","show":true,"lineWidth":1,"radius":3,"color":"rgb(255, 0, 0)","fill":true,"fillColor":"rgb(255, 0, 0)"}],"verticalMargin":4},"styles":[{"type":"line","show":false,"color":"rgb(255, 0, 0)","lineWidth":1},{"type":"lollipop","show":true,"lineWidth":5,"radius":0,"color":"rgb(255, 0, 0)","fill":false},{"type":"point","show":false,"lineWidth":1,"radius":2,"color":"rgb(255, 0, 0)","fill":true,"fillColor":"rgb(255, 0, 0)"},{"type":"value","show":true,"fillColor":"rgb(255, 0, 0)","marginWidth":5,"verticalOffset":7,"numberFormat":"###,##0.0##"}],"highlight":{"styles":[{"type":"value","show":true,"fillColor":"rgb(255, 0, 0)","marginWidth":5,"verticalOffset":7,"numberFormat":"###,##0.0##"}],"lineWidth":6}},
            min = 1331766000, max = 1333749600,
            dateAxis = new DateAxis("steps_dateAxis", "horizontal", {
            "min" : min,
            "max" : max
            }),
            yAxis = new NumberAxis("steps_yAxis", "vertical", {
                "min" : min,
                "max" : max
            });
        var plot = new DataSeriesPlot(channelDatasource(dashboardData.steps.user_id, "Fitbit", "steps"),
                                  dateAxis,
                                  yAxis,
                                  style);
        var plotContainer = new PlotContainer("stepsPlot", [plot]);

        var xMin = dateAxis.getMin();
        var xMax = dateAxis.getMax();

        var afterload = function(stats) {
            if (stats["has_data"]) {
                var yMin = stats["y_min"];
                var yMax = stats["y_max"];
                var yDiff = yMax - yMin;
                if(yDiff < 1e-10) {
                    yAxis.setRange(yMin - 0.5, yMin + 0.5);
                } else {
                    var padding = 0.1 * yDiff;
                    yAxis.setRange(yMin - padding, yMax + padding);
                }
                plot.setStyle(plot.getStyle()); // Trigger a repaint
            }
        };

        var initialStats = plot.getStatistics(xMin, xMax,
                                              ["has_data", "y_min", "y_max"],
                                              afterload);
        if (!("data_pending" in initialStats)
            || (!initialStats["data_pending"])) {
            afterload(initialStats);
        }


    }

    function channelDatasource(userId, deviceName, channelName) {
        var urlPrefix = "/bodytrack/tiles/" + userId + "/" + deviceName + "."
                            + channelName + "/";
        return __createDatasource(urlPrefix);
    }

    function __createDatasource(urlPrefix, urlParams) {
        if (!urlParams) {
            urlParams = {};
        }
        return function(level, offset, success_callback, failure_callback) {
            var onerr = function(jqXHR, textStatus, errorThrown) {
                try {
                    if (failure_callback) {
                        failure_callback(errorThrown);
                    }
                }
                catch (ex) {
                    console.log("channelDatasource.onErr(): FAILURE! ex:" + ex);
                }
            };
            $.ajax({
                       url     : urlPrefix + level + "." + offset + ".json",
                       data    : urlParams,
                       success : function(data, textStatus, jqXHR) {
                           try {
                               if (success_callback) {
                                   // we must always send the JSON as a String...
                                   success_callback(typeof data === 'string' ? data : JSON.stringify(data));
                               }
                           }
                           catch (ex) {
                               onerr(jqXHR, "JSON parse error", ex);
                           }
                       },
                       failure : onerr
                   });
        };
    };

    var Steps = {};
    Steps.render = render;
    return Steps;

});