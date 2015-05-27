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
    [ "core/FlxState", "ManageConnectors", "AddConnectors", "ConnectorConfig", "Settings", "SharingDialog",
      "libs/jquery.form", "libs/jquery.jeditable.mini", "libs/jquery.timeago" ],
    function(FlxState, ManageConnectors, AddConnectors, ConnectorConfig, Settings,
        SharingDialog ) {

        var App = {};

        App.apps = {};

        var compiledTemplates = {};

        var busyEditing;

        function initialize() {
            _.bindAll(this);
            // start loading all applications
            checkScreenDensity();
            loadApps();

            bindGlobalEventHandlers();
            checkForDataUpdates();

            App.addDataUpdatesListener("AppNotificationsListener",function(updates){
                if (updates.notification === true){
                    App.refreshNotifications();
                }
            });
        }

        function bindGlobalEventHandlers(){
            $(document).on("keyup.gloablAppEventHandler",function(event){
                nonIdleEventDetected();
                if (event.keyCode == 27 && App.modals.length > 0){
                    event.preventDefault();
                    App.modals[0].modal("hide");//close the top most modal dialog
                }
            });
            $(document).on("keydown.globalAppEventHandler",function(event){
                nonIdleEventDetected();
            });
            $(document).on("mousemove.globalAppEventHandler",function(event){
                nonIdleEventDetected();
            });
            $(document).on("scroll.globalAppEventHandler",function(event){
                nonIdleEventDetected();
            });
            $(document).on("mouseup.globalAppEventHandler",function(event){
                nonIdleEventDetected();
            });
            $(document).on("mousedown.globalAppEventHandler",function(event){
                nonIdleEventDetected();
            })

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
                fetchGuestInfo(setupURLRouting);
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
            FlxState.router.route("app/:name/*state", "app", appStateRoute);

            if (!Backbone.history.start({pushState : window.history && window.history.pushState})) {
                console.log("error loading routes!");
            }
        }

        function appStateRoute (appName, state) {
            var urlSplits = state.split("/");
            var btaPathParam = urlSplits[0];
            if (isNaN(btaPathParam)) {
                window.location = window.location.origin + "/app/" + appName + "/" + App.buddyToAccess.id + "/" + state;
                return;
            }
            state = state.substring(btaPathParam.length+1);

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
                renderDefault(app);
            } else {
                render(app, state);
            }
            if (hasBuddyToAccessChanged(btaPathParam)) {
                App.as(btaPathParam);
            }
        }

        function hasBuddyToAccessChanged(btaPathParam) {
            var btaChanged = false;
            if (isNaN(btaPathParam))
                btaChanged = (btaPathParam !== App.buddyToAccess["username"]);
            else
                btaChanged = (Number(btaPathParam) !== App.buddyToAccess["id"]);
            return btaChanged;
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

        App.modals = [];

        function makeModal(html) {
            var dialog = $(html);
            App.modals.unshift(dialog);
            dialog.addClass("modal");
            dialog.addClass("hide");
            $("body").append(dialog);
            dialog.modal();
            dialog.on("hidden.cleanupListener",function(event){
                event.stopImmediatePropagation();
                dialog.remove();
                var index = App.modals.indexOf(dialog);
                if (index == -1){
                    console.warn("couldn't find dialog in modal list");
                }
                else{
                    App.modals.splice(index,1);
                }
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
            return dialog;
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
            if (typeof(ga)!="undefined") {
                ga('send', 'event', 'button', 'click', 'eraseEverything', 1);
            }
            var confirmed = confirm("Are you sure?");
            if (confirmed) {
                $.ajax({
                    url: "/api/v1/settings/deleteAccount",
                    type: "POST",
                    success: function(body, statusText, jqXHR) {
                        console.log(statusText  + ":" + body);
                        window.location = "/logout";
                    }, error: function(jqXHR, statusText, errorThrown) {
                        alert("Could not delete account: " + statusText+" (" + errorThrown + ") " + ". Please contact us!");
                    }
                });
            }
        };

        App.as = function(usernameOrUID) {
            $("#wallDialog").dialog("destroy");
            if (!_.isUndefined(usernameOrUID)) {
                //App.chatBuddy = null;
                //App.buddyToAccess = null;
                fetchGuestInfo(function() {
                    if (App.buddyToAccess.isBuddy) {
                        $(".backtomydata").show();
                        $(".trustedbuddies").hide();
                    } else {
                        $(".backtomydata").hide();
                        $(".trustedbuddies").show();
                    }
                    App.activeApp.navigateState(App.state.getState(App.activeApp.name), {"as" : usernameOrUID});//force refresh of the current app state
                }, usernameOrUID);
            }
        };

        App.reachOutTo = function(usernameOrUID) {
            $("#wallDialog").dialog("destroy");
            if (!_.isUndefined(usernameOrUID)) {
                var url = "/api/v1/guest/trusted?includeAvatar=true";
                url+="&"+App.BUDDY_TO_ACCESS_PARAM+"="+usernameOrUID;
                $.ajax({
                    url: url,
                    success: function(guestInfo) {
                        App.chatBuddy = guestInfo;
                        loadWallDialog();
                    },
                    error: function(jqXHR, statusText, errorThrown) {
                        App.logError(jqXHR, statusText, errorThrown);
                    }
                });
            }
        };

        function loadWallDialog() {
            //App.loadMustacheTemplate("messagingTemplates.html","wallDialogTemplate",
            //    function(template) {
            //        var html = template.render({buddy: App.chatBuddy});
            //        $(html).dialog({
            //            autoOpen: true,
            //            modal: false,
            //            width: 300,
            //            height: 500,
            //            position: {
            //                my: "left top+20",
            //                at: "bottom",
            //                of: ".brand"
            //            },
            //            buttons : {
            //                Close : function() {
            //                    $("#wallDialog").dialog("close");
            //                }
            //            }
            //        });
            //        loadWallDialogContents();
            //        $("#sendWallPostBody").click(function() {
            //            $.ajax({
            //                url: "/api/v1/posts",
            //                type: "POST",
            //                data: {
            //                    message : $("#wallPostBody").val(),
            //                    to: App.chatBuddy["username"]
            //                },
            //                success: function() {
            //                    $("#wallPostBody").val("");
            //                    loadWallDialogContents();
            //                },
            //                error: function(jqXHR, status, errorThrown) {
            //                    var errorMessage = errorThrown + ": " + jqXHR.responseText;
            //                    console.log(errorMessage);
            //                    alert(errorMessage);
            //                    loadWallDialogContents();
            //                }
            //            });
            //        });
            //        $("#clearWallPostBody").click(function(evt){
            //            $("#wallPostBody").val("");
            //        });
            //    }
            //);
        }

        function loadWallDialogContents() {
            if (busyEditing) return;
            $.ajax({
                url: "/api/v1/posts/all/" + App.chatBuddy["username"] + "?includeComments=true",
                success: function(posts) {
                    for (var i=0; i<posts.length; i++) {
                        var post = posts[i];
                        if (post.from!=null) {
                            if (post.from.firstname!=null&&post.from.firstname!="")
                                post.author = post.from.firstname;
                            else
                                post.author = post.from.fullname;
                        } else {
                            post.author = "You";
                        }
                        if (!_.isNull(post.comments)&&!_.isUndefined(post.comments)){
                            for (var j=0; j<post.comments.length; j++) {
                                var comment = post.comments[j];
                                comment.postId = post.id;
                                if (comment.from!=null) {
                                    if (comment.from.firstname!=null&&comment.from.firstname!="")
                                        comment.author = comment.from.firstname;
                                    else
                                        comment.author = comment.from.fullname;
                                } else
                                    comment.author = "You";
                                comment.when = moment(comment.creationTime).fromNow();
                            }
                        }
                        post.when = moment(post.creationTime).fromNow();
                    }
                    App.loadMustacheTemplate("messagingTemplates.html","wallDialogContentsTemplate",
                        function(template) {
                            var html = template.render({posts: posts});
                            $("#wallDialogContents").empty().append(html);
                            $(".deleteWallPostButton").click(function(evt){
                                var postId = $(evt.target).closest("a").attr("data-id");
                                deletePost(postId);
                            });
                            $(".deleteWallPostCommentButton").click(function(evt){
                                var postId = $(evt.target).closest("a").attr("data-postId");
                                var commentId = $(evt.target).closest("a").attr("data-id");
                                console.log("delete comment" + postId + "/" + commentId);
                                deletePostComment(postId, commentId);
                            });
                            $(".editWallPostButton").click(function(evt){
                                var postId = $(evt.target).closest("a").attr("data-id");
                                console.log("edit " + postId);
                            });
//                            $(".editWallPostCommentButton").click(function(evt){
//                                var commentId = $(evt.target).closest("a").attr("data-id");
//                            });
                            bindAddWallCommentButton();
                        }
                    );
                    setTimeout(loadWallDialogContents, 1000*10);
                }
            });
        }

        function bindAddWallCommentButton() {
            $(".addWallCommentButton").unbind().click(function(evt){
                var targetAddCommentButton = $(evt.target);
                var postId = targetAddCommentButton.attr("data-postid");
                App.loadMustacheTemplate("messagingTemplates.html","wallCommentFormTemplate",
                    function(template) {
                        var html = template.render({postId:postId});
                        targetAddCommentButton.replaceWith(html);
                        busyEditing = true;
                        bindAddWallCommentForm(postId);
                    }
                );
            });
        }

        function bindAddWallCommentForm(postId) {
            var formId = "#wallCommentForm-" + postId;
            $(formId + " > .sendWallCommentBody").click(function(evt) {
                var message = $(formId + " > .wallCommentBody").val();
                console.log("send comment " + postId + ": " + message);
                $.ajax({
                    url: "/api/v1/posts/" + postId + "/comments",
                    type: "POST",
                    data: {message: message},
                    success: function() {
                        loadWallDialogContents();
                    },
                    error: function(jqXHR, status, errorThrown) {
                        var errorMessage = errorThrown + ": " + jqXHR.responseText;
                        console.log(errorMessage);
                        alert(errorMessage);
                        loadWallDialogContents();
                    }
                });
                busyEditing = false;
            });
            $(formId + " > .cancelAddComment").click(function(evt) {
                console.log("cancel add comment " + postId);
                App.loadMustacheTemplate("messagingTemplates.html","addWallCommentButtonTemplate",
                    function(template) {
                        busyEditing = false;
                        var html = template.render({postId: postId});
                        $("#wallCommentForm-" + postId).replaceWith(html);
                        bindAddWallCommentButton();
                    }
                );
            });
        }

        function deletePost(postId) {
            $.ajax({
                url : "/api/v1/posts/" + postId,
                type: "DELETE",
                success: loadWallDialogContents
            });
        }

        function deletePostComment(postId, commentId) {
            $.ajax({
                url : "/api/v1/posts/" + postId + "/comments/" + commentId,
                type: "DELETE",
                success: loadWallDialogContents
            });
        }

        App.apiUri = function(uri) {
          if (uri.indexOf("{buddyToAccess.id}")!=-1) {
            return uri.replace("{buddyToAccess.id}", App.buddyToAccess.id);
          }
          return uri;
        }

        function fetchGuestInfo(andDoThisAfter, buddyToAccessParam) {
            var url = "/api/v1/guest?includeAvatar=true";
            if (buddyToAccessParam)url+="&"+App.BUDDY_TO_ACCESS_PARAM+"="+buddyToAccessParam;
            $.ajax({
                url: url,
                success: function(guestInfo) {
                    App.buddyToAccess = guestInfo;
                    App.chatBuddy = guestInfo;
                    var loggedInUser = $("#loggedInUser");
                    loggedInUser.attr("self", guestInfo["fullname"]);
                    loggedInUser.html(guestInfo["fullname"] + "<span id=\"profileIcon\">&nbsp;</span> <b id=\"profileIconCaret\" class=\"caret\"></b>");
                    if (App.buddyToAccess["isBuddy"]) {
                        loggedInUser.css("text-shadow", "0 0 10px white");
                        loggedInUser.css("color", "#FFFEFD");
                        $("#addConnectorLink").addClass("disabled-link").unbind().click(function(evt){evt.preventDefault()});
                    }
                    else {
                        loggedInUser.css("text-shadow", "");
                        loggedInUser.css("color", "");
                        $("#addConnectorLink").removeClass("disabled-link").unbind().click(function(){App.connectors();});
                    }
                    if (guestInfo["avatar"]!=null) {
                        if (guestInfo["avatar"].type!="none") {
                            $("#profileIcon").replaceWith("<img src=\"" + guestInfo.avatar.url + "\" style=\"display:inline;width:27px;margin: 0 1px 0 4px;\" width=27 height=27>");
                            $("#profileIconCaret").css("margin-top", "10px");
                            $("#forumMenuItem").css("margin-top", "3px");
                            $("#helpDropdownToggle").css("margin-top", "3px");
                            $("#buddiesDropdownToggle").css("margin-top", "3px");
                            $("#connectorsDropdownToggle").css("margin-top", "3px");
                            $("#appsMenuWrapper").css("margin-top", "4px");
                            $(".brand").css("margin-top", "3px");
                        } else {
                            $("#profileIcon").replaceWith("<i class=\"icon-user icon-large\"></i>");
                        }
                    }
                    if (!_.isUndefined(andDoThisAfter))
                        andDoThisAfter();
                    App.activeApp.renderState(App.state.getState(App.activeApp.name),true);//force refresh of the current app state
                    if (App.buddyToAccess["isBuddy"])
                        loadWallDialog()
                    checkForDataUpdates();
                },
                error: function(jqXHR, statusText, errorThrown) {
                    App.logError(jqXHR, statusText, errorThrown);
                }
            });
        }

        App.logError = function(jqXHR, statusText, errorThrown) {
            console.log(statusText+" (" + errorThrown + ") ");
            console.log(jqXHR.responseText);
        }

        App.connectors = function() {
            AddConnectors.show();
        };

        App.manageConnectors = function(){
            ManageConnectors.show();
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
            config.objectType = facetName;
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
                    success : function(body, statusText, jqXHR) {
                        $("#modal").modal("hide");
                        App.activeApp.renderState(App.state.getState(App.activeApp.name),true);
                    },
                    error: function(jqXHR, statusText, errorThrown) {
                        alert("Could not add upload-only connector: " + jqXHR.responseText);
                    }
                });
            } else {
                var loading = $("#loading").clone().show();
                $(".addConnectorsMain").empty();
                $(".addConnectorsMain").append(loading);
                setTimeout("window.location='" + url + "'", 500);
            }
            if (typeof(ga)!='undefined') {ga('send', 'event', 'button', 'click', 'addConnector', 1);}
        };

        App.discardNotification = function(notificationId) {
            $.ajax({
                    url: "/api/v1/notifications/" + notificationId,
                    type: "DELETE",
                    success: function() {
                        $("#notification-" + notificationId).remove();
                        $(window).resize();
                    }
                }
            );
        };

        App.handleNotificationList = function(notificatons){

            $(".alert").remove();
            $("#notifications").empty();
            if (typeof(notificatons)!="undefined"&&notificatons!=null) {
                for (var n=0; n<notificatons.length; n++) {
                    console.log("showing a notification " + n);
                    if ($("#notification-" + notificatons[n].id).length==0) {
                        (function(n){
                            App.loadMustacheTemplate("notificationTemplates.html",notificatons[n].type+"Notification",function(template) {
                                if (notificatons[n].repeated>1) notificatons[n].message += " (" + notificatons[n].repeated + "x)";
                                var html = template.render(notificatons[n]);
                                $("#notifications").append(html);
                                $("abbr.timeago").timeago();
                                $(window).resize();
                            });
                        })(n);
                    }
                }
                $("#notifications").show();
            }

        }

        App.refreshNotifications = function(){
            $.ajax("/api/v1/notifications/all",{
                success:function(result){
                    App.handleNotificationList(result.notifications);
                },
                error: function(){
                    console.log(arguments);
                }
            })
        }

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

        var monthNames = ["Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Nov","Dec"];

        App.duplicateVisitedCityForDate = function(city,date){
            var cityToReturn = $.extend({},city,{});
            cityToReturn.date = date;
            var dateWithTimezoneParts = cityToReturn.dateWithTimezone.split(" ");
            dateWithTimezoneParts.splice(0,1,date);
            cityToReturn.dateWithTimezone = dateWithTimezoneParts.join(" ");

            var msDifference = new Date(cityToReturn.dateWithTimezone) - new Date(city.dateWithTimezone);
            cityToReturn.dayStart += msDifference;
            cityToReturn.dayEnd += msDifference;

            var startTime = city.startTime.split(" ");
            var endTime = city.endTime.split(" ");

            var targetDate = new Date(cityToReturn.dateWithTimezone);
            startTime[0] = endTime[0] = monthNames[targetDate.getMonth()];
            startTime[1] = endTime[1] = targetDate.getDate();
            if (startTime[1] < 10)
                startTime[1] = endTime[1] = "0" + startTime[1];
            startTime[1] = endTime[1] = startTime[1] + ",";

            cityToReturn.startTime = startTime.join(" ");
            cityToReturn.endTime = endTime.join(" ");


            return cityToReturn;

        }

        App.getFacetCity = function(facet, citiesList){
            var closestCity = null;
            var cityTimeDistance = 0;
            for (var i= 0, li = citiesList.length; i < li; i++) {
                var city = citiesList[i];
                if (city.date===facet.date) {
                    //console.log("found date for facet\ncity: " + JSON.stringify(city) + "\nfacet: " + JSON.stringify(facet));
                    return city;
                }
                else{
                    var distance = Math.abs(new Date(city.date) - new Date(facet.date));
                    if (closestCity == null || distance < cityTimeDistance){
                        cityTimeDistance = distance;
                        closestCity = city;
                    }


                }
            }
            //if we couldn't find the city for the facet we attempt to get a closest match, in many places having no city will make the facet unviewable
            return App.duplicateVisitedCityForDate(closestCity,facet.date);
        };

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
                else if ($(target).attr("data-toggle") == "collapse-custom"){
                    if ($($(target).attr("data-target")).hasClass("collapse")){
                        $($(target).attr("data-target")).removeClass("collapse");
                        $($(target).attr("data-target")).removeClass("in");

                    }
                    else{
                        $($(target).attr("data-target")).addClass("collapse");
                        $($(target).attr("data-target")).addClass("in");
                    }
                    event.preventDefault();
                    break;
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

        $(document).bind("touchstart",onEvent).bind("touchend",onEvent).bind("click",globalClickHandler).bind("mousedown", onEvent);

        App.addHideTooltipListener = function(hideFunction) {
            hideFunctions.push(hideFunction);
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
            var details = finedetails.html();
            finedetails.toggleClass("flx-collapsed");
            if (!finedetails.hasClass("flx-collapsed")){
                finedetails.empty();
                finedetails.append(details);
            }
            finedetails.parent().parent().trigger("contentchange");
        }

        App.setupBeginnersFriendlyUI = function (messageDisplayCounters, nApis) {
            App.messageDisplayCounters = messageDisplayCounters;
            if (nApis==0) {
                $("#manageConnectorsMenuItem").addClass("disabled");
                if (!App.buddyToAccess.isBuddy) {
                    $("#connectorsDropdownToggle").popover({
                        container: "body",
                        placement: "bottom",
                        title: "Click menu above to add your first Connector!",
                        content: "Connectors let Fluxtream link up your data",
                        animation: true
                    });
                    $("#connectorsDropdownToggle").popover("show");
                }
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
                if (!App.buddyToAccess.isBuddy) {
                    var popover = $("#" + element).popover({
                        container: "body",
                        placement: placement,
                        trigger: "hover",
                        title: title,
                        content: content,
                        animation: true
                    });
                }
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
                url: "/api/v1/settings/"+messageName+"/increment",
                method: "POST",
                success: function(status, statusText, jqXHR){
                    var count =parseInt(status["payload"],10);
                    App.messageDisplayCounters[messageName] = count;
                },
                error: function(jqXHR, statusText, errorThrown) {
                    console.log("Couldn't increment message display for " + messageName);
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

        App.adjustiFrameHeight = function(iFrameId) {
            var iFrame = document.getElementById(iFrameId);
            if(iFrame) {
                iFrame.height = "";
                var height = iFrame.contentWindow.document.body.scrollHeight + "px";
                iFrame.height = height;
            }
        }

        App.quickStart = function() {
            App.loadMustacheTemplate("settingsTemplates.html","quickStartDialog",function(template){
                var html = template.render({release : window.FLX_RELEASE_NUMBER});
                App.makeModal(html);
            });
        }

        App.privacyPolicy = function() {
            App.loadMustacheTemplate("settingsTemplates.html","privacyPolicyDialog",function(template){
                var html = template.render({release : window.FLX_RELEASE_NUMBER});
                App.makeModal(html);
            });
        }

        var dataUpdateListeners = {};

        App.addDataUpdatesListener = function(name,listener){
            dataUpdateListeners[name] = listener;
        }

        App.removeDataUpdatesListener = function(name){
            delete dataUpdateListeners[name];
        }


        //the last time they performed a nonidle event
        var lastNonIdleEvent = new Date().getTime();
        //the timestamp they went idle
        var idleStartTime = 0;
        //whether or not the user is marked idle
        var isIdle = false;

        //the amount of time a user can be idle before they are marked as idle
        var maxIdleTime = 1000 * 60 * 20;    //20 minutes
        //how often updates should be polled
        var updateCheckInterval = 1000 * 30;//30 seconds

        //used to prevent nonidle event spam
        var nonIdleMutex = false;
        //resolution of nonidle event detection
        var nonIdleMutexDuration = maxIdleTime / 60;

        function nonIdleEventDetected(){
            if (nonIdleMutex) return; //prevent from unnecessary spamming calls to Date().getTime()
            nonIdleMutex = true;
            lastNonIdleEvent = new Date().getTime();
            if (isIdle){
                isIdle = false;
                //idle to nonidle
                /*var millisSpentIdle = lastNonIdleEvent - idleStartTime;
                if (millisSpentIdle > 1000 * 60 * 60){//check if it's been longer than an hour
                    //do something...
                }*/
                checkForDataUpdates();
            }
            setTimeout(function(){
                nonIdleMutex = false;
            },nonIdleMutexDuration);
        }



        var lastCheckTimestamp = moment().format("YYYY-MM-DDThh:mm:ss.SSSZZ");

        function checkForDataUpdates(){
            if (isIdle || new Date().getTime() - lastNonIdleEvent > maxIdleTime){
                isIdle = true;
                idleStartTime = lastNonIdleEvent;
                return;
            }
            function afterDone(){
                setTimeout(checkForDataUpdates,updateCheckInterval);
            }
            var url = "/api/v1/dataUpdates/all";
            if (App.buddyToAccess["isBuddy"]) url += "?"+App.BUDDY_TO_ACCESS_PARAM+"="+App.buddyToAccess["id"];
            $.ajax(url, {
                type: "GET",
                dataType: "json",
                data: {since: lastCheckTimestamp},
                success: function(data){
                    lastCheckTimestamp = data.generationTimestamp;
                    for (var member in dataUpdateListeners){
                        dataUpdateListeners[member](data);
                    }
                    afterDone();
                },
                error: function(){
                    afterDone();
                }

            });
        }

        App.sandboxCounter = 0;
        App.eventListeners = [];
        App.addSandboxMessageListener = function(listener){
            window.addEventListener("message",listener,false);
            App.eventListeners.push(listener);
        }

        App.clearAllSandboxMessageListeners = function(){
            while (App.eventListeners.length > 0){
                window.removeEventListener("message",App.eventListeners.shift(),false);
            }
        }


        App.initialize = initialize;
        App.renderApp = renderApp;
        App.state = FlxState;
        App.fullHeight = fullHeight;
        App.invalidPath = invalidPath;
        App.geocoder = new google.maps.Geocoder();
        App.sharingDialog = SharingDialog;
        App.BUDDY_TO_ACCESS_PARAM = "buddyToAccess";
        App.buddyToAccess = {isBuddy : false};
        window.App = App;
        return App;

    });
