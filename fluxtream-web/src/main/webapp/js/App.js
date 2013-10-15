var dateFormat = function () {
    var	token = /d{1,4}|m{1,4}|yy(?:yy)?|([HhMsTt])\1?|[LloSZ]|"[^"]*"|'[^']*'/g,
        timezone = /\b(?:[PMCEA][SDP]T|(?:Pacific|Mountain|Central|Eastern|Atlantic) (?:Standard|Daylight|Prevailing) Time|(?:GMT|UTC)(?:[-+]\d{4})?)\b/g,
        timezoneClip = /[^-+\dA-Z]/g,
        pad = function (val, len) {
            val = String(val);
            len = len || 2;
            while (val.length < len) val = "0" + val;
            return val;
        };

    // Regexes and supporting functions are cached through closure
    return function (date, mask, utc) {
        var dF = dateFormat;

        // You can't provide utc if you skip other args (use the "UTC:" mask prefix)
        if (arguments.length == 1 && Object.prototype.toString.call(date) == "[object String]" && !/\d/.test(date)) {
            mask = date;
            date = undefined;
        }

        // Passing date through Date applies Date.parse, if necessary
        date = date ? new Date(date) : new Date;
        if (isNaN(date)) throw SyntaxError("invalid date");

        mask = String(dF.masks[mask] || mask || dF.masks["default"]);

        // Allow setting the utc argument via the mask
        if (mask.slice(0, 4) == "UTC:") {
            mask = mask.slice(4);
            utc = true;
        }

        var	_ = utc ? "getUTC" : "get",
            d = date[_ + "Date"](),
            D = date[_ + "Day"](),
            m = date[_ + "Month"](),
            y = date[_ + "FullYear"](),
            H = date[_ + "Hours"](),
            M = date[_ + "Minutes"](),
            s = date[_ + "Seconds"](),
            L = date[_ + "Milliseconds"](),
            o = utc ? 0 : date.getTimezoneOffset(),
            flags = {
                d:    d,
                dd:   pad(d),
                ddd:  dF.i18n.dayNames[D],
                dddd: dF.i18n.dayNames[D + 7],
                m:    m + 1,
                mm:   pad(m + 1),
                mmm:  dF.i18n.monthNames[m],
                mmmm: dF.i18n.monthNames[m + 12],
                yy:   String(y).slice(2),
                yyyy: y,
                h:    H % 12 || 12,
                hh:   pad(H % 12 || 12),
                H:    H,
                HH:   pad(H),
                M:    M,
                MM:   pad(M),
                s:    s,
                ss:   pad(s),
                l:    pad(L, 3),
                L:    pad(L > 99 ? Math.round(L / 10) : L),
                t:    H < 12 ? "a"  : "p",
                tt:   H < 12 ? "am" : "pm",
                T:    H < 12 ? "A"  : "P",
                TT:   H < 12 ? "AM" : "PM",
                Z:    utc ? "UTC" : (String(date).match(timezone) || [""]).pop().replace(timezoneClip, ""),
                o:    (o > 0 ? "-" : "+") + pad(Math.floor(Math.abs(o) / 60) * 100 + Math.abs(o) % 60, 4),
                S:    ["th", "st", "nd", "rd"][d % 10 > 3 ? 0 : (d % 100 - d % 10 != 10) * d % 10]
            };

        return mask.replace(token, function ($0) {
            return $0 in flags ? flags[$0] : $0.slice(1, $0.length - 1);
        });
    };
}();

// Some common format strings
dateFormat.masks = {
    "default":      "ddd mmm dd yyyy HH:MM:ss",
    shortDate:      "m/d/yy",
    mediumDate:     "mmm d, yyyy",
    longDate:       "mmmm d, yyyy",
    fullDate:       "dddd, mmmm d, yyyy",
    shortTime:      "h:MM TT",
    mediumTime:     "h:MM:ss TT",
    longTime:       "h:MM:ss TT Z",
    isoDate:        "yyyy-mm-dd",
    isoTime:        "HH:MM:ss",
    isoDateTime:    "yyyy-mm-dd'T'HH:MM:ss",
    isoUtcDateTime: "UTC:yyyy-mm-dd'T'HH:MM:ss'Z'"
};

