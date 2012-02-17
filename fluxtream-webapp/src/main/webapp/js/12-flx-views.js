function setup() {
	$("#graphics").hide();
	
	if (typeof(console) == "undefined") {
		console = {
			log : function () {},
			error : function () {},
			warn : function () {},
			dir : function () {}
		};
	}

	if (typeof(FlxState.paper)=="undefined" || $("#graphics svg")[0]==null) {
		if (FlxState.vtype=="CLOCK") {
			$("#graphics").css("width", "600px");
			$("#graphics").css("height", "620px");
			$(".donutsCont").css("height", "700px");
			FlxState.paper = Raphael("graphics", FlxState.CLOCK_WIDTH, FlxState.CLOCK_HEIGHT);
			var root = $("#graphics svg");
			root.click(function(event) {
				var svgRootPos = findPos(root[0]);
				var xpos = event.pageX-svgRootPos[0]-(FlxState.CLOCK_WIDTH/2);
				var ypos = -(event.pageY-svgRootPos[1]-(FlxState.CLOCK_HEIGHT/2));
				var angle = Math.atan2(ypos, xpos);
				var angleinminutes = Math.abs((720*angle)/Math.PI);
				if (angle>0)
					angleinminutes = 1080-angleinminutes;
				else if (-angle>Math.PI/2)
					angleinminutes = angleinminutes - 360;
				else
					angleinminutes = angleinminutes + 1080;
				showEventLocation(angleinminutes);
				showEventWeather(angleinminutes);
			});
		} else if (FlxState.vtype=="LIST") {
			$("#graphics").css("width", "876px");
			$("#graphics").css("height", "50px");
			FlxState.paper = Raphael("graphics", FlxState.TIMELINE_WIDTH, FlxState.TIMELINE_HEIGHT);
		}
	} else {
		if (typeof(FlxState.clockCircles)!="undefined"&&FlxState.clockCircles!=null)
			FlxState.clockCircles.remove();
		if (typeof(FlxState.timelineLanes)!="undefined"&&FlxState.timelineLanes!=null)
			FlxState.timelineLanes.remove();
		FlxState.paper.clear();
	}
	jQuery.fn.exists = function(){return jQuery(this).length>0;}
	$("#graphics").show();
}

function handleDigest(digestInfo) {
	FlxState.start = digestInfo.tbounds.start;
	FlxState.end = digestInfo.tbounds.end;
	handleHomeAddress(digestInfo.homeAddress);
	paintCachedData(digestInfo.cachedData);
	getUpdateNeeded(digestInfo.updateNeeded);
	handleNotifications(digestInfo);
	handleCityInfo(digestInfo);
	drawFirstLocation();
	handleConnectorButtons(digestInfo);
}

function handleConnectorButtons(digestInfo) {
	$(".btnList").unbind();
	$(".btnList").removeClass("btnListChecked");
	$(".btnList").removeClass("btnListNoData");
	for (i=0;i<digestInfo.selectedConnectors.length;i++) {
		$(".btn-"+digestInfo.selectedConnectors[i]).addClass("btnListChecked");
	} 
	for (i=0;i<digestInfo.haveDataConnectors.length;i++) {
		$(".btn-"+digestInfo.haveDataConnectors[i]).click(function() {
			if (FlxState.vtype=="CLOCK") {
				$(this).toggleClass('btnListChecked');
				updateVisualization();
			}
		})
	} 
	for (i=0;i<digestInfo.haveNoDataConnectors.length;i++) {
		$(".btn-"+digestInfo.haveNoDataConnectors[i]).addClass("btnListNoData");
	} 
}

function doDrawTimeline() {
	$("#tooltips").load("/me/tooltips");
	$.ajax({ url: "/api/log/all/"+FlxState.state, dataType: "json",
		data : getCheckedConnectors(),
		success: function(digestInfo) {
			setup();
			FlxState.timelineLanes = FlxState.paper.set();
			paintTLSolarInfo(digestInfo.solarInfo);
			paintTimespan(FlxState.paper, FlxState.NO_CATEGORY.lane, "#ffffff");
			FlxState.timelineLanes.hide();
			FlxState.timelineLanes.show();
			handleDigest(digestInfo);
		}
	});
	
}

