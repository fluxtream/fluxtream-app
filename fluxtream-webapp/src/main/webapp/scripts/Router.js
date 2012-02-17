define(["Flx"], function(Flx) {

	var router;

	var AppRouter = Backbone.Router.extend({
		routes : {
			'log/date/:date' : 'showDate'
		},

		showDate: function(date) {
			Flx.App.gotoDate(date, false);
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