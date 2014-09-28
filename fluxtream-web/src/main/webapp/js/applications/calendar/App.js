define(["core/Application", "core/FlxState", "applications/calendar/Builder", "libs/bootstrap-datepicker",
        "ConnectorConfig", "core/DateUtils", "core/StringUtils", "applications/calendar/tabs/photos/PhotoUtils"],
       function(Application, FlxState, Builder, Datepicker, ConnectorConfig, DateUtils, StringUtils,
           PhotoUtils) {

	var Calendar = new Application("calendar", "Candide Kemmler", "icon-calendar", "Calendar App");

    var needDigestReload = false;
    var currentCityPool;
    var foursquareVenueTemplate;

    Calendar.currentTabName = Builder.tabs["date"][0];
    Calendar.currentTab = null;
    Calendar.tabState = null;
    Calendar.digest = null;
    Calendar.weather = null;
    Calendar.timeUnit = "date";
    Calendar.digestTabState = false;
    Calendar.tabParam = null;
    Calendar.connectorEnabled = {"default":{}};
    Calendar.timespanState = null;
    Calendar.detailsTemplates = {};

    App.loadAllMustacheTemplates("applications/calendar/facetTemplates.html",function(templates){
        for (var templateId in templates){
            Calendar.detailsTemplates[templateId] = templates[templateId];
        }
    });

    Calendar.dateAxisCursorPosition = null;

    Calendar.timeRange = {
        updated: true,
        start: null,
        end: null
    };

	Calendar.setup = function() {
        $.ajax("/api/v1/connectors/filters", {
            async: false,
            success: function(data){
                for (var member in data){
                    Calendar.connectorEnabled[member] = data[member];
                }
            }
        });
        Builder.init(App, this);
        App.addDataUpdatesListener("calendarAppApiDataUpdateListener",function(update){
            if (update.apiData != null && Calendar.digest != null){
                var digestTimestamp = Calendar.digest.generationTimestamp;
                var stateToRequest = Calendar.timespanState;
                var connectorsToRefresh = [];
                for (var connector in update.apiData){
                    for (var objectType in update.apiData[connector]){
                        if ((update.apiData[connector][objectType].start < Calendar.digest.tbounds.end && update.apiData[connector][objectType].start >= Calendar.digest.tbounds.start) ||
                            (update.apiData[connector][objectType].end <= Calendar.digest.tbounds.end && update.apiData[connector][objectType].end > Calendar.digest.tbounds.start) ||
                            (update.apiData[connector][objectType].start <= Calendar.digest.tbounds.start && update.apiData[connector][objectType].end >= Calendar.digest.tbounds.end)){
                            connectorsToRefresh.push(update.connectorInfo[connector].apiKeyId + "-" + objectType);
                        }
                    }
                }
                $.ajax("/api/v1/calendar/" + connectorsToRefresh.join(",") + "/" + stateToRequest,{
                    success:function(result){
                        Calendar.mergeInFacets(result.facets,update.connectorInfo)
                    },
                    error:function(){
                        console.error(arguments);
                    }
                });
            }
        });
	};

	Calendar.initialize = function () {
		_.bindAll(this);
    };

    function updateTimespan(currentTimespan,currentState) {
        Calendar.timespanState = currentState;
        document.title = currentTimespan + " (" + Calendar.currentTabName + ")";
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

    function handleError(jqXHR, status, errorThrown) {
        stopLoading();
        var errorMessage = errorThrown + ": " + jqXHR.responseText;
        console.log(errorMessage);
        alert(errorMessage);
    }

    Calendar.stopLoading = stopLoading;

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
        Calendar.navigateState("clock/date/"+moment().format("YYYY-MM-DD"));
    };

    function updateTimeRange(digest, state) {
        Calendar.timeRange.start = digest.calendar.start;
        Calendar.timeRange.end = digest.calendar.end;
        if (Calendar.dateAxisCursorPosition * 1000 < Calendar.timeRange.start || Calendar.dateAxisCursorPosition * 1000 > Calendar.timeRange.end)
            Calendar.dateAxisCursorPosition = null;
        updateTimespan(digest.calendar.currentTimespanLabel,state.tabState);
        Calendar.timeRange.updated = true;
    }

    function fetchWeatherData() {
       $.ajax({ url: "/api/v1/calendar/weather/"+Calendar.tabState,
           success: function(response) {
               // we should check that time boundaries are in line with the digest data
               Calendar.weather = response;
               var label = weatherLabel();
               $(".ephemerisWrapper").remove();
               $("#mainCityMetadata").prepend($("<span class='ephemerisWrapper'>" + label + "&nbsp;&nbsp;</span>"));
           }
       });
    }

    function setDocumentTitle() {
        document.title = $("#currentTimespanLabel").text().trim() + " (" + Calendar.currentTabName + ")";
    }

	Calendar.renderState = function(state, forceUpdate) {
        startLoading();
        if (typeof state == "string")
            state = Calendar.parseState(state);

        var connectorToggleName;
        var sendConnectorToggled = false;

        //if we're showing a specific facet, we should make sure our state will hold the facet!
        if (this.params != null && this.params.facetToShow != null){
            var facet = this.params.facetToShow;
            state = Calendar.toState(state.tabName, state.timeUnit,new Date(facet.end == null ? facet.start : (facet.start + facet.end) / 2));

            connectorToggleName = this.params.facetToShow.type.split("-")[0];
            sendConnectorToggled = Calendar.connectorEnabled[state.tabName][connectorToggleName] == false;
            Calendar.connectorEnabled[state.tabName][connectorToggleName] = true;
        }

        if (Calendar.timespanState !== state.tabState) {
            // NOTE: when loading a URL like /app/calendar/date/2012-12-25 directly,
            // the FlxState routes invoke renderState() directly instead of going
            // through fetchState. That bypasses the timespan label fetching, so we
            // need to do that here.
            //fetchTimespan(state);
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
        if (sendConnectorToggled){
            for (var i = 0, li = Calendar.digest.selectedConnectors.length; i < li; i++){
                if (Calendar.digest.selectedConnectors[i].connectorName == connectorToggleName){
                    Calendar.currentTab.connectorToggled(connectorToggleName,Calendar.digest.selectedConnectors[i].facetTypes,true);
                    break;
                }
            }

        }

        App.state.saveState("calendar", Calendar.toStateURL(state));

        // Next time the page loads, won't accidentally believe that the timespan in the
        // title and calendar bar has already been initialized
        //Calendar.timespanInited = false;
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
                return new Date(state.year,state.month,1,0,0,0,0);
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
        var url = "/api/v1/calendar/all/" + state.tabState;
        if (App.buddyToAccess["isBuddy"]) url += "?"+App.BUDDY_TO_ACCESS_PARAM+"="+App.buddyToAccess["id"];
        lastFetch = $.ajax({
            url: url,
			success : function(response) {
                if (thisFetchId != fetchId)//we litter the callback with these in case a we got to the callback but a new request started
                    return;
                latestFetchFinished = thisFetchId;
                Calendar.digest = response;
                updateTimeRange(response, state);
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
                App.handleNotificationList(response.notifications);
                if (Calendar.timeUnit==="date") {
                    $(".ephemerisWrapper").show();
                    handleCityInfo(response);
                    fetchWeatherData();
                } else {
                    $(".ephemerisWrapper").hide();
                    $("#mainCity").empty();
                    $("#visitedCitiesDetails").hide();
                }
			},
            error: function(jqXHR, statusText, errorThrown) {
                if (thisFetchId != fetchId)//we don't really care about errors on old fetches
                    return;
                latestFetchFinished = thisFetchId;
                var errorMessage = errorThrown + ": " + jqXHR.responseText;
                console.log(errorMessage);
            }
		});
	}

    function addTimeInfo(facet){
        function pad(i){
           var is = ""+i;
           if (is.length<2) is = "0"+i;
           return is;
        }
        for (var member in facet){
            switch (member){
                case "eventStart":
                    var eventStart = moment(facet[member], "YYYYMMDD'T'HHmmss.SSSZ");
                    facet.startMinute = eventStart.hour()*60+eventStart.minute();
                    facet.startTime = {"hours" : eventStart.hour()>12?eventStart.hour()-12:eventStart.hour(), "minutes" : pad(eventStart.minute()), "ampm" : eventStart.hour()>=12?"pm":"am"};
                    facet.time = App.formatMinuteOfDay(facet.startMinute)[0];
                    facet.ampm = App.formatMinuteOfDay(facet.startMinute)[1];
                    facet.start = eventStart.utc().valueOf();
                    facet.date = DateUtils.constrainDate(eventStart, Calendar.digest.calendar.state);
                    break;
                case "eventEnd":
                    var eventEnd = moment(facet[member], "YYYYMMDD'T'HHmmss.SSSZ");
                    facet.endMinute = eventEnd.hour()*60+eventEnd.minute();
                    facet.endTime = {"hours" : eventEnd.hour()>12?eventEnd.hour()-12:eventEnd.hour(), "minutes" : pad(eventEnd.minute()), "ampm" : eventEnd.hour()>=12?"pm":"am"};
                    facet.end = eventEnd.utc().valueOf();
                    break;
            }
        }
        if (facet.type.indexOf("moves-")!=-1&&typeof(facet.activities)!="undefined"){
           for (var i=0; i<facet.activities.length;i++) {
               addTimeInfo(facet.activities[i]);
           }
        }
    }

    Calendar.processFacets = function(facets){
        if (facets == null || facets.length == 0 || facets[0].getDetails != null)
            return;
        for (var i = 0; i < facets.length; i++){
            var data = facets[i];
            addTimeInfo(data);
        }
        for (var i = 0, li = facets.length; i < li; i++){
            var facet = facets[i];
            facet.getDetails = function(array,showDate){
                if (typeof(array) == "boolean"){
                    showDate = array;
                    array = null;
                }
                if (array == null)
                    array = [this];
                return buildDetails(array,showDate);
            };
            facet.shouldGroup = function(facet){
                return shouldFacetsGroup(this,facet);
            };
            switch (facet.type){
                case "picasa-photo":
                case "flickr-photo":
                case "evernote-photo":
                case "up-serving":
                case "fluxtream_capture-photo":
                    facet.hasImage = true;
                    break;
                case "mymee-observation":
                case "sms_backup-sms":
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
                        photo42 = facet.thumbnailUrls[bestMatch];
                    }
                }
                facet.photo42 = photo42;
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
                        if (facet.startTime.ampm == "pm" && hours < 12)
                            hours += 12;
                        facet.start = new Date(year,month,day,hours,minutes,0,0).getTime();
                    }
                    if (facet.endTime != null) {
                        var hours = parseInt(facet.endTime.hours);
                        var minutes = parseInt(facet.endTime.minutes);
                        if (facet.endTime.ampm == "pm" && hours < 12)
                            hours += 12;
                        facet.end = new Date(year,month,day,hours,minutes,0,0).getTime();
                        if (facet.end < facet.start){
                            facet.end = new Date(year,month,day+1,hours,0,0).getTime();
                        }
                    }
                }
            }

            if (facet.type === "moves-move"){
                for (var j = 0, lj = facet.activities.length; j < lj; j++){
                    var subfacet = facet.activities[j];
                    subfacet.parentType = facet.type;
                    subfacet.parentId = facet.id;
                    subfacet.parentStartTime = facet.startTime;
                    subfacet.parentEndTime = facet.endTime;
                    subfacet.comment = facet.comment;
                    subfacet.getDetails = function(array,showDate){
                        if (typeof(array) == "boolean"){
                            showDate = array;
                            array = null;
                        }
                        if (array == null)
                            array = [this];
                        return buildDetails(array,showDate);
                    };
                    subfacet.shouldGroup = function(facet){
                        return shouldFacetsGroup(this,facet);
                    };

                    if (subfacet.start == undefined){
                        if (subfacet.date != undefined && subfacet.date != null){
                            var dateParts = subfacet.date.split("-");
                            var year = parseInt(dateParts[0]);
                            var month = parseInt(dateParts[1]) - 1;
                            var day = parseInt(dateParts[2]);
                            if (subfacet.startTime != null){
                                var hours = parseInt(subfacet.startTime.hours);
                                var minutes = parseInt(subfacet.startTime.minutes);
                                if (subfacet.startTime.ampm == "pm" && hours < 12)
                                    hours += 12;
                                subfacet.start = new Date(year,month,day,hours,minutes,0,0).getTime();
                            }
                            if (subfacet.endTime != null) {
                                var hours = parseInt(subfacet.endTime.hours);
                                var minutes = parseInt(subfacet.endTime.minutes);
                                if (subfacet.endTime.ampm == "pm" && hours < 12)
                                    hours += 12;
                                subfacet.end = new Date(year,month,day,hours,minutes,0,0).getTime();
                                if (subfacet.end < subfacet.start){
                                    subfacet.end = new Date(year,month,day+1,hours,0,0).getTime();
                                }
                            }
                        }
                    }
                }
            }

        }

    }

    function enhanceDigest(digest){
        if (typeof(digest.settings.messageDisplayCounters)!="undefined") {
            App.setupBeginnersFriendlyUI(digest.settings.messageDisplayCounters, digest.nApis);
        }
        if (digest.getConsensusCitiesList == null){
            digest.getConsensusCitiesList = function(){
                if (digest.metadata.timeUnit === "DAY")
                    return [digest.metadata.mainCity];
                return digest.metadata.consensusCities;
            };
            digest.getCitiesList = function(){
                if (digest.metadata.timeUnit === "DAY")
                    return [digest.metadata.mainCity];
                return digest.metadata.cities;
            };
        }
        var templatePath = "applications/calendar/facetTemplates.html";
        function loadTemplate(name) {
            if (Calendar.detailsTemplates[name] != null)
                return;
            App.loadMustacheTemplate(templatePath, name, function(template){
                if (template == null) {
                    console.log("WARNING: no template found for " + name + ".");
                }
                Calendar.detailsTemplates[name] = template;
            });
        }
        $.each(digest.selectedConnectors, function(i, connector) {
            $.each(connector.facetTypes, function(j, facetType) {
                loadTemplate(facetType);
            });
        });
        loadTemplate("foursquare-venue");
        loadTemplate("moves-move-activity");
        for (var connectorId in digest.facets){
            if (digest.facets[connectorId].processed === true)
                continue;
            digest.facets[connectorId].processed = true;
            Calendar.processFacets(digest.facets[connectorId]);
            digest.facets[connectorId].hasImages = false;
            switch (connectorId){
                case "evernote-photo":
                case "up-serving":
                case "picasa-photo":
                case "flickr-photo":
                case "fluxtream_capture-photo":
                    digest.facets[connectorId].hasImages = true;
                    break;
                case "mymee-observation":
                case "sms_backup-sms":
                    for (var i = 0; i < digest.facets[connectorId].length && !digest.facets[connectorId].hasImages; i++){
                        digest.facets[connectorId].hasImages = digest.facets[connectorId][i].hasImage;
                    }
                    break;
            }

            var connectorName = connectorId.split("-")[0];
            var config = App.getConnectorConfig(connectorName);
            var hasGeneralSettings = false;
            if (typeof(config.hasGeneralSettings)!="undefined"&&config.hasGeneralSettings)
                hasGeneralSettings = true;
            for (var i = 0; i < digest.facets[connectorId].length; i++){
                var facet = digest.facets[connectorId][i];
                if (typeof(config.isFilteredOut)!="undefined"&&config.isFilteredOut!=null) {
                    var filteredOut = config.isFilteredOut(facet, digest.settings.connectorSettings);
                    if (filteredOut) {
                        digest.facets[connectorId].splice(i, 1);
                        i = -1;
                        continue;
                    }
                }
            }
            for (var i = 0; i < digest.facets[connectorId].length; i++){
                var facet = digest.facets[connectorId][i];
                if (hasGeneralSettings && config.applySettings != null)
                    config.applySettings(facet, digest.settings.connectorSettings);
                if (digest.facets[connectorId].hasImages){
                    switch (connectorId){
                        case "picasa-photo":
                        case "flickr-photo":
                        case "up-serving":
                        case "evernote-photo":
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
                                photo42 = digest.facets[connectorId][i].thumbnailUrls[bestMatch];
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
                            if (facet.startTime.ampm == "pm" && hours > 12)
                                hours += 12;
                            facet.start = new Date(year,month,day,hours,minutes,0,0).getTime();
                        }
                        if (facet.endTime != null) {
                            var hours = parseInt(facet.endTime.hours);
                            var minutes = parseInt(facet.endTime.minutes);
                            if (facet.endTime.ampm == "pm" && hours > 12)
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

    }

    Calendar.mergeInFacets = function(newFacetsArray,connectorInfos){
        if (newFacetsArray.length == 0)
            return;
        //merge in the selected connectors
        for (var member in connectorInfos){
            var connectorInfo = connectorInfos[member];
            var inserted = false;
            for (var i = 0, li = Calendar.digest.selectedConnectors.length; i < li && !inserted; i++){
                if (Calendar.digest.selectedConnectors[i].apiKeyId == connectorInfo.apiKeyId){
                    Calendar.digest.selectedConnectors[i] = connectorInfo;
                    inserted = true;
                }
            }
            if (!inserted){
                Calendar.digest.selectedConnectors.push(connectorInfo);
                Calendar.digest.nApis++;
            }

        }
        for (var facetTypeId in newFacetsArray){//add all the new facets into the cached data
            Calendar.digest.facets[facetTypeId] = newFacetsArray[facetTypeId];
        }
        enhanceDigest(Calendar.digest);
        processDigest(Calendar.digest);//this will update the button states for any changes we made
        Calendar.digest.generationTimestamp = new Date().getTime();
        Calendar.digest.delta = true;
        Builder.updateTab(Calendar.digest, Calendar);
        Calendar.digest.delta = false;
    }

    function shouldFacetsGroup(facet1, facet2){
        if (facet1.type != facet2.type)
            return false;
        switch (facet1.type){
            case "twitter-dm":
                return facet1.sent == facet2.sent;
            case "sms_backup-call_log":
                return facet1.callType == facet2.callType;
            case "sms_backup-sms":
                return facet1.smsType == facet2.smsType;
        }
        return true;
    }

   /**
    * Inspects whats in the digest and sets the state of the filter buttons accordingly
    * @param digest
    */
    function processDigest(digest){
        $.each(digest.selectedConnectors, function(i, connector) {
            var buttonLink = Builder.getConnectorButton(connector,Calendar),
                button = buttonLink.parent();
            button.hide();
            var connectorConfig = App.getConnectorConfig(connector.connectorName);
            var connected = _.some(connector.facetTypes, function(facetType) {
                var hasTypedFacets = digest.facets[facetType] != null;
                //var objectType = facetType.split("-")[1];
                if(Calendar.currentTab.name==="photos") {
                    // handle special case of mymee observations
                    //if (hasTypedFacets&&objectType.indexOf("observation")!=-1){
                    //    hasTypedFacets = false;
                    //    for (var i=0;i<digest.facets[facetType].length; i++){
                    //        if (typeof(digest.facets[facetType][i].photoUrl)!="undefined") {
                    //            hasTypedFacets = true;
                    //            break;
                    //        }
                    //    }
                    //} else {
                    //    // handle evernote and jawbone servings
                    //    var isPhotoObjectType = objectType.indexOf("photo")!=-1;
                    //    var isServingObjectType = objectType.indexOf("serving")!=-1;
                    //    hasTypedFacets = hasTypedFacets && (isPhotoObjectType || isServingObjectType);
                    //
                    //}
                    hasTypedFacets = hasTypedFacets && digest.facets[facetType].hasImages;
                }
                return hasTypedFacets;
            });

            var configFilterLabel = connectorConfig.filterLabel,
                filterLabel = configFilterLabel || connector.prettyName;

            filterLabel = filterLabel.replace("_", " ");

            buttonLink
                .toggleClass("flx-disconnected", !connected)
                .text(filterLabel);
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
        if (Calendar.detailsTemplates["fluxtream-address"] == null){
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
        return Calendar.detailsTemplates["fluxtream-address"].render(params);
    }

    function buildDetails(facets,showDate){
        if (facets.length == 0)
            return"";
        if (Calendar.detailsTemplates[facets[0].type] == null){
            console.warn("hey, no template found for " + facets[0].type + ".");
            return $("");
        }
        var facetConfig = App.getFacetConfig(facets[0].type);
        var params = {color:facetConfig.color,facets:[],sent:facets[0].sent,callType:facets[0].callType,smsType:facets[0].smsType};
        for (var i = 0; i < facets.length; i++){
            var data = facets[i];
            if (typeof(facetConfig.applySettings)!="undefined")
                facetConfig.applySettings(data, Calendar.digest.settings.connectorSettings);
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
                            case "FLUXTREAM_CAPTURE":
                                newFacet[member] = "Fluxtream Capture";
                                break;
                            case "MOVES":
                                newFacet[member] = "Moves";
                                break;
                            case "OTHER":
                                newFacet[member] = "IP lookup";
                                break;
                            default:
                                newFacet[member] = data[member];
                                break;
                        }
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
                    case "position":
                        newFacet[member] = data[member];
                        newFacet.hasPosition = true;
                        break;
                    case "uri":
                        var parts = data.uri.split("/");
                        if (parts.length == 2){
                            if (data.source==="MOVES") {
                                var activityCode = "generic";
                                if (_.contains(["transport","cycling","running","walking"],parts[0]))
                                    activityCode = parts[0];
                                params.iconClass = "-activity " + activityCode;
                            } else
                                params.iconClass = "-activity " + parts[0];
                        }
                        break;
                    case "smsType":
                    case "callType":
                        newFacet[data[member]] = true;
                        newFacet[member] = data[member];
                        break;
                    case "message":
                        if (data.type == "sms_backup-sms"){
                            newFacet[member] = data[member];
                        }
                        else{
                            newFacet[member] = data[member];
                        }
                        break;

                    default:
                        newFacet[member] = data[member];
                }
            }
            if (showDate){
                var facetCity = App.getFacetCity(data, Calendar.digest.getConsensusCitiesList());
                newFacet.displayDate = App.formatDate(data.start + facetCity.tzOffset,false,true);
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

        // retrieve foursquare venues asynchronously
        var foursquareVenueIds = [];
        for (var i=0; i<params.facets.length; i++) {
            if (facets[i].type=="moves-place") {
                var placeType = facets[i].placeType;
                if (placeType==="foursquare")  {
                    foursquareVenueIds.push(facets[i].foursquareId);
                }
            }
        }

        foursquareVenueTemplate = Calendar.detailsTemplates["foursquare-venue"];
        var details = $(Calendar.detailsTemplates[data.type].render(params));
        details.find("img");
        setTimeout(function(){getFoursquareVenues(details,foursquareVenueIds);}, 100);
        return details;
    }

    function getFacet(facetType, facetId){
        var facets = Calendar.digest.facets[facetType];
        var facet = null;
        for (var i = 0, li = facets.length; i < li; i++){
            if (facets[i].id == facetId){
                facet = facets[i];
                break;
            }
        }
        if (facet == null){
            console.warn("Couldn't find " + facetType + " " + facetid);
        }
        return facet;

    }

    function switchToAppForFacet(appname,tabname,facet){
        App.renderApp(appname,tabname + (appname === "calendar" ? "/" + getTabState() : ""),{facetToShow:facet});
    }

    var activePopup = null;

    Calendar.bindShowOnXDropDown = function(jqElement,facet,onShowInX){
        jqElement.off("click.showOnXDropDown").on("click.showOnXDropDown",function(event){
            var popup = $('<ul id="menu1" class="dropdown-menu">' +
                          '<li><a class="clockLink" notthide="true" href="javascript:void(0)">Show in Clock</a></li>' +
                          '<li><a class="mapLink" href="javascript:void(0)">Show on Map</a></li>' +
                          '<li><a class="listLink" href="javascript:void(0)">Show in List</a></li>' +
                          '<li><a class="timelineLink" href="javascript:void(0)">Show on Timeline</a></li>' +
                          '<li><a class="bodytrackLink" href="javascript:void(0)">Show in Bodytrack</a></li>' +
                          '</ul>');

            var config = App.getFacetConfig(facet.type);
            if (!config.map || (App.activeApp.name === "calendar" && Calendar.currentTabName === "map")){
                popup.find(".mapLink").css("display","none");
            }
            if (!config.list || (App.activeApp.name === "calendar" && Calendar.currentTabName === "list")){
                popup.find(".listLink").css("display","none");
            }
            if (config.clock == null || (Calendar.timeUnit != null && Calendar.timeUnit !== "date") || (App.activeApp.name === "calendar" && Calendar.currentTabName === "clock")){
                popup.find(".clockLink").css("display","none");
            }
            if ((App.activeApp.name === "calendar" && Calendar.currentTabName === "timeline")){
                popup.find(".timelineLink").css("display","none");
            }
            if (App.activeApp.name === "bodytrack"){
                popup.find(".bodytrackLink").css("display","none");
            }

            popup.css("position","absolute");
            $("body").append(popup);

            var target = $(event.delegateTarget);

            var offset = target.offset();
            popup.css("top",offset.top + target.height());
            popup.css("left",offset.left);
            popup.css("display","inline-block");
            popup.css("zIndex",target.zIndex() + 1);

            popup.find(".mapLink").unbind('click').click(function(event){
                switchToAppForFacet("calendar","map",facet);
                if (onShowInX != null)
                    onShowInX();
            });
            popup.find(".clockLink").unbind('click').click(function(event){
                switchToAppForFacet("calendar","clock",facet);
                if (onShowInX != null)
                    onShowInX();
            });
            popup.find(".listLink").unbind('click').click(function(event){
                switchToAppForFacet("calendar","list",facet);
                if (onShowInX != null)
                    onShowInX();
            });
            popup.find(".timelineLink").unbind('click').click(function(event){
                switchToAppForFacet("calendar","timeline",facet);
                if (onShowInX != null)
                    onShowInX();
            });
            popup.find(".bodytrackLink").unbind('click').click(function(event){
                switchToAppForFacet("bodytrack","grapher",facet);
                if (onShowInX != null)
                    onShowInX();
            });

            if (activePopup != null)
                activePopup.remove();

            activePopup = popup;
            return false;
        });
    }

    Calendar.rebindDetailsControls = function(details,facetList){
        function getFacet(facetType,facetId){
            if (typeof(facetType)==="undefined")
                return;
            try{
                for (var i = 0, li = facetList.length; i < li; i++){
                    if (facetList[i].type === facetType && facetList[i].id === facetId)
                        return facetList[i];
                }
                var facets = facetList[facetType];
                for (var i = 0, li = facets.length; i < li; i++){
                    if (facets[i].id === facetId)
                        return facets[i];
                }
            } catch (e){}
            console.warn("Couldn't find " + facetType + " with id " + facetId);
            return null;
        }

        details.find(".facet-edit a").unbind("click").click(function(event){
            var facetType = $(event.delegateTarget).parent().parent().attr("facettype");
            var facetId = parseInt($(event.delegateTarget).parent().parent().attr("itemid"));
            Calendar.commentEdit(event,getFacet(facetType,facetId));
            return false;
        });
        var element;
        for (element = details.find(".timedropdown");element.length > 0 && element[0] != "BODY" && !element.hasClass("facetDetails"); element = element.parent());

        var facet = getFacet(element.attr("facettype"),parseInt(element.attr('itemid')));
        Calendar.bindShowOnXDropDown(details.find(".timedropdown"),facet);
    }

   $("body").mousedown(function(event){
       if (activePopup != null){
           var top = $("body");
           for (var cur = $(event.target); cur[0] != activePopup[0] && cur[0] != top[0]; cur = cur.parent());
           if (cur[0] == top[0]){
               activePopup.remove();
               activePopup = null;
           }
       }
   });

    $("body").click(function(){
        if (activePopup != null){
            activePopup.remove();
            activePopup = null;
        }
    });

    function getFoursquareVenues(details,foursquareVenueIds) {
        for (var i=0; i<foursquareVenueIds.length; i++) {
            $.ajax({
                url: "/api/v1/metadata/foursquare/venue/" + foursquareVenueIds[i],
                success: function(response) {
                    var html = foursquareVenueTemplate.render(response);
                    var foursquareVenueDiv = details.find("#foursquare-venue-"+response.foursquareId);
                    foursquareVenueDiv.replaceWith(html);
                    foursquareVenueDiv = details.find("#foursquare-venue-"+response.foursquareId);
                    var icon = foursquareVenueDiv.closest(".moves-place").find(".flx-deviceIcon");
                    icon.css("background-image","url(" + response.categoryIconUrlPrefix + "bg_32" + response.categoryIconUrlSuffix + ")");
                    icon.css("background-position", "7px 0px");
                    icon.css("background-size", "32px 32px");
                    details.trigger("contentchange");
                }
            });
        }
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
        if (digestInfo.metadata.mainCity) {
           $("#mainCity").html(cityLabel(digestInfo.metadata.mainCity))
            if (digestInfo.metadata.previousInferredCity!=null||
                digestInfo.metadata.nextInferredCity!=null) {
                $("#visitedCitiesDetails").html("Wrong city? You can fix it!");
                $("#mainCity").addClass("guessed");
            } else {
                $("#visitedCitiesDetails").html("More cities visited that day...");
                $("#mainCity").removeClass("guessed");
            }
        }
        $("#visitedCitiesDetails").show();
        $("#visitedCitiesDetails").off("click");
        $("#visitedCitiesDetails").on("click", function(){
            showVisitedCities(digestInfo.metadata);
        });
    }

    function showVisitedCities(metadata) {
        var cities = [];
        for(var i=0; i<metadata.cities.length;i++)
            cities.push(metadata.cities[i]);
        var timeUnit = metadata.timeUnit;
        var mainCity = metadata.mainCity;
        var cityData = [];
        if (typeof(metadata.previousInferredCity)!="undefined") {
            cities.push(metadata.previousInferredCity);
        }
        if (typeof(metadata.nextInferredCity)!="undefined") {
            cities.push(metadata.nextInferredCity);
        }
        for (var i=0; i<cities.length; i++) {
            var cityInfo = {};
            cityInfo.visitedCityId = cities[i].visitedCityId;
            cityInfo.count = cities[i].count;
            cityInfo.isUser = cities[i].source=="USER";
            cityInfo.source = toPrettySource(cities[i].source);
            cityInfo.description = cities[i].description;
            cityInfo.timezone = cities[i].shortTimezone;
            cityInfo.startMinute = cities[i].startMinute;
            cityInfo.endMinute = cities[i].endMinute;
            cityInfo.daysInferred = cities[i].daysInferred;
            if (cities[i].daysInferred!=0) {
                var dayOrDays = Math.abs(cities[i].daysInferred)==1?"day":"days";
                cityInfo.when = (cities[i].daysInferred<0)
                    ? Math.abs(cities[i].daysInferred) + " " + dayOrDays + " ago"
                    : cities[i].daysInferred + " " + dayOrDays + " later";
            }
            if (timeUnit=="DAY") {
                var minutes = cities[i].startMinute%60;
                minutes = minutes<10?"0"+minutes:""+minutes;
                cityInfo.firstSeenHere = Math.floor(cities[i].startMinute/60)+"h"+minutes;
                minutes = cities[i].endMinute%60;
                minutes = minutes<10?"0"+minutes:""+minutes;
                cityInfo.lastSeenHere = Math.floor(cities[i].endMinute/60)+"h"+minutes;
            } else {
                cityInfo.firstSeenHere = cities[i].startTime;
                cityInfo.lastSeenHere = cities[i].endTime;
            }
            cityData[cityData.length] = cityInfo;
        }
        for (var i=0; i<cityData.length; i++) {
            if (cityData[i].startMinute==metadata.mainCity.startMinute&&
                cityData[i].endMinute==metadata.mainCity.endMinute&&
                cityData[i].source==toPrettySource(metadata.mainCity.source)&&
                cityData[i].daysInferred==metadata.mainCity.daysInferred)
            cityData[i].consensus = true;
        }
        var mainCityMessage;
        var guessed = false;
        if (metadata.previousInferredCity!=null||metadata.nextInferredCity!=null) {
            guessed = true;
            mainCityMessage = "Based on past/future location data, we guessed the you were in <b>" + cityLabel(mainCity) + "</b>";
        } else if (mainCity.source!="USER")
            mainCityMessage="We detected that you were in <b>" + cityLabel(mainCity) + "</b>";
        else
            mainCityMessage="You have indicated that you were in <b>" + cityLabel(mainCity) + "</b>";
        switch (timeUnit) {
            case "DAY":
                mainCityMessage += " today.<br>";
                break;
            case "WEEK":
                mainCityMessage += " this week.<br>";
                break;
            case "MONTH":
                mainCityMessage += " this month.<br>";
                break;
        }
        var changeMainCityMessage;
        if (cities.length>0)
            changeMainCityMessage="You can choose an alternate city in the list below.<br><br>";

        else {
            changeMainCityMessage="You can change it by entering a city name below.<br><br>";
        }

        App.loadMustacheTemplate("applications/calendar/facetTemplates.html","visitedCities-details",function(template){

            bindCitySearch(template.render(
                {
                    cities:cityData,
                    guessed:guessed,
                    mainCityMessage:mainCityMessage,
                    changeMainCityMessage:changeMainCityMessage
                }), timeUnit);

        });
    }

    function toPrettySource(source) {
        source = source.replace(/_/g, ' ');
        source = source.toLowerCase().replace(/^.|\s\S/g, function(a) { return a.toUpperCase(); });
        return source;
    }

    function bindCitySearch(html, timeUnit) {
        App.makeModal(html);

        $("#mainCitySearch").off("click");
        $("#mainCitySearch").on("click", function(){
            var cityName = $("#mainCityInput").val();
            $("#mainCitySelect").attr("disabled","disabled");
            $("#mainCitySearch").attr("disabled","disabled");
            $("#mainCityInput").attr("disabled","disabled");
            App.geocoder.geocode({"address":cityName},function(results,status){
                var options = $("#mainCitySelect").children();
                for (var i = 1; i < options.length; i++)
                    $(options[i]).remove();
                if (status == google.maps.GeocoderStatus.OK) {
                    if (results.length>0) {
                        $("#selectMainCity").removeClass("disabled");
                        $("#selectMainCity").addClass("enabled");
                        $("#selectMainCity").click(function(){
                            selectMainCity(timeUnit);
                        });
                    }
                    for (var i = 0; i < results.length; i++){
                        $("#mainCitySelect").append('<option>' + results[i].formatted_address + '</option>')
                    }
                    currentCityPool = results;
                    $("#mainCitySelect")[0].selectedIndex = 1;
                }
                else{
                    $("#mainCitySelect")[0].selectedIndex = 0;
                    $("#selectMainCity").removeClass("enabled");
                    $("#selectMainCity").addClass("disabled");
                    currentCityPool = [];
                }
                $("#mainCitySelect").removeAttr("disabled");
                $("#mainCitySearch").removeAttr("disabled");
                $("#mainCityInput").removeAttr("disabled");
            });
        });
        $("#mainCityInput").off("keyup");
        $("#mainCityInput").on("keyup", function(event){
            event.preventDefault();
            if (event.keyCode == 13)
                $("#mainCitySearch").click();
            else{
                var options = $("#mainCitySelect").children();
                for (var i = 1; i < options.length; i++)
                    $(options[i]).remove();
                currentCityPool = [];
            }
        });
        $(".selectVisitedCity").click(function(evt){selectVisitedCity(evt);});

    }

    function selectVisitedCity(evt) {
        console.log("select visited city");
        var state = App.state.getState("calendar");
        state = state.substring(state.indexOf("/"));
        if ($(evt.target).hasClass("undo")) {
            removeMainVisitedCity("/api/v1/metadata/mainCity"+state);
        } else {
            var visitedCityId = evt.target.id.substring("visitedCity-".length);
            postMainVisitedCity("/api/v1/metadata/mainCity/"+visitedCityId+state);
        }
    }

    function selectMainCity(timeUnit) {
        console.log("select main city");
        var selectedIndex = $("#mainCitySelect")[0].selectedIndex-1;
        var selectedCity = currentCityPool[selectedIndex];
        if(typeof(selectedCity.geometry)!="undefined"&&
            selectedCity.geometry.location!="undefined") {
            var latitude = selectedCity.geometry.location.lat();
            var longitude = selectedCity.geometry.location.lng();
            var state = App.state.getState("calendar");
            state = state.substring(state.indexOf("/"));
            postMainCity("/api/v1/metadata/mainCity"+state, {"latitude":latitude,"longitude":longitude});
        } else {
            console.log("no city (" + timeUnit + ")");
        }
    }


    function removeMainVisitedCity(url) {
       $.ajax({
           url: url,
           type: "DELETE",
           success: function(body, statusText, jqXHR) {
               $("#visitedCitiesDialog").modal('hide');
               App.activeApp.renderState(App.state.getState(App.activeApp.name),true);//force refresh of the current app state
           }
       })
    }

    function postMainCity(url, data) {
       $.ajax({
           url: url,
           type: "POST",
           data: data,
           success: function(body, statusText, jqXHR) {
               $("#visitedCitiesDialog").modal('hide');
               App.activeApp.renderState(App.state.getState(App.activeApp.name),true);//force refresh of the current app state
           }
       })
    }

    function postMainVisitedCity(url) {
       $.ajax({
           url: url,
           type: "POST",
           success: function(body, statusText, jqXHR) {
               $("#visitedCitiesDialog").modal('hide');
               App.activeApp.renderState(App.state.getState(App.activeApp.name),true);//force refresh of the current app state
           }
       })
    }

    function cityLabel(cityInfo) {
       var s = "";
       s += cityInfo.name;
       if (cityInfo.country=="US"||cityInfo.country=="USA") s += ", " + cityInfo.state;
       s += ", " + cityInfo.country + " (" + cityInfo.timezone + ")";
       return s;
    }

    function ephemerisLabel() {
        var sunriseH = Math.floor(Calendar.weather.solarInfo.sunrise/60);
        var sunriseM = Calendar.weather.solarInfo.sunrise%60;
        var sunsetH = Math.floor(Calendar.weather.solarInfo.sunset/60);
        var sunsetM = Calendar.weather.solarInfo.sunset%60;
        if (sunriseM<10) sunriseM = "0" + sunriseM;
        if (sunsetM<10) sunsetM = "0" + sunsetM;
        if (sunsetH>12)sunsetH-=12;
        return "<span class=\"ephemeris\"><span title='Sunrise'><i class=\"flx-pict-sun\">&nbsp;</i><span>" + sunriseH + ":" + sunriseM + " am"+
               "</span></span>&nbsp;<span title='Sunset'><i class=\"flx-pict-moon\">&nbsp;</i><span>" + sunsetH + ":" + sunsetM + " pm</span></span></span>";
    }

    function weatherLabel() {
        function getFahrenheitTemps() {
            if (Calendar.weather.minTempF==null) return "";
            var FahrenheitTemps = "<span title='Temperature Min / Max'><i class=\"flx-pict-temp\">&nbsp;</i>"
                                      + "<span class=\"ephemeris\" style=\"font-weight:normal;\">&nbsp;"
                                      + Calendar.weather.minTempF
                                      + " / "
                                      + Calendar.weather.maxTempF
                + "&deg;F</span>";
            return FahrenheitTemps;
        }

        function getCelsiusTemps() {
            if (Calendar.weather.minTempC==null) return "";
            return "<i class=\"flx-pict-temp\">&nbsp;</i>"
                       + "<span class=\"ephemeris\" style=\"font-weight:normal;\">&nbsp;"
                       + Calendar.weather.minTempC
                       + " / "
                       + Calendar.weather.maxTempC
                       + "&deg;C"
                + "</span>";
        }

        if (Calendar.weather.temperatureUnit != "CELSIUS") {
            var FahrenheitTemps = getFahrenheitTemps();
            return ephemerisLabel() + FahrenheitTemps
                + "</span>";
        }
        else {
            return ephemerisLabel() + getCelsiusTemps();
        }
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

   function fetchTimespan(state,doneLoadingId) {
       $.ajax({
           url: "/api/v1/calendar/nav/model",
           async: false,
           type: "GET",
           data: {state: state.tabState},
           dataType: "JSON",
           success: function(response) {
               Calendar.timeRange.start = response.start;
               Calendar.timeRange.end = response.end;
               if (Calendar.dateAxisCursorPosition * 1000 < Calendar.timeRange.start || Calendar.dateAxisCursorPosition * 1000 > Calendar.timeRange.end)
                   Calendar.dateAxisCursorPosition = null;
               updateTimespan(response.currentTimespanLabel,state.tabState);
               Calendar.timeRange.updated = true;
               stopLoading(doneLoadingId);
           },
           error: function(jqXHR, statusText, errorThrown){
               alert("failed to fetch timespan label!")
           }
       });
   }

    function getTabState(){
        if (Calendar.tabState != null)
            return Calendar.tabState;
        else
            return "date/" + App.formatDateAsDatePicker(new Date());
    }

    var viewBtnIds = {date:"#dayViewBtn",week:"#weekViewBtn",month:"#monthViewBtn",year:"#yearViewBtn"};

    function updateDisplays(state){
        for (var type in viewBtnIds){
            $(viewBtnIds[type]).removeClass("active");
        }
        $(viewBtnIds[state.timeUnit]).addClass("active");
    }


    Calendar.showPhotoDialog = function(deviceName, channelName, uid, timestamp,optionalArguments) {
       PhotoUtils.showPhotoDialog(deviceName, channelName, uid, timestamp,optionalArguments);
    };


    Calendar.fbShare = function(ogLink) {
        window.open(
            'https://www.facebook.com/sharer/sharer.php?u='+encodeURIComponent(ogLink),
            'facebook-share-dialog',
            'width=626,height=436');
    };

    Calendar.commentEdit = function(evt,facet) {
        var target = $(evt.target);
        var facetDetails = target.parent().parent();
        if (facetDetails.find(".facet-comment").length > 0){
            facetDetails.find(".cancel").click();
            return;
        }
        facet = facet;

        var hasComment = facet.comment != null;
        if (hasComment) {
            facetDetails.find(".facet-comment-text").remove();
        }
        var commentDiv =     '<div class="facet-comment" style="margin-bottom:5px">'+
                             '<textarea placeholder="type a comment..." rows="3"></textarea>' +
                             '<button class="btn btn-small save disabled" type="button"><i class="icon icon-save"/> Save</button>&nbsp;' +
                             '<button class="btn btn-small cancel" type="button"><i class="icon icon-undo"/> Cancel</button>&nbsp;' +
                             '<button class="btn btn-link delete" type="button"><i class="icon icon-trash"/> Delete</button>';

        if (typeof(facet.ogLink)!="undefined") {
            commentDiv +=    '<a class="fbShare" title="Share on Facebook" onclick="App.apps[\'calendar\'].fbShare(\'' + facet.ogLink + '\');" type="button"><img src="/static/images/facetSharing/FB-f-Logo__blue_22.png"/></a>';
        }
        commentDiv += '</div>';

        facetDetails.append(commentDiv);
        var commentWarning = facetDetails.find(".commentWarning");
        commentWarning.removeClass("hidden");
        facetDetails.trigger("contentchange");
        var textarea = facetDetails.find("textarea");
        if (hasComment) {
            textarea.val(facet.comment);
        }
        var cancelButton = facetDetails.find(".cancel");
        var saveButton = facetDetails.find(".save");
        var deleteButton = facetDetails.find(".delete");
        textarea.focus();
        var originalComment = textarea.val();
        textarea.keyup(function(){
            if (originalComment==textarea.val()) {
                saveButton.addClass("disabled");
            } else {
                saveButton.removeClass("disabled");
            }
        });
        saveButton.click(function(event) {
            event.stopPropagation();
            $.ajax({
                url: "/api/v1/comments/" + facet.type + "/" + facet.id,
                data: {comment: textarea.val()},
                type: "POST",
                success: function() {
                    facet.comment = textarea.val();
                    facetDetails.find(".facet-comment").replaceWith('<div class="facet-comment-text">' + facet.comment + '</div>');
                    commentWarning.addClass("hidden");
                    facetDetails.trigger("contentchange");
                }
            });
        });
        cancelButton.click(function(event) {
            event.stopPropagation();
            if (hasComment)
                facetDetails.find(".facet-comment").replaceWith('<div class="facet-comment-text">' + originalComment + '</div>');
            else
                facetDetails.find(".facet-comment").remove();
            commentWarning.addClass("hidden");
            facetDetails.trigger("contentchange");
        });
        deleteButton.click(function(event) {
            event.stopPropagation();
            $.ajax({
                url: "/api/v1/comments/" + facet.type + "/" + facet.id,
                data: {comment: textarea.val()},
                type: "DELETE",
                success: function() {
                    facetDetails.find(".facet-comment").remove();
                    commentWarning.addClass("hidden");
                    facetDetails.trigger("contentchange");
                    delete facet.comment;
                }
            });
        });
    };

    Calendar.startLoading = startLoading;
    Calendar.stopLoading = stopLoading;
    Calendar.handleError = handleError;

	return Calendar;
});