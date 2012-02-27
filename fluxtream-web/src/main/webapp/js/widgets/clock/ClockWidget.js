define(["widgets/clock/ClockdrawingUtils", "widgets/clock/ClockConfig"], function(DrawingUtils, Config) {
	
	var ClockWidget = Backbone.View.extend({
		
		el: $("#widgets"),
		
		initialize: function() {
			_.bindAll(this, "render");
		},
		
		render: function() {
			console.log("rendering clock widget");	
			this.el.append("<div id=\"clockWidget\"class=\"span12\"><div id=\"paper\"></div></div>");
			var availableWidth = $("#clockWidget").width();
			var edgeWidth =  Math.min(availableWidth, 600);
			$("#paper").width(edgeWidth);
			var paper = Raphael("paper", edgeWidth, edgeWidth);
			var config = Config.getConfig(edgeWidth);
			var drawingUtils = DrawingUtils.getDrawingUtils(config);
			config.clockCircles = paper.set();
			drawingUtils.paintCircle(paper, config.BODY_CATEGORY.orbit, "#ff0000", 1);
			drawingUtils.paintCircle(paper, config.AT_HOME_CATEGORY.orbit, "#ff0000", 1);
			drawingUtils.paintCircle(paper, config.OUTSIDE_CATEGORY.orbit, "#ff0000", 1);
			drawingUtils.paintCircle(paper, config.MIND_CATEGORY.orbit, "#ff0000", 1);
			drawingUtils.paintCircle(paper, config.SOCIAL_CATEGORY.orbit, "#ff0000", 1);
			drawingUtils.paintCircle(paper, config.MEDIA_CATEGORY.orbit, "#ff0000", 1);
		}
		
	});
	
	var clockWidget = new ClockWidget();
	return clockWidget;
	
});
