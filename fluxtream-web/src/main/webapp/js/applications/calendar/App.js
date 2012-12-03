define(["core/Application", "core/FlxState", "applications/calendar/Builder", "libs/bootstrap-datepicker",
        "ConnectorConfig"],
       function(Application, FlxState, Builder, ConnectorConfig) {

	var Calendar = new Application("calendar", "Candide Kemmler", "icon-calendar");

    Calendar.currentTabName = Builder.tabs["DAY"][0];
    Calendar.currentTab = null;
    Calendar.tabState = null;
    Calendar.digest = null;
    Calendar.timeUnit = "DAY";
    Calendar.digestTabState = false;
    Calendar.tabParam = null;

	var start, end;
    Calendar.connectorEnabled = {"default":{}};
    var buttons = {};

	Calendar.setup = function() {
        $.ajax("/api/connectors/filters",{
            success:function(data){
                for (var member in data){
                    Calendar.connectorEnabled[member] = data[member];
                }
            }

        });
		$(".menuNextButton").click(function(e) {
            if (Calendar.currentTab.timeNavigation("next"))
                return;
			fetchState("POST", "/api/calendar/nav/incrementTimespan?state=" + Calendar.tabState); });
		$(".menuPrevButton").click(function(e) {
            if (Calendar.currentTab.timeNavigation("prev"))
                return;
			fetchState("POST", "/api/calendar/nav/decrementTimespan?state=" + Calendar.tabState); });
		$(".menuTodayButton").click(function(e) {
            if (Calendar.currentTab.timeNavigation("today"))
                return;
			fetchState("POST", "/api/calendar/nav/setToToday?timeUnit=DAY");
		});
        Builder.init(this);
	};

	Calendar.initialize = function () {
		_.bindAll(this);

        FlxState.router.route(/^app\/calendar\/?(.*?)$/, "", function(path) {
            var pathElements = path.split("/");
            if (pathElements[pathElements.length - 1] === '') {
                pathElements.pop();
            }
            if (_.isEmpty(pathElements)) {
                App.invalidPath();
            }
            var tabName = pathElements.shift(),
                timeUnit = pathElements.shift(),
                validTabName = _.include(["clock","map","diary","photos","list","timeline","dashboards"], tabName),
                validTimeUnit = _.include(["date","week","month","year"], timeUnit);
            if (!validTabName || !validTimeUnit) {
                App.invalidPath();
                return;
            }
            if (!Builder.tabExistsForTimeUnit(tabName, Calendar.timeUnit)) {
                tabName = Builder.tabs[Calendar.timeUnit][0];
            }
            var renderPathElements = [tabName, timeUnit];
            switch (timeUnit) {
                case "date":
                    var date = pathElements.shift();
                    if (_.isUndefined(date)) {
                        App.invalidPath();
                        return;
                    }
                    renderPathElements.push(date);
                    break;
                case "week":
                    var year = pathElements.shift(),
                        week = pathElements.shift();
                    if (_.isUndefined(year) || _.isUndefined(week)) {
                        App.invalidPath();
                        return;
                    }
                    renderPathElements.push(year);
                    renderPathElements.push(week);
                    break;
                case "month":
                    var year = pathElements.shift(),
                        month = pathElements.shift();
                    if (_.isUndefined(year) || _.isUndefined(month)) {
                        App.invalidPath();
                        return;
                    }
                    renderPathElements.push(year);
                    renderPathElements.push(month);
                    break;
                case "year":
                    var year = pathElements.shift();
                    if (_.isUndefined(year)) {
                        App.invalidPath();
                        return;
                    }
                    renderPathElements.push(year);
                    break;
            }
            var tabParam = pathElements.shift();
            if (!_.isUndefined(tabParam)) {
                renderPathElements.push(tabParam);
            }
            Calendar.render(renderPathElements.join('/'));
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
			fetchState("POST", "/api/calendar/nav/setToToday?timeUnit=DAY");
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
            document.title = "Fluxtream Calendar | " + $("#currentTimespanLabel").text().trim() + " (" + Calendar.currentTabName + ")";
			Builder.updateTab(Calendar.digest, Calendar);
			FlxState.router.navigate("app/calendar/" + state + (Calendar.tabParam == null ? "" : "/" + Calendar.tabParam));
			FlxState.saveState("calendar", state);
			return;
		} else {
            Builder.bindTimeUnitsMenu(Calendar);
            if ("DAY"===Calendar.timeUnit) {
                Calendar.tabParam = splits[3];
				fetchState("GET", "/api/calendar/nav/getDate?date=" + splits[2]);
			} else if ("WEEK"===Calendar.timeUnit) {
                Calendar.tabParam = splits[4];
				fetchState("GET", "/api/calendar/nav/getWeek?year=" + splits[2] + "&week=" + splits[3]);
			} else if ("MONTH"===Calendar.timeUnit) {
                Calendar.tabParam = splits[4];
				fetchState("GET", "/api/calendar/nav/getMonth?year=" + splits[2] + "&month=" + splits[3]);
			} else if ("YEAR"===Calendar.timeUnit) {
                Calendar.tabParam = splits[3];
				fetchState("GET", "/api/calendar/nav/getYear?year=" + splits[2]);
			}
		}
	};

    Calendar.setTabParam = function(tabParam){
        Calendar.tabParam = tabParam;
        FlxState.router.navigate("app/calendar/" + Calendar.currentTabName + "/" + Calendar.tabState + (Calendar.tabParam == null ? "" : "/" + Calendar.tabParam));
        FlxState.saveState("calendar", Calendar.currentTabName + "/" + Calendar.tabState + (Calendar.tabParam == null ? "" : "/" + Calendar.tabParam));
    }

	function fetchState(verb, url) {
        $(".calendar-navigation-button").addClass("disabled");
		$(".loading").show();
		$("#tabs").css("opacity", ".3");
		$.ajax({ url:url,
            type: verb,
			success : function(response) {
				if (Calendar.currentTab) {
					Calendar.currentTab.saveState();
				}
				Calendar.tabState = response.state;
                updateDisplays();
                Calendar.start = response.start;
                Calendar.end  = response.end;
				FlxState.router.navigate("app/calendar/" + Calendar.currentTabName + "/" + response.state + (Calendar.tabParam == null ? "" : "/" + Calendar.tabParam));
				FlxState.saveState("calendar", Calendar.currentTabName + "/" + response.state + (Calendar.tabParam == null ? "" : "/" + Calendar.tabParam));
                document.title = "Fluxtream Calendar | " + response.currentTimespanLabel + " (" + Calendar.currentTabName + ")";
				$("#currentTimespanLabel span").html(response.currentTimespanLabel);
                updateDatepicker();
                fetchCalendar("/api/calendar/all/" + response.state,response.state);
			},
			error : function() {
				alert("error");
			}
		});
	}

    function updateDatepicker(){
        switch (Calendar.timeUnit){
            case "DAY":
                setDatepicker(Calendar.tabState.split("/")[1]);
                break;
            case "WEEK":
                var splits = Calendar.tabState.split("/");
                if (typeof splits[2]=="undefined") {
                    setDatepicker(splits[1]);
                } else {
                    var d = getDateRangeForWeek(splits[1],splits[2])[0],
                        datePickerDate = App.formatDateAsDatePicker(d);
                    setDatepicker(datePickerDate);
                }
                break;
            case "MONTH":
                var splits = Calendar.tabState.split("/");
                var d = new Date(splits[1],splits[2]-1,1,0,0,0,0);
                setDatepicker(App.formatDateAsDatePicker(d));
                break;
            case "YEAR":
                var d = new Date(Calendar.tabState.split("/")[1],0,1,0,0,0,0);
                setDatepicker(App.formatDateAsDatePicker(d));
                break;
        }
    }

	function setDatepicker(currentDate) {
        $(".datepicker.dropdown-menu").remove();
        $("#datepicker").replaceWith("<a data-date-format=\"yyyy-mm-dd\" id=\"datepicker\"><i class=\"icon-calendar icon-large\"></i></a>");
        $("#datepicker").attr("data-date", currentDate);
		$("#datepicker").unbind("changeDate");
		$("#datepicker").datepicker().on(
			"changeDate", function(event) {
                if (Calendar.timeUnit == "DAY"){
                    var formatted = App._formatDateAsDatePicker(event.date.getUTCFullYear(),
                        event.date.getUTCMonth(),
                        event.date.getUTCDate());
                    if (Calendar.currentTab.timeNavigation("set/date/" + formatted)){
                        $(".datepicker").hide();
                        return;
                    }
                    fetchState("GET", "/api/calendar/nav/getDate?date=" + formatted + "&state=" + Calendar.tabState);
                }
                else if (Calendar.timeUnit == "WEEK"){
                    var weekNumber = getWeekNumber(event.date.getUTCFullYear(),
                        event.date.getUTCMonth(),
                        event.date.getUTCDate());
                    var range = getDateRangeForWeek(weekNumber[0],weekNumber[1]);
                    if (Calendar.currentTab.timeNavigation("set/week/" + App.formatDateAsDatePicker(range[0]) + "/" + App.formatDateAsDatePicker(range[1]))){
                        $(".datepicker").hide();
                        return;
                    }
                    fetchState("GET", "/api/calendar/nav/getWeek?week=" + weekNumber[1] + "&year=" + weekNumber[0] + "&state=" + Calendar.tabState);
                }
				$(".datepicker").hide();
			}
		);
        $("#datepicker").click(function(){
            if (Calendar.timeUnit == "MONTH" || Calendar.timeUnit == "YEAR"){
                $(".datepicker-days .switch").click();
            }
            if (Calendar.timeUnit == "YEAR"){
                $(".datepicker-months .switch").click();
            }
        });
        $(".datepicker-years td").click(function(event){
            if (Calendar.timeUnit == "YEAR" && $(event.target).hasClass("year")){
                if (Calendar.currentTab.timeNavigation("set/year/" + $(event.target).text())){
                    $(".datepicker").hide();
                    return;
                }
                fetchState("GET", "/api/calendar/nav/getYear?year=" + $(event.target).text() + "&state=" + Calendar.tabState);
                $(".datepicker").hide();
            }
        });
        $(".datepicker-months td").click(function(event){
            if (Calendar.timeUnit == "MONTH" && $(event.target).hasClass("month")){
                var month;
                switch ($(event.target).text()){
                    case "Jan":
                        month = 1;
                        break;
                    case "Feb":
                        month = 2;
                        break;
                    case "Mar":
                        month = 3;
                        break;
                    case "Apr":
                        month = 4;
                        break;
                    case "May":
                        month = 5;
                        break;
                    case "Jun":
                        month = 6;
                        break;
                    case "Jul":
                        month = 7;
                        break;
                    case "Aug":
                        month = 8;
                        break;
                    case "Sep":
                        month = 9;
                        break;
                    case "Oct":
                        month = 10;
                        break;
                    case "Nov":
                        month = 11;
                        break;
                    case "Dec":
                        month = 12;
                        break;
                }
                if (Calendar.currentTab.timeNavigation("set/month/" + $(".datepicker-months .switch").text() + "/" + month)){
                    $(".datepicker").hide();
                    return;
                }
                fetchState("GET", "/api/calendar/nav/getMonth?year=" + $(".datepicker-months .switch").text() + "&month=" + month + "&state=" + Calendar.tabState);
                $(".datepicker").hide();
            }
        });
        if (Calendar.timeUnit == "WEEK"){
            var dayStart = parseInt(currentDate.split("-")[2],10);
            var rowElements = $(".datepicker-days tr");
            for (var j=0; j<rowElements.length; j++) {
                var row = $(rowElements[j]);
                var element = $(row.children()[0]);
                var isDayStart = (element.text() == dayStart);
                if (isDayStart) {
                    for (var i=0; i<rowElements.length;i++)
                        $(row.children()[i]).addClass("active");
                }
            }
        }
	}

	function fetchCalendar(url,state) {
		$.ajax({ calendarState:state, url: url,
			success : function(response) {
                console.log(response);
                if (typeof response.result!="undefined" && response.result==="KO") {
                    alert(response.message);
                    return;
                }
                if (Calendar.timeUnit==="DAY")
                    handleCityInfo(response);
                else
                    $("#mainCity").empty();
                Calendar.digest = response;
                Calendar.digestTabState = this.calendarState;
                enhanceDigest(Calendar.digest);
                processDigest(Calendar.digest);
				Builder.updateTab(Calendar.digest, Calendar);
				$("#tabs").css("opacity", "1");
				$(".calendar-navigation-button").removeClass("disabled");
				$(".loading").hide();
                Builder.handleNotifications(response);
			},
			error: function() {
				alert("error fetching calendar");
			}
		});
	}

    function getTemplate(digest,i,j){
        App.loadMustacheTemplate("applications/calendar/facetTemplates.html",digest.selectedConnectors[i].facetTypes[j],function(template){
            if (template == null)
                console.log("WARNING: no template found for " + digest.selectedConnectors[i].facetTypes[j] + ".");
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
            var filterLabel = digest.selectedConnectors[i].prettyName;
            if (typeof (App.getConnectorConfig(digest.selectedConnectors[i].connectorName).filterLabel)!="undefined")
                filterLabel = App.getConnectorConfig(digest.selectedConnectors[i].connectorName).filterLabel;
            var button = $('<li><a href="#" id="flx-connector-btn-' + digest.selectedConnectors[i].connectorName + '" class="flx-active '
                               + enabled + '">' + filterLabel + '</button></li>');
            if (enabled == "")
                button.children().css("border-bottom-color",App.getConnectorConfig(digest.selectedConnectors[i].connectorName).color);
            selectedConnectors.append(button);
            button = $(button.children()[0]);
            buttons[digest.selectedConnectors[i].connectorName] = button;
            button.click({button:button,objectTypeNames:digest.selectedConnectors[i].facetTypes,connectorName:digest.selectedConnectors[i].connectorName}, function(event){
                event.preventDefault();
                $(document).click(); //needed for click away to work on tooltips in clock tab
                connectorClicked(event.data.button,event.data.objectTypeNames,event.data.connectorName);
                var uploadData = {};
                for (var member in Calendar.connectorEnabled){
                    if (member != "default")
                        uploadData[member] = Calendar.connectorEnabled[member];
                }
                $.ajax("/api/connectors/filters",{
                    type:"POST",
                    data:{filterState:JSON.stringify(uploadData)}
                });
                for (var member in uploadData){
                    Calendar.connectorEnabled[member] = uploadData[member];
                }

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
                        var formatted = addCommas(params.facets[i][member]);
                        params.facets[i]["_"+member] = formatted;
                    }
                }
            }
        }
        return digest.detailsTemplates[data.type].render(params);
    }


    function addCommas(nStr) {
       nStr += '';
       x = nStr.split('.');
       x1 = x[0];
       x2 = x.length > 1 ? '.' + x[1] : '';
       var rgx = /(\d+)(\d{3})/;
       while (rgx.test(x1)) {
           x1 = x1.replace(rgx, '$1' + ',' + '$2');
       }
       return x1 + x2;
    };


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

   var w2date = function(year, wn, dayNb){
       var j10 = new Date( year,0,10,12,0,0),
           j4 = new Date( year,0,4,12,0,0),
           mon1 = j4.getTime() - j10.getDay() * 86400000;
       return new Date(mon1 + ((wn - 1)  * 7  + dayNb) * 86400000);
   };

    Calendar.toDateString = function(date,rangeType){
        var dateString = "";
        switch (rangeType){
            case 'DAY':
                dateString = date.getFullYear() + "-" + (date.getMonth() < 9 ? 0 : "") + (date.getMonth() + 1) + "-" + (date.getDate() < 10 ? 0 : "") + date.getDate();
                break;
            case 'WEEK':
                var weekInfo = getWeekNumber(date.getFullYear(), date.getMonth(), date.getDate());
                dateString = weekInfo[0] + "/" + weekInfo[1];
                break;
            case 'MONTH':
                dateString = date.getFullYear() + "/" + (date.getMonth() < 9 ? 0 : "") + (date.getMonth() + 1);
                break;
            case 'YEAR':
                dateString =  date.getFullYear();
                break;
        }
        return dateString;
    }

    Calendar.dateChanged = function(date, rangeType) {
        var oldTimeUnit = Calendar.timeUnit;

        Calendar.timeUnit = rangeType;

        var dateLabel, state;
        var daysOfWeek = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];
        var monthsOfYear = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep",
                            "Oct", "Nov", "Dec"];

        var monthsOfYearFull = ["January","February","March","April","May","June","July",
                                "August","September","October","November","December"];
        switch (Calendar.timeUnit){
            case "DAY":
                state = "timeline/date/" + date;
                Calendar.tabState = "date/" + date;
                var dateSplits = date.split("-"),
                    d = new Date(Number(dateSplits[0]),Number(dateSplits[1])-1,Number(dateSplits[2]));
                dateLabel = daysOfWeek[d.getDay()] +
                                ", " + monthsOfYear[d.getMonth()] + " " + d.getDate() +
                                ", " + (d.getFullYear());
                break;
            case "WEEK":
                // TODO: Use the server's view of weeks, rather than determining weeks on the client
                state = "timeline/week/" + date;
                Calendar.tabState = "week/" + date;
                var dateSplits = date.split("/");
                var range = getDateRangeForWeek(parseInt(dateSplits[0],10),parseInt(dateSplits[1],10));
                dateLabel = monthsOfYear[range[0].getMonth()] + " " + range[0].getDate() + " - " +
                            monthsOfYear[range[1].getMonth()] + " " + range[1].getDate() + " " + range[1].getFullYear();
                break;
            case "MONTH":
                state = "timeline/month/" + date;
                Calendar.tabState = "month/" + date;
                var dateSplits = date.split("/");
                dateLabel = monthsOfYearFull[parseInt(dateSplits[1])-1] + " " + dateSplits[0];
                break;
            case "YEAR":
                state = "timeline/year/" + date;
                Calendar.tabState = "year/" + date;
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

    var viewBtnIds = {DAY:"#dayViewBtn",WEEK:"#weekViewBtn",MONTH:"#monthViewBtn",YEAR:"#yearViewBtn"};

    function updateDisplays(){
        var rangeType;
        switch (Calendar.tabState.substring(0,Calendar.tabState.indexOf("/"))){
            case "date":
                rangeType = "DAY";
                break;
            case "week":
                rangeType = "WEEK";
                break;
            case "month":
                rangeType = "MONTH";
                break;
            case "year":
                rangeType = "YEAR";
                break;
        }
        for (var type in viewBtnIds){
            if (type == rangeType){
                $(viewBtnIds[type]).addClass("active");
            }
            else{
                $(viewBtnIds[type]).removeClass("active");
            }
        }
    }

	return Calendar;
});