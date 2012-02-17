
$(document).ready(function($) {
	handleClicks();
//	document.body.onselectstart = function() { return false; };
//	document.body.style.MozUserSelect = "none";
//	document.body.style.KhtmlUserSelect = "none";
//	document.body.unselectable = "on";
	var now = new Date(),
		format = 'Y-m-d';
	var today = now.format(format);
	$(document.body).mousemove(function (event) {
		if (FlxState.lastHoveredEvent!=null) {
			var span = FlxState.lastHoveredEvent.target;
			var box = span.getBBox();
			if (box.width>50 || box.height>50)
				return;
			if (!isHovered(event)) {
				this.style.cursor = "default";
				hideEventInfo();
			} else {
				showEventInfo(FlxState.lastHoveredEvent);
			}
		}
	});
	var options = {
			format: format,
			date:today,
			current:today,
			position:'r',
			onChange:function(){$('#currentTimespanLabel').DatePickerHide();setSelectedDate($('#currentTimespanLabel').DatePickerGetDate(true));}
	};
	$("#currentTimespanLabel").DatePicker(options);
	$('.dropdown-menu').dropdown();
	handleComments();
    handleMouseEvents();
    keepSessionAlive();
	getModel();
	setTimeout("emailtip()", 3000);
	$("#searchBox").focus(function(){
		$("#searchBox").val("");
	});
	$("#searchBox").blur(function(){
		if ($("#searchBox").val()=="")
			$("#searchBox").val($("#searchBox").attr("title"));
	});
});

function emailtip() {
	$(".email_me_icon").qtip({
	   content: {
	      text: "Candide Kemmler, Co-Founder & CEO"
	   },
	   style: {
	      classes: 'ui-tooltip-light ui-tooltip-shadow ui-tooltip-rounded',
	   },
	   position: {
	   	   my: "bottom center",
	   	   at: "top center"
	   },
       show: {
          ready: false // Show it straight away
       },
       hide: {
		  effect: function(offset) {
		      $(this).slideDown(100); // "this" refers to the tooltip
		  },
		  inactive : 3000	
       }
	});
}

function hideConnectorsTip() {
	$(".connectors_menu").removeClass("lookma");
	$(".connectors_menu").qtip("api").hide();
}

function connectorstip() {
	$(".connectors_menu").click(
		function() {
			hideConnectorsTip();
		}
	);
	$(".connectors_menu.lookma").qtip({
	   content: {
	      text: "You only have the essentials. Go ahead, pick some more!"
	   },
	   style: {
	      classes: 'ui-tooltip-light ui-tooltip-shadow ui-tooltip-rounded'
	   },
	   position: {
	   	   my: "top right",
	   	   at: "bottom center"
	   },
       show: {
          ready: true
       }
	});
}

function isHovered(event) {
	var span = FlxState.lastHoveredEvent.target;
	var box = span.getBBox();
	box.x-=2; box.y-=2; box.width+=2; box.height+=2;
	var root = $("#graphics svg")[0];
	var svgRootPos = findPos(root);
	var rpos = root.createSVGRect();
	rpos.x = event.pageX-svgRootPos[0];
	rpos.y = event.pageY-svgRootPos[1];
	rpos.width = rpos.height = 1;
	return (rpos.x>box.x && rpos.x<box.x+box.width)
		&& (rpos.y>box.y && rpos.y<box.y+box.height);
}

function findPos(obj) {
	var curleft = curtop = 0;
	if (obj==null)
		return [curleft,curtop];
	if (typeof(obj.offsetParent)!="undefined") {
		do {
			curleft += obj.offsetLeft;
			curtop += obj.offsetTop;
		} while (obj = obj.offsetParent);
	}
	return [curleft,curtop];
}

function loadModal(url) {
	$.ajax({url:url, success: function(html) {
		$.facebox(html);
	}});
}
