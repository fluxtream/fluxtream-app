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
                digest.cachedData[connectorId][i].getDetails = function(array){
                    if (array == null)
                        array = [this];
                    return buildDetails(digest,array);
                }
                digest.cachedData[connectorId][i].shouldGroup = function(facet){
                    return shouldFacetsGroup(this,facet);
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

    function shouldFacetsGroup(facet1, facet2){
        if (facet1.type != facet2.type)
            return false;
        switch (facet1.type){
            case "twitter-dm":
                return facet1.sent == facet2.sent;
        }
        return true;
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

    function buildDetails(digest,facets){
        if (facets.length == 0)
            return"";
        if (digest.detailsTemplates[facets[0].type] == null){
            console.log("WARNING: no template found for " + facets[0].type + ".");
            return "";
        }
        var params = {color:App.getFacetConfig(facets[0].type).color,facets:[],sent:facets[0].sent};
        for (var i = 0; i < facets.length; i++){
            var data = facets[i];
            var newFacet = {};
            params.facets[i] = newFacet;
            for (var member in data){
                switch (member){
                    case "source":
                        switch (data[member]){
                            case "GOOGLE_LATITUDE":
                                newFacet[member] = "google latitude";
                                break;
                            case "OTHER":
                                newFacet[member] = "IP lookup";
                                break;
                            default:
                                newFacet[memeber] = data[member];
                                break;
                        }
                        break;
                    case "startMinute":
                        newFacet.time = App.formatMinuteOfDay(data[member]);
                        break;
                    case "userName":
                        newFacet[member] = data[member];
                        break;
                    case "imgUrls":
                        newFacet["imgUrl"] = data[member][0];
                        break;
                    case "description":
                        if (data.type == "twitter-dm" || data.type == "twitter-mention" || data.type == "twitter-tweet"){
                            newFacet[member] = parseTwitter(data[member]);
                        }
                        else{
                            newFacet[member] = data[member];
                        }
                        break;
                    default:
                        newFacet[member] = data[member];
                }
            }
        }
        for (var i=0; i<params.facets.length; i++) {
            params.facets[i].manyFacets = params.facets.length>0?" many":"";
        }
        return digest.detailsTemplates[data.type].render(params);
    }

    var topLevelDomains = ["aero","asia","biz","cat","com","coop","info", "int", "jobs", "mobi",
                           "museum", "name", "net", "org", "pro", "tel", "travel", "xxx", "edu",
                           "edu", "gov", "mil", "ac", "ad", "ae", "af", "ag", "ai", "al", "am",
                           "am", "an", "aq", "ar", "as", "at", "au", "aw", "ax", "az", "ba", "bb",
                           "be", "bf", "bg", "bh", "bi", "bj", "bm", "bn" , "bo", "br", "bs", "bt",
                           "bv", "bw", "by", "bz", "ca", "cc", "cd", "cf", "cg", "ch", "ci", "ck",
                           "cl", "cm", "cn", "co", "cr", "cs", "cu", "cv", "cx", "cy", "cz", "dd",
                           "de", "dj", "dk", "dm", "do", "dz", "ec", "ee", "eg", "eh", "er", "es", "et",
                           "eu", "fi", "fj", "fk", "fm", "fo", "fr", "ga", "gb", "gd", "ge", "gf", "gg",
                           "gh", "gi", "gl", "gm", "gn", "gp", "gq", "gr", "gs", "gt", "gu", "gw", "gy",
                           "hk", "hm", "hn", "hr", "ht", "hu", "id", "ie", "il", "im", "in", "io", "iq",
                           "iq", "ir", "is", "it", "je", "jm", "jo", "jp", "ke", "kg", "kh", "ki", "km",
                           "kn", "kp", "kr", "kw", "ky", "kz", "la", "lb", "lc", "li", "lk", "lr", "ls",
                           "lt", "lu", "lv", "ly", "ma", "mc", "md", "me", "mg", "mh", "mk", "ml", "mm",
                           "mn", "mo", "mp", "mq", "mr", "ms", "mt", "mu", "mv", "mw", "mx", "my", "mz",
                           "na", "nc", "ne", "nf", "ng", "ni", "nl", "no", "np", "nr", "nu", "nz", "om",
                           "pa", "pe", "pf", "pg", "ph", "pk", "pl", "pm", "pn", "pr", "ps", "pt", "pw",
                           "py", "qa", "re", "ro", "rs", "ru", "rw", "sa", "sb", "sc", "sd", "se", "sg",
                           "sh", "si", "sj", "sk", "sl", "sm", "sn", "so", "sr", "ss", "st", "su", "sv",
                           "sy", "sz", "tc", "td", "tf", "tg", "th", "tj", "tk", "tl", "tm", "tn", "to",
                           "tp", "tr", "tt", "tv", "tw", "tz", "ua", "uk", "us", "uy", "uz", "va", "vc",
                           "ve", "vg", "vi", "vn", "vu", "wf", "ws", "ye", "yt", "yu", "za", "zm", "zw"]

    var uriRegex = "\\b(http://|ftp://)?[A-z0-9\\-.]+[.](" + topLevelDomains.join("|") + ")(/[/A-z0-9#@_\\-&?=.]+\\b|\\b)";
    var twitterNameRegex = "@[A-Za-z0-9_]+";
    var hashtagRegex = "#[A-Za-z_]+";
    var twitterParserRegexp = new RegExp("(" + uriRegex + "|" + twitterNameRegex + "|" + hashtagRegex + ")","ig");

    function parseTwitter(text){
        var ret = "";
        var oldIndex = 0;
        text.replace(twitterParserRegexp, function(match){
            var index = arguments[arguments.length - 2];
            var fullString = arguments[arguments.length - 1];
            ret += fullString.substring(oldIndex,index);
            oldIndex = index + match.length;
            switch (match.charAt(0)){
                case "#"://hashtag
                    ret += "<a href='https://twitter.com/search/" + encodeURIComponent(match) + "'>" + match + "</a>";
                    break;
                case "@"://username
                    ret += "<a href='https://twitter.com/" + encodeURIComponent(match.substring(1)) + "'>" + match + "</a>";
                    break;
                default://uri
                    ret += "<a href='" + match + "'>" + match + "</a>";
                    break;
            }
        });
        ret += text.substring(oldIndex);

        return ret;
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