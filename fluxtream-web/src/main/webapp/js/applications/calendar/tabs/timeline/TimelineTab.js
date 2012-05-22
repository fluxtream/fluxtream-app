define(["applications/calendar/tabs/Tab", "core/FlxState", "applications/calendar/tabs/timeline/BodyTrack",
        "applications/calendar/App"],
    function(Tab, FlxState, BodyTrack, Calendar) {

    var APP 		= BodyTrack.APP;
    var PREFS 		= BodyTrack.PREFS;
    var TOOLS 		= BodyTrack.TOOLS;
    var LOGIN 	    = BodyTrack.LOGIN;
    var TAG_MANAGER = BodyTrack.TAG_MANAGER;
    var VIEWS 		= BodyTrack.VIEWS;
    var SOURCES 	= BodyTrack.SOURCES;

    var newViewName  		 = "Untitled View";
    var channelIdx   		 = 0;     // counter used to uniquely identify channels
    var dragSourceId 		 = null;  // DOM id of source is stored here during drag
    var dateAxis     		 = null;  // TIMELINE's dateAxis needed for addChannel()
    var sourcesMap   		 = {};    // maps DOM element ID to available source
    var channelsMap  		 = {};    // maps DOM element ID to view's y_axes
    var plotsMap     		 = {};    // maps DOM element ID to grapher widget
    var plotContainersMap    = {};    // maps DOM element ID to plot container
    var plotContainers       = [];    // array of plot containers
    var hasUnsavedChanges    = false; // used by unsaved changes dialog handler
    var loadedViewStr        = "";    // JSON string of loaded view
    var addPaneChannelsState = [];    // add channels pane channel visibility

    /// A helper to create a data fetcher for the specified URL prefix
    ///
    /// @param urlPrefix
    ///  The shared part of the URLs to fetch from.  Each fetch will come
    ///  from the URL (urlPrefix + level + "." + offset + ".json)
    /// @param urlParams
    ///  Optional parameter - if present, must be a dictionary.  This
    ///  is joined together and URL encoded to form the URL parameters
    ///  on each request
    /// @return
    ///  A function matching the requestData specification from the
    ///  grapher API specification, taking a level, an offset, a success
    ///  callback, and a failure callback, making a request to a URL
    ///  built from the URL prefix, level, and offset, and calling the
    ///  success continuation on success or the failure continuation on
    ///  failure
    function __createDatasource(urlPrefix, urlParams) {
        if (!urlParams) {
            urlParams = {};
        }
        return function(level, offset, success_callback, failure_callback) {
            var onerr = function(jqXHR, textStatus, errorThrown) {
                try {
                    if (failure_callback) {
                        failure_callback(errorThrown);
                    }
                }
                catch (ex) {
                    console.log("channelDatasource.onErr(): FAILURE! ex:" + ex);
                }
            };
            $.ajax({
                url     : urlPrefix + level + "." + offset + ".json",
                data    : urlParams,
                success : function(data, textStatus, jqXHR) {
                    try {
                        if (success_callback) {
                            // we must always send the JSON as a String...
                            success_callback(typeof data === 'string' ? data : JSON.stringify(data));
                        }
                    }
                    catch (ex) {
                        onerr(jqXHR, "JSON parse error", ex);
                    }
                },
                failure : onerr
            });
        };
    };

    function init(callback) {
        // Unsaved changes dialog handler
        $(window).bind("beforeunload", function() {
            updateViewData();
            var newvdata = JSON.stringify(VIEWS.data);
            if (loadedViewStr != newvdata) {
                hasUnsavedChanges = true;
            }

            if (hasUnsavedChanges) {
                return "You have unsaved changes";
            }
        });
        $("form").submit(function() {
            $(window).unbind("beforeunload");
        });

        // Deal with window resize events
        TOOLS.onWindowResizeResizeElementHeight("#_timeline_addChannelsArea #_timeline_sources_list");
        TOOLS.onWindowResizeResizeElementHeight("#_timeline_channelsWrapper");
        TOOLS.onWindowResizeListener(function() {
            // borderOffset is used to account for:
            // * 1 pixel of border on #_timeline_channels
            // * 1 pixel of border on ._timeline_channel
            var borderOffset = 2;

            var widthOfAreaLeftOfPlotContainer = $("._timeline_channeltd").width() + borderOffset;
            var widthOfAreaRightOfPlotContainer = $("._timeline_yaxistd").width() + borderOffset;

            // the .plotContainer has a 1 pixel border around it, so set this to 2 to account for the left and right sides
            var widthOfPlotContainerLeftAndRightBorder = 2;

            // compute the desired size of plot containers based on the current width of the _timeline_channelsArea (which
            // will automatically shrink if the Add Channels and/or Details pane is visible, so we don't explicitly need
            // to account for them here).
            var plotContainerWidth = $("#_timeline_channelsArea").width() - widthOfAreaLeftOfPlotContainer - widthOfAreaRightOfPlotContainer - widthOfPlotContainerLeftAndRightBorder - 20;

            // resize plot containers
            var plotContainerEventId = SequenceNumber.getNext();
            for (var i = 0; i < plotContainers.length; i++) {
                var plotContainerHeight = $("#" + plotContainers[i].getPlaceholder()).height();
                plotContainers[i].setSize(plotContainerWidth, plotContainerHeight, plotContainerEventId);
            }

            // resize date axis
            if (dateAxis) {
                dateAxis.setSize(plotContainerWidth, $("#_timeline_dateAxis").height(), SequenceNumber.getNext());
            }

            // resize y axes
            var yAxisWidth = $("._timeline_yAxis").width();
            for (var plotKey in plotsMap) {
                var plot = plotsMap[plotKey];
                if (plot) {
                    var yAxis = plot.getVerticalAxis();
                    var yAxisHeight = $("#" + yAxis.getPlaceholder()).height();
                    if (yAxis) {
                        yAxis.setSize(yAxisWidth, yAxisHeight);
                    }
                }
            }
        });

        // Make the channel list sortable
        $("#_timeline_channels").sortable({
            handle      : '._timeline_channelTab',
            axis        : 'y',
            tolerance   : 'pointer',
            containment : '#_timeline_channels',
            merge		: function(event, ui) {
            	var templateValues = {
                        "deviceName"       : "Devices",
                        "channelName"      : "Compare Stub",
                        "plotElementId"    : "_timeline_channel_helper",
                        "channelElementId" : "_timeline_plot_helper",
                        "yAxisElementId"   : "_timeline_yAxis_helper"
            	};
            	var html = $.mustache($("#_timeline_channel_template").html(), templateValues);
                    
            	$(ui.item[0]).remove();
            	$(ui.droppable.item[0]).replaceWith(html);
            },
            mergein		: function(event, ui) {
            	$(ui.droppable.item[0]).addClass("_timeline_channel_hover");
            },
            mergeout	: function(event, ui) {
            	$(ui.droppable.item[0]).removeClass("_timeline_channel_hover");
            },
            receive     : function(event, ui) {	// received new channel to add
                var i, l, c;
                var src = sourcesMap[dragSourceId];

                // Iterate through channels and call addChannel on
                // entries with no id
                // NOTE: We assume the only reason the id is blank is if the
                //       element is new (user dragged into channels)
                c = $("#_timeline_channels").children();
                l = c.length;
                for (i = 0; i < l; i++) {
                    if (c[i].id == "") {
                        addChannel(src, c[i]);
                    }
                }
            }
        });
        $("#_timeline_channels").disableSelection();

        // Click handlers
        $("#_timeline_new_view_btn").click(newView);
        $("#_timeline_load_view_btn").click(toggleLoadDialog);

        $("#_timeline_gotoBeginning_button").click(function() { gotoTime("beginning"); });
        $("#_timeline_gotoBack_button").click(function() { gotoTime("back"); });
        $("#_timeline_gotoForward_button").click(function() { gotoTime("forward"); });
        $("#_timeline_gotoEnd_button").click(function() { gotoTime("end"); });
        $("#_timeline_zoomOut_button").click(function() { zoomTime("out"); });
        $("#_timeline_zoomIn_button").click(function() { zoomTime("in"); });

        // Configure the photo dialog
        $("#_timeline_photo_dialog")['dialog'](
            {
                autoOpen  : false,
                modal     : true,
                width     : 'auto',
                height    : 'auto',
                minWidth  : 340,
                resizable : false
            }
        );

        // Load sources
        console.log("getSources is about to be called");
        getSources(function() {
            console.log("getSources is called");
            $("#_timeline_messageArea").hide();
            $("#_timeline_mainContentArea").show();

            if (typeof callback === "function") {
                console.log("callback is called: " + callback);
                callback();
            }
        });
    } // init

    // Check for unsaved changes to timeline and prompt user if needed
    function checkForTimelineChanges() {
        var newvdata;
        if (VIEWS.data != "") {
            updateViewData();
            newvdata = JSON.stringify(VIEWS.data);
            if (loadedViewStr != newvdata) {
                hasUnsavedChanges = true;
                return confirm("You have unsaved changes. Do you wish to continue?");
            }
        }
    }

    function getSourceChannelByName(device_name, channel_name) {
        var i, j, l, m;
        var src, channel;

        l = SOURCES.availableList.length;
        for (i = 0; i < l; i++) {
            src = SOURCES.availableList[i];

            // Match device_name
            if (device_name === src["name"]) {
                m = src.channels.length;
                for (j = 0; j < m; j++) {
                    channel = src.channels[j];

                    // Match channel_name
                    if (channel_name === channel["name"]) {
                        return channel;
                    }
                }
            }
        }
        return null;
    }; // getTimeFromSource

    function getSources(callback) {
        console.log("about to getAvailableList.");
        SOURCES.getAvailableList(function(data) {
            console.log("getting available list");

            var i, j, l, m;
            var src;
            var idx = 0;

            // Add unique identifier for each source channel
            // and populate sourcesMap
            l = SOURCES.availableList.length;
            for (i = 0; i < l; i++) {
                src = SOURCES.availableList[i];
                m = src.channels.length;
                for (j = 0; j < m; j++) {
                    src.channels[j]["id"] = "src_" + idx;

                    sourcesMap["src_" + idx] = {
                        "device_name"  : src["name"],
                        "channel_name" : src.channels[j]["name"],
                        "min"          : src.channels[j]["min"],
                        "max"          : src.channels[j]["max"],
                        "style"        : src.channels[j]["style"]
                    };

                    if ((src.channels[j].hasOwnProperty("min_time")) &&
                        (src.channels[j].hasOwnProperty("max_time"))) {
                        sourcesMap["src_" + idx]["min_time"]
                            = src.channels[j]["min_time"];
                        sourcesMap["src_" + idx]["max_time"]
                            = src.channels[j]["max_time"];
                    }

                    idx += 1;
                }
            }

            // Render add channels area
            $("#_timeline_addChannelsArea").html($.mustache($("#_timeline_sources_template").html(), {
                sources: SOURCES.availableList
            }));

            // Drag event handler for channels
            $("#_timeline_addChannelsArea ul ._timeline_sources_channel").draggable({
                connectToSortable : "#_timeline_channels",
                revert: "invalid",
                helper: function() {
                    // Create mock grapher widget to show while dragging
                    var src = sourcesMap[this.id];
                    dragSourceId = this.id;
                    var templateValues = {
                        "deviceName"       : src["device_name"],
                        "channelName"      : src["channel_name"],
                        "plotElementId"    : "_timeline_channel_helper",
                        "channelElementId" : "_timeline_plot_helper",
                        "yAxisElementId"   : "_timeline_yAxis_helper"
                    };
                    return $.mustache($("#_timeline_channel_template").html(), templateValues);
                },
                start : function(event, ui) {
                    // Set height to match grapher widget so that hovering
                    // over channels pane shifts existing widgets by correct amount
                    $(this).height("74px");
                },
                stop : function(event, ui) {
                    // Restore height
                    $(this).height("16px");
                }
            });
            $("#_timeline_addChannelsArea ul ._timeline_sources_channel").disableSelection();

            // Create new grapher widget if source receives a click
            $("#_timeline_addChannelsArea ul li ._timeline_sources_channel").click(function() {
                var c = sourcesMap[this.id];
                addChannel(c, null);
                $("#_timeline_channelsWrapper").animate({scrollTop:$("#_timeline_channelsWrapper").prop("scrollHeight")}, 500);
            });

            // Add channels pane reset button
            $("#_timeline_addChannelsArea #_timeline_sources_find_btn").click(function() {
                $("#_timeline_addChannelsArea input[type=text]").val("");

                addPaneRestoreState();
                return false;
            });

            // Add channels pane search functionality
            $("#_timeline_addChannelsArea input[type=text]").keyup(function(event) {
                var search_str = $("#_timeline_addChannelsArea input[type=text]").val();
                var regexp = new RegExp(search_str, 'i');

                if (search_str.length === 0) {
                    addPaneRestoreState();
                    return;
                }

                $("._timeline_sources_name").each(function() {
                    var ul = $(this).parent().find("ul");
                    var arrow = $(this).children("._timeline_sources_name_arrow");
                    if (ul.css("display") === "none") {
                        ul.show();
                        arrow.html("&#9660;");
                    }
                });

                $("#_timeline_addChannelsArea #_timeline_sources_list ._timeline_sources_channel").each(function() {
                    if ($.trim($(this).html()).search(regexp) == -1) {
                        $(this).hide();
                    }
                    else {
                        $(this).show();
                    }
                });
            });

            // Collapsible devices
            $("._timeline_sources_name").click(function() {
                var ul = $(this).parent().find("ul");
                var arrow = $(this).children("._timeline_sources_name_arrow");

                if (ul.css("display") === "none") {
                    ul.show();
                    arrow.html("&#9660;");
                }
                else {
                    ul.hide();
                    arrow.html("&#9658;");
                }

                addPaneSaveState();
            });

            addPaneSaveState();
            if (typeof callback === "function") {
                callback();
            }
        });
    } // getSources

    function addPaneRestoreState() {
        var i = 0;
        var l = addPaneChannelsState.length;

        $("#_timeline_addChannelsArea #_timeline_sources_list ._timeline_sources_channel").each(function() {
            $(this).show();
        });

        if (l > 0) {
            $("._timeline_sources_name").each(function() {
                var ul = $(this).parent().find("ul");
                var arrow = $(this).children("._timeline_sources_name_arrow");
                var state = addPaneChannelsState[i];
                if (state == false) {
                    ul.hide();
                    arrow.html("&#9658;");
                }
                else {
                    ul.show();
                    arrow.html("&#9660;");
                }
                i++;
            });
        }
    }

    function addPaneSaveState() {
        addPaneChannelsState = [];
        $("._timeline_sources_name").each(function() {
            var ul = $(this).parent().find("ul");
            if (ul.css("display") === "none") {
                addPaneChannelsState.push(false);
            }
            else {
                addPaneChannelsState.push(true);
            }
        });
    }

    function newView() {
        var now = new Date();
        now = now.getTime()/1000.0;

        VIEWS.data = {
            "name" : newViewName,
            "v2" : {
                "x_axis" : {
                    "min" : now - 86400.0,
                    "max" : now
                },
                y_axes : []
            }
        };
        console.log("newView is being called");
        loadedViewStr = JSON.stringify(VIEWS.data);
        hasUnsavedChanges = true;
        renderView(VIEWS.data);

        if ($("#_timeline_addChannelsArea").css("display") === "none") {
            toggleAddChannelsPane();
        }
    }

    function loadView(id, mode, callback) {
        $("#_timeline_save_view_btn").addClass("button_disabled").unbind("click");
        VIEWS.load(id, function(data) {
            loadedViewStr = JSON.stringify(data);
            hasUnsavedChanges = false;
            renderView(data, mode);
            if (typeof callback === "function") {
                callback();
            }
        });
    }

    function loadViewWithTimeRange(id, min, max, callback) {
        $("#_timeline_save_view_btn").addClass("button_disabled").unbind("click");
        VIEWS.load(id, function(data) {
            loadedViewStr = JSON.stringify(data);
            hasUnsavedChanges = true;
            data["v2"]["x_axis"]["min"] = min;
            data["v2"]["x_axis"]["max"] = max;
            renderView(data);
            if (typeof callback === "function") {
                callback();
            }
        });
    }

    // Save view then load saved view
    function saveView(name) {
        updateViewData();
        VIEWS.save(name, function(data, id) {
            loadedViewStr = JSON.stringify(VIEWS.data);
            hasUnsavedChanges = false;
            loadView(id);
        });
    }

    // Load all channels associated with device_name into a new view
    function loadSource(device_name, callback) {
        SOURCES.getDefaultGraphSpecs(device_name, function(data) {
            var i, l;

            VIEWS.data = {
                "name" : newViewName,
                "v2" : {
                    "x_axis" : {
                        "min" : data["min_time"],
                        "max" : data["max_time"]
                    },
                    "y_axes" : []
                }
            };

            l = data.channels.length;
            for (i = 0; i < l; i++) {
                VIEWS.data["v2"]["y_axes"].push({
                    "device_name"    : device_name,
                    "channel_name"   : data.channels[i]["name"],
                    "min"            : data.channels[i]["min"],
                    "max"            : data.channels[i]["max"],
                    "style"          : data.channels[i]["style"],
                    "channel_height" : data.channels[i]["channel_height"]
                });
            }

            loadedViewStr = JSON.stringify(VIEWS.data);
            hasUnsavedChanges = true;
            renderView(VIEWS.data);

            if ($("#_timeline_addChannelsArea").css("display") !== "none") {
                toggleAddChannelsPane();
            }

            if (typeof callback === "function") {
                callback();
            }
        });
    }

    function loadViewDialogModeHandler(view_id) {
        var mode = $("._timeline_load_dialog_head_content input:checked").val();
        var min, max;

        // Cancel load if user clicks cancel on load dialog
        if (checkForTimelineChanges() === false) {
            return false;
        }

        // Update x-axis of existing view
        if (mode === "time") {
            loadView(view_id, mode);
            return false;
        }

        // Load new view with existing x-axis range
        else if (mode === "channel") {
            if (VIEWS.data === "") {
                alert("Existing view not found");
                return false;
            }
            min = VIEWS.data["v2"]["x_axis"]["min"];
            max = VIEWS.data["v2"]["x_axis"]["max"];

            loadViewWithTimeRange(view_id, min, max, function() {
                TOOLS.resizeHandler();
            });
            return false;
        }

        // Otherwise, load the entire view
        loadView(view_id);
        return true;
    }

    function toggleAddChannelsPane() {
        var area = $("#_timeline_addChannelsArea");
        if (area.css("display") === "none") {
            $("#_timeline_add_channels_btn").addClass("button_toggle");
            area.show();
            TOOLS.resizeElementHeight($("#_timeline_addChannelsArea #_timeline_sources_list"));
        }
        else {
            $("#_timeline_add_channels_btn").removeClass("button_toggle");
            area.hide();
        }

        // call the resize handler to ensure that the grapher gets resized
        TOOLS.resizeHandler();

        return false;
    }

    function toggleLoadDialog() {
        // Close save dialog first if open
        if ($("#_timeline_save_view_btn").hasClass("button_toggle")) {
            toggleSaveDialog();
        }

        // Load dialog is closed
        if ($("#_timeline_view_dialog").css("display") === "none") {

            $("#_timeline_load_view_btn").addClass("button_toggle");
            $("#_timeline_view_dialog").html($.mustache($("#_timeline_view_load_template").html(), {
                available_views: VIEWS.availableList
            })).show();

            $("#_timeline_view_dialog .close_btn").unbind('click').click(function() {
                toggleLoadDialog();
            });

            $("#_timeline_view_dialog ul li a").unbind('click').click(function() {
                // hide dialog then allow browser to follow link
                $("#_timeline_load_view_btn").removeClass("button_toggle");
                $("#_timeline_view_dialog").hide();
                // extract view number from DOM element ID _timeline_view_{{ id }}
                loadViewDialogModeHandler($(this).attr("id").substr(15));
            });
        }
        // Load dialog is open
        else {
            $("#_timeline_load_view_btn").removeClass("button_toggle");
            $("#_timeline_view_dialog").hide();
        }
        return false;
    }

    function toggleSaveDialog() {
        var viewName;

        // Close load dialog first if open
        if ($("#_timeline_load_view_btn").hasClass("button_toggle")) {
            toggleLoadDialog();
        }

        // Save dialog is closed
        if ($("#_timeline_view_dialog").css("display") === "none") {
            $("#_timeline_save_view_btn").addClass("button_toggle");
            $("#_timeline_view_dialog").html($.mustache($("#_timeline_view_save_template").html(), {
                available_views: VIEWS.availableList
            })).show();

            // Fill in default save name
            viewName = VIEWS.data["name"];
            if (viewName != newViewName) {
                $("#_timeline_view_dialog input").val(VIEWS.data["name"]);
            }

            $("#_timeline_view_dialog input").focus();

            $("#_timeline_view_dialog .close_btn").unbind('click').click(function() {
                toggleSaveDialog();
            });

            $("#_timeline_view_dialog div .button").unbind('click').click(function() {
                var name = $("#_timeline_view_dialog input").val();
                if (name != "") {
                    saveView(name);
                    $("#_timeline_save_view_btn").removeClass("button_toggle");
                    $("#_timeline_view_dialog").hide();
                }
                else {
                    $("._timeline_view_dialog_status").html("Please enter a name for this view");
                }
                return false;
            });

            $("#_timeline_view_dialog ul li a").unbind('click').click(function() {
                $("#_timeline_view_dialog input").val(this.text);
                return false;
            });
        }
        // Save dialog is open
        else {
            $("#_timeline_save_view_btn").removeClass("button_toggle");
            $("#_timeline_view_dialog").hide();
        }
        return false;
    }

    function toggleDetailsPane() {
        var area = $("#_timeline_detailsArea");
        if (area.css("display") === "none") {
            $("#_timeline_show_details_btn").addClass("button_toggle");
            area.show();
        }
        else {
            $("#_timeline_show_details_btn").removeClass("button_toggle");
            area.hide();
        }

        // call the resize handler to ensure that the grapher gets resized
        TOOLS.resizeHandler();

        return false;
    }

    // Add new channel to target
    function addChannel(channel, target) {
        var max_time;

        // VERY important to clone the given channel here!
        channel = TOOLS.clone(channel);

        id = channelIdx;
        channelIdx += 1;

        var channelElementId = "_timeline_channel_" + id;
        var plotElementId = "_timeline_plot_" + id;
        var yAxisElementId = "_timeline_yAxis_" + id;
        var templateValues = {
            "deviceName"       : channel["device_name"],
            "channelName"      : channel["channel_name"],
            "channelHeight"    : channel["channel_height"],
            "plotId"           : id,
            "plotElementId"    : plotElementId,
            "channelElementId" : channelElementId,
            "yAxisElementId"   : yAxisElementId
        };

        // Render template
        var html = $.mustache($("#_timeline_channel_template").html(), templateValues);
        if (target == null || target == undefined || target == "") {
            $("#_timeline_channels").append(html);
        }
        else {
            $(target).replaceWith(html);
        }

        // Make sure the view name doesn't overflow the bounds of the box
        $("#" + channelElementId + "-timeline-channel-name")
            .html(channel["channel_name"])
            .shorten();

        var yAxis = new NumberAxis(yAxisElementId, "vertical", {
            "min" : channel["min"],
            "max" : channel["max"]
        });

        // Now that yAxis is initialized, if this is a new view,
        // set xAxis range to be the latest 24 hrs of data from the
        // first added channel
        if ((VIEWS.data["name"] == newViewName) &&
            channel.hasOwnProperty("max_time") &&
            ($("#_timeline_channels ._timeline_channel").length == 0)) {
            max_time = channel["max_time"];
            dateAxis.setRange(max_time - 86400.0, max_time);
        }

        // TODO: The following should be keying off of "type" rather than "name" fields
        var plot = null;
        if ("photos" == channel["channel_name"]) {
            var tags = [];
            var willJoinUsingAnd = false;
            var photoStyle = channel['style'];
            if (typeof photoStyle !== 'undefined' &&
                typeof photoStyle['filters'] !== 'undefined' &&
                typeof photoStyle['filters']['tag'] !== 'undefined') {

                if (jQuery.isArray(photoStyle['filters']['tag']['tags'])) {
                    tags = photoStyle['filters']['tag']['tags'];
                }
                willJoinUsingAnd = !!photoStyle['filters']['tag']['isAndJoin'];
            }
            plot = new PhotoSeriesPlot(photoDatasource(LOGIN.user_id, channel["device_name"], tags,	willJoinUsingAnd),
                dateAxis,
                yAxis,
                LOGIN.user_id,
                channel["style"]);
            plot.addDataPointListener(photoDataPointListener(channelElementId));
        } else if ("comments" == channel["channel_name"]) {
            var commentStyle = channel['style'];
            if (typeof commentStyle !== 'undefined' &&
                typeof commentStyle['filters'] !== 'undefined' &&
                typeof commentStyle['filters']['tag'] !== 'undefined') {

                if (jQuery.isArray(commentStyle['filters']['tag']['tags'])) {
                    tags = commentStyle['filters']['tag']['tags'];
                }
                willJoinUsingAnd = !!commentStyle['filters']['tag']['isAndJoin'];
            }
            alert("Implement commentDatasource and CommentSeriesPlot");
//			var commentDatasource = commentDatasource(LOGIN.user_id,
//			channel["device_name"],
//			tags,
//			willJoinUsingAnd);
//			plot = new CommentSeriesPlot(commentDatasource,
//			dateAxis,
//			yAxis,
//			LOGIN.user_id,
//			channel["style"]);
//			plot.addDataPointListener(commentDataPointListener(channelElementId));
        } else {
            // Set up the plot and axes for this channel using the grapher API
            plot = new DataSeriesPlot(channelDatasource(LOGIN.user_id, channel["device_name"], channel["channel_name"]),
                dateAxis,
                yAxis,
                channel["style"]);
            plot.addDataPointListener(dataPointListener);
        }

        var plotContainer = new PlotContainer(plotElementId, [plot]);

        channelsMap[channelElementId] = channel;
        plotsMap[channelElementId] = plot;
        plotContainersMap[channelElementId] = plotContainer;
        plotContainers.push(plotContainer);

        // Gear button
        $("._timeline_btnGear").unbind("click").click(function() {
            var channelConfigElement = $(this).parents("._timeline_channel")
                .children("._timeline_channelConfig");

            channelConfigElement.toggle();

            if (channelConfigElement.css("display") === "none") {
                $(this).find("img").attr("src", "/static/images/gear_b.png");
            }
            else {
                $(this).find("img").attr("src", "/static/images/gear_green.png");
            }
        });

        // Delete buton
        $("#" + channelElementId + "_delete_btn")
            .unbind('click')
            .click(function() {
                var channelElement = $(this).parents("._timeline_channel").parent();
                plotContainer.removePlot(plot);
                $(channelElement).remove();
            });

        // Drag to resize
        $("#" + channelElementId + "_dragArea").unbind("mousedown").mousedown(function() {
        	var channelElement = $(this).parents("._timeline_channel").parent();
        	// Extract plotId from channelElement id attribute
            dragAreaOnMouseDown(channelElement.attr("id").slice(18));
        });

        // Style configuration
        if (plot instanceof DataSeriesPlot) {

            // Define a function which handles updating a channel's style
            // whenever anything in the channel configuration changes
            var updateDataSeriesPlotChannelConfig = function() {
                var channelElement = $(this).parents("._timeline_channel").parent();
                var plot = plotsMap[channelElement.attr("id")];

                var newStyle = plot.getStyle();

                newStyle['styles'] = [];                // completely overwrite the existing styles array
                newStyle['highlight'] = {};             // completely overwrite the existing highlight object
                newStyle['highlight']['styles'] = [];   // initialize the highlight styles array

                var isZeo = $("#" + channelElementId + "-config-zeo-show").is(':checked');
                var highlightLineWidth = 0;

                if (isZeo) {
                    newStyle['styles'][newStyle['styles'].length] = {
                        "type" : "zeo",
                        "show" : true
                    };
                    highlightLineWidth = 1;
                } else {
                    var linesStyle = {
                        "type"      : "line",
                        "show"      : $("#" + channelElementId + "-config-lines-show").is(':checked'),
                        "color"     : $("#" + channelElementId + "-config-lines-color").next(".color_picker").css("background-color"),
                        "lineWidth" : TOOLS.parseInt($("#" + channelElementId + "-config-lines-lineWidth").val(), 1)
                    };

                    var pointsStyleType = $("#" + channelElementId + "-config-points-type").val();
                    var pointsStyleFill = pointsStyleType.match(/-filled$/) !== null;
                    var pointsStyle = {
                        "type"      : pointsStyleType.replace('-filled', ''),
                        "show"      : $("#" + channelElementId + "-config-points-show").is(':checked'),
                        "lineWidth" : 1,
                        "radius"    : TOOLS.parseInt($("#" + channelElementId + "-config-points-radius").val(), 2),
                        "color"     : $("#" + channelElementId + "-config-points-color").next(".color_picker").css("background-color"),
                        "fill"      : pointsStyleFill,
                        "fillColor" : $("#" + channelElementId + "-config-points-fillColor").next(".color_picker").css("background-color")
                    };

                    var barsStyle = {
                        "type"      : "lollipop",
                        "show"      : $("#" + channelElementId + "-config-bars-show").is(':checked'),
                        "lineWidth" : TOOLS.parseInt($("#" + channelElementId + "-config-bars-lineWidth").val(), 1),
                        "radius"    : 0,
                        "color"     : $("#" + channelElementId + "-config-bars-color").next(".color_picker").css("background-color"),
                        "fill"      : false
                    };

                    // Add the styles to the array--note that ordering here isn't arbitrary.  Styles are rendered in the order
                    // they appear in the array.  Thus, we put points AFTER lines and bars so that the point gets rendered on top.
                    // Value is placed last so that it's on top of everything.
                    newStyle['styles'][newStyle['styles'].length] = linesStyle;
                    newStyle['styles'][newStyle['styles'].length] = barsStyle;
                    newStyle['styles'][newStyle['styles'].length] = pointsStyle;

                    // Compute the lineWidth value for highlights by taking the max line width from the lines and bars
                    // styles (but only if they're visible) and incrementing by 1.
                    if (linesStyle['show']) {
                        highlightLineWidth = Math.max(highlightLineWidth, linesStyle['lineWidth']);
                    }
                    if (barsStyle['show']) {
                        highlightLineWidth = Math.max(highlightLineWidth, barsStyle['lineWidth']);
                    }
                    highlightLineWidth += 1;
                }

                // build the values style (we decide next where to stick it)
                var valuesStyle = {
                    "type"           : "value",
                    "show"           : $("#" + channelElementId + "-config-values-show").is(':checked'),
                    "fillColor"      : $("#" + channelElementId + "-config-values-fillColor").next(".color_picker").css("background-color"),
                    "marginWidth"    : TOOLS.parseInt($("#" + channelElementId + "-config-values-marginWidth").val(), 5),
                    "verticalOffset" : TOOLS.parseInt($("#" + channelElementId + "-config-values-verticalOffset").val(), 7),
                    "numberFormat"   : $("#" + channelElementId + "-config-values-numberFormat").val()
                };

                // We'll always put the values style in both the styles array AND the highlight styles array.  The "show"
                // field will be false for both if Values option is unchecked.  The "show" field will be true for both if the
                // Values option is checked and the showOnlyOnHighlight option is false.  If the showOnlyOnHighlight option is
                // true, then the instance in the styles array will have show set to false
                newStyle['highlight']['styles'][newStyle['highlight']['styles'].length] = valuesStyle;
                var onlyShowValuesOnHighlight = $("#" + channelElementId + "-config-values-showOnlyOnHighlight").val() === 'true';
                if (onlyShowValuesOnHighlight) {
                    // clone the valuesStyle instance
                    var valuesStyleCopy = TOOLS.clone(valuesStyle);
                    valuesStyleCopy["show"] = false;
                    newStyle['styles'][newStyle['styles'].length] = valuesStyleCopy;
                } else {
                    newStyle['styles'][newStyle['styles'].length] = valuesStyle;
                }

                // set the highlight line width
                newStyle['highlight']['lineWidth'] = highlightLineWidth;

                // Finally, build the comments style (this completely overwrites the existing comments object)
                var commentsStyleType = $("#" + channelElementId + "-config-comments-type").val();
                var commentsStyleFill = commentsStyleType.match(/-filled$/) !== null;
                newStyle['comments'] = {
                    "show"           : $("#" + channelElementId + "-config-comments-show").is(':checked'),
                    "styles"         : [{
                        "type"      : commentsStyleType.replace('-filled', ''),
                        "show"      : $("#" + channelElementId + "-config-comments-show").is(':checked'),
                        "lineWidth" : 1,
                        "radius"    : TOOLS.parseInt($("#" + channelElementId + "-config-comments-radius").val(), 3),
                        "color"     : $("#" + channelElementId + "-config-comments-color").next(".color_picker").css("background-color"),
                        "fill"      : commentsStyleFill,
                        "fillColor" : $("#" + channelElementId + "-config-comments-fillColor").next(".color_picker").css("background-color")
                    }],
                    "verticalMargin" : 4
                };

                plot.setStyle(newStyle);
            };

            // Show DataSeriesPlot config
            $("#" + channelElementId + " ._timeline_data_series_plot_config").show();

            // First, record whether this is a Zeo plot
            var isZeo = channel["channel_name"] == "Sleep_Graph";

            // Check for styles array
            if (!channel["style"].hasOwnProperty("styles")) {
                channel["style"]["styles"] = [];
            }
            // Check for highlight object
            if (!channel["style"].hasOwnProperty("highlight")) {
                channel["style"]["highlight"] = {};
            }
            // Check for highlight styles array
            if (!channel["style"]["highlight"].hasOwnProperty("styles")) {
                channel["style"]["highlight"]["styles"] = [];
            }
            // Check for comments object
            if (!channel["style"].hasOwnProperty("comments")) {
                channel["style"]["comments"] = {};
            }
            // Check for comments styles array
            if (!channel["style"]["comments"].hasOwnProperty("styles")) {
                channel["style"]["comments"]["styles"] = [];
            }

            // get the next default color
            var defaultColor = "#" + jQuery.fn.colorPicker.getNextColor();

            // Load up the existing styles (if any) from the styles array
            var linesStyle = {"type" : "line", "show" : false, "lineWidth" : 1, "color" : defaultColor};
            var pointsStyle = {"type" : "point", "show" : false, "radius" : 2, "fill" : true, "color" : defaultColor, "fillColor" : defaultColor};
            var barsStyle = {"type" : "lollipop", "show" : false, "color" : defaultColor};
            var valuesStyle1 = {"type" : "value", "show" : false, "fillColor" : defaultColor};
            var valuesStyle2 = {"type" : "value", "show" : false, "fillColor" : defaultColor};
            var commentsStyle = {"type" : "point", "show" : true, "radius" : 3, "fill" : true, "color" : defaultColor, "fillColor" : defaultColor};

            for (var styleTypeIndex = 0; styleTypeIndex < channel["style"]["styles"].length; styleTypeIndex++) {
                var theStyle = channel["style"]["styles"][styleTypeIndex];
                if (typeof theStyle["type"] !== 'undefined') {
                    if (theStyle["type"] == "line") {
                        linesStyle = theStyle;
                    } else if (theStyle["type"] == "point" ||
                        theStyle["type"] == "square" ||
                        theStyle["type"] == "cross" ||
                        theStyle["type"] == "plus") {
                        // fill defaults to true if unspecified
                        if (typeof theStyle["fill"] === 'undefined') {
                            theStyle["fill"] = true;
                        }
                        pointsStyle = theStyle;
                    } else if (theStyle["type"] == "lollipop") {
                        // fill defaults to true if unspecified
                        if (typeof theStyle["fill"] === 'undefined') {
                            theStyle["fill"] = true;
                        }
                        barsStyle = theStyle;
                    } else if (theStyle["type"] == "value") {
                        valuesStyle1 = theStyle;
                    }

                    // show defaults to true if unspecified
                    if (typeof theStyle["show"] === 'undefined') {
                        theStyle["show"] = true;
                    }
                }
            }

            // build the type-ui field
            pointsStyle['type-ui'] = pointsStyle['type'];
            if (pointsStyle['fill'] && (pointsStyle['type'] == 'point' || pointsStyle['type'] == 'square')) {
                pointsStyle['type-ui'] += '-filled';
            }

            // Load up the existing styles (if any) from the highlight styles array--we currently only support the values style
            for (var highlightStyleTypeIndex = 0; highlightStyleTypeIndex < channel["style"]["highlight"]["styles"].length; highlightStyleTypeIndex++) {
                var theHighlightStyle = channel["style"]["highlight"]["styles"][highlightStyleTypeIndex];
                if (theHighlightStyle["type"] == "value") {
                    valuesStyle2 = theHighlightStyle;
                }

                // show defaults to true if unspecified
                if (typeof theHighlightStyle["show"] === 'undefined') {
                    theHighlightStyle["show"] = true;
                }
            }

            // now merge valuesStyle1 and valuesStyle2 (they should be identical, except for the "show" field)
            var valuesStyle = jQuery.extend(true, {}, valuesStyle1, valuesStyle2);
            valuesStyle["show"] = valuesStyle1["show"] || valuesStyle2["show"];

            // map the verticalOffset in valuesStyle to one of the three options we support.
            valuesStyle["verticalOffset"] = TOOLS.parseInt(valuesStyle["verticalOffset"], 7);
            if (valuesStyle["verticalOffset"] > -3) {
                valuesStyle["verticalOffset"] = 7;
            } else if (valuesStyle["verticalOffset"] < -3) {
                valuesStyle["verticalOffset"] = -13;
            }

            // determine whether values should always be shown, or only on highlight (mouseover).  Note that the
            // concatenation here ensures that it's a string, which is required for when we set the selected index
            // of the select menu below.
            var showValuesOnlyOnHighlight = "" + (!valuesStyle1["show"] && valuesStyle2["show"]);

            // Load up the existing styles (if any) from the comments styles array--we currently only support a single point style
            for (var commentsStyleTypeIndex = 0; commentsStyleTypeIndex < channel["style"]["comments"]["styles"].length; commentsStyleTypeIndex++) {
                var theCommentsStyle = channel["style"]["comments"]["styles"][commentsStyleTypeIndex];
                if (theCommentsStyle["type"] == "point" ||
                    theCommentsStyle["type"] == "square" ||
                    theCommentsStyle["type"] == "cross" ||
                    theCommentsStyle["type"] == "plus") {
                    commentsStyle = theCommentsStyle;
                }

                // show defaults to true if unspecified
                if (typeof commentsStyle["show"] === 'undefined') {
                    commentsStyle["show"] = true;
                }
            }

            // build the type-ui field
            commentsStyle['type-ui'] = commentsStyle['type'];
            if (commentsStyle['fill'] && (commentsStyle['type'] == 'point' || commentsStyle['type'] == 'square')) {
                commentsStyle['type-ui'] += '-filled';
            }

            /* add event handler for the Save As Default Style link --------------------------------------------------- */
            $("#" + channelElementId + "-save-default-style > a").click(function() {
                $("#" + channelElementId + "-save-default-style").hide();
                $("#" + channelElementId + "-save-default-style-status").html("Saving...").show();
                saveDefaultChannelStyle(channel, plot.getStyle(), {
                    success : function() {
                        getSources();
                        $("#" + channelElementId + "-save-default-style-status").html("Default style saved.").delay(1000).fadeOut(1000,
                            function() {
                                $("#" + channelElementId + "-save-default-style").show();
                            }
                        );
                    },
                    error:function(textStatus, errorThrown){
                        console.log("saveDefaultChannelStyle(): Failed due to ["+textStatus+"].  Error thrown: " + errorThrown);
                        $("#" + channelElementId + "-save-default-style-status").html("Failed to save default style.").delay(1000).fadeOut(1000,
                            function() {
                                $("#" + channelElementId + "-save-default-style").show();
                            });
                    }
                });
            });

            /* add event handler for the Show all Y range link */
            $("#" + channelElementId + " ._timeline_btnShowAllY").click(function() {
                var plot = plotsMap[channelElementId];
                if (!(plot && !!plot.getStatistics)) {
                    // Photo plots don't have a getStatistics method
                    return false;
                }

                var xAxis = plot.getHorizontalAxis();
                var yAxis = plot.getVerticalAxis();
                var xMin = xAxis.getMin();
                var xMax = xAxis.getMax();

                var afterload = function(stats) {
                    if (stats["has_data"]) {
                        var yMin = stats["y_min"];
                        var yMax = stats["y_max"];
                        var yDiff = yMax - yMin;
                        if(yDiff < 1e-10) {
                            yAxis.setRange(yMin - 0.5, yMin + 0.5);
                        } else {
                            var padding = 0.1 * yDiff;
                            yAxis.setRange(yMin - padding, yMax + padding);
                        }
                        plot.setStyle(plot.getStyle()); // Trigger a repaint
                    }
                };

                var initialStats = plot.getStatistics(xMin, xMax,
                    ["has_data", "y_min", "y_max"],
                    afterload);
                if (!("data_pending" in initialStats)
                    || (!initialStats["data_pending"])) {
                    afterload(initialStats);
                }
                // Else the getStatistics method will call afterload when
                // the data arrives

                return false;
            });

            /* Configure the Zeo options ------------------------------------------------------------------------------ */
            $("#" + channelElementId + "-config-zeo-show").prop("checked", isZeo);

            /* Configure the Color Override options ------------------------------------------------------------------- */
            $("#" + channelElementId + "-config-color-override-color").colorPicker();
            $("#" + channelElementId + "-config-color-override-color").val("#000000"); // we always want this one to start out black
            $("#" + channelElementId + "-config-color-override-color").change();
            $("#" + channelElementId + "-config-color-override-color").change(function() {
                var overrideColor = $("#" + channelElementId + "-config-color-override-color").next(".color_picker").css("background-color");
                $("#" + channelElementId + "-config-lines-color").val(overrideColor).change();
                $("#" + channelElementId + "-config-points-color").val(overrideColor).change();
                $("#" + channelElementId + "-config-points-fillColor").val(overrideColor).change();
                $("#" + channelElementId + "-config-bars-color").val(overrideColor).change();
                $("#" + channelElementId + "-config-values-fillColor").val(overrideColor).change();
                $("#" + channelElementId + "-config-comments-color").val(overrideColor).change();
                $("#" + channelElementId + "-config-comments-fillColor").val(overrideColor).change();
            });

            /* Configure the Lines options ---------------------------------------------------------------------------- */

            // don't show this section if this is a Zeo plot
            $("#" + channelElementId + "-config-lines").toggle(!isZeo);

            // Set the initial value of the show checkbox
            $("#" + channelElementId + "-config-lines-show").prop("checked", linesStyle["show"] && !isZeo);
            $("#" + channelElementId + "-config-lines-show").change(updateDataSeriesPlotChannelConfig);

            // Set the initial value of the lineWidth select menu
            $("#" + channelElementId + "-config-lines-lineWidth").val(TOOLS.parseInt(linesStyle["lineWidth"], 1));
            $("#" + channelElementId + "-config-lines-lineWidth").change(updateDataSeriesPlotChannelConfig);
            $("#" + channelElementId + "-config-lines-lineWidth").msDropDown();

            // Create the color colorpicker, and set its initial value
            $("#" + channelElementId + "-config-lines-color").colorPicker();
            $("#" + channelElementId + "-config-lines-color").val(typeof linesStyle["color"] === 'undefined' ? defaultColor : linesStyle["color"]);
            $("#" + channelElementId + "-config-lines-color").change();
            $("#" + channelElementId + "-config-lines-color").change(updateDataSeriesPlotChannelConfig);

            /* Configure the Points options --------------------------------------------------------------------------- */

            // don't show this section if this is a Zeo plot
            $("#" + channelElementId + "-config-points").toggle(!isZeo);

            // Set the initial value of the show checkbox
            $("#" + channelElementId + "-config-points-show").prop("checked", pointsStyle["show"] && !isZeo);
            $("#" + channelElementId + "-config-points-show").change(updateDataSeriesPlotChannelConfig);

            // Set the initial value of the type select menu and the initial state of the fillColor color picker
            $("#" + channelElementId + "-config-points-type").val(pointsStyle['type-ui']);
            $("#" + channelElementId + "-config-points-type").change(updateDataSeriesPlotChannelConfig);
            $("#" + channelElementId + "-config-points-type").change(function() {
                var isFilledType = $("#" + channelElementId + "-config-points-type").val().match(/-filled$/) !== null;
                $("#" + channelElementId + "-config-points-fillColor-container").toggle(isFilledType);
            });
            $("#" + channelElementId + "-config-points-type").msDropDown();
            $("#" + channelElementId + "-config-points-fillColor-container").toggle(pointsStyle['fill']);

            // Set the initial value of the radius select menu
            $("#" + channelElementId + "-config-points-radius").val(TOOLS.parseInt(pointsStyle["radius"], 2));
            $("#" + channelElementId + "-config-points-radius").change(updateDataSeriesPlotChannelConfig);
            $("#" + channelElementId + "-config-points-radius").msDropDown();

            // Create the color colorpicker, and set its initial value
            $("#" + channelElementId + "-config-points-color").colorPicker();
            $("#" + channelElementId + "-config-points-color").val(typeof pointsStyle["color"] === 'undefined' ? defaultColor : pointsStyle["color"]);
            $("#" + channelElementId + "-config-points-color").change();
            $("#" + channelElementId + "-config-points-color").change(updateDataSeriesPlotChannelConfig);

            // Create the fillColor colorpicker, and set its initial value
            $("#" + channelElementId + "-config-points-fillColor").colorPicker();
            $("#" + channelElementId + "-config-points-fillColor").val(typeof pointsStyle["fillColor"] === 'undefined' ? defaultColor : pointsStyle["fillColor"]);
            $("#" + channelElementId + "-config-points-fillColor").change();
            $("#" + channelElementId + "-config-points-fillColor").change(updateDataSeriesPlotChannelConfig);

            /* Configure the Bars options ----------------------------------------------------------------------------- */

            // don't show this section if this is a Zeo plot
            $("#" + channelElementId + "-config-bars").toggle(!isZeo);

            // Set the initial value of the show checkbox
            $("#" + channelElementId + "-config-bars-show").prop("checked", barsStyle["show"] && !isZeo);
            $("#" + channelElementId + "-config-bars-show").change(updateDataSeriesPlotChannelConfig);

            // Set the initial value of the lineWidth select menu
            $("#" + channelElementId + "-config-bars-lineWidth").val(TOOLS.parseInt(barsStyle["lineWidth"], 1));
            $("#" + channelElementId + "-config-bars-lineWidth").change(updateDataSeriesPlotChannelConfig);
            $("#" + channelElementId + "-config-bars-lineWidth").msDropDown();

            // Create the color colorpicker, and set its initial value
            $("#" + channelElementId + "-config-bars-color").colorPicker();
            $("#" + channelElementId + "-config-bars-color").val(typeof barsStyle["color"] === 'undefined' ? defaultColor : barsStyle["color"]);
            $("#" + channelElementId + "-config-bars-color").change();
            $("#" + channelElementId + "-config-bars-color").change(updateDataSeriesPlotChannelConfig);

            /* Configure the Values options --------------------------------------------------------------------------- */

            // Set the initial value of the show checkbox
            $("#" + channelElementId + "-config-values-show").prop("checked", valuesStyle["show"]);
            $("#" + channelElementId + "-config-values-show").change(updateDataSeriesPlotChannelConfig);

            // Create the fillColor colorpicker, and set its initial value
            $("#" + channelElementId + "-config-values-fillColor").colorPicker();
            $("#" + channelElementId + "-config-values-fillColor").val(typeof valuesStyle["fillColor"] === 'undefined' ? defaultColor : valuesStyle["fillColor"]);
            $("#" + channelElementId + "-config-values-fillColor").change();
            $("#" + channelElementId + "-config-values-fillColor").change(updateDataSeriesPlotChannelConfig);

            // Set the initial value of the numberFormat select menu
            $("#" + channelElementId + "-config-values-numberFormat").val(typeof valuesStyle["numberFormat"] === 'undefined' ? "###,##0.0##" : valuesStyle["numberFormat"]);
            $("#" + channelElementId + "-config-values-numberFormat").change(updateDataSeriesPlotChannelConfig);
            $("#" + channelElementId + "-config-values-numberFormat").msDropDown();

            // Set the initial value of the verticalOffset select menu
            $("#" + channelElementId + "-config-values-verticalOffset").val(TOOLS.parseInt(valuesStyle["verticalOffset"], 7));
            $("#" + channelElementId + "-config-values-verticalOffset").change(updateDataSeriesPlotChannelConfig);
            $("#" + channelElementId + "-config-values-verticalOffset").msDropDown();

            // Set the initial value of the showOnlyOnHighlight select menu and the initial visibility of the marginWidth select menu
            $("#" + channelElementId + "-config-values-showOnlyOnHighlight").val(showValuesOnlyOnHighlight);
            $("#" + channelElementId + "-config-values-showOnlyOnHighlight").change(updateDataSeriesPlotChannelConfig);
            $("#" + channelElementId + "-config-values-showOnlyOnHighlight").change(function() {
                var shouldShowMarginMenu = $("#" + channelElementId + "-config-values-showOnlyOnHighlight").val() == 'false';
                $("#" + channelElementId + "-config-values-marginWidth-label-container").toggle(shouldShowMarginMenu);
                $("#" + channelElementId + "-config-values-marginWidth-container").toggle(shouldShowMarginMenu);
            });
            $("#" + channelElementId + "-config-values-showOnlyOnHighlight").msDropDown();
            var showValuesOnlyOnHighlightBoolean = showValuesOnlyOnHighlight == 'true';
            $("#" + channelElementId + "-config-values-marginWidth-label-container").toggle(!showValuesOnlyOnHighlightBoolean);
            $("#" + channelElementId + "-config-values-marginWidth-container").toggle(!showValuesOnlyOnHighlightBoolean);

            // Set the initial value of the marginWidth select menu
            $("#" + channelElementId + "-config-values-marginWidth").val(TOOLS.parseInt(valuesStyle["marginWidth"], 5));
            $("#" + channelElementId + "-config-values-marginWidth").change(updateDataSeriesPlotChannelConfig);
            $("#" + channelElementId + "-config-values-marginWidth").msDropDown();

            /* Configure the Comments options ------------------------------------------------------------------------- */

            // Set the initial value of the show checkbox
            $("#" + channelElementId + "-config-comments-show").prop("checked", commentsStyle["show"]);
            $("#" + channelElementId + "-config-comments-show").change(updateDataSeriesPlotChannelConfig);

            // Set the initial value of the type select menu and the initial state of the fillColor color picker
            $("#" + channelElementId + "-config-comments-type").val(commentsStyle['type-ui']);
            $("#" + channelElementId + "-config-comments-type").change(updateDataSeriesPlotChannelConfig);
            $("#" + channelElementId + "-config-comments-type").change(function() {
                var isFilledType = $("#" + channelElementId + "-config-comments-type").val().match(/-filled$/) !== null;
                $("#" + channelElementId + "-config-comments-fillColor-container").toggle(isFilledType);
            });
            $("#" + channelElementId + "-config-comments-type").msDropDown();
            $("#" + channelElementId + "-config-comments-fillColor-container").toggle(commentsStyle['fill']);

            // Set the initial value of the radius select menu
            $("#" + channelElementId + "-config-comments-radius").val(TOOLS.parseInt(commentsStyle["radius"], 3));
            $("#" + channelElementId + "-config-comments-radius").change(updateDataSeriesPlotChannelConfig);
            $("#" + channelElementId + "-config-comments-radius").msDropDown();

            // Create the color colorpicker, and set its initial value
            $("#" + channelElementId + "-config-comments-color").colorPicker();
            $("#" + channelElementId + "-config-comments-color").val(typeof commentsStyle["color"] === 'undefined' ? defaultColor : commentsStyle["color"]);
            $("#" + channelElementId + "-config-comments-color").change();
            $("#" + channelElementId + "-config-comments-color").change(updateDataSeriesPlotChannelConfig);

            // Create the fillColor colorpicker, and set its initial value
            $("#" + channelElementId + "-config-comments-fillColor").colorPicker();
            $("#" + channelElementId + "-config-comments-fillColor").val(typeof commentsStyle["fillColor"] === 'undefined' ? defaultColor : commentsStyle["fillColor"]);
            $("#" + channelElementId + "-config-comments-fillColor").change();
            $("#" + channelElementId + "-config-comments-fillColor").change(updateDataSeriesPlotChannelConfig);

            // Finally, trigger a call updateDataSeriesPlotChannelConfig() so that the grapher properly represents the config settings
            $("#" + channelElementId + "-config-comments-fillColor").change();
        } else if (plot instanceof PhotoSeriesPlot) {

            // returns the array of tags already selected for this photo
            var getUserSelectedTags = function() {
                var tags = [];
                $.each($("#" + channelElementId + "-photo-tags-filter .tagedit-listelement-old input"),
                    function(index, inputElement) {
                        var val = inputElement['value'];
                        if (typeof val === 'string' && val != '') {
                            tags[tags.length] = val;
                        }
                    }
                );
                return tags;
            };

            var updatePhotoSeriesPlotChannelConfig = function() {
                var channelElement = $(this).parents("._timeline_channel").parent();
                var plot = plotsMap[channelElement.attr("id")];
                var newStyle = plot.getStyle();
                //console.log("----------------------------------------\nOLD JSON: " + JSON.stringify(newStyle,null,3));

                // completely overwrite the existing tag filters object
                if (typeof newStyle['filters'] === 'undefined') {
                    newStyle['filters'] = {};
                }

                var isAndJoin = $("#" + channelElementId + "-photo-tags-isAndJoin").val() === 'true';
                var userSelectedTags = getUserSelectedTags();
                newStyle['filters']["tag"] = {
                    "tags" : userSelectedTags,
                    "isAndJoin" : isAndJoin
                };

                // Display the filter settings in the channel tab
                if (userSelectedTags.length > 0) {
                    var filterHtml = $.mustache($("#_timeline_channel_tab_filter_template").html(), {"value":userSelectedTags.join(", ")});
                    $("#" + channelElementId + "-timeline-channel-filter").html(filterHtml).shorten();
                } else {
                    $("#" + channelElementId + "-timeline-channel-filter").text('').hide();
                }

                //console.log("NEW JSON: " + JSON.stringify(newStyle,null,3));

                plot.setStyle(newStyle);

                plot.setDatasource(photoDatasource(LOGIN.user_id,
                    channel["device_name"],
                    newStyle['filters']["tag"]["tags"],
                    newStyle['filters']["tag"]["isAndJoin"]
                ));
            };

            // Check for filters object
            if (!channel["style"].hasOwnProperty("filters")) {
                channel["style"]["filters"] = {};
            }
            // Check for filters.tag object
            if (!channel["style"]["filters"].hasOwnProperty("tag")) {
                channel["style"]["filters"]["tag"] = {};
            }
            // Check for filters.tag.tags array
            if (!channel["style"]["filters"]["tag"].hasOwnProperty("tags")) {
                channel["style"]["filters"]["tag"]["tags"] = [];
            }
            // Check for filters.tag.isAndJoin property
            if (!channel["style"]["filters"]["tag"].hasOwnProperty("isAndJoin")) {
                channel["style"]["filters"]["tag"]["isAndJoin"] = false;  // default to joining with OR
            }

            // Load up the existing tag filter (if any)
            var tagFilter = channel["style"]["filters"]["tag"];

            // Set the initial value of the isAndJoin select menu
            $("#" + channelElementId + "-photo-tags-isAndJoin").val("" + tagFilter["isAndJoin"]);
            $("#" + channelElementId + "-photo-tags-isAndJoin").change(updatePhotoSeriesPlotChannelConfig);

            // seed the tag filter editor with the tags currently saved in the channel (if any)
            if (tagFilter['tags'].length > 0) {
                $.each(tagFilter['tags'], function(index, value) {
                    var tagHtml = $.mustache($("#_timeline_photo_dialog_tags_editor_tag_template").html(), {"value" : value});
                    $("#" + channelElementId + "-photo-tags-filter").append(tagHtml);
                });
            } else {
                var tagHtml = $.mustache($("#_timeline_photo_dialog_tags_editor_tag_template").html(), {"value" : ""});
                $("#" + channelElementId + "-photo-tags-filter").append(tagHtml);
            }

            // construct the tag filter editor
            var tagFilterOptions = {
                autocompleteOptions : {
                    "minLength" : 0, // TODO: make this 1 or 2 if the list of tags is huge
                    "delay"     : 0,
                    "autoFocus" : false,
                    source      : function(request, response) {
                        var tagsToExclude = getUserSelectedTags();
                        var cachedTagsData = TAG_MANAGER.getCachedTagsForTagEditor(tagsToExclude);
                        return response($.ui.autocomplete.filter(cachedTagsData, request.term));
                    }
                },
                // return, comma, space, period, semicolon
                breakKeyCodes       : [ 13, 44, 32, 59 ],
                additionalListClass : '_timeline_photo_tags_filter',
                animSpeed           : 100,
                allowAdd            : false,
                allowEdit           : false,
                allowDelete         : false,
                texts               : {
                    removeLinkTitle    : 'Remove this tag from the list',
                    saveEditLinkTitle  : 'Save changes',
                    breakEditLinkTitle : 'Undo changes'
                }
            };
            $("#" + channelElementId + "-photo-tags-filter input.tag").tagedit(tagFilterOptions);
            $("#" + channelElementId + "-photo-tags-filter").bind('tagsChanged', updatePhotoSeriesPlotChannelConfig);
            //$("#" + channelElementId + "-photo-tags-filter").bind('tagAdded', function(){console.log('tagAdded')});
            //$("#" + channelElementId + "-photo-tags-filter").bind('tagEdited', function(){console.log('tagEdited')});
            //$("#" + channelElementId + "-photo-tags-filter").bind('tagRemoved', function(){console.log('tagRemoved')});

            // Show PhotoSeriesPlot config
            $("#" + channelElementId + " ._timeline_photo_series_plot_config").show();

            // Finally, trigger a call updatePhotoSeriesPlotChannelConfig() so that the grapher properly represents the config settings
            $("#" + channelElementId + "-photo-tags-isAndJoin").change();
        }

        // Update scroll area
        TOOLS.resizeHandler();

        return html;
    }

    // Fetch ordering and latest values from channelsMap and
    // update VIEWS.data
    function updateViewData() {
        var i, l;
        var channelIds;
        var xAxis, yAxis, plot;
        var channel, yAxes = [];

        channelIds = $("#_timeline_channels").sortable('toArray');

        l = channelIds.length;

        // Update xAxis min/max
        if (l > 0) {
            plot = plotsMap[channelIds[0]];
            xAxis = plot.getHorizontalAxis();
            VIEWS.data["v2"]["x_axis"]["min"] = xAxis.getMin();
            VIEWS.data["v2"]["x_axis"]["max"] = xAxis.getMax();
        }

        // Update yAxis min/max, order, height
        for (i = 0; i < l; i++) {
            plot = plotsMap[channelIds[i]];
            yAxis = plot.getVerticalAxis();
            channel = channelsMap[channelIds[i]];
            if (plot instanceof DataSeriesPlot || plot instanceof PhotoSeriesPlot) {
                channel["style"] = plot.getStyle();
                channel["channel_height"] = $("#" + yAxis.getPlaceholder()).height();
            }
            channel["min"] = yAxis.getMin();
            channel["max"] = yAxis.getMax();

            yAxes.push(channel);
        }
        VIEWS.data["v2"]["y_axes"] = yAxes;

        // Update status of add channels pane
        if ($("#_timeline_addChannelsArea").css("display") === "none") {
            VIEWS.data["v2"]["show_add_pane"] = false;
        }
        else {
            VIEWS.data["v2"]["show_add_pane"] = true;
        }
    }

    // Render view to page
    function renderView(view, mode) {
        console.log("renderView is being called");
        var yAxes, i, l;
        var channel;
        mode = mode || "both";

        if ((typeof view === 'undefined') ||
            (view == null) ||
            (!validateView(view))) {
            alert("Invalid view: " + ((view == null) ? null : view["name"]));
            return;
        }

        // Update existing view with new x-axis range only
        if (mode === "time") {
            if (typeof dateAxis === "undefined") {
                alert("Existing view not found");
                return;
            }
            $("#_timeline_save_view_btn").unbind('click')
                .click(toggleSaveDialog)
                .removeClass("button_disabled");
            $("#_timeline_add_channels_btn").unbind('click')
                .click(toggleAddChannelsPane)
                .removeClass("button_disabled");
            $("#_timeline_show_details_btn").unbind('click')
                .click(toggleDetailsPane)
                .removeClass("button_disabled");

            dateAxis.setRange(view["v2"]["x_axis"]["min"],
                view["v2"]["x_axis"]["max"]);
            return;
        }
        else {
            // Reset view specific data
            channelsMap = {};
            plotsMap = {};
            plotContainersMap = {};
            plotContainers = [];

            // Reset colorpicker color cycling
            jQuery.fn.colorPicker.resetGetNextColor();

            // Clear the channels area
            $("#_timeline_dateAxis").empty();
            $('#_timeline_channels').empty();
            $('#_timeline_dateAxisAndChannelsArea').show();
            $('#_timeline_channelsArea').show();

            // Set view name and add click handlers for various buttons
            $("#_timeline_viewName").html(view["name"]).shorten();
            // TODO: only enable this when the view has changed
            $("#_timeline_save_view_btn").unbind('click')
                .click(toggleSaveDialog)
                .removeClass("button_disabled");
            $("#_timeline_add_channels_btn").unbind('click')
                .click(toggleAddChannelsPane)
                .removeClass("button_disabled");
            $("#_timeline_show_details_btn").unbind('click')
                .click(toggleDetailsPane)
                .removeClass("button_disabled");

            // Show/hide add channels pane
            if ((typeof view["v2"]["show_add_pane"] === "undefined") ||
                (view["v2"]["show_add_pane"] === false)) {
                if ($("#_timeline_addChannelsArea").css("display") !== "none") {
                    toggleAddChannelsPane();
                }
            }
            else {
                if ($("#_timeline_addChannelsArea").css("display") === "none") {
                    toggleAddChannelsPane();
                }
            }

            // Create x-axis
            dateAxis = new DateAxis("_timeline_dateAxis", "horizontal", {
                "min" : view["v2"]["x_axis"]["min"],
                "max" : view["v2"]["x_axis"]["max"]
            });
            var prevDateString = null;
            dateAxis.addAxisChangeListener(function() {
                var center = (dateAxis.getMin() + dateAxis.getMax()) / 2.0;
                var date = new Date(center * 1000);
                var dateChangeBuffer = 24 * 3600 * 1000 / 12;
                var dateEarly = new Date(center * 1000 - dateChangeBuffer);
                var dateLater = new Date(center * 1000 + dateChangeBuffer);
                var dateString = date.getFullYear() + "-" + (date.getMonth() + 1) + "-" + date.getDate();
                var dateStringEarly = dateEarly.getFullYear() + "-" + (dateEarly.getMonth() + 1) + "-" + dateEarly.getDate();
                var dateStringLater = dateLater.getFullYear() + "-" + (dateLater.getMonth() + 1) + "-" + dateLater.getDate();
                if (dateString != prevDateString && dateStringEarly != prevDateString && dateStringLater != prevDateString) {
                    Calendar.dateChanged(dateString, "day");
                    prevDateString = dateString;
                }
            });

            // Create y-axes
            yAxes = view["v2"]["y_axes"];
            l = yAxes.length;
            for (i = 0; i < l; i++) {

                // Update min_time, max_time for each channel with latest from
                // SOURCES if available
                channel = getSourceChannelByName(yAxes[i]["device_name"], yAxes[i]["channel_name"]);
                if (!!channel && channel.hasOwnProperty("min_time")
                    && channel.hasOwnProperty("max_time")) {
                    yAxes[i]["min_time"] = channel["min_time"];
                    yAxes[i]["max_time"] = channel["max_time"];
                }

                addChannel(yAxes[i], null);
            }
        }
    }

    // Helper function which converts the given channels object to an array
    // of channels, sorted in the order specified by the value of each
    // channel's y_axis property.
    function validateView(view) {
        var xAxes, yAxes;
        var viewName, channels;
        var deviceChannels, channelName;
        var obj, styles;
        var i, l;

        if (typeof view === 'undefined' ||
            view == null ||
            typeof view['error'] !== 'undefined') {
            return false;
        }

        /* V2 structure
         {
         "name" : "test",
         "v2"   : {
         "show_add_pane" : true,
         "x_axis" : {
         "min" : 0.00,
         "max" : 0.00
         },
         "y_axes" : [
         {
         "device_name" : "",
         "channel_name" : "",
         "min" : 0.00,
         "max" : 0.00,
         "style" : {},
         "y_range" : {"min_val" : 0, "max_val" : 0}, // optional
         "channel_height" : 0 // pixels, optional
         },
         {
         }
         ]
         }
         }
         */
        if (typeof view['v2'] !== 'undefined') {
            if (typeof view['v2']['x_axis'] !== 'object' ||
                typeof view['v2']['y_axes'] !== 'object') {
                return false;
            }

            // Rename style-types to styles if present
            yAxes = view['v2']['y_axes'];
            l = yAxes.length;
            for (i = 0; i < l; i++) {
                if (yAxes[i].hasOwnProperty("style")) {
                    if (!yAxes[i]['style'].hasOwnProperty("styles")) {
                        console.log("Patching v2 view");
                        view['v2']['y_axes'][i]['style']['styles'] = yAxes[i]['style']['style-types'];
                    }
                }
            }
            return true;
        }

        /*** Upgrade view data to v2 format ***/
        console.log("Upgrading view to v2");

        // Validate fields
        xAxes = view['x_axes'];
        yAxes = view['y_axes'];
        viewName = view['name'];
        channels = view['channels'];

        if (typeof xAxes !== 'object' || typeof xAxes[0] !== 'object' ||
            typeof yAxes !== 'object' || typeof yAxes[0] !== 'object' ||
            typeof viewName !== 'string' || viewName.length === 0 ||
            typeof channels !== 'object') {
            return false;
        }

        // Create v2 structure
        view["v2"] = {
            "x_axis" : {
                "min" : xAxes[0]["min_time"],
                "max" : xAxes[0]["max_time"]
            },
            "y_axes" : []
        };

        for (var deviceName in channels) {
            if (channels.hasOwnProperty(deviceName)) {
                deviceChannels = channels[deviceName];
                for (channelName in deviceChannels) {
                    if (deviceChannels.hasOwnProperty(channelName)) {
                        obj = channels[deviceName][channelName];
                        index = obj['y_axis'];

                        // Customize default style for zeo
                        if (channelName == "Sleep_Graph") {
                            styles = [{
                                "type" : "zeo"
                            }];
                        }
                        else {
                            styles = [{
                                "type"      : "line",
                                "color"     : obj['color'],
                                "lineWidth" : 1
                            }];
                        }
                        view["v2"]["y_axes"][index] = {
                            'device_name'  : deviceName,
                            'channel_name' : channelName,
                            'min'          : yAxes[index]['min_val'],
                            'max'          : yAxes[index]['max_val'],
                            'style'        : { "styles" : styles }
                        };
                    }
                }
            }
        }
        return true;
    }

    function channelDatasource(userId, deviceName, channelName) {
        var urlPrefix = "/bodytrack/tiles/" + userId + "/" + deviceName + "."
            + channelName + "/";
        return __createDatasource(urlPrefix);
    }

    // If allTags is true, we require all tags to be present.
    // Otherwise, any tag in tags is OK (the default)
    function photoDatasource(userId, deviceName, tags, allTags, nsfw) {
        var urlPrefix = "/bodytrack/photos/" + userId + "/";
        var urlParams = {};
        if (deviceName != null && deviceName.toLowerCase() != "all") {
            urlParams["dev_nickname"] = deviceName;
        }
        if (tags != null && tags.length > 0) {
            if (!!allTags) {
                urlParams["all_tags"] = tags.join(",");
            } else {
                urlParams["any_tags"] = tags.join(",");
            }

            // TODO: This line is only for compatibility with the server
            // until the server supports any_tags and all_tags.  This
            // can be safely removed, with no impact on correctness,
            // when that server support is added
            urlParams["tags_filter"] = tags.join(",");
        }
        if (!!nsfw) {
            urlParams["nsfw"] = "1";
        }
        return __createDatasource(urlPrefix, urlParams);
    }

    function createPhotoDialogCache(channelFilterTags, isAndJoin) {
        var cache = {
            photos                             : [],
            photosById                         : {}, // maps photo ID to an index in the photos array
            isLoadingPreceding                 : false,
            isLoadingFollowing                 : false,
            NUM_PHOTOS_TO_FETCH                : 20,
            DISTANCE_FROM_END_TO_TRIGGER_FETCH : 10,
            __loadNeighboringPhotoMetadata     : function(currentPhotoId,
                                                          currentPhotoTimestamp,
                                                          tagsFilterArray,
                                                          isAndJoin,
                                                          shouldLoadPreviousNeighbor, // flag which determines whether the previous or following neighbor will be loaded
                                                          callbacks) {
                currentPhotoId = TOOLS.parseInt(currentPhotoId, -1);
                if (currentPhotoId >= 0) {
                    if (typeof callbacks === 'undefined') {
                        callbacks = {};
                    }
                    var successCallback = callbacks['success'];
                    var errorCallback = callbacks['error'];
                    var completeCallback = callbacks['complete'];

                    shouldLoadPreviousNeighbor = !!shouldLoadPreviousNeighbor;
                    isAndJoin = !!isAndJoin;

                    var url = "/bodytrack/users/" + LOGIN.user_id + "/log_items/get";
                    var urlParams = {
                        "id"         : currentPhotoId,
                        "time"       : currentPhotoTimestamp,
                        "type"       : "photos",
                        "descending" : shouldLoadPreviousNeighbor,
                        "exclusive"  : false,
                        "count"      : cache.NUM_PHOTOS_TO_FETCH
                    };

                    if (isAndJoin) {
                        urlParams["all_tags"] = tagsFilterArray.join(",");
                    } else {
                        urlParams["any_tags"] = tagsFilterArray.join(",");
                    }

                    TOOLS.loadJson(url, urlParams, {
                        "success"  : function(photos) {
                            if ($.isArray(photos)) {
                                if (typeof successCallback === 'function') {
                                    var photosMetadata = [];
                                    $.each(photos, function(index, photo) {
                                        photosMetadata[index] = {
                                            "photoId"         : photo['id'],
                                            "comment"         : photo['comment'],
                                            "tags"            : photo['tags'],
                                            "timestamp"       : photo['end_d'],
                                            "timestampString" : photo['end']
                                        };
                                    });

                                    // mark the last photo as the end if we got fewer photos than we wanted
                                    if (photos.length < cache.NUM_PHOTOS_TO_FETCH) {
                                        console.log("PhotoDialogCache.__loadNeighboringPhotoMetadata(): Requested ["+cache.NUM_PHOTOS_TO_FETCH+"] photos, but only got ["+photos.length+"].  Marking the last photo as the end to prevent spurious fetches.");
                                        photosMetadata[photosMetadata.length-1]['isEndingPhoto'] = true;
                                    }

                                    successCallback(photosMetadata);
                                }
                            } else if (typeof errorCallback == 'function') {
                                errorCallback("loadNeighboringPhotoMetadata(): Returned data is not an array");
                            }
                        },
                        "error"    : errorCallback,
                        "complete" : completeCallback
                    });
                }
            }, __loadPreceding                 : function(photoId, timestamp, successCallback) {
                if (cache.isLoadingPreceding) {
                    console.log("PhotoDialogCache.__loadPreceding(): doing nothing since we're already loading");
                } else {
                    cache.isLoadingPreceding = true;
                    cache.__loadNeighboringPhotoMetadata(photoId,
                        timestamp,
                        channelFilterTags,
                        isAndJoin,
                        true,
                        {
                            "success" : successCallback,
                            "complete": function() {
                                cache.isLoadingPreceding = false;
                            }
                        });
                }
            },
            __loadFollowing                    : function(photoId, timestamp, successCallback) {
                if (cache.isLoadingFollowing) {
                    console.log("PhotoDialogCache.__loadFollowing(): doing nothing since we're already loading");
                } else {
                    cache.isLoadingFollowing = true;
                    cache.__loadNeighboringPhotoMetadata(photoId,
                        timestamp,
                        channelFilterTags,
                        isAndJoin,
                        false,
                        {
                            "success" : successCallback,
                            "complete": function() {
                                cache.isLoadingFollowing = false;
                            }
                        });
                }
            },
            initialize                         : function(photoId, timestamp, callback) {
                //console.log("PhotoDialogCache.initialize()------------------------------------------");

                // To build up the initial cache, fetch the photos BEFORE this photo, then the photos AFTER it.
                cache.__loadPreceding(photoId,
                    timestamp,
                    function(precedingPhotosMetadata) {
                        cache.__loadFollowing(photoId,
                            timestamp,
                            function(followingPhotosMetadata) {
                                // Create the initial cache.  We do this by first reversing the array
                                // containing the preceding photos, then slicing off the first element
                                // of the array containing the following photos (since it's a dupe
                                // of the last element in the reverse precedingPhotosMetadata
                                // array) and then concatenating the two together.
                                cache.photos = precedingPhotosMetadata.reverse().concat(followingPhotosMetadata.slice(1));

                                // now create the map which maps photo ID to photo array element index
                                $.each(cache.photos, function(index, photo) {
                                    cache.photosById[photo['photoId']] = index;
                                });

                                // now that the cache is created, we can call the callback
                                if (typeof callback === 'function') {
                                    callback();
                                }
                            })
                    });
            },

            __getPhotoMetadata : function(photoId, offset) {
                if (photoId in cache.photosById) {
                    var indexOfRequestedPhoto = cache.photosById[photoId] + offset;
                    if (indexOfRequestedPhoto >= 0 && indexOfRequestedPhoto < cache.photos.length) {
                        return cache.photos[indexOfRequestedPhoto];
                    }
                }
                //console.log("PhotoDialogCache.__getPhotoMetadata(): Failed to get photo offset [" + offset + "] for ID [" + photoId + "]");
                return null;
            },

            getPreviousPhotoMetadata : function(photoId) {
                var photo = cache.__getPhotoMetadata(photoId, -1);

                if (photo != null) {
                    // Check how close we are to the beginning of the array.  If it's within __DISTANCE_FROM_END_TO_TRIGGER_FETCH,
                    // then spawn an asyncrhonous job to fetch more photos
                    var distance = cache.photosById[photoId];
                    if (distance < cache.DISTANCE_FROM_END_TO_TRIGGER_FETCH) {
                        var endingPhoto = cache.photos[0];
                        if ('isEndingPhoto' in endingPhoto) {
                            console.log("PhotoDialogCache.getPreviousPhotoMetadata(): No need to fetch more photos since we've already loaded up to the end [" + endingPhoto['photoId'] + "]");
                        } else {
                            console.log("PhotoDialogCache.getPreviousPhotoMetadata(): Fetching more photos preceding id ["+endingPhoto['photoId']+"]");
                            cache.__loadPreceding(endingPhoto['photoId'],
                                endingPhoto['timestamp'],
                                function(photosMetadata) {
                                    console.log("PhotoDialogCache.getPreviousPhotoMetadata(): Fetched ["+photosMetadata.length+"] more previous photos.");

                                    // make sure that the cache didn't change while we were doing the fetch
                                    if (endingPhoto['photoId'] == cache.photos[0]['photoId']) {
                                        // create a new photos array for the cache
                                        var newPhotos = photosMetadata.slice(1).reverse().concat(cache.photos);
                                        var newPhotosById = {};

                                        // now recreate the map which maps photo ID to photo array element index
                                        $.each(newPhotos, function(index, photo) {
                                            newPhotosById[photo['photoId']] = index;
                                        });

                                        //var s = "";
                                        //$.each(newPhotos, function(index, photo) {
                                        //  s += photo['photoId'] + ","
                                        //});
                                        //console.log("length=[" + newPhotos.length + "," + cache.photos.length + "]: " + s);

                                        // update the cache's array and map
                                        cache.photos = newPhotos;
                                        cache.photosById = newPhotosById;
                                    } else {
                                        console.log("PhotoDialogCache.getPreviousPhotoMetadata(): cache has changed, won't update");
                                    }
                                });
                        }
                    }
                }

                return photo;
            },

            getNextPhotoMetadata : function(photoId) {
                var photo = cache.__getPhotoMetadata(photoId, 1);

                if (photo != null) {
                    // Check how close we are to the beginning of the array.  If it's within __DISTANCE_FROM_END_TO_TRIGGER_FETCH,
                    // then spawn an asyncrhonous job to fetch more photos
                    var distance = cache.photos.length - 1 - cache.photosById[photoId];
                    if (distance < cache.DISTANCE_FROM_END_TO_TRIGGER_FETCH) {
                        var endingPhoto = cache.photos[cache.photos.length - 1];
                        if ('isEndingPhoto' in endingPhoto) {
                            console.log("PhotoDialogCache.getNextPhotoMetadata(): No need to fetch more photos since we've already loaded up to the end [" + endingPhoto['photoId'] + "]");
                        } else {
                            console.log("PhotoDialogCache.getNextPhotoMetadata(): Fetching more photos following id ["+endingPhoto['photoId']+"]");
                            cache.__loadFollowing(endingPhoto['photoId'],
                                endingPhoto['timestamp'],
                                function(photosMetadata) {
                                    console.log("PhotoDialogCache.getNextPhotoMetadata(): Fetched ["+photosMetadata.length+"] more following photos.");

                                    // make sure that the cache didn't change while we were doing the fetch
                                    if (endingPhoto['photoId'] == cache.photos[cache.photos.length - 1]['photoId']) {
                                        // create a new photos array for the cache
                                        var newPhotos = cache.photos.concat(photosMetadata.slice(1));
                                        var newPhotosById = {};

                                        // now recreate the map which maps photo ID to photo array element index
                                        $.each(newPhotos, function(index, photo) {
                                            newPhotosById[photo['photoId']] = index;
                                        });

                                        //var s = "";
                                        //$.each(newPhotos, function(index, photo) {
                                        //  s += photo['photoId'] + ","
                                        //});
                                        //console.log("length=[" + newPhotos.length + "," + cache.photos.length + "]: " + s);

                                        // update the cache's array and map
                                        cache.photos = newPhotos;
                                        cache.photosById = newPhotosById;
                                    } else {
                                        console.log("PhotoDialogCache.getNextPhotoMetadata(): cache has changed, won't update");
                                    }
                                });
                        }
                    }
                }

                return photo;
            },

            getPhotoMetadata : function(photoId) {
                return cache.__getPhotoMetadata(photoId, 0);
            },

            update : function(photoId, newData) {
                if (photoId in cache.photosById) {
                    var index = cache.photosById[photoId];
                    cache.photos[index] = {
                        "photoId"         : newData['id'],
                        "comment"         : newData['comment'],
                        "tags"            : newData['tags'],
                        "timestamp"       : newData['end_d'],
                        "timestampString" : newData['end']
                    };
                }
            }
        };
        return cache;
    }

    function dataPointListener(pointObj, sourceInfo) {
        if (pointObj) {
            $("#_timeline_dataPointValueLabel").html($.mustache($("#_timeline_data_point_value_label_template").html(), pointObj));
        } else {
            $("#_timeline_dataPointValueLabel").html("");
        }
    }

    function loadLogrecMetadata(logrecId, callbacks) {
        logrecId = TOOLS.parseInt(logrecId, -1);
        if (logrecId >= 0) {

            var url = "/bodytrack/users/" + LOGIN.user_id + "/logrecs/" + logrecId + "/get";

            TOOLS.loadJson(url, {}, callbacks);
        }
    }

    function photoDataPointListener(channelElementId) {
        return function(pointObj, sourceInfo) {
            if (pointObj && sourceInfo && sourceInfo['info']) {

                // returns the array of tags currently selected for this photo
                var getUserSelectedTags = function() {
                    var tags = [];
                    $.each($("#_timeline_photo_dialog_tags_editor .tagedit-listelement-old input"),
                        function(index, inputElement) {
                            var val = inputElement['value'];
                            if (typeof val === 'string' && val != '') {
                                tags[tags.length] = val;
                            }
                        });
                    return tags;
                };

                // returns the array of tags selected for this channel's filter
                var getTagFilterForChannel = function() {
                    var tags = [];
                    $.each($("#" + channelElementId + "-photo-tags-filter .tagedit-listelement-old input"),
                        function(index, inputElement) {
                            var val = inputElement['value'];
                            if (typeof val === 'string' && val != '') {
                                tags[tags.length] = val;
                            }
                        });
                    return tags;
                };
                // get the channel's current settings for tag filtering
                var isAndJoin = $("#" + channelElementId + "-photo-tags-isAndJoin").val() === 'true';
                var channelFilterTags = getTagFilterForChannel();

                // create the photo cache
                var photoCache = createPhotoDialogCache(channelFilterTags, isAndJoin);

                var createPhotoDialog = function(photoId, timestamp, completionCallback) {

                    var mediumResImageUrl = $.mustache($("#_timeline_photo_dialog_medium_res_image_url_template").html(), {"photoId" : photoId, "userId" : LOGIN.user_id});
                    var highResImageUrl = $.mustache($("#_timeline_photo_dialog_high_res_image_url_template").html(), {"photoId" : photoId, "userId" : LOGIN.user_id});
                    $("#_timeline_photo_dialog").html($.mustache($("#_timeline_photo_dialog_template").html(), {"photoUrl" : mediumResImageUrl}));

                    var updateGoToNeighborOnSaveWidgets = function() {
                        var isEnabled = $("#_timeline_photo_dialog_save_should_goto_neighbor").is(':checked');
                        var direction = TOOLS.parseInt($("#_timeline_photo_dialog_save_should_goto_neighbor_choice").val(),0);
                        PREFS.set("photo_dialog.goto_neighbor_on_save.enabled", isEnabled);
                        PREFS.set("photo_dialog.goto_neighbor_on_save.direction", direction);

                        if (isEnabled) {
                            $("#_timeline_photo_dialog_save_should_goto_neighbor_choice").removeAttr("disabled");
                            $("#_timeline_photo_dialog_save_preferences label").css("color", "#000000");
                            if (direction < 0) {
                                $("#_timeline_photo_dialog_save_button").html("Save &amp; Previous");
                            } else {
                                $("#_timeline_photo_dialog_save_button").html("Save &amp; Next");
                            }
                        } else {
                            $("#_timeline_photo_dialog_save_should_goto_neighbor_choice").attr("disabled", "disabled");
                            $("#_timeline_photo_dialog_save_preferences label").css("color", "#aaaaaa");
                            $("#_timeline_photo_dialog_save_button").text("Save");
                        }
                    };

                    // set the widgets for the Save button behavior based on saved prefs
                    var goToNeighborOnSaveEnabled = !!PREFS.get("photo_dialog.goto_neighbor_on_save.enabled", false);
                    var goToNeighborOnSaveDirection = TOOLS.parseInt(PREFS.get("photo_dialog.goto_neighbor_on_save.direction", 0), 0);
                    $("#_timeline_photo_dialog_save_should_goto_neighbor").prop("checked", goToNeighborOnSaveEnabled);
                    $("#_timeline_photo_dialog_save_should_goto_neighbor").change(updateGoToNeighborOnSaveWidgets);
                    $("#_timeline_photo_dialog_save_should_goto_neighbor_choice").val(goToNeighborOnSaveDirection == 0 ? 1 : goToNeighborOnSaveDirection);
                    $("#_timeline_photo_dialog_save_should_goto_neighbor_choice").change(updateGoToNeighborOnSaveWidgets);

                    // display Loading status message
                    $("#_timeline_photo_dialog_form_status").text("Loading...").show();

                    // set previous and next buttons initially hidden
                    $("#_timeline_photo_dialog_previous_button").hide();
                    $("#_timeline_photo_dialog_next_button").hide();

                    // Fetch the metadata for the preceding, following, and current photos from the cache.
                    var previousPhotoMetadata = photoCache.getPreviousPhotoMetadata(photoId);
                    var nextPhotoMetadata = photoCache.getNextPhotoMetadata(photoId);
                    var data = photoCache.getPhotoMetadata(photoId);

                    var isPreviousPhoto = previousPhotoMetadata != null &&
                        typeof previousPhotoMetadata !== 'undefined' &&
                        typeof previousPhotoMetadata['photoId'] !== 'undefined';
                    if (isPreviousPhoto) {
                        $("#_timeline_photo_dialog_previous_button").show().click(function() {
                            createPhotoDialog(previousPhotoMetadata['photoId'],
                                previousPhotoMetadata['timestamp']);
                        });
                    }

                    var isNextPhoto = nextPhotoMetadata != null &&
                        typeof nextPhotoMetadata !== 'undefined' &&
                        typeof nextPhotoMetadata['photoId'] !== 'undefined';
                    if (isNextPhoto) {
                        $("#_timeline_photo_dialog_next_button").show().click(function() {
                            createPhotoDialog(nextPhotoMetadata['photoId'],
                                nextPhotoMetadata['timestamp']);
                        });
                    }

                    if (typeof data === 'string') {
                        data = JSON.parse(data);
                    }

                    // treat undefined or null comment as an empty comment
                    if (typeof data['comment'] === 'undefined' || data['comment'] == null) {
                        data['comment'] = '';
                    }

                    // treat undefined or null tags as an empty array
                    if (typeof data['tags'] === 'undefined' || data['tags'] == null) {
                        data['tags'] = [];
                    }

                    // add click handler for photo to allow viewing of high-res version
                    $("#_timeline_photo_dialog_image").click(function() {
                        var theImage = $(this);
                        var imageAspectRatio = $(this).width() / $(this).height();
                        var formContainer = $("#_timeline_photo_dialog_form_container");
                        if ($("#_timeline_photo_dialog_form_container").is(":visible")) {
                            // fade out the form and show the hi-res version of the image
                            formContainer.fadeOut(100, function() {
                                var imageHeight = $("body").height() - 60;
                                var imageWidth = imageAspectRatio * imageHeight;

                                // make sure the image isn't too wide now
                                if (imageWidth > $("body").width()) {
                                    imageWidth = $("body").width() - 100;
                                    imageHeight = imageWidth / imageAspectRatio;
                                }

                                theImage.attr("src",highResImageUrl).height(imageHeight).width(imageWidth);
                                $("#_timeline_photo_dialog_photo_table").height(imageHeight).width(imageWidth);
                                centerPhotoDialog();
                            });
                        } else {
                            // fade the form back in and show the medium-res version of the image
                            formContainer.fadeIn(100, function() {
                                var imageHeight = 300;
                                var imageWidth = imageAspectRatio * imageHeight;
                                theImage.height(imageHeight).width(imageWidth);
                                $("#_timeline_photo_dialog_photo_table").height(imageHeight).width(imageWidth);
                                centerPhotoDialog();
                                theImage.attr("src", mediumResImageUrl);
                            });
                        }
                    });

                    var createCommentAndTagForm = function(comment, tags) {

                        var isDirty = function() {
                            // first check the comment, since it's easy and cheap
                            if ($("#_timeline_photo_dialog_comment").val() != comment) {
                                return true;
                            }

                            // if the comment isn't dirty, then check the tags
                            var newTags = getUserSelectedTags();

                            // start by checking the length
                            if (newTags.length != tags.length) {
                                return true;
                            }

                            // now compare individual tags
                            for (var i = 0; i < newTags.length; i++) {
                                if (newTags[i] != tags[i]) {
                                    return true;
                                }
                            }

                            return false;
                        };

                        var setEnabledStateOfRevertAndSaveButtons = function() {
                            if (isDirty()) {
                                //$("#_timeline_photo_dialog_save_button").removeAttr("disabled");
                                $("#_timeline_photo_dialog_revert_button").removeAttr("disabled");
                            } else {
                                //$("#_timeline_photo_dialog_save_button").attr("disabled", "disabled");
                                $("#_timeline_photo_dialog_revert_button").attr("disabled", "disabled");
                            }
                        };

                        // build the form for the metadata editor
                        var photoMetadataForm = $.mustache($("#_timeline_photo_dialog_form_template").html(), {});
                        $("#_timeline_photo_dialog_form").html(photoMetadataForm);

                        // fill in the timestamp
                        if (typeof data['timestampString'] === 'undefined') {
                            $("#_timeline_photo_dialog_timestamp").html("&nbsp;");
                        } else {
                            $("#_timeline_photo_dialog_timestamp").text(new Date(data['timestampString']).toString());
                        }

                        // fill in the comment, if any
                        if (typeof comment === 'undefined' || comment == null) {
                            $("#_timeline_photo_dialog_comment").val('');
                        } else {
                            $("#_timeline_photo_dialog_comment").val(comment);
                        }

                        // Set up focus and blur event handlers for the comment field, to toggle
                        // close on ESC for the photo dialog.  We don't want the ESC key to close
                        // the dialog when the user is editing the comment.
                        $("#_timeline_photo_dialog_comment").focus(function() {
                            $("#_timeline_photo_dialog")['dialog']("option", "closeOnEscape", false);
                        });
                        $("#_timeline_photo_dialog_comment").blur(function() {
                            $("#_timeline_photo_dialog")['dialog']("option", "closeOnEscape", true);
                        });
                        $("#_timeline_photo_dialog_comment").keyup(setEnabledStateOfRevertAndSaveButtons);

                        // add the tags, if any
                        if ($.isArray(tags) && tags.length > 0) {
                            $.each(tags,
                                function(index, value) {
                                    var tagHtml = $.mustache($("#_timeline_photo_dialog_tags_editor_tag_template").html(), {"value" : value});
                                    $("#_timeline_photo_dialog_tags_editor").append(tagHtml);
                                });
                        } else {
                            var tagHtml = $.mustache($("#_timeline_photo_dialog_tags_editor_tag_template").html(), {"value" : ""});
                            $("#_timeline_photo_dialog_tags_editor").append(tagHtml);
                        }

                        // construct the tag editor
                        var tagEditorOptions = {
                            autocompleteOptions : {
                                "minLength" : 0, // TODO: make this 1 or 2 if the list of tags is huge
                                "delay"     : 0,
                                "autoFocus" : false,
                                source      : function(request, response) {
                                    var tagsToExclude = getUserSelectedTags();
                                    var cachedTagsData = TAG_MANAGER.getCachedTagsForTagEditor(tagsToExclude);
                                    return response($.ui.autocomplete.filter(cachedTagsData, request.term));
                                }
                            },
                            // return, comma, space, period, semicolon
                            breakKeyCodes       : [ 13, 44, 32, 59 ],
                            additionalListClass : '_timeline_photo_tags_input',
                            animSpeed           : 100,
                            allowAdd            : true,
                            allowEdit           : true,
                            allowDelete         : false,
                            texts               : {
                                removeLinkTitle    : 'Remove this tag from the list',
                                saveEditLinkTitle  : 'Save changes',
                                breakEditLinkTitle : 'Undo changes'
                            }
                        };
                        $('#_timeline_photo_dialog_tags_editor input.tag').tagedit(tagEditorOptions);
                        $('#_timeline_photo_dialog_tags_editor').bind('tagsChanged', setEnabledStateOfRevertAndSaveButtons);
                        $('#_timeline_photo_dialog_tags_editor').bind('receivedFocus', function() {
                            $("#_timeline_photo_dialog")['dialog']("option", "closeOnEscape", false);
                        });
                        $('#_timeline_photo_dialog_tags_editor').bind('tabToNextElement', function(event) {
                            $("#_timeline_photo_dialog")['dialog']("option", "closeOnEscape", true);

                            $("#_timeline_photo_dialog_tags_editor_tabhelper_post_proxy_forward").focus();
                            return false;
                        });
                        $('#_timeline_photo_dialog_tags_editor').bind('tabToPreviousElement', function(event) {
                            $("#_timeline_photo_dialog")['dialog']("option", "closeOnEscape", true);

                            $("#_timeline_photo_dialog_comment").select().focus();
                            return false;
                        });

                        // set form buttons to initially disabled
                        //$("#_timeline_photo_dialog_save_button").attr("disabled", "disabled");
                        $("#_timeline_photo_dialog_revert_button").attr("disabled", "disabled");

                        // configure the Revert button
                        $("#_timeline_photo_dialog_revert_button").click(function() {
                            $("#_timeline_photo_dialog_form").hide();
                            $("#_timeline_photo_dialog_form_status").text("Loading...").show();

                            // recreate the comment and tag form
                            createCommentAndTagForm(comment, tags);
                            $("#_timeline_photo_dialog_form_status").hide();
                            $("#_timeline_photo_dialog_form").show();

                            // focus on the comment
                            $("#_timeline_photo_dialog_comment").select().focus();
                        });

                        // configure the Save button
                        $("#_timeline_photo_dialog_save_button").click(function() {

                            // set form buttons to disabled while saving
                            //$("#_timeline_photo_dialog_save_button").attr("disabled", "disabled");
                            $("#_timeline_photo_dialog_revert_button").attr("disabled", "disabled");

                            $("#_timeline_photo_dialog_form").hide();
                            $("#_timeline_photo_dialog_form_status").text("Saving...").show();

                            $.ajax({
                                cache    : false,
                                type     : "POST",
                                url      : "/bodytrack/users/" + LOGIN.user_id + "/logrecs/" + photoId + "/set",
                                data     : {
                                    "tags"    : getUserSelectedTags().join(','),
                                    "comment" : $("#_timeline_photo_dialog_comment").val()
                                },
                                dataType : "json",
                                success  : function(savedData, textStatus, jqXHR) {
                                    if (typeof savedData === 'object') {
                                        console.log("Successfully saved comment and tags for photo [" + photoId + "]");
                                        console.log(savedData);
                                        photoCache.update(photoId, savedData);
                                        TAG_MANAGER.refreshTagCache(function() {

                                            $("#_timeline_photo_dialog_form_status")
                                                .text("Saved.")
                                                .delay(250)
                                                .fadeOut(500, function() {
                                                    // read the desired direction from the prefs
                                                    goToNeighborOnSaveEnabled = !!PREFS.get("photo_dialog.goto_neighbor_on_save.enabled", false);
                                                    goToNeighborOnSaveDirection = TOOLS.parseInt(PREFS.get("photo_dialog.goto_neighbor_on_save.direction", 0), 0);

                                                    // now determine what action to take upon save
                                                    if (goToNeighborOnSaveEnabled && isPreviousPhoto && goToNeighborOnSaveDirection < 0) {
                                                        $("#_timeline_photo_dialog_previous_button").click();
                                                    } else if (goToNeighborOnSaveEnabled && isNextPhoto && goToNeighborOnSaveDirection > 0) {
                                                        $("#_timeline_photo_dialog_next_button").click();
                                                    } else {
                                                        // recreate the comment and tag form
                                                        createCommentAndTagForm(savedData['comment'], savedData['tags']);

                                                        $("#_timeline_photo_dialog_form").show();

                                                        // focus on the comment
                                                        $("#_timeline_photo_dialog_comment").select().focus();
                                                    }
                                                });
                                        });
                                    } else {
                                        console.log("Unexpected response when saving comment and tags for photo [" + photoId + "]:  savedData=[" + savedData + "] textStatus=[" + textStatus + "]");
                                        $("#_timeline_photo_dialog_form_status").text("Saved failed.").show();
                                    }
                                },
                                error    : function(jqXHR, textStatus, errorThrown) {
                                    console.log("Failed to save comment and tags for photo [" + photoId + "]:  textStatus=[" + textStatus + "] errorThrown=[" + errorThrown + "]");
                                    $("#_timeline_photo_dialog_form_status").text("Saved failed.").show();
                                }
                            });
                        });

                        updateGoToNeighborOnSaveWidgets();

                        // set up tabbing and focus handling
                        $("#_timeline_photo_dialog_form #tagedit-input").attr("tabindex", 102);
                        $("#_timeline_photo_dialog_tabhelper_pre_proxy_backward").focus(function() {
                            if ($("#_timeline_photo_dialog_save_should_goto_neighbor_choice").is(":enabled")) {
                                $("#_timeline_photo_dialog_save_should_goto_neighbor_choice").focus();
                            } else {
                                $("#_timeline_photo_dialog_save_should_goto_neighbor").focus();
                            }
                            return false;
                        });
                        $("#_timeline_photo_dialog_previous_button").focus(function() {
                            $(this).css("background-position", "0 -38px");
                        }).blur(function() {
                                $(this).css("background-position", "0 0");
                            });
                        $("#_timeline_photo_dialog_next_button").focus(function() {
                            $(this).css("background-position", "0 -38px");
                        }).blur(function() {
                                $(this).css("background-position", "0 0");
                            });
                        $("#_timeline_photo_dialog_comment_tabhelper_pre_proxy_forward").focus(function() {
                            $("#_timeline_photo_dialog_comment").focus().select();
                            return false;
                        });
                        $("#_timeline_photo_dialog_comment_tabhelper_pre_proxy_backward").focus(function() {
                            if (isNextPhoto) {
                                $("#_timeline_photo_dialog_next_button").focus();
                            } else if (isPreviousPhoto) {
                                $("#_timeline_photo_dialog_previous_button").focus();
                            } else {
                                $("#_timeline_photo_dialog_tabhelper_pre_proxy_backward").focus();
                            }
                            return false;
                        });
                        $("#_timeline_photo_dialog_comment").focus(function() {
                            return false;
                        });
                        $("#_timeline_photo_dialog_tags_editor_tabhelper_pre_proxy_forward").focus(function() {
                            $("#_timeline_photo_dialog_tags_editor ul").click();
                        });
                        $("#_timeline_photo_dialog_tags_editor_tabhelper_post_proxy_forward").focus(function() {
                            if ($("#_timeline_photo_dialog_save_button").is(":disabled")) {
                                $("#_timeline_photo_dialog_save_should_goto_neighbor").focus();
                            } else {
                                $("#_timeline_photo_dialog_save_button").focus();
                            }
                            return false;
                        });
                        $("#_timeline_photo_dialog_tags_editor_tabhelper_post_proxy_backward").focus(function() {
                            $("#_timeline_photo_dialog_tags_editor ul").click();
                        });
                        $("#_timeline_photo_dialog_revert_button").focus(function() {
                            $(this).css("color", "#18B054");
                        }).blur(function() {
                                $(this).css("color", "#000000");
                            });
                        $("#_timeline_photo_dialog_save_button").focus(function(event) {
                            $(this).css("color", "#18B054");
                        }).blur(function(event) {
                                $(this).css("color", "#000000");
                            });
                        $("#_timeline_photo_dialog_post_proxy_forward").focus(function() {
                            if (isPreviousPhoto) {
                                $("#_timeline_photo_dialog_previous_button").focus();
                            } else if (isNextPhoto) {
                                $("#_timeline_photo_dialog_next_button").focus();
                            } else {
                                $("#_timeline_photo_dialog_comment").focus().select();
                            }
                            return false;
                        });

                        // set focus on the comment input, and select all the text
                        $("#_timeline_photo_dialog_comment").select().focus();

                    };

                    // create the comment and tag form, hide the status area, and show the form
                    createCommentAndTagForm(data['comment'], data['tags']);
                    $("#_timeline_photo_dialog_form_status").hide();
                    $("#_timeline_photo_dialog_form").show();

                    // Finally, call the completion callback, if any
                    if (typeof completionCallback === 'function') {
                        completionCallback();
                    }
                };

                // initialize the photo cache--when it's done preloading then open the photo dialog
                photoCache.initialize(sourceInfo['info']['imageId'],
                    pointObj['date'],
                    function() {
                        createPhotoDialog(sourceInfo['info']['imageId'],
                            pointObj['date'],
                            function() {
                                centerPhotoDialog();
                            });
                    });

                // Open the dialog
                $("#_timeline_photo_dialog").html($.mustache($("#_timeline_photo_dialog_loading_template").html()));
                $("#_timeline_photo_dialog")['dialog']('open');
            }
        };
    }

    function centerPhotoDialog() {
        // center the dialog
        $("#_timeline_photo_dialog")['dialog']("option", "position", 'center');
    }

    function saveDefaultChannelStyle(channel, defaultStyleObj, callbacks) {
        if (typeof channel === 'object' &&
            typeof defaultStyleObj === 'object' &&
            typeof channel["device_name"] === 'string' &&
            typeof channel["channel_name"] === 'string') {

            if (typeof callbacks === 'undefined') {
                callbacks = {};
            }
            var successCallback = callbacks['success'];
            var errorCallback = callbacks['error'];
            var completeCallback = callbacks['complete'];

            var url = "/bodytrack/users/" + LOGIN.user_id + "/channels/" + encodeURIComponent(channel["device_name"]) + "." + encodeURIComponent(channel["channel_name"]) + "/set";
            $.ajax({
                cache    : false,
                type     : "POST",
                url      : url,
                data     : {"user_default_style" : JSON.stringify(defaultStyleObj)},
                success  : function(data, textStatus, jqXHR) {
                    try {
                        if (typeof successCallback === 'function') {
                            successCallback(data);
                        }
                    }
                    catch (ex) {
                        console.log("saveDefaultChannelStyle.success: JSON parse error, or failure in the successCallback: " + ex);
                    }
                },
                error    : function(jqXHR, textStatus, errorThrown) {
                    try {
                        if (typeof errorCallback === 'function') {
                            errorCallback(textStatus, errorThrown);
                        }
                    }
                    catch (ex) {
                        console.log("saveDefaultChannelStyle.error: Failure in the errorCallback: " + ex);
                    }
                },
                complete : function(jqXHR, textStatus) {
                    try {
                        if (typeof completeCallback === 'function') {
                            completeCallback(textStatus);
                        }
                    }
                    catch (ex) {
                        console.log("saveDefaultChannelStyle.complete: Failure in the completeCallback: " + ex);
                    }
                }
            });
        }
    }

    function dragAreaOnMouseDown(plotId) {
        var channelElementId = "_timeline_channel_" + plotId;
        var plotElementId = "_timeline_plot_" + plotId;
        var yAxisElementId = "_timeline_yAxis_" + plotId;
    	
    	var mostRecentY = null;
        var resizeTimer = null;
        var dylist = [];

        var resizePlot = function(dy) {
            var container = plotContainersMap[channelElementId];
            var cPlaceholder = $("#" + container.getPlaceholder());
            var containerW = cPlaceholder.width();
            var containerH = cPlaceholder.height();

            var plot = plotsMap[channelElementId];
            var yAxis = plot.getVerticalAxis();
            var yAixsW = $("#" + yAxis.getPlaceholder()).width();

            var dragAreaH = $("._timeline_dragArea").height();

            if ((dy > 0) || (Math.abs(dy) < containerH)) {
                // There is a min height of 70, which is taken from the
                // min height of the channel label
                if (containerH + dy + dragAreaH < 70) {
                    dy = 70 - containerH - dragAreaH;
                }

                // Set the size of the plot container itself
                $("#" + plotElementId).height(containerH + dy);
                container.setSize(containerW, containerH + dy,
                    SequenceNumber.getNext());

                // Set the size of the Y-axis
                $("#" + yAxisElementId).height(containerH + dy);
                yAxis.setSize(yAixsW, containerH + dy,
                    SequenceNumber.getNext());

                // Set the size of the channel label
                $("#_timeline_channelTab_" + plotId).height(containerH + dy);

                // Update the view data to match the new channel height
                if ((!!VIEWS.data) && (!!VIEWS.data["v2"])
                    && (!!VIEWS.data["v2"]["y_axes"])
                    && (VIEWS.data["v2"]["y_axes"].length > plotId)) {
                    VIEWS.data["v2"]["y_axes"][plotId]["channel_height"] =
                        containerH + dy;
                }
            }

            mostRecentY = mostRecentY + dy;
            return false;
        };
        var mouseup = null;
        var mousemove = null;
        var updatePlotSize = function() {
            if (dylist.length > 0) {
                resizePlot(dylist[dylist.length - 1]);
                dylist = [];
            }
        };
        var stopListening = function() {
            $(window).unbind({
                mousemove: mousemove,
                mouseup: mouseup
            });
            clearInterval(resizeTimer);
            return false;
        };
        mousemove = function(event) {
            if (mostRecentY == null) {
                mostRecentY = event.pageY;
            } else {
                dylist.push(event.pageY - mostRecentY);
            }
            return false; // Stops the event from propagating
        };
        mouseup = function(event) {
            if (mostRecentY == null) {
                return stopListening();
            }
            stopListening();
            resizePlot(event.pageY - mostRecentY);
            return false; // Stops the event from propagating
        };

        // TODO: Add mouseout event handler as well?
        $(window).bind({
            mousemove: mousemove,
            mouseup: mouseup
        });
        resizeTimer = setInterval(updatePlotSize, 100);

        return false;
    }

    function gotoTime(action) {
        var xAxis = dateAxis;
        if (!xAxis) {
            console.log("Missing date axis: cannot goto another time");
            return false;
        }
        var xMin = xAxis.getMin();
        var xMax = xAxis.getMax();
        var xWidth = xMax - xMin;

        if (action == "beginning") {
            var minTime = Number.MAX_VALUE;
            for (var channelKey in channelsMap) {
                var channel = channelsMap[channelKey];
                if (!!channel && channel.hasOwnProperty("min_time")) {
                    minTime = Math.min(minTime, channel["min_time"]);
                }
            }

            if (minTime < 0.99 * Number.MAX_VALUE) {
                xAxis.setRange(minTime, minTime + xWidth);
            }
        } else if (action == "back") {
            xAxis.setRange(xMin - xWidth, xMin);
        } else if (action == "forward") {
            xAxis.setRange(xMax, xMax + xWidth);
        } else if (action == "end") {
            var maxTime = -Number.MAX_VALUE;
            for (channelKey in channelsMap) {
                channel = channelsMap[channelKey];
                if (!!channel && channel.hasOwnProperty("max_time")) {
                    maxTime = Math.max(maxTime, channel["max_time"]);
                }
            }

            if (maxTime > -0.99 * Number.MAX_VALUE) {
                xAxis.setRange(maxTime - xWidth, maxTime);
            }
        } else {
            return false; // No change, so don't need to repaint plots
        }

        repaintAllPlots();

        return false;
    }

    function zoomTime(action) {
        var xAxis = dateAxis;
        if (!xAxis) {
            console.log("Missing date axis: cannot goto another time");
            return false;
        }
        var xMin = xAxis.getMin();
        var xMax = xAxis.getMax();
        var xWidth = xMax - xMin;
        var newXWidth = xWidth; // Just so newXWidth is always initialized

        if (action == "out") {
            newXWidth = xWidth * 1.4;
        } else if (action == "in") {
            newXWidth = xWidth / 1.4;
        } else {
            return false; // No change, so don't need to repaint plots
        }

        var dEndpoint = (newXWidth - xWidth) / 2.0;
        xAxis.setRange(xMin - dEndpoint, xMax + dEndpoint);

        repaintAllPlots();

        return false;
    }

    function repaintAllPlots() {
        for (var plotKey in plotsMap) {
            var plot = plotsMap[plotKey];
            if (plot) {
                plot.setStyle(plot.getStyle());
            }
        }
    }

    function render(digest, timeUnit) {
        this.getTemplate("text!applications/calendar/tabs/timeline/template.html", "timeline", function() {
            setup(digest, timeUnit);
        });
    }

    var timelineTab = new Tab("timeline", "Candide Kemmler", "icon-film", false);

    function setup(digest, timeUnit) {
        $(window).resize(function(){
            clearTimeout(BodyTrack.TOOLS.resizeTimer);
            BodyTrack.TOOLS.resizeTimer = setTimeout(BodyTrack.TOOLS.resizeHandler, 100);
        });

        APP.init(function() {
            timelineTab.init(function() {
                timelineTab.newView();
            });
        });
    }

    timelineTab.render = render;
    timelineTab.init = init;
    timelineTab.newView = newView;
    return timelineTab;
});