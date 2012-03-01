define(function() {
	
	function Application(name, author, icon) {
		this.name = name;
		this.author = author;
		this.icon = icon;
	}
	
	Application.prototype.destroy = function() {
		console.log("WARNING: DESTROY IS NOT IMPLEMENTED!!!")
	}
	
	Application.prototype.render= function() {
		console.log("WARNING: RENDER IS NOT IMPLEMENTED!!!")
	}

	return Application;
	
});