function doDrawClock() {
	$("#tooltips").load("/me/tooltips");
	if (typeof(_gaq)!="undefined") _gaq.push(['_trackEvent', 'clockview', FlxState.state, "digest.json"]);
	$.ajax({ url: "/api/log/all/"+FlxState.state, dataType: "json",
		data : getCheckedConnectors(),
		success: function(digestInfo) {
			setup();
			opacity = .5;
			FlxState.clockCircles = FlxState.paper.set();
			paintCircle(FlxState.paper, FlxState.BODY_CATEGORY.orbit, "#ffffff", opacity);
			paintCircle(FlxState.paper, FlxState.AT_HOME_CATEGORY.orbit, "#ffffff", opacity);
			paintCircle(FlxState.paper, FlxState.OUTSIDE_CATEGORY.orbit, "#ffffff", opacity);
			paintCircle(FlxState.paper, FlxState.MIND_CATEGORY.orbit, "#ffffff", opacity);
			paintCircle(FlxState.paper, FlxState.SOCIAL_CATEGORY.orbit, "#ffffff", opacity);
			paintCircle(FlxState.paper, FlxState.MEDIA_CATEGORY.orbit, "#ffffff", opacity);
			FlxState.clockCircles.hide();
			paintSolarInfo(digestInfo.solarInfo);
			if (digestInfo.hasPictures)
				showPictures();
			else
				hidePictures();
			FlxState.clockCircles.show();
			handleDigest(digestInfo);
		}
	});
	
}

function handleCityInfo(digestInfo) {
	FlxState.cities = digestInfo.cities;
	$("#mainCity").empty();
	if (digestInfo.cities&&digestInfo.cities.length>0) {
		$("#mainCity").html(cityLabel(digestInfo.cities[0]) +
			temperaturesLabel(digestInfo))
//		$("#ephemeris").html(ephemerisLabel(digestInfo));
	}
}

function handleHomeAddress(homeAddress) {
	if (homeAddress.isSet) {
		$("#setAddressLink").hide();
		FlxState.homeAddress = homeAddress;
	} else {
		$("#setAddressLink").show();
	}
}

function showInCategory(items, category) {
	if (typeof(items)=="undefined"||items==null) {
		return;
	}
	switch (FlxState.vtype) {
	case "CLOCK":
		showInWheel(items, category.orbit, category.color);
		break;
	case "LIST":
		showInTimeline(items, FlxState.NO_CATEGORY.lane, category.color);
		break;
	}
}

function showInTimeline(items, lane, color) {
	if (typeof(items)=="undefined") return;
	for (i = 0; i < items.length; i++) {
		try {
			var item = items[i];
			FlxState.timelineLanes.push(
				function() {
					var span = paintTimelineSpan(FlxState.paper, item.startMinute,
						(typeof(item.endMinute)=="undefined")
								? item.startMinute+10
								:(item.startMinute<=item.endMinute?item.endMinute: 1440),
						lane,
						color);
					span.node.item = item;
					span.node.onclick = function() {
						showEventLocation(this.item.startMinute);
						showEventWeather(this.item.startMinute);
					};
					$(span.node).mouseover(function(event) {
						this.style.cursor = "pointer";
						if (event.target.item.type!="zeo")
							showEventInfo(event);
					});
					$(span.node).mouseout(function() {
						hideEventInfo();
						this.style.cursor = "default";
					});
					return span;
				}()
			);
		} catch (e) {
			console.log("showInTimeline error");
		}
	}
}

function showInWheel(items, orbit, color) {
	if (typeof(items)=="undefined") return;
	for (i = 0; i < items.length; i++) {
		try {
			var item = items[i],
				instantWidth=1;
			if (orbit==FlxState.BODY_CATEGORY.orbit)
				instantWidth=18;
			FlxState.clockCircles.push(
				function() {
					var start = item.startMinute;
					var end = item.endMinute;
					if (start>end) { start = 0; }
					instantaneous = typeof(item.endMinute)=="undefined"||item.endMinute===item.startMinute;
					var span;
					if (instantaneous)
						span = paintSpan(FlxState.paper, start,start+instantWidth, orbit, color, .9);
					else
						span = paintSpan(FlxState.paper, start,(start<=end?end:1440), orbit, color, .9);
					span.node.item = item;
					span.node.onclick = function() {
						showEventLocation(start);
						showEventWeather(start);
					};
					$(span.node).mouseover(function(event) {
						this.style.cursor = "pointer";
						showEventInfo(event);
					});
					$(span.node).mouseout(function() {
						hideEventInfo();
						this.style.cursor = "default";
					});
					return span;
				}()
			);
		} catch (e) {
			if (typeof(console)!="undefined"&&console.log)
				console.log("there was an error parsing this json: " + e);
		}
	}
}