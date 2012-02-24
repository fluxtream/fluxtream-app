define([], function() {

	var router;

	var AppRouter = Backbone.Router.extend({
		routes : {
			'home/date/:date' : 'showDate'
		},

		showDate: function(date) {
			window.App.gotoDate(date, false);
		}
		
	});

	function initialize() {
		router = new AppRouter();
		Backbone.history.start({
			pushState : true
		});
	};
	
	function navigate(to) {
		router.navigate(to);
	}
	
	return {
		initialize : initialize,
		navigate: navigate
	};
});