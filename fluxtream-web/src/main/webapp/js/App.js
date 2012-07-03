define(
    [ "core/FlxState", "Addresses", "ManageConnectors", "AddConnectors", "ConnectorConfig",
      "libs/jquery.form", "libs/jquery.qtip.min" ],
    function(FlxState, Addresses, ManageConnectors, AddConnectors, ConnectorConfig) {

        var App = {};
        var toLoad = 0, loaded = 0;
        var apps = {};
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
            toLoad = FlxState.apps.length;
            for ( var i = 0; i < FlxState.apps.length; i++) {
                require([ "applications/" + FlxState.apps[i] + "/App" ],
                        function(app) {
                            apps[app.name] = app;
                            app.initialize();
                            appLoaded(app.name);
                        });
            }
        }

        /**
         * Add the buttons to the top apps menu
         */
        function createAppsMenu(appName, appIcon) {
            for ( var i = 0; i < FlxState.apps.length; i++) {
                var app = apps[FlxState.apps[i]];
                $("#apps-menu")
                    .append(
                    "<button id=\""
                        + app.name
                        + "MenuButton\" class=\"btn\" "
                        + "onclick=\"javascript:App.renderApp('"
                        + app.name + "')\">"
                        + "<i class=\"" + app.icon
                        + "  icon-large\"></i></button>");
            }
        }

        /**
         * Application-is-loaded callback
         */
        function appLoaded(appName) {
            // we keep track of how many apps have been loaded
            loaded++;
            // when all apps are loaded...
            if (loaded === toLoad) {
                App.apps = apps;
                // we create the top apps menu
//                createAppsMenu();
                // we start the history
                Backbone.history.start({
                                           pushState : true
                                       });
                // finally we render the default - or url-specified - app
                renderMainApp();
            }
        }

        /**
         * Render main app or the one that's specified in the location bar's
         * contents
         */
        function renderMainApp() {
            var parse_url = /^(?:([A-Za-z]+):)?(\/{0,3})([0-9.\-A-Za-z]+)(?::(\d+))?(?:\/([^?#]*))?(?:\?([^#]*))?(?:#(.*))?$/;
            var result = parse_url.exec(window.location.href);
            var names = [ 'url', 'scheme', 'slash', 'host', 'port', 'path',
                          'query', 'hash' ];
            var blanks = ' ';
            var i;
            var parts = {};
            for (i = 0; i < names.length; i += 1)
                parts[names[i]] = result[i];
            var splits = parts.path.split("/");
            if (splits[0] === "app" && typeof (splits[1]) != "undefined") {
                var appState = parts.path.substring("app/".length
                                                        + splits[1].length + 1);
                var appName = splits[1];
                FlxState.saveState(appName, appState);
                if (typeof(apps[appName])=="undefined") {
                    if (console && console.log) console.log("invalid app: " + appName);
                    App.invalidPath();
                }
                App.activeApp = apps[appName];
            } else {
                App.activeApp = apps[FlxState.defaultApp];
                apps[FlxState.defaultApp].render("");
            }
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

        function renderApp(appName) {
            App.activeApp.saveState();
            App.activeApp=App.apps[appName];
            App.apps[appName].render("last");
        }

        App.settings = function() {
            $.ajax({
                       url : "/settings/main",
                       success : function(html) {
                           makeModal(html);
                           $("#settingsForm").ajaxForm(function() {
                               $("#modal").empty();
                           });
                       }
                   });
        };

        function makeModal(html) {
            $("#modal").replaceWith(html);
            $("#modal").modal();
        }

        App.makeModal = makeModal;
        App.carousel = carousel;

        App.loadMustacheTemplate = function(templatePath,templateId,onLoad){

            if (typeof(compiledTemplates[templateId])!="undefined") {
                onLoad(compiledTemplates[templateId]);
                return;
            }

            var that = this;
            require(["text!" + templatePath], function(template) {
                var html = template;
                var templateStartSearch = "<template id=\"" + templateId + "\">";
                var htmlStart = html.indexOf(templateStartSearch);
                if (htmlStart == -1){//template not found
                    onLoad(null);
                    return;
                }
                htmlStart += templateStartSearch.length;
                var htmlEnd = html.indexOf("</template>",htmlStart);
                html = html.substring(htmlStart,htmlEnd);
                compiledTemplates[templateId] = Hogan.compile(html);
                onLoad(compiledTemplates[templateId]);
            });
        }

        App.closeModal = function(){
            $("#modal").modal("hide");
        }

        App.eraseEverything = function() {
            var confirmed = confirm("Are you sure?");
        };

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
                                + "you will loose them forever.\n"
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
            console.log("showing connectors page " + page);
            $("#availableConnectors").load(
                "/connectors/availableConnectors?page=" + page);
        };

        App.discardNotifications = function() {
            var ids = $("#notificationIds").html();
            $.ajax({
                       url : "/notifications/discard?ids=" + ids,
                       success : function() {
                           $("#notifications").alert("close");
                       }
                   });
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

        App.formatDate = function(date, includeTime){
            if (includeTime == null)
                includeTime = false;
            if (typeof(date) == "number")
                date = new Date(date);
            if (isNaN(date.getFullYear()))
                return "Present";
            var value = "";
            switch (date.getMonth()){
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
            value += " " + date.getDate();
            value += ", " + date.getFullYear();
            if (includeTime){
                value += " " + date.getHours();
                value += ":"
                if (date.getMinutes() < 10)
                    value += "0";
                value += date.getMinutes();
                value += ":"
                if (date.getSeconds() < 10)
                    value += "0";
                value += date.getSeconds();
            }
            return value;
        }

        App.formatMinuteOfDay = function(minuteOfDay){
            var hour = Math.floor(minuteOfDay/60);
            var minutes = Math.floor(minuteOfDay%60);
            if (minutes<10) minutes = "0" + minutes;
            if (hour<12)
                return (hour == 0 ? 12 : hour) + ":" + minutes + " AM";
            else
                return (hour > 12 ? hour - 12 : 12) + ":" + minutes + " PM";
        }

        App.formatDateAsDatePicker = function(date){
            if (typeof(date) == "number")
                date = new Date(date);
            if (isNaN(date.getFullYear()))
                return "Present";
            return date.getFullYear() + "-" + (date.getMonth() < 9 ? "0" : "") + (date.getMonth() + 1) + "-" + (date.getDate() < 9 ? "0" : "") + date.getDate();
        }


        App.search = function() {
            $(".application").load("/search/0?q=" + $(".search-query").val());
        };

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

        App.getUsername = getUsername;
        App.initialize = initialize;
        App.renderApp = renderApp;
        App.state = FlxState;
        App.fullHeight = fullHeight;
        App.invalidPath = invalidPath;
        App.geocoder = new google.maps.Geocoder();
        window.App = App;
        return App;

    });
