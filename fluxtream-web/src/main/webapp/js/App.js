define([], function() {
		
		function initialize() {
			require(["applications/calendar/Calendar", "text!applications/calendar/template.html"],
				    function(Calendar, html) {
						$(".application").empty();
						$(".application").append(html);
						Calendar.initialize();
				    }
				);
		}
		
		var App = {};
		App.initialize = initialize;
		window.App = App;
		return App;
		
    }
);