// Internationalization strings
dateFormat.i18n = {
    dayNames: [
        "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat",
        "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
    ],
    monthNames: [
        "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
        "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"
    ]
};

// For convenience...
Date.prototype.format = function (mask, utc) {
    return dateFormat(this, mask, utc);
};

define(
    [ "core/FlxState", "Addresses", "ManageConnectors", "AddConnectors", "ConnectorConfig", "Settings", "SharingDialog",
      "libs/jquery.form", "libs/jquery.jeditable.mini" ],
    function(FlxState, Addresses, ManageConnectors, AddConnectors, ConnectorConfig, Settings,
        SharingDialog ) {

        var App = {};

        App.apps = {};

        var compiledTemplates = {};

        function initialize() {
            _.bindAll(this);
            // start loading all applications
            checkScreenDensity();
            loadApps();
        }

        function checkScreenDensity() {
            var retina = window.devicePixelRatio > 1;
            setCookie("retina", retina?"1":"0", 30);
        }

        function setCookie(c_name,value,exdays) {
            var exdate=new Date();
            exdate.setDate(exdate.getDate() + exdays);
            var c_value=escape(value) + ((exdays==null) ? "" : "; expires="+exdate.toUTCString());
            document.cookie=c_name + "=" + c_value;
        }

        /**
         * Preload all applications dynamically; we do this because a) the
         * applications list can/will be user-dependent b) we need to
         * preload in order to setup the (global) router object and (in the
         * future) to let every application know of the existence of others
         */
        function loadApps() {
            var appModules = FlxState.apps.map(function(appName) {
                return "applications/" + appName + "/App";
            });
            require(appModules, function(/* apps */) {

                for (var i = 0; i < arguments.length; i++) {
                    var app = arguments[i];
                    App.apps[app.name] = app;
                    app.initialize();
                }

                createAppsMenu();
                loadAppTemplates();
            });
        }

        function renderAppTemplate(app, html) {
            var appDiv = $("<div/>", {
                class: "application",
                id: app.name + "-app"
            }).addClass("dormant").html(html);
            $("#applications").append(appDiv);
        }

        function loadAppTemplates() {
            var apps = _.values(App.apps),
                appTemplates = apps.map(function(app) {
                    return "text!applications/" + app.name + "/template.html";
                });
            require(appTemplates, function(/* templates */) {
                for (var i = 0; i < arguments.length; i++)  {
                    renderAppTemplate(apps[i], arguments[i]);
                    apps[i].setup();
                }
                setupURLRouting();
            });
        }

        function setAppDivEnabled(app, enabled) {
            var appDiv = $("#" + app.name + "-app");
            appDiv.toggleClass("active", enabled);
            appDiv.toggleClass("dormant", !enabled);
        }

        function maybeSwapApps(app) {
            // TODO: add destroy()/setup() calls again...
            $(".appMenuBtn.active").removeClass("active");
            $("#"+app.name+"MenuButton").addClass('active');
            var appChanged = app !== App.activeApp;
            if (appChanged) {
                if (!_.isUndefined(App.activeApp)) {
                    setAppDivEnabled(App.activeApp, false);
                }
                App.activeApp = app;
            }
            setAppDivEnabled(app, true);
        }

        function renderDefault(app) {
            maybeSwapApps(app);
            App.activeApp.renderDefaultState();
        }

        function render(app, state) {
            maybeSwapApps(app);
            App.activeApp.renderState(state);
        }

        function setupURLRouting() {
            FlxState.router.route("*path", "default", function(path) {
                console.log("default route: path=" + path);
                var appName = FlxState.defaultApp,
                    app = App.apps[appName];
                renderDefault(app);
            });
            FlxState.router.route("app/:name", "app-default", function(appName) {
                console.log("app-default route: name=" + appName);
                var app = App.apps[appName];
                renderDefault(app);
            });
            FlxState.router.route("app/:name/*state", "app", function(appName, state) {
                console.log("app route: name=" + appName + ", state=" + state);
                var app = App.apps[appName];
                if (_.isUndefined(app)) {
                    console.log("invalid app: " + appName);
                    App.invalidPath();
                }
                // strip trailing slash from state, if any
                if (state.endsWith("/")) {
                    state = state.slice(0, -1);
                }
                FlxState.saveState(appName, state);
                state = app.parseState(state);
                if (state === null) {
                    console.log("invalid state: " + state);
                    App.invalidPath();
                    return;
                }
                render(app, state);
            });

            if (!Backbone.history.start({pushState : window.history && window.history.pushState})) {
                console.log("error loading routes!");
            }
        }

        /**
         * Add the buttons to the top apps menu
         */
        function createAppsMenu() {
            $.each(FlxState.apps, function(i, appName) {
                var app = App.apps[appName],
                    button = $("<button/>", {
                        id: app.name + "MenuButton",
                        class: "btn appMenuBtn",
                        text: app.prettyName
                    }).click(function(event) {
                        App.renderApp(app.name);
                    });
                $("#apps-menu").append(button);
            });
        }

        function fullHeight() {
            var windowHeight = $(window).height();
            var footerHeight = $("#footer").outerHeight(false);
            if ($(".fullHeight").length>0) {
                tabsY = $("#tabs").position().top;

                fHeight = (windowHeight-tabsY-footerHeight);
                $(".fullHeight").height(fHeight);
            }
            var contentHeight = (windowHeight - footerHeight);
            $("#content").css("min-height",contentHeight + "px");
        }

        $(window).resize(function() {
            $.doTimeout("fullSizeHandler");//cancel original
            $.doTimeout("fullSizeHandler",100,App.fullHeight);
        });

        $(window).resize();

        function renderApp(appName,state,params) {
            var app = App.apps[appName];
            if (_.isUndefined(state)) {
                state = FlxState.getState(appName);
            }
            app.navigateState(state,params);
        }

        App.settings = function() {
            Settings.show();
        };

        function makeModal(html) {
            var dialog = $(html);
            dialog.addClass("modal");
            dialog.addClass("hide");
            $("body").append(dialog);
            dialog.modal();
            dialog.on("hidden",function(event){
                event.stopImmediatePropagation();
                dialog.remove();
            });
            var backdrops = $(".modal-backdrop");
            if (backdrops.length > 1){
                var zIndex = $(backdrops[backdrops.length - 2]).css("zIndex");
                zIndex++;
                zIndex += 19;
                $(backdrops[backdrops.length - 1]).css("zIndex",zIndex);
                zIndex++;
                dialog.css("zIndex",zIndex);
            }
        }

        App.makeModal = makeModal;
        App.carousel = carousel;

        App.fetchCompiledMustacheTemplate = function(templatePath,templateId) {
            if (typeof compiledTemplates[templatePath] !== 'undefined') {
                return compiledTemplates[templatePath][templateId];
            }
            console.log("App.fetchCompiledMustacheTemplate(): Unknown template path [" + templatePath + "] and/or template ID [" + templateId + "].  Returning null.");
            return null;
        };

        App.loadMustacheTemplate = function(templatePath,templateId,onLoad){
            App.loadAllMustacheTemplates(templatePath,function(templates){
                onLoad(templates[templateId]);
            });
        };

        var templateRegexp = new RegExp("<template id=\"[A-z0-9\\-.]*\">","ig");

        App.loadAllMustacheTemplates = function(templatePath,onLoad){
            if (compiledTemplates[templatePath] != null){
                onLoad(compiledTemplates[templatePath]);
                return;
            }
            require(["text!" + templatePath], function(template){
                var templateData = {};
                var matches = template.match(templateRegexp);
                for (var i = 0; i < matches.length; i++){
                    var curMatch = matches[i];
                    var templateName = curMatch.substring(14,curMatch.length - 2);
                    var start = template.indexOf(curMatch) + curMatch.length;
                    var end = template.indexOf("</template>",start);
                    var html = template.substring(start,end);
                    templateData[templateName] = Hogan.compile(html);
                }
                compiledTemplates[templatePath] = templateData;
                onLoad(compiledTemplates[templatePath]);
            });
        };

        App.closeModal = function(){
            $("#modal").modal("hide");
        };

        App.htmlEscape = function(str){
            return str.replace(/&/g, '&amp;')
                .replace(/"/g, '&quot;')
                .replace(/'/g, '&#39;')
                .replace(/</g, '&lt;')
                .replace(/>/g, '&gt;');
        }

        App.eraseEverything = function() {
            var confirmed = confirm("Are you sure?");
            if (confirmed) {
                $.ajax({
                    url: "/api/settings/deleteAccount",
                    type: "POST",
                    success: function(status) {
                        if (status.result=="OK") {
                            window.location = "/logout";
                        } else {
                            alert(status.message);
                        }
                    }
                });
            }
        };

        App.as = function(username) {
            $.ajax({
                url: "/api/coaching/coachees/" + username,
                type: "POST",
                success: function(status) {
                    if (status.result=="OK") {
                        location.reload();
                    } else
                        alert(status.message);
                }
            });
        };

        App.connectors = function() {
            AddConnectors.show();
        };

        App.addresses = function() {
            Addresses.show();
        };

        App.manageConnectors = function(){
            ManageConnectors.show();
        };

        App.removeConnector = function(api) {
            var c = confirm("If you wrote comments on events related to this connector, "
                                + "you will lose them forever.\n"
                                + "Are your sure you want to continue?");
            if (c) {
                $.ajax({
                           url : "/connectors/removeConnector?api=" + api,
                           dataType : "json",
                           success : function(data) {
                               if (data.result == "ok") {
                                   $("#userConnectors").load(
                                       "/connectors/userConnectors");
                                   $("#availableConnectors").load(
                                       "/connectors/availableConnectors");
                                   App.showConnectorsPage(0);
                               }
                           }
                       });
            }
        };

        App.getConnectorSettings = function(connectorId) {
            console.log("getting connector settings: " + connectorId);
        };

        App.getConnectorConfig = function(connectorName){
            var config = ConnectorConfig[connectorName];
            if (config == null){
                console.log("WARNING: No config found for connector: " + connectorName);
                config = {};
            }
            config = $.extend({}, config);
            config.facets = false;

            for (var member in ConnectorConfig.default){
                if (typeof config[member] === "undefined")
                    config[member] = ConnectorConfig.default[member];
            }

            return config;
        };

        App.getFacetConfig = function(facetName){
            var config = ConnectorConfig[App.getFacetConnector(facetName)];
            if (config == null){
                console.log("WARNING: No config found for Connector: " + App.getFacetConnector(facetName));
                config = {};
            }
            var finalConfig = $.extend({},config);
            finalConfig.facets = null;
            if (config.facets == null || config.facets[App.getFacetObjectType(facetName)] == null){
                console.log("WARNING: No config found for Facet: " + facetName);
            }
            else{
                var facet = config.facets[App.getFacetObjectType(facetName)];
                if (facet != null){
                    for (var member in facet){
                        finalConfig[member] = facet[member];
                    }
                }
            }

            for (var member in ConnectorConfig.default){
                if (typeof finalConfig[member] === "undefined")
                    finalConfig[member] = ConnectorConfig.default[member];
            }
            return finalConfig;

        }

        App.getFacetConnector = function(facetName){
            var firstDash = facetName.indexOf("-");
            if (firstDash != -1)
                return facetName.substring(0,firstDash);
            return facetName;
        }

        App.getFacetObjectType = function(facetName){
            var firstDash = facetName.indexOf("-");
            if (firstDash != -1)
                return facetName.substring(firstDash + 1);
            return facetName;

        }

        function startsWith(s, prefix) {
            return s.substr(0, prefix.length) === prefix;
        };

        App.addConnector = function(url) {
            if (startsWith(url, "ajax:")) {
                $.ajax({
                   url : url.substring(5),
                   success : function(html) {
                       $(".addConnectorsMain").html(html);
                       $(".focushere").focus();
                   }
                });
            } else if (startsWith(url, "upload:")) {
                var connectorName = url.substring(7);
                $.ajax({
                    url : "/upload/addConnector",
                    type: "POST",
                    data: {connectorName : connectorName},
                    success : function(response) {
                        var status;
                        try { status = JSON.parse(response); }
                        catch(err) { alert("Couldn't add upload-only connector:" + err); }
                        if (status.result==="OK") {
                            $("#modal").modal("hide");
                            App.activeApp.renderState(App.state.getState(App.activeApp.name),true);
                        }
                        else {
                            if (typeof(status.stackTrace)!="undefined")
                                console.log(status.stackTrace);
                            alert("Could not add upload-only connector: " + status.message);
                        }
                    }
                });
            } else {
                var loading = $("#loading").clone().show();
                $(".addConnectorsMain").empty();
                $(".addConnectorsMain").append(loading);
                setTimeout("window.location='" + url + "'", 500);
            }
        };

        App.showConnectorsPage = function(page) {
            $("#availableConnectors").load(
                "/connectors/availableConnectors?page=" + page);
        };

        App.discardNotification = function(notificationId) {
            $.ajax({
                    url: "/api/notifications/" + notificationId,
                    type: "DELETE",
                    success: function() {
                        $("#notification-" + notificationId).remove();
                        $(window).resize();
                    }
                }
            );
        };

        App.showCarousel = function(photoId) {
            if ($("#photosCarousel").length==0) {
                $.ajax({
                   url : "/tabs/photos/carousel",
                   success: function(html) {
                       makeModal(html);
                       carousel(photoId);
                   }
                });
            } else {
                carousel(photoId);
            }
        };


        App.getFacetCity = function(facet, citiesList){
            var time = (facet.start + (facet.end != null ? facet.end : facet.start)) / 2
            if (time < citiesList[0].dayStart)
                return citiesList[0];
            for (var i= 0, li = citiesList.length; i < li; i++) {
                var city = citiesList[i];
                if ((city.dayStart<=time && time<city.dayEnd) || (city.dayStart<=facet.start && facet.start<city.dayEnd))
                    return city;
            }
            return citiesList[citiesList.length-1];
        }

        App.prettyDateFormat = function(dateString) {
            dateString = dateString.split(" ")[0];
            var date = new Date(Date.parse(dateString) + 1000 * 60 * 60 * 12);   // place it in the middle of the day to help prevent errors
            return date.format("dddd, mmmm d");
        };

        App.formatDate = function(date, includeTime, UTC){
            if (includeTime == null)
                includeTime = false;
            if (UTC == null)
                UTC = false;
            if (typeof(date) == "number"){
                if (!UTC)
                    date = new Date(date);
                else{
                    var ms = date;
                    date = new Date(0);
                    date.setUTCMilliseconds(ms);
                }
            }
            if (isNaN(date.getFullYear()))
                return "Present";
            var value = "";
            var year, month, day, hour, minute, second;
            if (UTC){
                year = date.getUTCFullYear();
                month = date.getUTCMonth();
                day = date.getUTCDate();
                hour = date.getUTCHours();
                minute = date.getUTCMinutes();
                second = date.getUTCSeconds();
            }
            else{
                year = date.getFullYear();
                month = date.getMonth();
                day = date.getDate();
                hour = date.getHours();
                minute = date.getMinutes();
                second = date.getSeconds();

            }

            switch (month){
                case 0:
                    value += "January";
                    break;
                case 1:
                    value += "February";
                    break;
                case 2:
                    value += "March";
                    break;
                case 3:
                    value += "April";
                    break;
                case 4:
                    value += "May";
                    break;
                case 5:
                    value += "June";
                    break;
                case 6:
                    value += "July";
                    break;
                case 7:
                    value += "August";
                    break;
                case 8:
                    value += "September";
                    break;
                case 9:
                    value += "October";
                    break;
                case 10:
                    value += "November";
                    break;
                case 11:
                    value += "December";
                    break;
            }
            value += " " + day;
            value += ", " + year;
            if (includeTime){
                value += " " + hour;
                value += ":";
                if (minute < 10)
                    value += "0";
                value += minute;
                value += ":";
                if (second < 10)
                    value += "0";
                value += second;
            }
            return value;
        }

        App.formatMinuteOfDay = function(minuteOfDay){
            var hour = Math.floor(minuteOfDay/60);
            var minutes = Math.floor(minuteOfDay%60);
            if (minutes<10) minutes = "0" + minutes;
            if (hour<12)
                return [(hour == 0 ? 12 : hour) + ":" + minutes, "am"];
            else
                return [(hour > 12 ? hour - 12 : 12) + ":" + minutes, "pm"];
        }

        App.formatDateAsDatePicker = function(date) {
            if (typeof(date) == "number")
                date = new Date(date);
            return App._formatDateAsDatePicker(date.getFullYear(), date.getMonth(), date.getDate());
        }

        App._formatDateAsDatePicker = function(year, month, date) {
            if (isNaN(year))
                return "Present";
            return year + "-" + (month < 9 ? "0" : "") + (month + 1) + "-" + (date < 9 ? "0" : "") + date;
        }

        //This is a hack to force enable dropdown on all specified elements since bootstrap doesn't seem to be doing it on its own
        function globalClickHandler(event){
            for (var target = event.target; target != null; target = target.parentElement){
                if ($(target).attr("data-toggle") == "dropdown"){
                    $(target).dropdown("toggle");
                    break;
                }
                else if ($(target).attr("data-toggle") == "collapse"){
                    $($(target).attr("data-target")).addClass("collapse");
                    $($(target).attr("data-target")).collapse("toggle");
                }
            }
        }

        var hideFunctions = [];

        var onEvent = function(event){ //hides the tooltip if an element clicked on or any of its parents has the notthide property
            for (var target = event.target; target != null; target=target.parentElement){
                if ($(target).attr("notthide") != null)
                    return;
            }
            for (var i = 0, li = hideFunctions.length; i < li; i++)
                hideFunctions[i]();
        };

        $(document).bind("touchend",onEvent).bind("click",globalClickHandler).bind("mousedown", onEvent);

        var hideFunctions = [];

        App.addHideTooltipListener = function(hideFunction) {
            hideFunctions.push(hideFunction);
            var onEvent = function(event){ //hides the tooltip if an element clicked on or any of its parents has the notthide property
                for (var target = event.target; target != null; target=target.parentElement){
                    if ($(target).attr("notthide") != null)
                        return;
                }
                for (var i = 0, li = hideFunctions.length; i < li; i++)
                    hideFunctions[i];
            };
            $(document).unbind("click").unbind("touchend").bind("touchend",onEvent).bind("click",globalClickHandler).bind("click", onEvent);
        }

        App.search = function() {
            $(".application").load("/search/0?q=" + $(".search-query").val());
        };

        var monthEndDays = [31,28,31,30,31,30,31,31,30,31,30,31];

        App.getLastDayOfMonth = function(year,month){
            return monthEndDays[month] + ((month == 1 && App.isLeapYear(year)) ? 1 : 0);
        }

        App.isLeapYear = function(year){
            return (year % 400 == 0) || (year % 100 != 0 && year % 4 == 0);
        };

        App.expandCollapse = function(o) {
            var finedetails = $(o).closest(".facetDetails").find(".flx-finedetails");
            finedetails.toggleClass("flx-collapsed");
        }

        App.setupBeginnersFriendlyUI = function (messageDisplayCounters, nApis) {
            App.messageDisplayCounters = messageDisplayCounters;
            if (nApis==0) {
                $("#manageConnectorsMenuItem").addClass("disabled");
                $("#connectorsDropdownToggle").popover({
                    container: "body",
                    placement: "bottom",
                    title: "Click here to add your first Connector!",
                    content: "Connectors let Fluxtream link up your data",
                    animation: true
                });
                $("#connectorsDropdownToggle").popover("show");
            } else {
                $("#manageConnectorsMenuItem").removeClass("disabled");
            }
            var messages = [
                {
                    element     : "bodytrackMenuButton",
                    title       : "This is the BodyTrack Application",
                    content     : "It lets you explore your data in a zoomable timeline, load and save different views.",
                    placement   : "bottom"
                },{
                    element     : "calendarMenuButton",
                    title       : "This is the Calendar application",
                    content     : "This app gives you different aggregated views of your data: as a clock, a list, a map " +
                                  "or a photo gallery. It also provides a timeline, but it only shows the default channels " +
                                  "for each connector and doesn't let you load and save views like the BodyTrack app.",
                    placement   : "bottom"
                },{
                    element     : "timelineRuler",
                    title       : "Pan & Zoom",
                    content     : "If you have a trackpad, go up/down to Zoom in and out, left/right to pan.\n" +
                                  "If you have a mouse, use the scrollwheel to zoom in and out and drag the ruler left and right to pan.",
                    placement   : "top"
                }
            ];
            for (var i=0; i<messages.length; i++) {
                bindPopover(messages[i].element, messages[i].title, messages[i].content, messages[i].placement);
            }
        };

        function bindPopover(element, title, content, placement){
            if (typeof(App.messageDisplayCounters[element])=="undefined"||
                App.messageDisplayCounters[element]<3) {
                var popover = $("#"+element).popover({
                    container: "body",
                    placement: placement,
                    trigger: "hover",
                    title: title,
                    content: content,
                    animation: true
                });
                popover.on("hidden", function(e){
                    var element = e.target.id;
                    incrementMessageDisplay(element);
                    if (App.messageDisplayCounters[element]==2) {
                        $("#"+element).unbind();
                        $("#"+element).popover("destroy");
                    }
                });
            }
        }

        function incrementMessageDisplay(messageName){
            $.ajax({
                url: "/api/settings/"+messageName+"/increment",
                method: "POST",
                success: function(status){
                    if (status.result=="OK") {
                        var count =parseInt(status.payload,10);
                        App.messageDisplayCounters[messageName] = count;
                    } else
                        console.log("Couldn't increment message display for " + messageName)
                }
            });
        }

        function carousel(photoId) {
            $(".carousel-inner div.item").removeClass("active");
            $(".carousel-inner #photo-"+photoId).addClass("active");
            $('.carousel').carousel();
            $("#modal").modal("show");
            $(window).resize();
        }

        function invalidPath() {
            require([ "text!applications/invalidPath.html"], function(html) {
                $(".application").removeClass("active");
                $(".application").addClass("dormant");
                $("#applications").append(html);
            });
        }

        function getUsername(){
            return $("#flxUsername").html();
        }

        function getUID(){
            return $("#flxUID").html();
        }

        window.FlxUtils = {};
        FlxUtils.rowsOf = function(array, size) {
            if (array.length==0) return [[]];
            var row = [array[0]], rows = [{row : row}], i=1;
            for (; i<array.length; i++) {
                if (i%size===0) {
                    row = [];
                    rows.push({row : row});
                }
                row.push(array[i]);
            }
            return rows;
        }

        App.toPolar = function(center, x, y){
            x -= center[0];
            y -= center[1];
            var r = Math.sqrt(x * x + y * y);
            var theta;
            if (x == 0){
                if (y > 0)
                    theta = Math.PI / 2;
                else
                    theta = 3 * Math.PI / 2;
            }
            else if (y == 0){
                if (x > 0)
                    theta = 0;
                else
                    theta = Math.PI;
            }
            else if (x > 0)
                theta = Math.atan(y/x);
            else
                theta = Math.PI + Math.atan(y/x);
            theta *= 180 / Math.PI;
            if (theta < 0)
                theta += 360;
            return [r,theta];
        }


        App.getUsername = getUsername;
        App.getUID = getUID;
        App.initialize = initialize;
        App.renderApp = renderApp;
        App.state = FlxState;
        App.fullHeight = fullHeight;
        App.invalidPath = invalidPath;
        App.geocoder = new google.maps.Geocoder();
        App.sharingDialog = SharingDialog;
        window.App = App;
        return App;

    });
