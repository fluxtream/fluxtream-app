define([], function() {
	
	var ClockWidget = Backbone.View.extend({
		
		el: $("#widgets"),
		
		initialize: function() {
			_.bindAll(this, "render");
		},
		
		render: function() {
			$(this.el).append("<div class=\"span12\">haha, this is the widget</div>");
		}
		
	});
	
	var clockWidget = new ClockWidget();
	return clockWidget;
	
});
