define(function() {
	
	function privateFunction() {
		
	}

	var MyModule = {};
	MyModule.publicFunction = function() {
		privateFunction();
	}
	return MyModule;
	
});
