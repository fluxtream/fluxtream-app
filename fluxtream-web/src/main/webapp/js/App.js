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
            var retina = window.devicePixelRatio > 1 ? true : false;
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
                // we create the top apps menu
                createAppsMenu();
                // we start the history
                Backbone.history.start({
                    pushState : true
                });
                // finally we render the default - or url-specified - app
                renderMainApp();
            });
        }

        /**
         * Add the buttons to the top apps menu
         */
        function createAppsMenu(appName, appIcon) {
            for ( var i = 0; i < FlxState.apps.length; i++) {
                var app = App.apps[FlxState.apps[i]];
                $("#apps-menu")
                    .append(
                    "<button id=\""
                        + app.name
                        + "MenuButton\" class=\"btn appMenuBtn\" "
                        + "onclick=\"javascript:App.renderApp('"
                        + app.name + "','last')\">"
                        + "<i class=\"" + app.icon
                        + "  icon-large\"></i></button>");
            }
        }

        /**
         * Render main app or the one that's specified in the location bar's
         * contents
         */
        function renderMainApp() {
            var path = Backbone.history.getFragment(),
                splits = path.split("/"),
                appString = splits.shift(),
                appName = splits.shift();
            if (appString !== 'app' || _.isUndefined(appName)) {
                App.activeApp = App.apps[FlxState.defaultApp];
                App.apps[FlxState.defaultApp].render("");
                return;
            }
            var app = App.apps[appName],
                appState = splits.join('/');
            FlxState.saveState(appName, appState);
            if (_.isUndefined(app)) {
                if (console && console.log) console.log("invalid app: " + appName);
                App.invalidPath();
            }
            App.activeApp = app;
        }

        function fullHeight() {
            if ($(".fullHeight").length>0) {
                tabsY = $("#tabs").position().top;
                windowHeight = $(window).height();
                footerHeight = $("#footer").height();
                fHeight = (windowHeight-tabsY-footerHeight-20);
                $(".fullHeight").height(fHeight);
            }
            $(window).resize(function() {
                setTimeout(App.fullHeight, 100);
            });
        }

        function renderApp(appName,state,params) {
            if (params == null)
                params = {};
            App.activeApp.saveState();
            App.activeApp=App.apps[appName];
            App.apps[appName].render(state,params);
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
            dialog.on("hidden",function(){
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
        }

        App.closeModal = function(){
            $("#modal").modal("hide");
        }

        App.eraseEverything = function() {
            var confirmed = confirm("Are you sure?");
            //TODO: Woot?! Why is this empty?
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
            })
        }

        function glow(element) {
            element.css("text-shadow", "0 0 10px white")
                .css("color", "white");
        }

        App.connectors = function() {
            AddConnectors.show();
        };

        App.addresses = function() {
            Addresses.show();
        }

        App.manageConnectors = function(){
            ManageConnectors.show();
        }

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

        App.getConnectorConfig = function(connectorName){
            var config = ConnectorConfig[connectorName];
            if (config == null){
                console.log("WARNING: No config found for connector: " + connectorName);
                config = ConnectorConfig.default;
            }
            config = $.extend({}, config);
            config.facets = false;
            return config;
        }

        App.getFacetConfig = function(facetName){
            var config = ConnectorConfig[App.getFacetConnector(facetName)];
            if (config == null){
                console.log("WARNING: No config found for Connector: " + App.getFacetConnector(facetName));
                config = ConnectorConfig.default;
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
                var savedConnectorContent = $(".addConnectorsMain").html();
                $.ajax({
                           url : url.substring(5),
                           success : function(html) {
                               $(".addConnectorsMain").html(html);
                               $(".focushere").focus();
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
                        $("#notification-" + notificationId).remove()
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

        App.addHideTooltipListener = function(hideFunction) {
            var onEvent = function(event){ //hides the tooltip if an element clicked on or any of its parents has the notthide property
                for (var target = event.target; target != null; target=target.parentElement){
                    if ($(target).attr("notthide") != null)
                        return;
                }
                hideFunction();
            };
            $(document).unbind("click").unbind("touchend").bind("touchend",onEvent).bind("click", onEvent);
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
        }

        function carousel(photoId) {
            $(".carousel-inner div.item").removeClass("active");
            $(".carousel-inner #photo-"+photoId).addClass("active");
            $("#modal").modal("show");
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
