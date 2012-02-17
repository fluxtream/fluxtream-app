
function showLocationBreakdownInfo(event) {
	ttpdiv = $("#tooltip");
	FlxState.lastHoveredEvent = event;
	var span = event.target;
	var facetId = span.item.id;
	var tip_y = event.pageY;
	var tip_x = event.pageX;
	ttpdiv.qtip({
	   content: {
	      text: span.item.description
	   },
	   style: {
	      classes: 'ui-tooltip-light ui-tooltip-shadow ui-tooltip-rounded',
	   },
	   position: {
           target: [tip_x,tip_y], // ... in the window
	   	   my: "top center",
	   	   adjust: { y: 13 }
	   },
       show: {
          ready: true, // Show it straight away
       },
       hide: {
		  effect: function(offset) {
		      $(this).slideDown(100); // "this" refers to the tooltip
		  },
		  inactive : 4500
       }
	});
}


function showEventInfo(event) {
	ttpdiv = $("#tooltip");
	FlxState.lastHoveredEvent = event;
	var span = event.target;
	var facetId = span.item.id;
	var facetType = span.item.type;
	if (facetType=="google_latitude") 
		return;
	var tip_y = event.pageY;
	var tip_x = event.pageX;
	var tooltip = $("#" + facetType + "_" + facetId);
	ttpdiv.qtip({
	   content: {
	      text: tooltip.html()
	   },
	   style: {
	      classes: 'ui-tooltip-light ui-tooltip-shadow ui-tooltip-rounded',
	   },
	   position: {
           target: [tip_x,tip_y], // ... in the window
	   	   my: "top center",
	   	   adjust: { y: 13 }
	   },
       show: {
          ready: true, // Show it straight away
       },
       hide: {
		  effect: function(offset) {
		      $(this).slideDown(100); // "this" refers to the tooltip
		  },
		  inactive : 4500	
       }
	});
	
}

function hideEventInfo() {
	ttpdiv.qtip('hide');
}
