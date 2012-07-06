define(["core/DashboardWidget","applications/calendar/tabs/timeline/BodyTrack"], function(DashboardWidget,BodyTrack) {

    var StepsHistory = new DashboardWidget();

    var grapherStyle = {"comments":{"show":false,"styles":[{"type":"point","show":false,"lineWidth":1,"radius":3,"color":"rgb(0, 102, 0)","fill":true,"fillColor":"rgb(0, 102, 0)"}],"verticalMargin":4},"styles":[{"type":"line","show":false,"color":"rgb(0, 102, 0)","lineWidth":1},{"type":"lollipop","show":true,"lineWidth":5,"radius":0,"color":"rgb(0, 102, 0)","fill":false},{"type":"point","show":false,"lineWidth":1,"radius":2,"color":"rgb(0, 102, 0)","fill":true,"fillColor":"rgb(0, 102, 0)"},{"type":"value","show":true,"fillColor":"rgb(0, 102, 0)","marginWidth":5,"verticalOffset":7,"numberFormat":"###,##0"}],"highlight":{"styles":[{"type":"value","show":true,"fillColor":"rgb(0, 102, 0)","marginWidth":5,"verticalOffset":7,"numberFormat":"###,##0"}],"lineWidth":6}};

    StepsHistory.init = function() {
        require(["text!" + this.manifest.WidgetRepositoryURL + "/"
                + this.manifest.WidgetName + "/stepsHistory.mustache"], function(template) {
            StepsHistory.postLoad(template);
        });
    };

    StepsHistory.postLoad = function(template) {
        var html = Hogan.compile(template);
        $("#stepsHistory-widget .flx-body").empty();
        $("#stepsHistory-widget .flx-body").append(
            html.render({"manifest" : this.manifest})
        );

        var tbounds = this.digest.tbounds;

        var dateAxis = new DateAxis("stepsHistoryDateAxis","horizontal",{min:tbounds.start/1000,max:tbounds.end/1000});
        var numberAxis = new NumberAxis("stepsHistoryNumberAxis","vertical");

        /*dateAxis.addAxisChangeListener(function(){
            if (dateAxis.getMin() != tbounds.start/1000 || dateAxis.getMax() != tbounds.end/1000)
                dateAxis.setRange(tbounds.start/1000,tbounds.end/1000);
        });*/


        BodyTrack.LOGIN.getStatus(function(status){
            var plot = new DataSeriesPlot(channelDatasource(status.user_id, "Fitbit", "steps"),dateAxis,numberAxis,grapherStyle);

            var afterload = function(stats){
                if (stats.has_data){
                    var yMax = stats.y_max;
                    var yMin = 0;
                    var yDiff = yMax - yMin;
                    if(yDiff < 1e-10) {
                        numberAxis.setRange(yMin, yMin + 0.5);
                    } else {
                        var padding = 0.2 * yDiff;
                        numberAxis.setRange(yMin, yMax + padding);
                    }
                    plot.setStyle(plot.getStyle()); // Trigger a repaint)
                }
            };

            $.doTimeout(100,function(){
                afterload(plot.getStatistics(dateAxis.getMin(),dateAxis.getMax(),["has_data", "y_max"],afterload));
            });


            var plotContainer = new PlotContainer("stepsHistroyGraph",[plot]);

        });


    }

    return StepsHistory;
})