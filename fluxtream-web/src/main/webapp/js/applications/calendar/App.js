define(["core/Application", "core/FlxState", "applications/calendar/Builder", "libs/bootstrap-datepicker",
        "ConnectorConfig", "core/DateUtils", "core/StringUtils"],
       function(Application, FlxState, Builder, Datepicker, ConnectorConfig, DateUtils, StringUtils) {

	var Calendar = new Application("calendar", "Candide Kemmler", "icon-calendar");

    Calendar.currentTabName = Builder.tabs["date"][0];
    Calendar.currentTab = null;
    Calendar.tabState = null;
    Calendar.digest = null;
    Calendar.timeUnit = "date";
    Calendar.digestTabState = false;
    Calendar.tabParam = null;

	var start, end;
    Calendar.connectorEnabled = {"default":{}};

	Calendar.setup = function() {
        $.ajax("/api/connectors/filters", {
            async: false,
            success: function(data){
                for (var member in data){
                    Calendar.connectorEnabled[member] = data[member];
                }
            }
        });
        Builder.init(App, this);
	};

	Calendar.initialize = function () {
		_.bindAll(this);
    };

    function updateTimespan(currentTimespan) {
        document.title = "Fluxtream Calendar | " + currentTimespan + " (" + Calendar.currentTabName + ")";
        $("#currentTimespanLabel span").html(currentTimespan);
    }

    function startLoading() {
        $(".calendar-navigation-button").addClass("disabled");
        $(".loading").show();
        $("#tabs").css("opacity", ".3");
    }

    function stopLoading() {
        $("#tabs").css("opacity", "1");
        $(".calendar-navigation-button").removeClass("disabled");
        $(".loading").hide();
    }

    Calendar.fetchState = function(url, params) {
        startLoading();
        $.ajax({
           url: url,
           type: "GET",
           data: params,
           success: function(response) {
               updateTimespan(response.currentTimespanLabel);
               Calendar.navigateState(Calendar.currentTabName + "/" + response.state);
           },
           error: function() {
               console.log(arguments);
           }
        });
    }

    Calendar.parseState = function(state) {
        var splits = state.split("/");
        if (_.isEmpty(splits)) {
            return null;
        }
        var obj = {};
        obj.tabName = splits.shift();
        obj.tabState = splits.join("/");
        obj.timeUnit = splits.shift();
        if (!Builder.isValidTabName(obj.tabName) || !Builder.isValidTimeUnit(obj.timeUnit)) {
            return null;
        }
        if (!Builder.tabExistsForTimeUnit(obj.tabName, obj.timeUnit)) {
            obj.tabName = Builder.tabs[obj.timeUnit][0];
        }
        switch (obj.timeUnit) {
            case "date":
                obj.date = splits.shift();
                if (_.isUndefined(obj.date)) {
                    return null;
                }
                break;
            case "week":
                obj.year = splits.shift();
                obj.week = splits.shift();
                if (_.isUndefined(obj.year) || _.isUndefined(obj.week)) {
                    return null;
                }
                break;
            case "month":
                obj.year = splits.shift();
                obj.month = splits.shift();
                if (_.isUndefined(obj.year) || _.isUndefined(obj.month)) {
                    return null;
                }
                break;
            case "year":
                obj.year = splits.shift();
                if (_.isUndefined(obj.year)) {
                    return null;
                }
                break;
        }
        obj.tabParam = splits.shift();
        return obj;
    };

    Calendar.renderDefaultState = function() {
        Calendar.fetchState("/api/calendar/nav/setToToday", {timeUnit: "DAY"});
    };

	Calendar.renderState = function(state) {
        var tabChanged = Calendar.tabState === state.tabState;
        Calendar.tabState = state.tabState;
        Calendar.currentTabName = state.tabName;
        Calendar.timeUnit = state.timeUnit;
        Calendar.updateButtonStates();
        Builder.createTabs(Calendar);
        if (tabChanged) {
            document.title = "Fluxtream Calendar | " + $("#currentTimespanLabel").text().trim() + " (" + Calendar.currentTabName + ")";
			Builder.updateTab(Calendar.digest, Calendar);
		} else {
            updateDisplays(state);
            updateDatepicker(state);
            fetchCalendar(state);
        }
	};

    Calendar.setTabParam = function(tabParam){
        Calendar.tabParam = tabParam;
    };

    function getDatepickerDate(state) {
        switch (state.timeUnit) {
            case "date":
                return new Date(state.date);
            case "week":
                return getDateRangeForWeek(state.year, state.week)[0];
            case "month":
                return new Date(state.year,state.month-1,1,0,0,0,0);
            case "year":
                return new Date(state.year,0,1,0,0,0,0);
        }
        return null;
    }

    function updateDatepicker(state) {
        var currentDate = getDatepickerDate(state);
        $("#datepicker").datepicker("setUTCDate", currentDate);
    }

	function fetchCalendar(state) {
		$.ajax({
            url: "/api/calendar/all/" + state.tabState,
			success : function(response) {
                console.log(response);
                if (typeof response.result!="undefined" && response.result==="KO") {
                    alert(response.message);
                    return;
                }
                if (Calendar.timeUnit==="date") {
                    handleCityInfo(response);
                } else {
                    $("#mainCity").empty();
                }
                Calendar.digest = response;
                Calendar.digestTabState = state.tabState;
                enhanceDigest(Calendar.digest);
                processDigest(Calendar.digest);
				Builder.updateTab(Calendar.digest, Calendar);
                stopLoading();
                Builder.handleNotifications(response);
			},
			error: function() {
				alert("error fetching calendar");
			}
		});
	}

    function enhanceDigest(digest){
        digest.detailsTemplates = {};
        var templatePath = "applications/calendar/facetTemplates.html";
        function loadTemplate(name) {
            App.loadMustacheTemplate(templatePath, name, function(template){
                if (template == null) {
                    console.log("WARNING: no template found for " + name + ".");
                }
                digest.detailsTemplates[name] = template;
            });
        }
        $.each(digest.selectedConnectors, function(i, connector) {
            $.each(connector.facetTypes, function(j, facetType) {
                loadTemplate(facetType);
            });
        });
        loadTemplate("fluxtream-address");
        for (var connectorId in digest.cachedData){
            $.each(digest.cachedData[connectorId], function(i, connector) {
                connector.getDetails = function(array){
                    if (array == null)
                        array = [this];
                    return buildDetails(digest,array);
                };
                connector.shouldGroup = function(facet){
                    return shouldFacetsGroup(this,facet);
                };
            });
        }

        for (var addressType in digest.addresses){
            $.each(digest.addresses[addressType], function(i, address) {
                address.getDetails = function(){
                    return buildAddressDetails(digest,this);
                };
            });
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
        $.each(Builder.getConnectorNames(), function(i, connectorName) {
            var buttonLink = Builder.getConnectorButton(connectorName),
                button = buttonLink.parent();
            button.hide();
        });
        $.each(digest.selectedConnectors, function(i, connector) {
            var connected = _.some(connector.facetTypes, function(facetType) {
                return digest.cachedData[facetType] != null;
            });
            var buttonLink = Builder.getConnectorButton(connector.connectorName),
                button = buttonLink.parent();
            buttonLink
                .toggleClass("flx-disconnected", !connected)
                .text(connector.prettyName);
            if (connected) {
                buttonLink.css("border-bottom-color",App.getConnectorConfig(connector.connectorName).color);
            }
            if (Calendar.connectorEnabled["default"][connector.connectorName] == null) {
                Calendar.connectorEnabled["default"][connector.connectorName] = true;
            }
            button.show();
        });
        Calendar.updateButtonStates();
    }

    Calendar.updateButtonStates = function(){
        if (Calendar.connectorEnabled[Calendar.currentTabName] == null)
            Calendar.connectorEnabled[Calendar.currentTabName] = {};
        for (var connectorName in Calendar.connectorEnabled["default"]){
            if (Calendar.connectorEnabled[Calendar.currentTabName][connectorName] == null)
                Calendar.connectorEnabled[Calendar.currentTabName][connectorName] = Calendar.connectorEnabled["default"][connectorName];
            var button = Builder.getConnectorButton(connectorName),
                enabled = Calendar.connectorEnabled[Calendar.currentTabName][connectorName];
            button.toggleClass("flx-active", enabled);
            button.toggleClass("flx-inactive", !enabled);
        }
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
            console.log("WARNING: hey, no template found for " + facets[0].type + ".");
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
                                newFacet[member] = data[member];
                                break;
                        }
                        break;
                    case "startMinute":
                        newFacet.time = App.formatMinuteOfDay(data[member])[0];
                        newFacet.ampm = App.formatMinuteOfDay(data[member])[1];
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

        // START HACK
        var zeo = false;
        for (var i=0; i<params.facets.length; i++) {
            if (facets[i].type=="zeo-sleep") {
                if (zeo) delete facets[i];
                zeo = true;
            }
        }
        // END HACK

        for (var i=0; i<params.facets.length; i++) {
            params.facets[i].manyFacets = params.facets.length>0?" many":"";
            for (var member in params.facets[i]) {
                if (params.facets[i].hasOwnProperty(member)) {
                    if (typeof(params.facets[i][member])==="number") {
                        var formatted = StringUtils.addCommas(params.facets[i][member]);
                        params.facets[i]["_"+member] = formatted;
                    }
                }
            }
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
                    ret += "<a target='_blank' href='https://twitter.com/search/" + encodeURIComponent(match) + "'>" + match + "</a>";
                    break;
                case "@"://username
                    ret += "<a target='_blank' href='https://twitter.com/" + encodeURIComponent(match.substring(1)) + "'>" + match + "</a>";
                    break;
                default://uri
                    ret += "<a target='_blank' href='" + match + "'>" + match + "</a>";
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
        return s;
    }

   function getWeekNumber(year, month, date) {
       //OK let's have java compute that for us and avoid the discrepancy bug that way for now
       var dateString = year + "-" + (month < 9 ? 0 : "") + (month + 1) + "-" + (date < 10 ? 0 : "") + date,
           result = null;
       $.ajax({
           url: "/api/calendar/nav/getMeTheJavaComputedWeekForThisDate?formattedDate=" + dateString,
           async: false,
           success: function(data) {
               result = data;
           }
       });
       return result;
   }

   function getDateRangeForWeek(year, week) {
       if (week==null) return null;
       var result = null, range = null;
       $.ajax({
           url: "/api/calendar/nav/getDateRangeForWeek?year=" + year + "&week=" + week,
           async: false,
           success: function(data) {
               result = data;
           }
       });
       if (result == null) {
           return null;
       }
       var startParts = result[0].split("-");
       var endParts = result[1].split("-");
       if (startParts == null || endParts == null) {
           return null;
       }

       var startYear = startParts[0];
       var startMonth = startParts[1];
       var startDate = startParts[2];

       var endYear = endParts[0];
       var endMonth = endParts[1];
       var endDate = endParts[2];

       return [new Date(startYear, startMonth, startDate),
               new Date(endYear, endMonth, endDate)];
   }

    Calendar.toDateString = function(date,rangeType){
        var dateString = "";
        switch (rangeType){
            case 'date':
                dateString = date.getFullYear() + "-" + (date.getMonth() < 9 ? 0 : "") + (date.getMonth() + 1) + "-" + (date.getDate() < 10 ? 0 : "") + date.getDate();
                break;
            case 'week':
                var weekInfo = getWeekNumber(date.getFullYear(), date.getMonth(), date.getDate());
                dateString = weekInfo[0] + "/" + weekInfo[1];
                break;
            case 'month':
                dateString = date.getFullYear() + "/" + (date.getMonth() < 9 ? 0 : "") + (date.getMonth() + 1);
                break;
            case 'year':
                dateString =  date.getFullYear();
                break;
        }
        return dateString;
    }

    Calendar.dateChanged = function(date, rangeType) {
        var oldTimeUnit = Calendar.timeUnit;

        Calendar.timeUnit = rangeType;

        var dateLabel,
            state = "timeline/" + Calendar.timeUnit + "/" + date;
        Calendar.tabState = Calendar.timeUnit + "/" + date;
        switch (Calendar.timeUnit){
            case "date":
                var dateSplits = date.split("-"),
                    d = new Date(Number(dateSplits[0]),Number(dateSplits[1])-1,Number(dateSplits[2]));
                dateLabel = d.getDayName() +
                                ", " + d.getMonthName() + " " + d.getDate() +
                                ", " + (d.getFullYear());
                break;
            case "week":
                // TODO: Use the server's view of weeks, rather than determining weeks on the client
                var dateSplits = date.split("/");
                var range = getDateRangeForWeek(parseInt(dateSplits[0],10),parseInt(dateSplits[1],10));
                dateLabel = range[0].getMonthName() + " " + range[0].getDate() + " - " +
                            range[1].getMonthName() + " " + range[1].getDate() + " " + range[1].getFullYear();
                break;
            case "month":
                var dateSplits = date.split("/");
                dateLabel = DateUtils.getMonthFullName(parseInt(dateSplits[1])-1) + " " + dateSplits[0];
                break;
            case "year":
                dateLabel = date;
                break;
        }
        FlxState.router.navigate("app/calendar/" + state, {trigger: false, replace: true});
        FlxState.saveState("calendar", state);

        if (oldTimeUnit != Calendar.timeUnit)
            Builder.createTabs(Calendar);

        updateDisplays();

        $("#currentTimespanLabel span").html(dateLabel);
        updateDatepicker();
    };

    var viewBtnIds = {date:"#dayViewBtn",week:"#weekViewBtn",month:"#monthViewBtn",year:"#yearViewBtn"};

    function updateDisplays(state){
        for (var type in viewBtnIds){
            $(viewBtnIds[type]).removeClass("active");
        }
        $(viewBtnIds[state.timeUnit]).addClass("active");
    }

	return Calendar;
});