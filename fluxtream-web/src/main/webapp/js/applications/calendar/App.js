define(["core/Application", "core/FlxState", "applications/calendar/Builder", "libs/bootstrap-datepicker",
        "ConnectorConfig", "core/DateUtils", "core/StringUtils"],
       function(Application, FlxState, Builder, Datepicker, ConnectorConfig, DateUtils, StringUtils) {

	var Calendar = new Application("calendar", "Candide Kemmler", "icon-calendar");

    var needDigestReload = false;

    Calendar.currentTabName = Builder.tabs["date"][0];
    Calendar.currentTab = null;
    Calendar.tabState = null;
    Calendar.digest = null;
    Calendar.timeUnit = "date";
    Calendar.digestTabState = false;
    Calendar.tabParam = null;
    Calendar.connectorEnabled = {"default":{}};
    Calendar.timespanInited = false;
    Calendar.timeRange = {
        updated: true,
        start: null,
        end: null
    };

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
        Calendar.timespanInited = true;
        document.title = "Fluxtream Calendar | " + currentTimespan + " (" + Calendar.currentTabName + ")";
        $("#currentTimespanLabel span").html(currentTimespan);
    }

    function startLoading() {
        loading = true;
        //$(".calendar-navigation-button").addClass("disabled");
        $(".loading").show();
        $("#tabs").css("opacity", ".3");
    }

    function stopLoading() {
        if (fetchId == latestFetchFinished){
            $("#tabs").css("opacity", "1");
            //$(".calendar-navigation-button").removeClass("disabled");
            $(".loading").hide();
        }
        $(window).resize();
    }

    function handleError(msg) {
        return function(xhr, status, error) {
            stopLoading();
            console.log(xhr, status, error);
            alert(msg);
        }
    }

    Calendar.stopLoading = stopLoading;

    Calendar.fetchState = function(url, params) {
        startLoading();
        $.ajax({
           url: url,
           type: "GET",
           data: params,
           success: function(response) {
               Calendar.timeRange.start = response.start;
               Calendar.timeRange.end = response.end;
               updateTimespan(response.currentTimespanLabel);
               Calendar.timeRange.updated = true;
               Calendar.navigateState(Calendar.currentTabName + "/" + response.state);
               // TODO: Change visible date in the datepicker to Sunday
               // TODO: Would be nice to use updateDatepicker, but what's the state argument?
               //stopLoading is now called by the tab once it's done processing the data.
           },
           error: handleError("failed to fetch next calendar state!")
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

    function fetchTimespan(state,doneLoadingId) {
        $.ajax({
            url: "/api/calendar/nav/model",
            async: false,
            type: "GET",
            data: {state: state.tabState},
            success: function(response) {
                Calendar.timeRange.start = response.start;
                Calendar.timeRange.end = response.end;
                updateTimespan(response.currentTimespanLabel);
                stopLoading(doneLoadingId);
            },
            error: handleError("failed to fetch timespan label!")
        });
    }

    function setDocumentTitle() {
        document.title = "Fluxtream Calendar | " + $("#currentTimespanLabel").text().trim() + " (" + Calendar.currentTabName + ")";
    }

	Calendar.renderState = function(state, forceUpdate) {
        startLoading();
        if (typeof state == "string")
            state = Calendar.parseState(state);
        if (!Calendar.timespanInited) {
            // NOTE: when loading a URL like /app/calendar/date/2012-12-25 directly,
            // the FlxState routes invoke renderState() directly instead of going
            // through fetchState. That bypasses the timespan label fetching, so we
            // need to do that here.
            fetchTimespan(state);
            //stopLoading();
        }
        var tabChanged = Calendar.tabState === state.tabState;
        Calendar.tabState = state.tabState;
        Calendar.currentTabName = state.tabName;
        Calendar.timeUnit = state.timeUnit;
        Calendar.updateButtonStates();
        Builder.createTabs(Calendar);
        if (tabChanged) {
            setDocumentTitle();
            Builder.updateTab(Calendar.digest, Calendar);
            if (forceUpdate || needDigestReload)
                fetchCalendar(state)
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
                return DateUtils.getDateRangeForWeek(state.year, state.week)[0];
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

    var fetchId = 0;
    var latestFetchFinished = 0;
    var lastFetch = null;

	function fetchCalendar(state) {
        needDigestReload = false;
        startLoading();
        var thisFetchId = ++fetchId;
        if (lastFetch != null)
            lastFetch.abort();
        lastFetch = $.ajax({
            url: "/api/calendar/all/" + state.tabState,
			success : function(response) {
                if (thisFetchId != fetchId)//we litter the callback with these in case a we got to the callback but a new request started
                    return;
                latestFetchFinished = thisFetchId;
                if (response.result === "KO") {
                    handleError(response.message)();
                    return;
                }
                Calendar.digest = response;
                Calendar.digestTabState = state.tabState;
                if (thisFetchId != fetchId)
                    return;
                enhanceDigest(Calendar.digest);
                if (thisFetchId != fetchId)
                    return;
                Builder.updateTab(Calendar.digest, Calendar);
                if (thisFetchId != fetchId)
                    return;
                processDigest(Calendar.digest);
                if (thisFetchId != fetchId)
                    return;
                Builder.updateTab(Calendar.digest, Calendar); //TODO: shouldn't be calling this twice really
                if (thisFetchId != fetchId)
                    return;
                Builder.handleNotifications(response);
                if (Calendar.timeUnit==="date") {
                    handleCityInfo(response);
                } else {
                    $("#mainCity").empty();
                }
			},
			error: function(){
                if (thisFetchId != fetchId)//we don't really care about errors on old fetches
                    return;
                latestFetchFinished = thisFetchId;
                handleError("failed to fetch calendar data!")
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
                console.log("loading facetType: " + facetType);
                loadTemplate(facetType);
            });
        });
        loadTemplate("fluxtream-address");
        for (var connectorId in digest.cachedData){
            $.each(digest.cachedData[connectorId], function(i, connector) {
                connector.getDetails = function(array,showDate){
                    if (typeof(array) == "boolean"){
                        showDate = array;
                        array = null;
                    }
                    if (array == null)
                        array = [this];
                    return buildDetails(digest,array,showDate);
                };
                connector.shouldGroup = function(facet){
                    return shouldFacetsGroup(this,facet);
                };
            });
            digest.cachedData[connectorId].hasImages = false;
            switch (connectorId){
                case "picasa-photo":
                case "flickr-photo":
                case "fluxtream_capture-photo":
                    digest.cachedData[connectorId].hasImages = true;
                    break;
                case "mymee-observation":
                    for (var i = 0; i < digest.cachedData[connectorId].length && !digest.cachedData[connectorId].hasImages; i++){
                        digest.cachedData[connectorId].hasImages = digest.cachedData[connectorId][i].photoUrl != null;
                    }
                    break;
            }

            for (var i = 0; i < digest.cachedData[connectorId].length; i++){
                var facet = digest.cachedData[connectorId][i];
                if (digest.cachedData[connectorId].hasImages){
                    switch (connectorId){
                        case "picasa-photo":
                        case "flickr-photo":
                        case "fluxtream_capture-photo":
                            facet.hasImage = true;
                            break;
                        case "mymee-observation":
                            facet.hasImage = facet.photoUrl != null;
                            break;
                    }
                    if (facet.hasImage) {
                        var photo42 = facet.photoUrl;
                        if (typeof(facet.thumbnailUrl)!="undefined")
                            photo42 = facet.thumbnailUrl;
                        if (facet.thumbnailUrls != null){
                            var bestMatch = -1;
                            var bestMatchAmmount = 0;
                            for (var j in facet.thumbnailSizes){
                                var size = facet.thumbnailSizes[j];
                                var matchAmmount = (size.width - 42) * (size.width - 42) + (size.height - 42) * (size.height - 42);
                                if (bestMatch == -1 || matchAmmount < bestMatchAmmount){
                                    bestMatchAmmount = matchAmmount;
                                    bestMatch = j;
                                }
                            }
                            if (bestMatch != -1){
                                photo42 = digest.cachedData[connectorId][i].thumbnailUrls[bestMatch];
                            }
                        }
                        facet.photo42 = photo42;
                    }
                }
                if (facet.start == undefined){
                    if (facet.date != undefined && facet.date != null){
                        var dateParts = facet.date.split("-");
                        var year = parseInt(dateParts[0]);
                        var month = parseInt(dateParts[1]) - 1;
                        var day = parseInt(dateParts[2]);
                        if (facet.startTime != null){
                            var hours = parseInt(facet.startTime.hours);
                            var minutes = parseInt(facet.startTime.minutes);
                            if (facet.startTime.ampm == "pm")
                                hours += 12;
                            facet.start = new Date(year,month,day,hours,minutes,0,0).getTime();
                        }
                        if (facet.endTime != null) {
                            var hours = parseInt(facet.endTime.hours);
                            var minutes = parseInt(facet.endTime.minutes);
                            if (facet.endTime.ampm == "pm")
                                hours += 12;
                            facet.end = new Date(year,month,day,hours,minutes,0,0).getTime();
                            if (facet.end < facet.start){
                                facet.end = new Date(year,month,day+1,hours,0,0).getTime();
                            }
                        }
                    }
                }
            }
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

    function buildDetails(digest,facets,showDate){
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
                    case "date":
                        if (showDate){
                            newFacet[member] = data[member];
                        }
                        break;
                    case "source":
                        switch (data[member]){
                            case "GOOGLE_LATITUDE":
                                newFacet[member] = "Google Latitude";
                                break;
                            case "RUNKEEPER":
                                newFacet[member] = "Runkeeper";
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
                    case "averageHeartRate":
                        newFacet[member] = data[member];
                        newFacet.showHeartRate = newFacet[member] != 0;
                        break;
                    case "total_climb":
                        newFacet[member] = data[member];
                        newFacet.showClimb = newFacet[member] != 0;
                        break;
                    default:
                        newFacet[member] = data[member];
                }
            }
            if (showDate){
                newFacet.displayDate = App.formatDate(data.start + digest.timeZoneOffset,false,true);
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

    Calendar.toState = function(tabName, timeUnit, date) {
        var dateString = "";
        switch (timeUnit){
            case 'date':
                dateString = date.getFullYear() + "-" + (date.getMonth() < 9 ? 0 : "") + (date.getMonth() + 1) + "-" + (date.getDate() < 10 ? 0 : "") + date.getDate();
                break;
            case 'week':
                var weekInfo = DateUtils.getWeekNumber(date.getFullYear(), date.getMonth(), date.getDate());
                dateString = weekInfo[0] + "/" + weekInfo[1];
                break;
            case 'month':
                dateString = date.getFullYear() + "/" + (date.getMonth() < 9 ? 0 : "") + (date.getMonth() + 1);
                break;
            case 'year':
                dateString =  date.getFullYear();
                break;
        }
        return Calendar.parseState([tabName, timeUnit, dateString].join("/"));
    };

    Calendar.toURL = function(state) {
        return ["app/calendar", state.tabName, state.tabState].join("/");
    };

    Calendar.toStateURL = function(state){
        return [state.tabName, state.tabState].join("/");
    }

    /**
     * Updates the state of the current tab using the given state object. The
     * tab is not re-rendered, but Calendar interface elements may be updated as
     * necessary (e.g. time units buttons, datepicker, etc.)
     *
     * @param state  new state to be loaded
     */
    Calendar.changeTabState = function(state,waitUpdate) {
        if (state.tabName !== Calendar.currentTabName) {
            var msg = "invalid state: cannot use changeTabState() to switch tabs!";
            console.log(msg);
            //alert(msg); //the alert is commented out because the timeline tab sometimes calls this late and will give the user a nasty popup
            return;
        }
        var updated = false;
        if (state.timeUnit !== Calendar.timeUnit) {
            Calendar.timeUnit = state.timeUnit;
            Builder.createTabs(Calendar);
            updateDisplays(state);
            updated = true;
        }
        if (state.tabState !== Calendar.tabState) {
            Calendar.tabState = state.tabState;
            fetchTimespan(state);
            updated = true;
        }
        if (updated) {
            setDocumentTitle();
            updateDatepicker(state);
            if (waitUpdate){
                needDigestReload = true;
            }
            else{
                fetchCalendar(state);
            }
            FlxState.router.navigate(Calendar.toURL(state), {trigger: false, replace: true});
            FlxState.saveState("calendar", Calendar.toStateURL(state));
        }
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