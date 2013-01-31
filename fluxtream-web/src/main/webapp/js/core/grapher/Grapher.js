define(["core/grapher/BTCore"], function(BTCore) {

    var Grapher = function(parentElement, options) {
        if (options == null) options = {};
        parentElement = $(parentElement);
        this.plotContainersMap = {}; // maps DOM element ID to plot container
        this.sourcesMap = {}; // maps DOM element ID to available source
        this.channelsMap = {}; // maps DOM element ID to view's y_axes
        this.plotsMap = {}; // maps DOM element ID to grapher widget
        this.grapherId = new Date().getTime() + "-" + Math.round(Math.random()*10000000);
        var grapher = this;
        for (var param in options)
            grapher[param] = options[param];
        if (grapher.onLoadActions == null)
            grapher.onLoadActions = [];
        if (grapher.loaded != null || grapher.onLoad != null)
            console.log("grapher.loaded and grapher.onLoad should not be set with options to constructor");
        grapher.loaded = false;
        grapher.onLoad = function() {
            var l = grapher.onLoadActions.length;
            for (var i = 0; i < l; i++) {
                grapher.onLoadActions[i]();
            }
            grapher.loaded = true;
        };
        App.loadMustacheTemplate("core/grapher/timelineTemplates.html", "mainGrapherApp", function(template) {
            parentElement.append(template.render(grapher));
            setup(grapher);
        });
    };

    var APP 		= BTCore.APP;
    var PREFS 		= BTCore.PREFS;
    var TOOLS 		= BTCore.TOOLS;
    var TAG_MANAGER = BTCore.TAG_MANAGER;
    var VIEWS 		= BTCore.VIEWS;
    var SOURCES 	= BTCore.SOURCES;

    var newViewName  		 = "Untitled View";
    var channelIdx   		 = 0;     // counter used to uniquely identify channels
    var dragSourceId 		 = null;  // DOM id of source is stored here during drag

    var plotContainers       = [];    // array of plot containers
    var hasUnsavedChanges    = false; // used by unsaved changes dialog handler
    var loadedViewStr        = "";    // JSON string of loaded view
    var addPaneChannelsState = [];    // add channels pane channel visibility
    var CHANNEL_PADDING      = 3;     // Pixels between plot and drag area

    var connectorEnabled;

    function _performAfterLoad(grapher, callback) {
        if (grapher.loaded) {
            callback();
        } else {
            grapher.onLoadActions.push(callback);
        }
    }

    function init(grapher, callback) {
        // Unsaved changes dialog handler
        $(window).bind("beforeunload", function() {
            updateViewData(grapher);
            var newvdata = JSON.stringify(VIEWS.data);
            if (loadedViewStr != newvdata) {
                hasUnsavedChanges = true;
            }

            if (hasUnsavedChanges) {
                //return "You have unsaved changes";
            }
        });
        $("form").submit(function() {
            $(window).unbind("beforeunload");
        });

        // Deal with window resize events
        TOOLS.onWindowResizeResizeElementHeight("#" + grapher.grapherId + "_timeline_addChannelsArea #_timeline_sources_list");
        TOOLS.onWindowResizeResizeElementHeight("#" + grapher.grapherId + "_timeline_channelsWrapper");
        TOOLS.onWindowResizeListener(function() {
            // borderOffset is used to account for:
            // * 1 pixel of border on #_timeline_channels
            // * 1 pixel of border on ._timeline_channel
            var borderOffset = 2;

            // Could also use ._timeline_channeltd, but $("._timeline_gotozoom").width() returns 0
            // whenever there are no channels
            var widthOfAreaLeftOfPlotContainer = $("._timeline_gotozoom").width() + borderOffset;
            // TODO: Find a good way to get the ._timeline_yaxistd width even when with no channels
            var widthOfAreaRightOfPlotContainer = $("._timeline_yaxistd").width() + borderOffset;

            // the .plotContainer has a 1 pixel border around it, so set this to 2 to account for the left and right sides
            var widthOfPlotContainerLeftAndRightBorder = 2;

            // compute the desired size of plot containers based on the current width of the _timeline_channelsArea (which
            // will automatically shrink if the Add Channels and/or Details pane is visible, so we don't explicitly need
            // to account for them here).
            var plotContainerWidth = $("#" + grapher.grapherId + "_timeline_channelsArea").width() - widthOfAreaLeftOfPlotContainer - widthOfAreaRightOfPlotContainer - widthOfPlotContainerLeftAndRightBorder - 20;

            // resize plot containers
            var plotContainerEventId = SequenceNumber.getNext();
            for (var i = 0; i < plotContainers.length; i++) {
                var plotContainerHeight = $("#" + plotContainers[i].getPlaceholder()).height();
                plotContainers[i].setSize(plotContainerWidth, plotContainerHeight, plotContainerEventId);
            }

            // resize date axis
            if (grapher.dateAxis) {
                grapher.dateAxis.setSize(plotContainerWidth, $("#" + grapher.grapherId + "_timeline_dateAxis").height(), SequenceNumber.getNext());
            }

            // resize y axes
            var yAxisWidth = $("._timeline_yAxis").width();
            for (var plotKey in grapher.plotsMap) {
                var plot = grapher.plotsMap[plotKey];
                if (plot) {
                    var yAxis = plot.getVerticalAxis();
                    var yAxisHeight = $("#" + yAxis.getPlaceholder()).height();
                    if (yAxis) {
                        yAxis.setSize(yAxisWidth, yAxisHeight);
                    }
                }
            }
            SOURCES.initialized = true
            if (grapher.onLoad != null) {
                var onload = grapher.onLoad;
                grapher.onLoad = null;
                onload();
                $.doTimeout(1000, function() {
                    $.ajax("/api/timezones/mapping", {success: function(mapping) {
                        grapher.dateAxis.setTimeZoneMapping(mapping);
                    }});
                });
            }
        });

        // Make the channel list sortable
        App.loadMustacheTemplate("core/grapher/timelineTemplates.html","channelTemplate",function(template){
            $("#" + grapher.grapherId + "_timeline_channels").sortable({
                handle      : '.flx-channel',
                axis        : 'y',
                tolerance   : 'pointer',
                containment : "#" + grapher.grapherId + "_timeline_channels",
                /*merge		: function(event, ui) {
                    var templateValues = {
                        "deviceName"       : "Devices",
                        "channelName"      : "Compare Stub",
                        "plotElementId"    : "_timeline_channel_helper",
                        "channelElementId" : "_timeline_plot_helper",
                        "yAxisElementId"   : "_timeline_yAxis_helper",
                        "showDeleteBtn"    : grapher.showDeleteBtn,
                        "grapherId"        : grapher.grapherId
                    };
                    var html = template.render(templateValues);

                    $(ui.item[0]).remove();
                    $(ui.droppable.item[0]).replaceWith(html);
                },
                mergein		: function(event, ui) {
                    $(ui.droppable.item[0]).addClass("_timeline_channel_hover");
                },
                mergeout	: function(event, ui) {
                    $(ui.droppable.item[0]).removeClass("_timeline_channel_hover");
                },*/
                receive     : function(event, ui) {	// received new channel to add
                    var i, l, c;
                    var src = grapher.sourcesMap[dragSourceId];

                    // Iterate through channels and call addChannel on
                    // entries with no id
                    // NOTE: We assume the only reason the id is blank is if the
                    //       element is new (user dragged into channels)
                    c = $("#" + grapher.grapherId + "_timeline_channels").children();
                    l = c.length;
                    for (i = 0; i < l; i++) {
                        if (c[i].id == "") {
                            grapher.addChannel(src, c[i]);
                        }
                    }
                }
            });
        });
        $("#" + grapher.grapherId + "_timeline_channels").disableSelection();

        // Click handlers
        $("#" + grapher.grapherId + "_timeline_new_view_btn").click(function(event){event.preventDefault(); grapher.newView()});

        updateLoadViewDropdown(grapher);
        updateSaveViewDropdown(grapher);

        $("#" + grapher.grapherId + "_timeline_save_view_dropdown").click(function(event){
            $("#" + grapher.grapherId + "_timeline_save_view_dropdown_name").doTimeout(100,"focus");
        });

        $("#" + grapher.grapherId + "_timeline_save_view_btn").click(function(event){
            event.preventDefault();
            if ($(event.delegateTarget).hasClass("disabled"))
                return;
            if ($("#" + grapher.grapherId + "_timeline_viewName").text() != newViewName)
                grapher.saveView($("#" + grapher.grapherId + "_timeline_viewName").text());
            else
                $("#" + grapher.grapherId + "_timeline_save_view_dropdown").doTimeout(50,"click");
        });


        $("#" + grapher.grapherId + "_timeline_new_gotoBeginning_button").click(function(event) { event.preventDefault(); grapher.gotoTime("beginning"); });
        $("#" + grapher.grapherId + "_timeline_new_gotoBack_button").click(function(event) { event.preventDefault(); grapher.gotoTime("back"); });
        $("#" + grapher.grapherId + "_timeline_new_gotoForward_button").click(function(event) { event.preventDefault(); grapher.gotoTime("forward"); });
        $("#" + grapher.grapherId + "_timeline_new_gotoEnd_button").click(function(event) { event.preventDefault(); grapher.gotoTime("end"); });
        $("#" + grapher.grapherId + "_timeline_new_zoomOut_button").click(function(event) { event.preventDefault(); grapher.zoomTime("out"); });
        $("#" + grapher.grapherId + "_timeline_new_zoomIn_button").click(function(event) { event.preventDefault(); grapher.zoomTime("in"); });

        // Configure the photo dialog
        $("#" + grapher.grapherId + "_timeline_photo_dialog")['dialog'](
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
        getSources(grapher, function() {
            $("#" + grapher.grapherId + "_timeline_messageArea").hide();
            $("#" + grapher.grapherId + "_timeline_mainContentArea").show();

            if (typeof callback === "function") {
                callback();
            }
        });
    } // init

    function updateLoadViewDropdown(grapher){
        App.loadMustacheTemplate("core/grapher/timelineTemplates.html","loadViewsDropdown",function(template){
            if (VIEWS.availableList.length > 0)
                VIEWS.availableList[0].first = true;
            VIEWS.grapherId = grapher.grapherId;
            var newloadDropdown = $(template.render(VIEWS));
            if (VIEWS.availableList.length > 0)
                delete VIEWS.availableList[0].first;
            delete VIEWS.grapherId;
            $("#" + grapher.grapherId + "_timeline_load_view_submenu").replaceWith(newloadDropdown);

            $("#" + grapher.grapherId + "_timeline_load_view_submenu").click(function(event){
                event.stopImmediatePropagation();
            })

            $("a._timeline_load_link").click(function (event){
                event.preventDefault();
                var viewId = $(event.delegateTarget).attr("viewid");
                var mode = $(event.delegateTarget).hasClass("_timeline_channel_only") ? "channel" : $(event.delegateTarget).hasClass("_timeline_time_only") ? "time" : "all";
                loadViewDialogModeHandler(grapher,viewId,mode);
                $("body").click();
            });
        });
    }

    function updateSaveViewDropdown(grapher){
        App.loadMustacheTemplate("core/grapher/timelineTemplates.html","saveViewDropdown",function(template){
            var params = {viewsPresent: VIEWS.availableList.length != 0,
                          grapherId: grapher.grapherId,
                          availableList: VIEWS.availableList.map(function(view){
                              return {name: view.name,
                                      last_used: App.formatDate(view.last_used,true)};
                          })};
            var newSaveDropDown = $(template.render(params));
            $("#" + grapher.grapherId + "_timeline_save_view_dropdown-submenu").replaceWith(newSaveDropDown);

            $("#" + grapher.grapherId + "_timeline_save_view_dropdown-submenu").click(function (event){
                event.stopImmediatePropagation();
            });

            $("a._timeline_save_view_dropdown_save_link").click(function (event){
                event.preventDefault();
                var viewname = $(event.delegateTarget).attr("viewname");
                grapher.saveView(viewname);
                $("body").click();
            });

            $("#" + grapher.grapherId + "_timeline_save_view_dropdown_save_btn").click(function(event){
                event.preventDefault();
                grapher.saveView($("#" + grapher.grapherId + "_timeline_save_view_dropdown_name").val());
                $("body").click();
            });

            $("#" + grapher.grapherId + "_timeline_save_view_dropdown_name").keypress(function(event){
                if (event.keyCode == 13){
                    event.preventDefault();
                    $("#" + grapher.grapherId + "_timeline_save_view_dropdown_save_btn").click();
                }
            })

        })
    }

    // Check for unsaved changes to timeline and prompt user if needed
    function checkForTimelineChanges(grapher) {
        var newvdata;
        if (VIEWS.data != "") {
            updateViewData(grapher);
            newvdata = JSON.stringify(VIEWS.data);
            if (loadedViewStr != newvdata) {
                hasUnsavedChanges = true;
                return confirm("You have unsaved changes. Do you wish to continue?");
            }
        }
    }

    function getSourceChannelByFullName(full_name){
        var firstPeriod = full_name.indexOf(".");
        return getSourceChannelByName(full_name.substring(0,firstPeriod),full_name.substring(firstPeriod + 1));
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

    function getSources(grapher, callback) {
        SOURCES.getAvailableList(function(data) {

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

                    grapher.sourcesMap["src_" + idx] = {
                        "device_name"  : src["name"],
                        "channel_name" : src.channels[j]["name"],
                        "min"          : src.channels[j]["min"],
                        "max"          : src.channels[j]["max"],
                        "style"        : src.channels[j]["style"],
                        "type"        : src.channels[j]["type"]
                    };

                    if ((src.channels[j].hasOwnProperty("min_time")) &&
                        (src.channels[j].hasOwnProperty("max_time"))) {
                        grapher.sourcesMap["src_" + idx]["min_time"]
                            = src.channels[j]["min_time"];
                        grapher.sourcesMap["src_" + idx]["max_time"]
                            = src.channels[j]["max_time"];
                    }

                    idx += 1;
                }
            }

            // Render add channels area
            App.loadMustacheTemplate("core/grapher/timelineTemplates.html","sourcesList",function(template){
                $("#" + grapher.grapherId + "_timeline_addChannelsArea").html(template.render({
                    sources: SOURCES.availableList
                }));
            });

            // Drag event handler for channels
            /*App.loadMustacheTemplate("core/grapher/timelineTemplates.html","channelTemplate",function(template){
                $("#" + grapher.grapherId + "_timeline_addChannelsArea ul ._timeline_sources_channel").draggable({
                    connectToSortable : "#" + grapher.grapherId + "_timeline_channels",
                    revert: "invalid",
                    helper: function() {
                        // Create mock grapher widget to show while dragging
                        var src = grapher.sourcesMap[this.id];
                        dragSourceId = this.id;
                        var templateValues = {
                            "deviceName"       : src["device_name"],
                            "channelName"      : src["channel_name"],
                            "plotElementId"    : "_timeline_channel_helper",
                            "channelElementId" : "_timeline_plot_helper",
                            "yAxisElementId"   : "_timeline_yAxis_helper",
                            "showDeleteBtn"    : grapher.showDeleteBtn,
                            "grapherId"        : grapher.grapherId
                        };
                        return template.render(templateValues);
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
            });*/
            $("#" + grapher.grapherId + "_timeline_addChannelsArea ul ._timeline_sources_channel").disableSelection();

            // Create new grapher widget if source receives a click
            $("#" + grapher.grapherId + "_timeline_addChannelsArea ul li ._timeline_sources_channel").click(function() {
                var c = grapher.sourcesMap[this.id];
                grapher.addChannel(c, null);
                //$("#" + grapher.grapherId + "_timeline_channelsWrapper").animate({scrollTop:0}, 500);
            });

            // Add channels pane reset button
            $("#" + grapher.grapherId + "_timeline_addChannelsArea #_timeline_sources_find_btn").click(function() {
                $("#" + grapher.grapherId + "_timeline_addChannelsArea input[type=text]").val("");

                addPaneRestoreState(grapher);
                return false;
            });

            // Add channels pane search functionality
            $("#" + grapher.grapherId + "_timeline_addChannelsArea input[type=text]").keyup(function(event) {
                var search_str = $("#" + grapher.grapherId + "_timeline_addChannelsArea input[type=text]").val();
                var regexp = new RegExp(search_str, 'i');

                if (search_str.length === 0) {
                    addPaneRestoreState(grapher);
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

                $("#" + grapher.grapherId + "_timeline_addChannelsArea #_timeline_sources_list ._timeline_sources_channel").each(function() {
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

    function addPaneRestoreState(grapher) {
        var i = 0;
        var l = addPaneChannelsState.length;

        $("#" + grapher.grapherId + "_timeline_addChannelsArea #_timeline_sources_list ._timeline_sources_channel").each(function() {
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

    Grapher.prototype.newView = function(start, end) {
        if (start == null || end == null){
            end = new Date().getTime()/1000.0;
            start = end - 86400;
        }

        VIEWS.data = {
            "name" : newViewName,
            "v2" : {
                "x_axis" : {
                    "min" : start,
                    "max" : end
                },
                y_axes : []
            }
        };
        loadedViewStr = JSON.stringify(VIEWS.data);
        hasUnsavedChanges = true;
        renderView(this, VIEWS.data);

        if ($("#" + this.grapherId + "_timeline_addChannelsArea").css("display") === "none") {
            this.toggleAddChannelsPane();
        }
        $(window).resize();
    }

    Grapher.prototype.loadView = function(id, mode, callback) {
        $("#" + this.grapherId + "_timeline_save_view_btn").addClass("disabled");
        var grapher = this;
        VIEWS.load(id, function(data) {
            loadedViewStr = JSON.stringify(data);
            hasUnsavedChanges = false;
            renderView(grapher, data, mode);
            if (typeof callback === "function") {
                callback();
            }
        });
    }

    function loadViewWithTimeRange(grapher, id, min, max, callback) {
        $("#_timeline_save_view_btn").addClass("disabled");
        VIEWS.load(id, function(data) {
            loadedViewStr = JSON.stringify(data);
            hasUnsavedChanges = true;
            data["v2"]["x_axis"]["min"] = min;
            data["v2"]["x_axis"]["max"] = max;
            renderView(grapher, data);
            if (typeof callback === "function") {
                callback();
            }
        });
    }

    // Save view then load saved view
    Grapher.prototype.saveView = function(name) {
        updateViewData(this);
        var grapher = this;
        VIEWS.save(name, function(data, id) {
            loadedViewStr = JSON.stringify(VIEWS.data);
            hasUnsavedChanges = false;
            updateLoadViewDropdown(grapher);
            updateSaveViewDropdown(grapher);
            grapher.loadView(id);
        });
    }

    // Load all channels associated with device_name into a new view
    Grapher.prototype.loadSource = function(device_name, callback) {
        var grapher = this;
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
                    "channel_height" : 67
                });
            }

            loadedViewStr = JSON.stringify(VIEWS.data);
            hasUnsavedChanges = true;
            renderView(grapher, VIEWS.data);

            if ($("#_timeline_addChannelsArea").css("display") !== "none") {
                grapher.toggleAddChannelsPane();
            }

            if (typeof callback === "function") {
                callback();
            }
        });
    }

    function loadViewDialogModeHandler(grapher, view_id, mode) {
        var min, max;

        // Cancel load if user clicks cancel on load dialog
        if (checkForTimelineChanges(grapher) === false) {
            return false;
        }

        // Update x-axis of existing view
        if (mode === "time") {
            grapher.loadView(view_id, mode);
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

            loadViewWithTimeRange(grapher, view_id, min, max, function() {
                TOOLS.resizeHandler();
            });
            return false;
        }

        // Otherwise, load the entire view
        grapher.loadView(view_id);
        return true;
    }

    Grapher.prototype.toggleAddChannelsPane = function() {
        if (!this.showFullControls)
            return;
        var area = $("#" + this.grapherId + "_timeline_addChannelsArea");
        if (area.css("display") === "none") {
            $("#" + this.grapherId + "_timeline_add_channels_btn").addClass("active");
            area.show();
            TOOLS.resizeElementHeight($("#" + this.grapherId + "_timeline_addChannelsArea #_timeline_sources_list"));
        }
        else {
            $("#"  + this.grapherId + "_timeline_add_channels_btn").removeClass("active");
            area.hide();
        }

        // call the resize handler to ensure that the grapher gets resized
        TOOLS.resizeHandler();

        return false;
    }

    Grapher.prototype.toggleDetailsPane = function() {
        var area = $("#" + this.grapherId + "_timeline_detailsArea");
        if (area.css("display") === "none") {
            $("#" + this.grapherId + "_timeline_show_details_btn").addClass("active");
            area.show();
        }
        else {
            $("#" + this.grapherId + "_timeline_show_details_btn").removeClass("active");
            area.hide();
        }

        // call the resize handler to ensure that the grapher gets resized
        TOOLS.resizeHandler();

        return false;
    }

    Grapher.prototype.removeChannel = function(channel){
        var firstPeriod = channel.indexOf(".");
        var deviceName = channel.substring(0,firstPeriod);
        var channelName = channel.substring(firstPeriod + 1);

        var channelElement = $("#" + this.grapherId +"_timeline_channel_" + deviceName + "_" + channelName);
        if (channelElement.length != 0){
            var channelElementId = channelElement.parent().attr("id");
            this.plotContainersMap[channelElementId].removePlot(this.plotsMap[channelElementId]);
            $(channelElement.parent()).remove();
        }
    }

    // Add new channel to target
    Grapher.prototype.addChannel = function(channel, target) {
        var grapher = this;
        if (typeof channel == "string"){
            if (!SOURCES.initialized){
                $.doTimeout(33,function(){
                    console.log('timeout');
                    grapher.addChannel(channel,target);
                });
                return;
            }
            var channel = getSourceChannelByFullName(channel);
            if (channel == null)
                return;
            var channel = grapher.sourcesMap[channel.id];
        }

        App.loadMustacheTemplate("core/grapher/timelineTemplates.html","channelTemplate",function(template){
            var max_time;

            // VERY important to clone the given channel here!
            channel = TOOLS.clone(channel);

            id = channelIdx;
            channelIdx += 1;

            var channelElementId = grapher.grapherId + "_timeline_channel_" + id;
            var plotElementId = grapher.grapherId + "_timeline_plot_" + id;
            var yAxisElementId = grapher.grapherId + "_timeline_yAxis_" + id;
            var templateValues = {
                "deviceName"       : channel["device_name"],
                "channelName"      : channel["channel_name"],
                "channelHeight"    : channel["channel_height"],
                "channelTabHeight" : channel["channel_height"] + CHANNEL_PADDING,
                "CHANNEL_PADDING"  : CHANNEL_PADDING,
                "plotId"           : id,
                "plotElementId"    : plotElementId,
                "channelElementId" : channelElementId,
                "yAxisElementId"   : yAxisElementId,
                "showDeleteBtn"    : grapher.showDeleteBtn,
                "grapherId"        : grapher.grapherId
            };

            // Render template

            var html = template.render(templateValues);
            if (target == null || target == undefined || target == "") {
                $("#" + grapher.grapherId + "_timeline_channels").prepend(html);
            }
            else {
                $(target).replaceWith(html);
            }

            // Make sure the view name doesn't overflow the bounds of the box
            $("#" + channelElementId + "-timeline-channel-name")
                .html(channel["channel_name"])
                .shorten();

            var yMin = channel.min;
            var yMax = channel.max;
            if (yMin == yMax){
                yMin -= 1;
                yMax += 1;
            }

            var yAxis = new NumberAxis(yAxisElementId, "vertical", {
                "min" : yMin,
                "max" : yMax
            });

            // Now that yAxis is initialized, if this is a new view,
            // set xAxis range to be the latest 24 hrs of data from the
            // first added channel
            /*if ((VIEWS.data["name"] == newViewName) &&
                channel.hasOwnProperty("max_time") &&
                ($("#_timeline_channels ._timeline_channel").length == 0)) {
                max_time = channel["max_time"];
                grapher.dateAxis.setRange(max_time - 86400.0, max_time);
            }*/

            var plot = null;
            if (("photo" == channel['type']) || "photo" == channel["channel_name"] || "photos" == channel["channel_name"]) {
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
                plot = new PhotoSeriesPlot(photoDatasource(App.getUID(), channel["device_name"], channel["channel_name"], tags, willJoinUsingAnd),
                    grapher.dateAxis,
                    yAxis,
                    App.getUID(),
                    channel["style"]);
                plot.addDataPointListener(photoDataPointListener(grapher, channel, channelElementId));
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
                //			var commentDatasource = commentDatasource(App.getUID(),
                //			channel["device_name"],
                //			tags,
                //			willJoinUsingAnd);
                //			plot = new CommentSeriesPlot(commentDatasource,
                //			dateAxis,
                //			yAxis,
                //			App.getUID(),
                //			channel["style"]);
                //			plot.addDataPointListener(commentDataPointListener(channelElementId));
            } else {
                // Set up the plot and axes for this channel using the grapher API
                plot = new DataSeriesPlot(channelDatasource(App.getUID(), channel["device_name"], channel["channel_name"]),
                    grapher.dateAxis,
                    yAxis,
                    channel["style"]);
                plot.addDataPointListener(function(pointObj, sourceInfo){dataPointListener(grapher,pointObj, sourceInfo)});
            }

            var plotContainer = new PlotContainer(plotElementId, false, [plot]);

            grapher.channelsMap[channelElementId] = channel;
            grapher.plotsMap[channelElementId] = plot;
            grapher.plotContainersMap[channelElementId] = plotContainer;
            plotContainers.push(plotContainer);

            // Gear button
            $("#" + channelElementId + "_btnGear").unbind("click").click(function(event) {
                event.preventDefault();
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
                .addClass("_timeline_channel_" + channel.device_name + "_" + channel.channel_name + "_delete_btn")
                .unbind('click')
                .click(function(event) {
                    event.preventDefault();
                    if (!grapher.showDeleteBtn)
                        return;
                    var channelElement = $(this).parents("._timeline_channel").parent();
                    plotContainer.removePlot(plot);
                    $(channelElement).remove();
                });

            // Drag to resize
            $("#" + channelElementId + "_dragArea").unbind("mousedown").mousedown(function() {
                var channelElement = $(this).parents("._timeline_channel").parent();
                // Extract plotId from channelElement id attribute
                dragAreaOnMouseDown(grapher, channelElement.attr("id").slice(18 + grapher.grapherId.length));
            });

            // Style configuration
            if (plot instanceof DataSeriesPlot) {

                // Define a function which handles updating a channel's style
                // whenever anything in the channel configuration changes
                var updateDataSeriesPlotChannelConfig = function() {
                    var channelElement = $(this).parents("._timeline_channel").parent();
                    var plot = grapher.plotsMap[channelElement.attr("id")];

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
                            getSources(grapher);
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
                $("#" + channelElementId + " #" + channelElementId + "_btnShowAllY").click(function(event) {
                    event.preventDefault();
                    var plot = grapher.plotsMap[channelElementId];
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

                $("#" + channelElementId + " #" + channelElementId + "_btnShowAllY").click(function(event){
                    event.preventDefault();
                });

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
                    var plot = grapher.plotsMap[channelElement.attr("id")];
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
                        var filterHtml = App.fetchCompiledMustacheTemplate("core/grapher/timelineTemplates.html","_timeline_channel_tab_filter_template").render({"value":userSelectedTags.join(", ")});
                        $("#" + channelElementId + "-timeline-channel-filter").html(filterHtml).shorten();
                    } else {
                        $("#" + channelElementId + "-timeline-channel-filter").text('').hide();
                    }

                    //console.log("NEW JSON: " + JSON.stringify(newStyle,null,3));

                    plot.setStyle(newStyle);

                    plot.setDatasource(photoDatasource(App.getUID(),
                        channel["device_name"],
                        channel["channel_name"],
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
                        var tagHtml = App.fetchCompiledMustacheTemplate("core/grapher/timelineTemplates.html","_timeline_photo_dialog_tags_editor_tag_template").render({"value" : value});
                        $("#" + channelElementId + "-photo-tags-filter").append(tagHtml);
                    });
                } else {
                    var tagHtml = App.fetchCompiledMustacheTemplate("core/grapher/timelineTemplates.html","_timeline_photo_dialog_tags_editor_tag_template").render({"value" : ""});
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

            // Force initial resize
            resizePlot(grapher, id, 0);

            // Update scroll area
            TOOLS.resizeHandler();

            return html;
        });
    }

    // Fetch ordering and latest values from channelsMap and
    // update VIEWS.data
    function updateViewData(grapher) {
        var i, l;
        var channelIds;
        var xAxis, yAxis, plot;
        var channel, yAxes = [];

        channelIds = $("#" + grapher.grapherId + "_timeline_channels").sortable('toArray');

        l = channelIds.length;

        // Update xAxis min/max
        if (l > 0) {
            plot = grapher.plotsMap[channelIds[0]];
            xAxis = plot.getHorizontalAxis();
            VIEWS.data["v2"]["x_axis"]["min"] = xAxis.getMin();
            VIEWS.data["v2"]["x_axis"]["max"] = xAxis.getMax();
        }

        // Update yAxis min/max, order, height
        for (i = 0; i < l; i++) {
            plot = grapher.plotsMap[channelIds[i]];
            yAxis = plot.getVerticalAxis();
            channel = grapher.channelsMap[channelIds[i]];
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
        if ($("#" + grapher.grapherId + "_timeline_addChannelsArea").css("display") === "none") {
            VIEWS.data["v2"]["show_add_pane"] = false;
        }
        else {
            VIEWS.data["v2"]["show_add_pane"] = true;
        }
    }

    // Render view to page
    function renderView(grapher, view, mode) {
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
            if (typeof grapher.dateAxis === "undefined") {
                alert("Existing view not found");
                return;
            }
            $("#" + grapher.grapherId + "_timeline_save_view_btn").removeClass("disabled");
            $("#" + grapher.grapherId + "_timeline_add_channels_btn").unbind('click')
                .click(function(){grapher.toggleAddChannelsPane(); return false;})
                .removeClass("disabled");
            $("#" + grapher.grapherId + "_timeline_show_details_btn").unbind('click')
                .click(function(){grapher.toggleDetailsPane(); return false;})
                .removeClass("disabled");

            grapher.dateAxis.setRange(view["v2"]["x_axis"]["min"],
                view["v2"]["x_axis"]["max"]);
            return;
        }
        else {
            // Reset view specific data
            grapher.channelsMap = {};
            grapher.plotsMap = {};
            grapher.plotContainersMap = {};
            plotContainers = [];

            // Reset colorpicker color cycling
            jQuery.fn.colorPicker.resetGetNextColor();

            // Clear the channels area
            $("#" + grapher.grapherId + "_timeline_dateAxis").empty();
            $("#" + grapher.grapherId + "_timeline_channels").empty();
            $("#" + grapher.grapherId + "_timeline_dateAxisAndChannelsArea").show();
            $("#" + grapher.grapherId + "_timeline_channelsArea").show();

            // Set view name and add click handlers for various buttons
            $("#" + grapher.grapherId + "_timeline_viewName").html(view["name"]).shorten();
            // TODO: only enable this when the view has changed
            $("#" + grapher.grapherId + "_timeline_save_view_btn").removeClass("disabled");
            $("#" + grapher.grapherId + "_timeline_add_channels_btn").unbind('click')
                .click(function(){grapher.toggleAddChannelsPane(); return false;})
                .removeClass("disabled");
            $("#" + grapher.grapherId + "_timeline_show_details_btn").unbind('click')
                .click(function(){grapher.toggleDetailsPane(); return false;})
                .removeClass("disabled");

            // Show/hide add channels pane
            if ((typeof view["v2"]["show_add_pane"] === "undefined") ||
                (view["v2"]["show_add_pane"] === false)) {
                if ($("#" + grapher.grapherId + "_timeline_addChannelsArea").css("display") !== "none") {
                    grapher.toggleAddChannelsPane();
                }
            }
            else {
                if ($("#" + grapher.grapherId + "_timeline_addChannelsArea").css("display") === "none") {
                    grapher.toggleAddChannelsPane();
                }
            }

            // Create x-axis
            grapher.dateAxis = new DateAxis(grapher.grapherId + "_timeline_dateAxis", "horizontal", {
                "min" : view["v2"]["x_axis"]["min"],
                "max" : view["v2"]["x_axis"]["max"]
            });
            grapher.dateAxis.addAxisChangeListener(function() {
                var center = (grapher.dateAxis.getMin() + grapher.dateAxis.getMax()) / 2.0;
                var utcOffsetHrs = new Date(center * 1000).getTimezoneOffset() / -60;
                // 60 mins/hour, and offset is backwards of the convention
                // e.g. Pittsburgh's date.getTimezoneOffset() is 240 or 300 depending on time of year

                var utcOffset = "UTC";

                if (utcOffsetHrs < 0)
                    utcOffset = "UTC - " + (-1 * utcOffsetHrs);
                else if (utcOffsetHrs > 0)
                    utcOffset = "UTC + " + utcOffsetHrs;

                $("#" + grapher.grapherId + "_timeline_dataPointTimeZoneLabel").html(utcOffset);
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

                grapher.addChannel(yAxes[i], null);
            }
        }
        $(window).resize();//fixes issue of no date axis when window no channels are in view.
    }

    Grapher.prototype.getCurrentTimeUnit = function(){
        var range = this.dateAxis.getMax() - this.dateAxis.getMin();
        if (range > 364 * 24 * 3600)
            return "year";
        if (range > 27 * 24 * 3600)
            return "month";
        if (range > 6 * 24 * 3600)
            return "week";
        return "date";
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
                        view['v2']['y_axes'][i]['style']['styles'] = yAxes[i]['style']['style-types'];
                    }
                }
            }
            return true;
        }

        /*** Upgrade view data to v2 format ***/

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

    function createPhotoDialogCache(channel, channelFilterTags, isAndJoin) {
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

                    var url = "/api/bodytrack/photos/" + App.getUID() + "/" + channel['device_name'] + "." + channel['channel_name'] + "/" + currentPhotoTimestamp + "/" + cache.NUM_PHOTOS_TO_FETCH;
                    var urlParams = {
                        "isBefore" : shouldLoadPreviousNeighbor
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
                                            "timestampString" : photo['end'],
                                            "url"             : photo['url'],
                                            "thumbnails"      : photo['thumbnails'],
                                            "orientation"     : photo['orientation']
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

                                // Create the initial cache.  We do this by first slicing off the first element
                                // of the array containing the following photos (since it's a dupe
                                // of the last element in the reverse precedingPhotosMetadata
                                // array) and then concatenating with the preceding photos.
                                cache.photos = precedingPhotosMetadata.concat(followingPhotosMetadata.slice(1));

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

    function dataPointListener(grapher, pointObj, sourceInfo) {
        if (pointObj) {
            App.loadMustacheTemplate("core/grapher/timelineTemplates.html","dataPointValueLabel",function (template){
                $("#" + grapher.grapherId + "_timeline_dataPointValueLabel").html(template.render(pointObj));
            });
        } else {
            $("#" + grapher.grapherId + "_timeline_dataPointValueLabel").html("");
        }
    }

    function loadLogrecMetadata(logrecId, callbacks) {
        logrecId = TOOLS.parseInt(logrecId, -1);
        if (logrecId >= 0) {

            var url = "/bodytrack/users/" + App.getUID() + "/logrecs/" + logrecId + "/get";

            TOOLS.loadJson(url, {}, callbacks);
        }
    }

    function photoDataPointListener(grapher, channel, channelElementId) {
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
                var photoCache = createPhotoDialogCache(channel, channelFilterTags, isAndJoin);

                var createPhotoDialog = function(photoId, timestamp, completionCallback) {

                    var thumbnails = photoCache.getPhotoMetadata(photoId)['thumbnails'];
                    // This assumes the thumbnails are ordered from smallest to largest.  Might be better to eventually search for the largest.
                    var mediumResImageUrl = (thumbnails != null && thumbnails.length > 0) ? thumbnails[thumbnails.length - 1]['url'] : photoCache.getPhotoMetadata(photoId)['url'];
                    var highResImageUrl = photoCache.getPhotoMetadata(photoId)['url'];
                    var photoOrientation = photoCache.getPhotoMetadata(photoId)['orientation'];
                    if (typeof photoOrientation === 'undefined' || photoOrientation == null) {
                        photoOrientation = 1;
                    }
                    var highResOrientationCssClass = "_timeline_photo_dialog_image_orientation_" + photoOrientation;
                    var photoDialogTemplate = App.fetchCompiledMustacheTemplate("core/grapher/timelineTemplates.html","_timeline_photo_dialog_template");
                    var photoDialogHtml = photoDialogTemplate.render({"photoUrl" : mediumResImageUrl});
                    $("#" + grapher.grapherId + "_timeline_photo_dialog").html(photoDialogHtml);

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
                        var formContainer = $("#_timeline_photo_dialog_form_container");
                        if ($("#_timeline_photo_dialog_form_container").is(":visible")) {
                            // fade out the form and show the hi-res version of the image
                            formContainer.fadeOut(100, function() {
                                var imageAspectRatio = theImage.width() / theImage.height();
                                var imageHeight = $("body").height() - 60;
                                var imageWidth = imageAspectRatio * imageHeight;

                                // make sure the image isn't too wide now
                                if (imageWidth > $("body").width()) {
                                    imageWidth = $("body").width() - 100;
                                    imageHeight = imageWidth / imageAspectRatio;
                                }

                                theImage.attr("src",highResImageUrl);
                                if (photoOrientation <= 4) {
                                    theImage.width(imageWidth).height(imageHeight);
                                } else {
                                    theImage.width(imageHeight).height(imageWidth);
                                }
                                theImage.removeClass("_timeline_photo_dialog_image_orientation_1");
                                theImage.addClass(highResOrientationCssClass);
                                $("._timeline_photo_dialog_photo_table").width(Math.max(imageHeight,imageWidth)).height(imageHeight);
                                centerPhotoDialog(grapher);
                            });
                        } else {
                            // fade the form back in and show the medium-res version of the image
                            formContainer.fadeIn(100, function() {

                                theImage.attr("src", mediumResImageUrl);

                                var imageHeight = 300;
                                var imageWidth = 300;
                                var imageAspectRatio = (photoOrientation <= 4 ) ? theImage.width() / theImage.height() : theImage.height() / theImage.width();
                                if (imageAspectRatio > 1) {
                                    imageHeight = Math.round(imageWidth / imageAspectRatio);
                                } else {
                                    imageWidth = imageAspectRatio * imageHeight;
                                }

                                theImage.width(imageWidth).height(imageHeight);
                                $("._timeline_photo_dialog_photo_table").width(300).height(300);
                                centerPhotoDialog(grapher);
                                theImage.removeClass(highResOrientationCssClass);
                                theImage.addClass("_timeline_photo_dialog_image_orientation_1");
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
                        var photoMetadataForm = App.fetchCompiledMustacheTemplate("core/grapher/timelineTemplates.html","_timeline_photo_dialog_form_template").render({});
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
                            $("#" + grapher.grapherId + "_timeline_photo_dialog")['dialog']("option", "closeOnEscape", false);
                        });
                        $("#_timeline_photo_dialog_comment").blur(function() {
                            $("#" + grapher.grapherId + "_timeline_photo_dialog")['dialog']("option", "closeOnEscape", true);
                        });
                        $("#_timeline_photo_dialog_comment").keyup(setEnabledStateOfRevertAndSaveButtons);

                        // add the tags, if any
                        if ($.isArray(tags) && tags.length > 0) {
                            $.each(tags,
                                function(index, value) {
                                    var tagHtml =App.fetchCompiledMustacheTemplate("core/grapher/timelineTemplates.html","_timeline_photo_dialog_tags_editor_tag_template").render({"value" : value});
                                    $("#_timeline_photo_dialog_tags_editor").append(tagHtml);
                                });
                        } else {
                            var tagHtml = App.fetchCompiledMustacheTemplate("core/grapher/timelineTemplates.html","_timeline_photo_dialog_tags_editor_tag_template").render({"value" : ""});
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
                            $("#" + grapher.grapherId + "_timeline_photo_dialog")['dialog']("option", "closeOnEscape", false);
                        });
                        $('#_timeline_photo_dialog_tags_editor').bind('tabToNextElement', function(event) {
                            $("#" + grapher.grapherId + "_timeline_photo_dialog")['dialog']("option", "closeOnEscape", true);

                            $("#_timeline_photo_dialog_tags_editor_tabhelper_post_proxy_forward").focus();
                            return false;
                        });
                        $('#_timeline_photo_dialog_tags_editor').bind('tabToPreviousElement', function(event) {
                            $("#" + grapher.grapherId + "_timeline_photo_dialog")['dialog']("option", "closeOnEscape", true);

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
                                url      : "/bodytrack/users/" + App.getUID() + "/logrecs/" + photoId + "/set",
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
                                centerPhotoDialog(grapher);
                            });
                    });

                // Open the dialog
                $("#" + grapher.grapherId + "_timeline_photo_dialog").html(App.fetchCompiledMustacheTemplate("core/grapher/timelineTemplates.html","_timeline_photo_dialog_loading_template").render({}));
                $("#" + grapher.grapherId + "_timeline_photo_dialog")['dialog']('open');
            }
        };
    }

    function centerPhotoDialog(grapher) {
        // center the dialog
        $("#" + grapher.grapherId + "_timeline_photo_dialog")['dialog']("option", "position", 'center');
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

            var url = "/api/bodytrack/users/" + App.getUID() + "/channels/" + encodeURIComponent(channel["device_name"]) + "." + encodeURIComponent(channel["channel_name"]) + "/set";
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

    function resizePlot(grapher, plotId, dy) {
        var channelElementId = grapher.grapherId + "_timeline_channel_" + plotId;
        var plotElementId = grapher.grapherId + "_timeline_plot_" + plotId;
        var yAxisElementId = grapher.grapherId + "_timeline_yAxis_" + plotId;

        var container = grapher.plotContainersMap[channelElementId];
        var cPlaceholder = $("#" + container.getPlaceholder());
        var containerW = cPlaceholder.width();
        var containerH = cPlaceholder.height();

        var plot = grapher.plotsMap[channelElementId];
        var yAxis = plot.getVerticalAxis();
        var yAixsW = $("#" + yAxis.getPlaceholder()).width();

        var dragAreaH = $("._timeline_dragArea").height() - CHANNEL_PADDING;

        if ((dy > 0) || (Math.abs(dy) < containerH)) {
            // There is a min height of 67, which is taken from the
            // min height of the channel label
            if (containerH + dy + dragAreaH < 67) {
                dy = 67 - containerH - dragAreaH;
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
            $("#_timeline_channelTab_" + plotId).height(
                containerH + dy + CHANNEL_PADDING);

            // Update the view data to match the new channel height
            if ((!!VIEWS.data) && (!!VIEWS.data["v2"])
                    && (!!VIEWS.data["v2"]["y_axes"])
                && (VIEWS.data["v2"]["y_axes"].length > plotId)) {
                VIEWS.data["v2"]["y_axes"][plotId]["channel_height"] =
                containerH + dy;
            }
        }

        return false;
    };

    function dragAreaOnMouseDown(grapher, plotId) {
        var mostRecentY = null;
        var resizeTimer = null;
        var dylist = [];

        var mouseup = null;
        var mousemove = null;
        var updatePlotSize = function() {
            if (dylist.length > 0) {
                var dy = dylist[dylist.length - 1];
                resizePlot(grapher, plotId, dy);
                mostRecentY = mostRecentY + dy;
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
            resizePlot(grapher, plotId, event.pageY - mostRecentY);
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

    Grapher.prototype.gotoTime = function(action) {
        var xAxis = this.dateAxis;
        if (!xAxis) {
            console.log("Missing date axis: cannot goto another time");
            return false;
        }
        var xMin = xAxis.getMin();
        var xMax = xAxis.getMax();
        var xWidth = xMax - xMin;

        if (action == "beginning") {
            var minTime = Number.MAX_VALUE;
            for (var channelKey in this.channelsMap) {
                var channel = this.channelsMap[channelKey];
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
            for (channelKey in this.channelsMap) {
                channel = this.channelsMap[channelKey];
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

        repaintAllPlots(this);

        return false;
    }

    Grapher.prototype.zoomTime = function(action) {
        var xAxis = this.dateAxis;
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

        repaintAllPlots(this);

        return false;
    }

    Grapher.prototype.setRange = function(start, end) {
        var grapher = this;
        _performAfterLoad(grapher, function() {
            grapher.dateAxis.setRange(start, end);
            repaintAllPlots(grapher);
        });
    }

    function repaintAllPlots(grapher) {
        for (var plotKey in grapher.plotsMap) {
            var plot = grapher.plotsMap[plotKey];
            if (plot) {
                plot.setStyle(plot.getStyle());
            }
        }
    }

    function setup(grapher) {

        $(window).resize(function(){
            clearTimeout(BTCore.TOOLS.resizeTimer);
            BTCore.TOOLS.resizeTimer = setTimeout(BTCore.TOOLS.resizeHandler, 100);
        });
        APP.init(function() {
            init(grapher, function() {
                grapher.newView();
            });
        });
    }

    Grapher.prototype.setZoom = function(seconds){
        var curMin = this.dateAxis.getMin();
        var curMax = this.dateAxis.getMax();
        var diff = curMax - curMin;
        var changeAmt = (seconds - diff)/2;
        this.dateAxis.setRange(curMin - changeAmt, curMax + changeAmt);
    }

    Grapher.prototype.updateViews = function(){
        updateSaveViewDropdown(this);
        updateLoadViewDropdown(this);
    }

    return Grapher;
});