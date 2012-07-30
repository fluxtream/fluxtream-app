define(["core/grapher/BTCore"],function(BodyTrack){

    var defaultStyle = {"styles":[{"type":"line","lineWidth":1}]};

    var GrapherComponent = function(parentElement, channelName, tbounds, options){
        if (options == null)
            options = {};

        var yAxisWidth = options.yAxisWidth == null ? 0 : options.yAxisWidth;
        var xAxisHeight = options.xAxisHeight == null ? 0 : options.xAxisHeight;
        var xAxisPosition = options.xAxisPosition == null ? "bottom" : options.xAxisPosition;
        var yAxisPosition = options.yAxisPosition == null ? "right" : options.yAxisPosition;
        var positiveOnly = options.positiveOnly == null ? false : options.positiveOnly;
        var grapherStyle = options.style == null ? defaultStyle : options.style;

        var component = this;
        this.channelName = channelName;
        this.parent = $(parentElement);
        var width = this.parent.width() - yAxisWidth;
        var height = this.parent.height() - xAxisHeight;
        this.width = width;
        this.height = height;
        this.tbounds = tbounds;


        var dateAxisDivId = this.parent.attr("id") + "_GrapherDateAxis";
        var numberAxisDivId = this.parent.attr("id") + "_GrapherNumberAxis";
        var plotContainerDivId = this.parent.attr("id") + "_GrapherPlotContainer";


        this.dateAxisContainer = $("<div id=\"" + dateAxisDivId + "\"></div>");
        this.numberAxisContainer = $("<div id=\"" + numberAxisDivId + "\"></div>");
        this.plotContainerContainer = $("<div id=\"" + plotContainerDivId + "\"></div>");



        this.dateAxisContainer.css("width",width + "px");
        this.dateAxisContainer.css("height",height + "px");
        this.numberAxisContainer.css("width",width + "px");
        this.numberAxisContainer.css("height",height + "px");
        this.plotContainerContainer.css("width",width + "px");
        this.plotContainerContainer.css("height",height + "px");
        this.plotContainerContainer.css("display","inline-block");

        this.dateAxisContainer.css("left","-" + (width * 2) + "px");
        this.numberAxisContainer.css("left","-" + (width * 2) + "px");

        this.dateAxisContainer.css("position","fixed");
        this.numberAxisContainer.css("position","fixed");


        var yCanvas = $("<canvas width='" + yAxisWidth+ "' height='" + height + "'></canvas>");

        switch (yAxisPosition){
            default:
            case "left":
                this.parent.append(yCanvas);
                this.parent.append(this.plotContainerContainer);
                break;
            case "right":
                this.parent.append(this.plotContainerContainer);
                this.parent.append(yCanvas);
                break;
        }

        this.parent.append(this.dateAxisContainer);
        this.parent.append(this.numberAxisContainer);



        this.xAxis = new DateAxis(this.dateAxisContainer.attr('id'),"horizontal",{min:tbounds.start/1000,max:tbounds.end/1000});
        this.yAxis = new NumberAxis(this.numberAxisContainer.attr('id'),"vertical");

        this.yAxis.canvas = yCanvas;

        this.xAxis.canvas = $("<canvas width='" + width + "' height='" + xAxisHeight + "'></canvas>");
        this.xAxis.canvas.css("position","relative");
        this.xAxis.canvas.css("top","-8px");
        this.parent.append(this.xAxis.canvas);

        this.xAxis.canvas = this.xAxis.canvas[0];
        this.yAxis.canvas = this.yAxis.canvas[0];

        this.xAxis.paint = function(){};
        this.yAxis.paint = function(){};

        this.xAxis.addAxisChangeListener(function(){
            component.xAxis.paint(component.xAxis);
        });

        this.yAxis.addAxisChangeListener(function(){
            component.yAxis.paint(component.yAxis);
        });

        this.xAxis.setPaint = function(paintFunc){
            component.xAxis.paint = paintFunc;
            component.xAxis.paint(component.xAxis);
        };

        this.yAxis.setPaint = function(paintFunc){
            component.yAxis.paint = paintFunc;
            component.yAxis.paint(component.yAxis);
        }



        var periodLocation = channelName.indexOf(".");
        var deviceName = channelName.substring(0,periodLocation);
        var subChannelName = channelName.substring(periodLocation+1);

        component.plot = new DataSeriesPlot(channelDatasource(App.getUID(), deviceName, subChannelName),component.xAxis,component.yAxis,grapherStyle);
        component.parent.css("opacity",0);

        var afterload = function(stats){
            if (stats != null && stats.has_data){
                var yMax = stats.y_max;
                var yMin = positiveOnly ? 0 : stats.y_min;
                var yDiff = yMax - yMin;
                var bounds;
                if(yDiff < 1e-10) {
                    bounds = [yMin - 0.5, yMin + 0.5];
                } else {
                    var padding = 0.075 * yDiff;
                    bounds = [positiveOnly ? yMin : yMin - padding, yMax + padding];
                }
                if (bounds[0] == component.yAxis.getMin() && bounds[1] == component.yAxis.getMax()){
                    $.doTimeout(10,function(){
                        component.parent.css("opacity",1);
                    })
                    return;
                }
                component.yAxis.setRange(bounds[0], bounds[1]);
                component.plot.setStyle( component.plot.getStyle()); // Trigger a repaint)
            }
            else if (stats != null && stats.data_pending == false){
                component.parent.css("opacity",1);
                return;
            }
            $.doTimeout(10,getStats);
            return;
        };

        var getStats = function(){
            afterload(component.plot.getStatistics( component.xAxis.getMin(), component.xAxis.getMax(),["has_data", "y_max", "y_min"],afterload));
        }
        getStats();

        component.plotContainer = new PlotContainer(component.plotContainerContainer.attr('id'), true,[ component.plot]);



    };

    return GrapherComponent;
});