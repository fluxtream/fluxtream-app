require(['App', 'Connectors'], function(App, Connectors) {
	document.body.onselectstart = function() { return false; };
	document.body.style.MozUserSelect = "none";
	document.body.style.KhtmlUserSelect = "none";
	document.body.unselectable = "on";
	App.initialize();
});