define(["core/DashboardWidget", "core/GrapherComponent"], function(DashboardWidget,GrapherComponent) {

    var StepsHistory = new DashboardWidget();

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

        this.grapher = new GrapherComponent($("#stepsHistoryWidget"), 50, 0, "Fitbit.steps", this.digest.tbounds, "right");

        this.grapher.yAxis.setPaint(function(axis){
            var ctx = axis.canvas.getContext("2d");
            ctx.clearRect(0,0,axis.canvas.width,axis.canvas.height);//clear drawing area

            ctx.fillStyle="black";

            var min = axis.getMin();
            var max = axis.getMax();


            for (var i = 0; i < 10; i++){//draw 10 numbers;
                var value = i * 2000;
                var y = axis.canvas.height - axis.canvas.height * i / 10;
                ctx.fillText(Math.round(value),0,y);
            }
        });
    }

    return StepsHistory;
})