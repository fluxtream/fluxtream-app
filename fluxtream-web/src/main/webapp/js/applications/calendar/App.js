define(["core/Application", "core/FlxState", "applications/calendar/Builder", "libs/bootstrap-datepicker"],
       function(Application, FlxState, Builder) {

	var Calendar = new Application("calendar", "Candide Kemmler", "icon-calendar");

    Calendar.currentTabName = Builder.tabs["DAY"][0];
    Calendar.currentTab = null;
    Calendar.tabState = null;
    Calendar.digest = null;
    Calendar.timeUnit = "DAY";

	var start, end;
    Calendar.connectorEnabled = {"default":{}};
    var buttons = {};

	Calendar.setup = function() {
		$(".menuNextButton").click(function(e) {
			fetchState("/nav/incrementTimespan.json?state=" + Calendar.tabState); });
		$(".menuPrevButton").click(function(e) {
			fetchState("/nav/decrementTimespan.json?state=" + Calendar.tabState); });
		$(".menuTodayButton").click(function(e) {
			Calendar.timeUnit = "DAY";
			var t = Builder.tabExistsForTimeUnit(Calendar.currentTabName, Calendar.timeUnit)?Calendar.currentTabName:Builder.tabs[Calendar.timeUnit][0];
			Calendar.currentTabName = t;
            Calendar.updateButtonStates();
			Builder.bindTimeUnitsMenu(Calendar);
			Builder.createTabs(Calendar);
			fetchState("/nav/setToToday.json");
		});
	};

	Calendar.initialize = function () {
		_.bindAll(this);

        FlxState.router.route(/^app\/calendar(\/?)(.*?)$/, "", function() {
            var parse_url = /^(?:([A-Za-z]+):)?(\/{0,3})([0-9.\-A-Za-z]+)(?::(\d+))?(?:\/([^?#]*))?(?:\?([^#]*))?(?:#(.*))?$/;
            var result = parse_url.exec(window.location.href);
            var names = [ 'url', 'scheme', 'slash', 'host', 'port', 'path' ];
            var i;
            var parts = {};
            for (i = 0; i < names.length; i += 1)
                parts[names[i]] = result[i];
            console.log("path: " + parts.path);
            var pathElements = parts.path.split("/");
            if (pathElements.length<3)
                App.invalidPath();
            var splits = {};
            var splitNames = ["app", "appName", "tabName", "timeUnit"];
            for (i = 0; i < pathElements.length; i += 1)
                splits[splitNames[i]] = pathElements[i];
            var validTab = _.include(["clock","map","diary","photos","list","timeline","dashboards"], splits.tabName),
                validTimeUnit = _.include(["date","week","month","year"], splits.timeUnit);
            if (validTab && validTimeUnit) {
                var tab = Builder.tabExistsForTimeUnit(splits.tabName, Calendar.timeUnit)?splits.tabName:Builder.tabs[Calendar.timeUnit][0];
                switch (splits.timeUnit) {
                    case "date":
                        var date = pathElements[4];
                        Calendar.render(tab + "/date/" + date);
                        break;
                    case "week":
                        var year = pathElements[4],
                            week = pathElements[5];
                        Calendar.render(tab + "/week/" + year + "/" + week);
                        break;
                    case "month":
                        var year = pathElements[4],
                            month = pathElements[5];
                        Calendar.render(tab + "/month/" + year + "/" + month);
                        break;
                    case "year":
                        var year = pathElements[4];
                        Calendar.render(tab + "/year/" + year);
                        break;
                }
            } else
                App.invalidPath();
        });
    };

	Calendar.renderState = function(state, forceReload) {
        forceReload = typeof(forceReload)!="undefined"&&forceReload;
        if (!forceReload&&FlxState.getState("calendar")===state) {
			return;
		}
		if (state==null||state==="") {
			Builder.bindTimeUnitsMenu(Calendar);
			Builder.createTabs(Calendar);
			fetchState("/nav/setToToday.json");
            return;
		}
		var splits = state.split("/");
		Calendar.currentTabName = splits[0];
        if (!Builder.isValidTabName(splits[0])) {
            App.invalidPath();
            return;
        }
        if (!Builder.isValidTimeUnit(splits[1])) {
            App.invalidPath();
            return;
        }
        Calendar.timeUnit = toTimeUnit(splits[1]);
		var nextTabState = state.substring(splits[0].length+1);
        var w = Builder.tabExistsForTimeUnit(Calendar.currentTabName, Calendar.timeUnit)?Calendar.currentTabName:Builder.tabs[Calendar.timeUnit][0];
        Calendar.currentTabName = w;
        Calendar.updateButtonStates();
        Builder.createTabs(Calendar);
        if (!forceReload&&Calendar.tabState==nextTabState) {
			// time didn't change
			Builder.updateTab(Calendar.digest, Calendar);
			FlxState.router.navigate("app/calendar/" + state);
			FlxState.saveState("calendar", state);
			return;
		} else {
            Builder.bindTimeUnitsMenu(Calendar);
            if ("DAY"===Calendar.timeUnit) {
				fetchState("/nav/setDate.json?date=" + splits[2]);
			} else if ("WEEK"===Calendar.timeUnit) {
				fetchState("/nav/setWeek.json?year=" + splits[2] + "&week=" + splits[3]);
			} else if ("MONTH"===Calendar.timeUnit) {
				fetchState("/nav/setMonth.json?year=" + splits[2] + "&month=" + splits[3]);
			} else if ("YEAR"===Calendar.timeUnit) {
				fetchState("/nav/setYear.json?year=" + splits[2]);
			}
		}
	};

	function fetchState(url) {
		$(".calendar-navigation-button").toggleClass("disabled");
		$(".loading").show();
		$("#tabs").css("opacity", ".3");
		$.ajax({ url:url,
			success : function(response) {
				if (Calendar.currentTab) {
					Calendar.currentTab.saveState();
				}
				Calendar.tabState = response.state;
                Calendar.start = response.start;
                Calendar.end  = response.end;
				FlxState.router.navigate("app/calendar/" + Calendar.currentTabName + "/" + response.state);
				FlxState.saveState("calendar", Calendar.currentTabName + "/" + response.state);
                document.title = "Fluxtream - Personal Analytics | " + response.currentTimespanLabel;
				$("#currentTimespanLabel span").html(response.currentTimespanLabel);
				if (Calendar.timeUnit==="DAY") {
					setDatepicker(response.state.split("/")[1]);
				}
                fetchCalendar("/api/calendar/all/" + response.state);
			},
			error : function() {
				alert("error");
			}
		});
	}

	function setDatepicker(currentDate) {
        $(".datepicker.dropdown-menu").remove();
        $("#datepicker").replaceWith("<a data-date-format=\"yyyy-mm-dd\" id=\"datepicker\"><i class=\"icon-calendar icon-large\"></i></a>");
        $("#datepicker").attr("data-date", currentDate);
		$("#datepicker").unbind("changeDate");
		$("#datepicker").datepicker().on(
			"changeDate", function(event) {
				var curr_date = event.date.getDate();
				var curr_month = event.date.getMonth() + 1;
				var curr_year = event.date.getFullYear();
				var formatted = curr_year + "-" + curr_month + "-" + curr_date;
				fetchState("/nav/setDate.json?date=" + formatted);
				$(".datepicker").hide();
			}
		);
	}

	function fetchCalendar(url) {
		$.ajax({ url: url,
			success : function(response) {
                if (Calendar.timeUnit==="DAY")
                    handleCityInfo(response);
                else
                    $("#mainCity").empty();
                Calendar.digest = response;
                enhanceDigest(Calendar.digest);
                processDigest(Calendar.digest);
				Builder.updateTab(Calendar.digest, Calendar);
				$("#tabs").css("opacity", "1");
				$(".calendar-navigation-button").toggleClass("disabled");
				$(".loading").hide();
			},
			error: function() {
				alert("error fetching calendar");
			}
		});
	}

    function getTemplate(digest,i,j){
        App.loadMustacheTemplate("applications/calendar/facetTemplates.html",digest.selectedConnectors[i].facetTypes[j],function(template){
            if (template == null)
                console.log("WANRING: no template found for " + digest.selectedConnectors[i].facetTypes[j] + ".");
            digest.detailsTemplates[digest.selectedConnectors[i].facetTypes[j]] = template;
        });
    }

    function enhanceDigest(digest){
        digest.detailsTemplates = {};
        for (var i = 0; i < digest.selectedConnectors.length; i++){
            for (var j = 0; j < digest.selectedConnectors[i].facetTypes.length; j++){
                getTemplate(digest,i,j);
            }
        }
        App.loadMustacheTemplate("applications/calendar/facetTemplates.html","fluxtream-address",function(template){
            if (template == null)
                console.log("WANRING: no template found for fluxtream-address.");
            digest.detailsTemplates["fluxtream-address"] = template;
        });
        for (var connectorId in digest.cachedData){
            for (var i = 0; i < digest.cachedData[connectorId].length; i++){
                digest.cachedData[connectorId][i].getDetails = function(){
                    return buildDetails(digest,this);
                }
            }
        }

        for (var addressType in digest.addresses){
            for (var i = 0; i < digest.addresses[addressType].length; i++){
                digest.addresses[addressType][i].getDetails = function(){
                    return buildAddressDetails(digest,this);
                }

            }
        }
    }


    function processDigest(digest){
        var selectedConnectors = $("#selectedConnectors");
        selectedConnectors.empty();
        for (var i = 0; i < digest.selectedConnectors.length; i++){
            var enabled = false;
            for (var j = 0; j < digest.selectedConnectors[i].facetTypes.length && !enabled; j++){
                enabled =  digest.cachedData[digest.selectedConnectors[i].facetTypes[j]] != null;
            }
            enabled = enabled ? "" : "flx-disconnected";
            var button = $('<li><a href="#" id="flx-connector-btn-' + digest.selectedConnectors[i].connectorName + '" class="flx-active ' + enabled + '">' + digest.selectedConnectors[i].prettyName + '</button></li>');
            if (enabled == "")
                button.children().css("border-bottom-color",App.getConnectorConfig(digest.selectedConnectors[i].connectorName).color);
            selectedConnectors.append(button);
            button = $(button.children()[0]);
            buttons[digest.selectedConnectors[i].connectorName] = button;
            button.click({button:button,objectTypeNames:digest.selectedConnectors[i].facetTypes,connectorName:digest.selectedConnectors[i].connectorName}, function(event){
                event.preventDefault();
                $(document).click(); //needed for click away to work on tooltips in clock tab
                connectorClicked(event.data.button,event.data.objectTypeNames,event.data.connectorName);
            });
            if (Calendar.connectorEnabled["default"][digest.selectedConnectors[i].connectorName] == null)
                Calendar.connectorEnabled["default"][digest.selectedConnectors[i].connectorName] = true;
            Calendar.updateButtonStates();
        }
    }

    Calendar.updateButtonStates = function(){
        if (Calendar.connectorEnabled[Calendar.currentTabName] == null)
            Calendar.connectorEnabled[Calendar.currentTabName] = {};
        for (var connectorName in Calendar.connectorEnabled["default"]){
            if (Calendar.connectorEnabled[Calendar.currentTabName][connectorName] == null)
                Calendar.connectorEnabled[Calendar.currentTabName][connectorName] = Calendar.connectorEnabled["default"][connectorName];
            if (Calendar.connectorEnabled[Calendar.currentTabName][connectorName]){
                buttons[connectorName].addClass("flx-active");
                buttons[connectorName].removeClass("flx-inactive")
            }
            else{
                buttons[connectorName].removeClass("flx-active");
                buttons[connectorName].addClass("flx-inactive");
            }
        }
    }

    function connectorClicked(button,objectTypeNames,connectorName){
        if (button.is(".flx-disconnected"))
            return;
        Calendar.connectorEnabled[Calendar.currentTabName][connectorName] = !Calendar.connectorEnabled[Calendar.currentTabName][connectorName];
        if (Calendar.connectorEnabled[Calendar.currentTabName][connectorName]){
            button.addClass("flx-active");
            button.removeClass("flx-inactive")
        }
        else{
            button.removeClass("flx-active");
            button.addClass("flx-inactive");
        }
        Calendar.currentTab.connectorToggled(connectorName,objectTypeNames,Calendar.connectorEnabled[Calendar.currentTabName][connectorName]);
        return;
    }

    function buildAddressDetails(digest, address){
        if (digest.detailsTemplates["fluxtream-address"] == null){
            console.log("WARNING: no template found for fluxtream-address.");
            return "";
        }
        var params = {};
        for (var member in address){
            switch (member){
                case "type":
                    switch (address[member]){
                        case "ADDRESS_HOME":
                            params[member] = "Home";
                            break;
                        case "ADDRESS_WORK":
                            params[member] = "Work";
                            break;
                        case "ADDRESS_OTHER":
                            params[member] = "Other";
                            break;
                        default:
                            params[member] = address[member];
                    }
                    break;
                default:
                    params[member] = address[member];
            }
        }
        return digest.detailsTemplates["fluxtream-address"].render(params);
    }

    function buildDetails(digest,data){
        if (digest.detailsTemplates[data.type] == null){
            console.log("WARNING: no template found for " + data.type + ".");
            return "";
        }
        var params = {color:App.getConnectorConfig(App.getFacetConnector(data.type)).color};
        for (var member in data){
            switch (member){
                case "startMinute":
                    params.time = App.formatMinuteOfDay(data[member]);
                    break;
                case "userName":
                    if (data.type == "twitter-dm"){
                        params.sender = data.sent ? "You" : data[member];
                        params.receiver = data.sent ? data[member] : "You";
                        break;
                    }
                    params[member] = data[member];
                    break;
                case "imgUrls":
                    params["imgUrl"] = data[member][0];
                    break;
                case "description":
                    if (data.type == "twitter-dm" || data.type == "twitter-mention" || data.type == "twitter-tweet"){
                        params[member] = parseTwitter(data[member]);
                    }
                    else{
                        params[member] = data[member];
                    }
                    break;
                default:
                    params[member] = data[member];
            }
        }
        return digest.detailsTemplates[data.type].render(params);
    }

    function parseTwitter(text){
        var indexAt = 0;
        var result = "";
        while (indexAt != text.length){
            var foundHashAt = text.indexOf("#",indexAt);
            var foundAtAt = text.indexOf("@",indexAt);
            if (foundHashAt == -1 && foundAtAt == -1){
                result += text.substring(indexAt);
                break;
            }
            else if (foundAtAt != -1 && (foundAtAt < foundHashAt || foundHashAt == -1)){
                result += text.substring(indexAt,foundAtAt);
                var foundSpaceAt = text.indexOf(" ",foundAtAt);
                var foundLineBreakAt = text.indexOf("\n",foundAtAt);
                if (foundSpaceAt == -1 || (foundLineBreakAt != -1 && foundLineBreakAt < foundSpaceAt))
                    foundSpaceAt = foundLineBreakAt;
                var foundTabAt = text.indexOf("\t",foundAtAt);
                if (foundSpaceAt == -1 || (foundTabAt != -1 && foundTabAt < foundSpaceAt))
                    foundSpaceAt = foundTabAt;
                var foundColonAt = text.indexOf(":",foundAtAt);
                if (foundSpaceAt == -1 || (foundColonAt != -1 && foundColonAt < foundSpaceAt))
                    foundSpaceAt = foundColonAt;
                if (foundAtAt == text.length - 1 || foundSpaceAt == foundAtAt + 1 || foundHashAt == foundAtAt + 1 || text.charAt(foundAtAt + 1) == '@'){
                    result += "@";
                    indexAt = foundAtAt + 1;
                }
                else if (foundSpaceAt == -1 && foundHashAt == -1){
                    var linkText = text.substring(foundAtAt);
                    var userName = linkText.substring(1);
                    result += "<a href='https://twitter.com/" + encodeURIComponent(userName) + "'>" + linkText + "</a>";
                    break;
                }
                else if (foundSpaceAt != -1 && (foundSpaceAt < foundHashAt || foundHashAt == -1)){
                    var linkText = text.substring(foundAtAt,foundSpaceAt);
                    var userName = linkText.substring(1);
                    result += "<a href='https://twitter.com/" + encodeURIComponent(userName) + "'>" + linkText + "</a>";
                    indexAt = foundSpaceAt;
                }
                else{
                    var linkText = text.substring(foundAtAt,foundHashAt);
                    var userName = linkText.substring(1);
                    result += "<a href='https://twitter.com/" + encodeURIComponent(userName) + "'>" + linkText + "</a>";
                    indexAt = foundHashAt;
                }
            }
            else{
                result += text.substring(indexAt,foundHashAt);
                var foundSpaceAt = text.indexOf(" ",foundHashAt);
                var foundLineBreakAt = text.indexOf("\n",foundHashAt);
                if (foundSpaceAt == -1 || (foundLineBreakAt != -1 && foundLineBreakAt < foundSpaceAt))
                    foundSpaceAt = foundLineBreakAt;
                var foundTabAt = text.indexOf("\t",foundHashAt);
                if (foundSpaceAt == -1 || (foundTabAt != -1 && foundTabAt < foundSpaceAt))
                    foundSpaceAt = foundTabAt;
                var foundColonAt = text.indexOf(":",foundHashAt);
                if (foundSpaceAt == -1 || (foundColonAt != -1 && foundColonAt < foundSpaceAt))
                    foundSpaceAt = foundColonAt;
                if (foundHashAt == text.length - 1 || foundSpaceAt == foundHashAt + 1 || foundAtAt == foundHashAt + 1 || text.charAt(foundHashAt + 1) == '#'){
                    result += "#";
                    indexAt = foundHashAt + 1;
                }
                else if (foundSpaceAt == -1 && foundAtAt == -1){
                    var linkText = text.substring(foundHashAt);
                    result += "<a href='https://twitter.com/search/" + encodeURIComponent(linkText) + "'>" + linkText + "</a>";
                    break;
                }
                else if (foundSpaceAt != -1 && (foundSpaceAt < foundAtAt || foundAtAt == -1)){
                    var linkText = text.substring(foundHashAt,foundSpaceAt);
                    result += "<a href='https://twitter.com/search/" + encodeURIComponent(linkText) + "'>" + linkText + "</a>";
                    indexAt = foundSpaceAt;
                }
                else{
                    var linkText = text.substring(foundHashAt,foundAtAt);
                    result += "<a href='https://twitter.com/search/" + encodeURIComponent(linkText) + "'>" + linkText + "</a>";
                    indexAt = foundAtAt;
                }
            }
        }
        return result;

    }

    function handleCityInfo(digestInfo) {
        $("#mainCity").empty();
        if (digestInfo.cities&&digestInfo.cities.length>0) {
            $("#mainCity").html(cityLabel(digestInfo.cities[0]) +
                                temperaturesLabel(digestInfo))
        }
    }

    function ephemerisLabel(digestInfo) {
        var sunriseH = Math.floor(digestInfo.solarInfo.sunrise/60);
        var sunriseM = digestInfo.solarInfo.sunrise%60;
        var sunsetH = Math.floor(digestInfo.solarInfo.sunset/60);
        var sunsetM = digestInfo.solarInfo.sunset%60;
        if (sunriseM<10) sunriseM = "0" + sunriseM;
        if (sunsetM<10) sunsetM = "0" + sunsetM;
        return "<span class=\"ephemeris\"><i class=\"flx-pict-sun\">&nbsp;</i><span>" + sunriseH + ":" + sunriseM + " am"+
               "</span>&nbsp;<i class=\"flx-pict-moon\">&nbsp;</i><span>" + sunsetH + ":" + sunsetM + " pm</span></span>";
    }

    function temperaturesLabel(digestInfo) {
        if (digestInfo.maxTempC == -10000) {
            return "";
        }
        else if (digestInfo.settings.temperatureUnit != "CELSIUS") {
            return ephemerisLabel(digestInfo) + "<i class=\"flx-pict-temp\">&nbsp;</i>"
                       + "<span class=\"ephemeris\" style=\"font-weight:normal;\">&nbsp;"
                       + digestInfo.minTempF
                       + " / "
                       + digestInfo.maxTempF
                       + "&deg;F"
                + "</span>";
        }
        else {
            return ephemerisLabel(digestInfo) + "<i class=\"flx-pict-temp\">&nbsp;</i>"
                       + "<span class=\"ephemeris\" style=\"font-weight:normal;\">&nbsp;"
                       + digestInfo.minTempC
                       + " / "
                       + digestInfo.maxTempC
                       + "&deg;C"
                + "</span>";
        }
    }

    function cityLabel(cityInfo) {
        var s = "";
        s += cityInfo.name;
        if (cityInfo.state) s += ", " + cityInfo.state;
        s += ", " + cityInfo.country;
        //if (traveling)
        //    s += " <i>- on holiday</i>";
        //if (FlxState.cities.length>20)
        //    s += " <i>- in transit</i>";
        return s;
    }

    function toTimeUnit(urlTimeUnit) {
		if (urlTimeUnit==="date") return "DAY";
		return urlTimeUnit.toUpperCase();
	}

    Calendar.dateChanged = function(date, rangeType) {
        console.log("Calendar.dateChanged(" + date + ", " + rangeType + ")");
        console.log("updating url...");

        var state = "timeline/date/" + date;
        FlxState.router.navigate("app/calendar/" + state, {trigger: false, replace: true});
        FlxState.saveState("calendar", state);
        Calendar.tabState = "date/" + date;
        Calendar.timeUnit = "DAY";

        var dateSplits = date.split("-"),
            d = new Date(Number(dateSplits[0]),Number(dateSplits[1])-1,Number(dateSplits[2]));
        var daysOfWeek = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];
        var monthsOfYear = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep",
            "Oct", "Nov", "Dec"];
        var dateLabel = daysOfWeek[d.getDay()] +
                    ", " + monthsOfYear[d.getMonth()] + " " + d.getDate() +
                    ", " + (d.getYear()+1900);
        console.log("dateLabel: " + dateLabel);

        $("#currentTimespanLabel span").html(dateLabel);
    };

	return Calendar;


});