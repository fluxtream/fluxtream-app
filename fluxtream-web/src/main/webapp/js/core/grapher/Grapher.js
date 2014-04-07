define(["core/grapher/BTCore","applications/calendar/tabs/list/ListUtils", "core/Tooltip","applications/calendar/tabs/photos/PhotoUtils"], function(BTCore,ListUtils,Tooltip,PhotoUtils) {

    var Grapher = function(parentElement, options) {
        if (options == null) options = {};
        parentElement = $(parentElement);
        this.plotContainersMap = {}; // maps DOM element ID to plot container
        this.sourcesMap = {}; // maps DOM element ID to available source
        this.channelsMap = {}; // maps DOM element ID to view's y_axes
        this.plotsMap = {}; // maps DOM element ID to grapher widget
        this.grapherId = new Date().getTime() + "-" + Math.round(Math.random()*10000000);
        this.plotContainers = [];    // array of plot containers
        this.currentTooltip = null; //holds a reference to the current tooltip object
        var grapher = this;
        for (var param in options)
            grapher[param] = options[param];
        if (grapher.onLoadActions == null)
            grapher.onLoadActions = [];
        if (grapher.loadViewOverride == null)
            grapher.loadViewOverride = function(){return false;};
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
        App.addDataUpdatesListener("grapherListener-" + this.grapherId, function(data){
            grapher.onDataUpdate(data);
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


    var hasUnsavedChanges    = false; // used by unsaved changes dialog handler
    var loadedViewStr        = "";    // JSON string of loaded view
    var addPaneChannelsState = [];    // add channels pane channel visibility
    var CHANNEL_PADDING      = 0;     // Pixels between plot and drag area

    var connectorEnabled;

    function _performAfterLoad(grapher, callback) {
        if (grapher.loaded) {
            callback();
        } else {
            grapher.onLoadActions.push(callback);
        }
    }

    var channelTemplate;

    function makeChannelsSortable(grapher){
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
        TOOLS.onWindowResizeResizeElementHeight("#" + grapher.grapherId + "_timeline_addChannelsArea ._timeline_sources_list");
        TOOLS.onWindowResizeResizeElementHeight("#" + grapher.grapherId + "_timeline_channelsWrapper");
        TOOLS.onWindowResizeListener(function() {
            // borderOffset is used to account for:
            // * 1 pixel of border on #_timeline_channels
            // * 1 pixel of border on ._timeline_channel
            var borderOffset = 2;

            // Could also use ._timeline_channeltd, but $("._timeline_gotozoom").width() returns 0
            // whenever there are no channels
            var widthOfAreaLeftOfPlotContainer = $("#" + grapher.grapherId + "_timeline_gotozoom").width() + borderOffset;
            // TODO: Find a good way to get the ._timeline_yaxistd width even when with no channels
            var widthOfAreaRightOfPlotContainer = $("._timeline_yaxistd").width() + borderOffset;

            // the .plotContainer has a 1 pixel border around it, so set this to 2 to account for the left and right sides
            var widthOfPlotContainerLeftAndRightBorder = 2;

            // compute the desired size of plot containers based on the current width of the _timeline_channelsArea (which
            // will automatically shrink if the Add Channels and/or Details pane is visible, so we don't explicitly need
            // to account for them here).
            var plotContainerWidth = $("#" + grapher.grapherId + "_timeline_channelsArea").width() - widthOfAreaLeftOfPlotContainer - widthOfAreaRightOfPlotContainer - widthOfPlotContainerLeftAndRightBorder - 20;
            if (plotContainerWidth < 1) plotContainerWidth = 1; //sometimes the calculated value can be negative. This can cause crashes in IE so we force it to be > 0

            // resize plot containers
            var plotContainerEventId = SequenceNumber.getNext();
            for (var i = 0; i < grapher.plotContainers.length; i++) {
                var plotContainerHeight = $("#" + grapher.plotContainers[i].getPlaceholder()).height();
                grapher.plotContainers[i].setSize(plotContainerWidth, plotContainerHeight, plotContainerEventId);
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
        });

        // Make the channel list sortable
        App.loadMustacheTemplate("core/grapher/timelineTemplates.html","channelTemplate",function(template){
            channelTemplate = template;
            makeChannelsSortable(grapher);
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

        $("#" + grapher.grapherId + "_timeline_export_to_csv").click(function(event){event.preventDefault(); grapher.exportToCSV();});

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
            if (false && loadedViewStr != newvdata) {//TODO: fix this, it's disabled for now...
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
            grapher.sourcesMap = {};
            for (i = 0; i < l; i++) {
                src = SOURCES.availableList[i];
                m = src.channels.length;
                for (j = 0; j < m; j++) {
                    src.channels[j]["id"] = "src_" + idx;

                    grapher.sourcesMap["src_" + idx] = {
                        "device_name"      : src["name"],
                        "channel_name"     : src.channels[j]["name"],
                        "object_type_name" : (typeof src.channels[j]["objectTypeName"] === 'undefined' ? src.channels[j]["name"] : src.channels[j]["objectTypeName"]),
                        "min"              : src.channels[j]["min"],
                        "max"              : src.channels[j]["max"],
                        "style"            : src.channels[j]["style"],
                        "time_type"        : src.channels[j]["time_type"],
                        "type"             : src.channels[j]["type"],
                        "usingDefaultStyle": true
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
                SOURCES.initialized = true;
            }

            App.loadMustacheTemplate("core/grapher/timelineTemplates.html","sourceListItem",function(sourceListItemTemplate){
                App.loadMustacheTemplate("core/grapher/timelineTemplates.html","channelListItem",function(channeListItemTemplate){
                    var addChannelsArea = $("#" + grapher.grapherId + "_timeline_addChannelsArea");
                    var deviceRemoveList = addChannelsArea.find("._timeline_sources_list").children();;
                    for (var i = 0, li = SOURCES.availableList.length; i < li; i++){
                        var device = SOURCES.availableList[i];
                        deviceRemoveList = deviceRemoveList.filter(":not(._timeline_source_" + device.name + ")");
                        var sourceListItem = addChannelsArea.find("._timeline_source_" + device.name);
                        if (sourceListItem.length == 0){
                            sourceListItem = $(sourceListItemTemplate.render(device));
                            addChannelsArea.find("._timeline_sources_list").append(sourceListItem);
                        }
                        var channelList = sourceListItem.find("._timeline_channel_list");
                        var channelRemoveList = channelList.children();
                        for (var j = 0, lj = device.channels.length; j < lj; j++){
                            var channel = device.channels[j];
                            channelRemoveList = channelRemoveList.filter(":not(._timeline_channel_" + channel.name + ")");
                            var channelListItem = channelList.find("._timeline_channel_" + channel.name);
                            if (channelListItem.length == 0){
                                channelListItem = $(channeListItemTemplate.render(channel));
                                channelList.append(channelListItem);
                            }
                        }
                        channelRemoveList.remove();

                    }
                    deviceRemoveList.remove();

                    //TODO: remove leftover elements for sources that weren't accounted for

                    $("#" + grapher.grapherId + "_timeline_addChannelsArea ul ._timeline_sources_channel").disableSelection();

                    // Create new grapher widget if source receives a click
                    $("#" + grapher.grapherId + "_timeline_addChannelsArea ul li ._timeline_sources_channel").unbind("click").click(function() {
                        var c = grapher.sourcesMap[this.id];
                        grapher.addChannel(c, null);
                        //$("#" + grapher.grapherId + "_timeline_channelsWrapper").animate({scrollTop:0}, 500);
                    });

                    // Add channels pane reset button
                    $("#" + grapher.grapherId + "_timeline_addChannelsArea #_timeline_sources_find_btn").unbind("click").click(function() {
                        $("#" + grapher.grapherId + "_timeline_addChannelsArea input[type=text]").val("");

                        addPaneRestoreState(grapher);
                        return false;
                    });

                    // Add channels pane search functionality
                    $("#" + grapher.grapherId + "_timeline_addChannelsArea input[type=text]").unbind("keyup").keyup(function(event) {
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

                        $("#" + grapher.grapherId + "_timeline_addChannelsArea ._timeline_sources_list ._timeline_sources_channel").each(function() {
                            if ($.trim($(this).html()).search(regexp) == -1) {
                                $(this).hide();
                            }
                            else {
                                $(this).show();
                            }
                        });
                    }).keyup();

                    // Collapsible devices
                    $("._timeline_sources_name").unbind("click").click(function() {
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

                    $(window).resize();//fixes issue where panel wouldn't have a scrollbar
                    if (typeof callback === "function") {
                        callback();
                    }
                });
            });

        });
    } // getSources

    function addPaneRestoreState(grapher) {
        var i = 0;
        var l = addPaneChannelsState.length;

        $("#" + grapher.grapherId + "_timeline_addChannelsArea ._timeline_sources_list ._timeline_sources_channel").each(function() {
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
        if (this.loadViewOverride(null)) return;
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
        if (this.loadViewOverride(id)) return;
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

    Grapher.prototype.exportToCSV = function(){
        var exportChannelMap = {};
        for (var element in this.channelsMap){
            var channel = this.channelsMap[element];
            if (channel.type == null){//this signifies a data channel (aka we can export it!)
                exportChannelMap[channel.device_name + "." + channel.channel_name] = element;
            }
        }
        var channelsArray = [];
        for (var element in exportChannelMap){
            channelsArray.push(element);
        }

        var start = Math.floor(this.dateAxis.getMin());
        var end = Math.ceil(this.dateAxis.getMax());

        var grapher = this;

        App.loadMustacheTemplate("core/grapher/timelineTemplates.html","timelineExportModal",function(template){
            var modalContents = {
                channels:[],
                downloadLink: "javascript:void(0);",
                filename: "fluxtream-export-from-" + start + "-to-" + end + ".csv",
                noChannels: channelsArray.length == 0
            };
            var modal = App.makeModal(template.render(modalContents));
            modal.find(".dataDownloadLink").click(function(event){
                if ($(event.delegateTarget).hasClass("disabled")){
                    event.preventDefault();
                    event.stopPropagation();
                }
            })
            var li = channelsArray.length;
            function calculateNext(i){
                if (i == li){
                    App.loadMustacheTemplate("core/grapher/timelineTemplates.html","timelineExportContent",function(contentTemplate){
                        var downloadLink = "/api/bodytrack/exportCSV/" + App.getUID() + "/fluxtream-export-from-" + start + "-to-" + end + ".csv?channels=" + encodeURIComponent(JSON.stringify(channelsArray));
                        //update contents and download link and enable the download button
                        modal.find(".dataDownloadLink").attr("href",downloadLink)
                            .removeClass("disabled");
                        modal.find(".modal-body").html(contentTemplate.render(modalContents));

                        //this will focus on the appropriate button for the enter key to work
                        if (modal.hasClass("in")){
                            if (li > 0){
                                modal.find(".dataDownloadLink").focus();
                            }
                            else{
                                modal.find(".dataDownloadLink").remove();
                                modal.find("#cancel").focus();
                            }
                        }
                        else{
                            modal.on("shown",function(){
                                if (li > 0){
                                    modal.find(".dataDownloadLink").focus();
                                }
                                else{
                                    modal.find(".dataDownloadLink").remove();
                                    modal.find("#cancel").focus();
                                }
                            })
                        }
                    })
                    return;
                }
                grapher.plotsMap[exportChannelMap[channelsArray[i]]].getStatistics(start,end,null,function(stats){
                    console.log(stats);
                    modalContents.channels.push({name: channelsArray[i],
                        count: stats.count});
                    modal.find(".channelCount.channel" + i).text(stats.count);
                    for (var sideChannel in stats.sideChannelCounts){
                        modalContents.channels.push({
                            name: channelsArray[i] + "._" + sideChannel,
                            count: stats.sideChannelCounts[sideChannel]
                        });
                        channelsArray.push(channelsArray[i] + "._" + sideChannel);
                    }
                    calculateNext(i+1);
                });
            }
            calculateNext(0);
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
            $("#" + this.grapherId + "_timeline_dateAxisLabelRegion").addClass("channelAreaShowing");
            TOOLS.resizeElementHeight($("#" + this.grapherId + "_timeline_addChannelsArea ._timeline_sources_list"));
        }
        else {
            $("#"  + this.grapherId + "_timeline_add_channels_btn").removeClass("active");
            $("#" + this.grapherId + "_timeline_dateAxisLabelRegion").removeClass("channelAreaShowing");
            area.hide();
        }

        // call the resize handler to ensure that the grapher gets resized
        TOOLS.resizeHandler();

        return false;
    }

    Grapher.prototype.removeChannel = function(channel){
        var deviceName;
        var channelName;
        var channelElementId = null;
        var firstPeriod = channel.indexOf(".");
        if (firstPeriod > 0){
            deviceName = channel.substring(0,firstPeriod);
            channelName = channel.substring(firstPeriod + 1);
        }
        else{
            channelElementId = channel;
        }

        if (channelElementId == null){
            var channelElement = $("#" + this.grapherId +"_timeline_channel_" + deviceName + "_" + channelName);
            if (channelElement.length != 0){
                channelElementId = channelElement.parent().attr("id");
            }

        }
        if (channelElementId != null){
            this.plotContainersMap[channelElementId].removePlot(this.plotsMap[channelElementId]);
            $("#" + channelElementId).remove();
            delete this.channelsMap[channelElementId];
        }

    }

    Grapher.prototype.hasChannel = function(channelName){
        for (var member in this.channelsMap){
            if (this.channelsMap[member].device_name + "." + this.channelsMap[member].channel_name == channelName)
                return true;
        }
        return false;
    }

    Grapher.prototype.doCursorClick = function(plot){
        if (plot.indexOf)
            plot = this.getPlot(plot);
        if (plot != null)
            plot.doCursorClick();
    }

    Grapher.prototype.getPlot = function(channel){
        var deviceName;
        var channelName;
        var channelElementId = null;
        var firstPeriod = channel.indexOf(".");
        if (firstPeriod > 0){
            deviceName = channel.substring(0,firstPeriod);
            channelName = channel.substring(firstPeriod + 1);
        }
        else{
            channelElementId = channel;
        }

        if (channelElementId == null){
            var channelElement = $("#" + this.grapherId +"_timeline_channel_" + deviceName + "_" + channelName);
            if (channelElement.length != 0){
                channelElementId = channelElement.parent().attr("id");
            }

        }
        if (channelElementId == null)
            return null
        return this.plotsMap[channelElementId];

    }

    // Add new channel to target
    Grapher.prototype.addChannel = function(channel, target, dontPad) {
        var grapher = this;
        if (!SOURCES.initialized){
            $.doTimeout(33,function(){
                console.log('timeout');
                grapher.addChannel(channel,target,dontPad);
            });
            return;
        }
        else if (SOURCES.availableList == null || (SOURCES.availableList.length > 0 && SOURCES.availableList[0].channels.length > 0 && SOURCES.availableList[0].channels[0].id == null)){
            getSources(grapher,function(){
                grapher.addChannel(channel,target,dontPad);
            });
            return;

        }
        if (typeof channel == "string"){
            var channel = getSourceChannelByFullName(channel);
            if (channel == null)
                return;
            var channel = grapher.sourcesMap[channel.id];
        }
        else{
            if (getSourceChannelByName(channel.device_name,channel.channel_name) == null)
                return; //if the channel doesn't exist we shouldn't add it, there will just be crashes
        }

        App.loadMustacheTemplate("core/grapher/timelineTemplates.html","channelTemplate",function(template){
            var max_time;

            // VERY important to clone the given channel here!
            channel = TOOLS.clone(channel);

            var id = channelIdx;
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
                "grapherId"        : grapher.grapherId,
                "channelType"      : channel["type"] == null ? "CONTINUOUS" : channel["type"].toUpperCase(),
                "hideYAxis"        : false
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
            var yDiff = yMax - yMin;
            var padding;
            if (channel["type"] == "timespan"){
                padding = 0;
            }
            else if(yDiff < 1e-10) {
                padding = 0.5;
            } else {
                padding = 0.1 * yDiff;
            }


            if (dontPad) padding = 0;

            var yAxis = new NumberAxis(yAxisElementId, "vertical", {
                "min" : yMin - padding,
                "max" : yMax + padding
            });

            var oldMin = yMin - padding;
            var oldMax = yMax + padding;
            yAxis.addAxisChangeListener(function(event){
                if ((oldMin != event.min || oldMax != event.max) && grapher.currentTooltip != null)//this is to avoid processing on events where the axis bounds didn't change
                    grapher.currentTooltip.remove();
                oldMin = event.min;
                oldMax = event.max;
            });

            // Now that yAxis is initialized, if this is a new view,
            // set xAxis range to be the latest 24 hrs of data from the
            // first added channel
            /*if ((VIEWS.data["name"] == newViewName) &&
                channel.hasOwnProperty("max_time") &&
                ($("#_timeline_channels ._timeline_channel").length == 0)) {
                max_time = channel["max_time"];
                grapher.dateAxis.setRange(max_time - 86400.0, max_time);
            }

             "#e9e9e9",
             MOVES_CYCLING_COLOR: "#68abef",
             MOVES_WALKING_COLOR: "#23ee70",
             MOVES_TRANSPORT_COLOR: "#8f8f8d",
             MOVES_RUNNING_COLOR: "#e674ec"*/

            var plot = null;
            if ("timespan" == channel["type"]){
                plot = new TimespanSeriesPlot(timespanDatasource(App.getUID(), channel["device_name"], channel["channel_name"]), grapher.dateAxis,
                    yAxis,
                    {"style": channel["style"], "localDisplay": channel["time_type"] == "local"});
                plot.addDataPointListener(timespanDataPointListener(grapher,plot));
            }
            else if (("photo" == channel['type']) || "photo" == channel["channel_name"] || "photos" == channel["channel_name"]) {
                var tags = [];
                var matchingStrategy = "any";
                var photoStyle = channel['style'];
                if (typeof photoStyle !== 'undefined' &&
                    typeof photoStyle['filters'] !== 'undefined' &&
                    typeof photoStyle['filters']['tag'] !== 'undefined') {

                    if (jQuery.isArray(photoStyle['filters']['tag']['tags'])) {
                        tags = photoStyle['filters']['tag']['tags'];
                    }
                    matchingStrategy = photoStyle['filters']['tag']['matchingStrategy'];
                }
                // if defined, we must use the object_type_name here and not the channel_name!
                var objectTypeOrChannelName = (typeof channel["object_type_name"] === 'undefined' ? channel["channel_name"] : channel["object_type_name"]);
                plot = new PhotoSeriesPlot(photoDatasource(App.getUID(), channel["device_name"], objectTypeOrChannelName, tags, matchingStrategy),
                    grapher.dateAxis,
                    yAxis,
                    App.getUID(),
                    {"style": channel["style"], "localDisplay": channel["time_type"] == "local"});
                plot.addDataPointListener(photoDataPointListener(grapher, channel, channelElementId));
            } else if ("comments" == channel["channel_name"]) {
                var commentStyle = channel['style'];
                if (typeof commentStyle !== 'undefined' &&
                    typeof commentStyle['filters'] !== 'undefined' &&
                    typeof commentStyle['filters']['tag'] !== 'undefined') {

                    if (jQuery.isArray(commentStyle['filters']['tag']['tags'])) {
                        tags = commentStyle['filters']['tag']['tags'];
                    }
                    matchingStrategy = commentStyle['filters']['tag']['matchingStrategy'];
                }
                alert("Implement commentDatasource and CommentSeriesPlot");
                //			var commentDatasource = commentDatasource(App.getUID(),
                //			channel["device_name"],
                //			tags,
                //			matchingStrategy);
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
                    {"style": channel["style"], "localDisplay": channel["time_type"] == "local"});
                plot.addDataPointListener(function(pointObj, sourceInfo){dataPointListener(grapher,pointObj, sourceInfo)});
            }

            var plotContainer = new PlotContainer(plotElementId, false, [plot]);
            plot.plotContainer = plotContainer;

            grapher.channelsMap[channelElementId] = channel;
            grapher.plotsMap[channelElementId] = plot;
            grapher.plotContainersMap[channelElementId] = plotContainer;
            grapher.plotContainers.push(plotContainer);

            // Gear button
            $("#" + channelElementId + "_btnGear").unbind("click").click(function(event) {
                event.preventDefault();
                var channelConfigElement = $(this).parents("._timeline_channel")
                    .children("._timeline_channelConfig");

                channelConfigElement.toggle();

                makeChannelsSortable(grapher);
            });

            // Delete buton
            $("#" + channelElementId + "_delete_btn")
                .addClass("_timeline_channel_" + channel.device_name + "_" + channel.channel_name + "_delete_btn")
                .unbind('click')
                .click(function(event) {
                    event.preventDefault();
                    if (!grapher.showDeleteBtn)
                        return;
                    grapher.removeChannel(channelElementId);
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
                    channel.usingDefaultStyle = false;

                    var plot = grapher.plotsMap[channelElementId];

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
                            "lineWidth" : TOOLS.parseInt($("#" + channelElementId + " .configLineWidth button").attr('value'), 1)
                        };

                        var pointsStyleType = $("#" + channelElementId + " .configPointsType button").attr('value');
                        var pointsStyleFill = pointsStyleType.match(/-filled$/) !== null;

                        $("#" + channelElementId + "-config-points-fillColor-container").toggle(pointsStyleFill);

                        var pointsStyle = {
                            "type"      : pointsStyleType.replace('-filled', ''),
                            "show"      : $("#" + channelElementId + "-config-points-show").is(':checked'),
                            "lineWidth" : 1,
                            "radius"    : TOOLS.parseInt($("#" + channelElementId + " .configPointsRadius button").attr('value'), 2),
                            "color"     : $("#" + channelElementId + "-config-points-color").next(".color_picker").css("background-color"),
                            "fill"      : pointsStyleFill,
                            "fillColor" : $("#" + channelElementId + "-config-points-fillColor").next(".color_picker").css("background-color")
                        };

                        var barsStyle = {
                            "type"      : "lollipop",
                            "show"      : $("#" + channelElementId + "-config-bars-show").is(':checked'),
                            "lineWidth" : TOOLS.parseInt($("#" + channelElementId + " .configBarsLineWidth button").attr('value'), 1),
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
                        "marginWidth"    : TOOLS.parseInt($("#" + channelElementId + " .configValuesMarginWidth button").attr('value'), 5),
                        "verticalOffset" : TOOLS.parseInt($("#" + channelElementId + " .configValuesVerticalOffset button").attr('value'), 7),
                        "numberFormat"   : $("#" + channelElementId + " .configValuesNumberFormat button").attr('value')
                    };



                    // We'll always put the values style in both the styles array AND the highlight styles array.  The "show"
                    // field will be false for both if Values option is unchecked.  The "show" field will be true for both if the
                    // Values option is checked and the showOnlyOnHighlight option is false.  If the showOnlyOnHighlight option is
                    // true, then the instance in the styles array will have show set to false
                    newStyle['highlight']['styles'][newStyle['highlight']['styles'].length] = valuesStyle;
                    var onlyShowValuesOnHighlight = $("#" + channelElementId + " .configValuesShowOnlyOnHighlight button").attr('value') === 'true';
                    $("#" + channelElementId + "-config-values-marginWidth-label-container").toggle(!onlyShowValuesOnHighlight);
                    $("#" + channelElementId + "-config-values-marginWidth-container").toggle(!onlyShowValuesOnHighlight);
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
                    var commentsStyleType = $("#" + channelElementId + " .configCommentsType button").attr('value');
                    var commentsStyleFill = commentsStyleType.match(/-filled$/) !== null;
                    $("#" + channelElementId + "-config-comments-fillColor-container").toggle(commentsStyleFill);
                    newStyle['comments'] = {
                        "show"           : $("#" + channelElementId + "-config-comments-show").is(':checked'),
                        "styles"         : [{
                                                "type"      : commentsStyleType.replace('-filled', ''),
                                                "show"      : $("#" + channelElementId + "-config-comments-show").is(':checked'),
                                                "lineWidth" : 1,
                                                "radius"    : TOOLS.parseInt($("#" + channelElementId + " .configCommentsRadius button").attr('value'), 3),
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



                /* add event handler for the Save As Default Style link --------------------------------------------------- */
                $("#" + channelElementId + "-save-default-style > a").click(function() {
                    $("#" + channelElementId + "-save-default-style").hide();
                    $("#" + channelElementId + "-save-default-style-status").html("Saving...").show();
                    saveDefaultChannelStyle(channel, plot.getStyle(), {
                        success : function() {
                            channel.usingDefaultStyle = true;
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

                //bind dropdown menus
                $("#" + channelElementId + " .configDropdown").each(function(index,dropdown){
                    dropdown = $(dropdown);
                    dropdown.find("a").click(function(event){
                        event.preventDefault();
                        var button = dropdown.find("button");
                        var target = $(event.delegateTarget);
                        button.html(target.html() + ' <span class="caret"></span>');
                        button.attr("value",target.attr("value"));
                        updateDataSeriesPlotChannelConfig();
                    });
                });

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


                // Set the initial value of the show checkbox
                $("#" + channelElementId + "-config-lines-show").change(updateDataSeriesPlotChannelConfig);


                // Create the color colorpicker, and set its initial value
                $("#" + channelElementId + "-config-lines-color").colorPicker();
                $("#" + channelElementId + "-config-lines-color").change(updateDataSeriesPlotChannelConfig);

                /* Configure the Points options --------------------------------------------------------------------------- */

                // Set the initial value of the show checkbox
                $("#" + channelElementId + "-config-points-show").change(updateDataSeriesPlotChannelConfig);

                // Create the color colorpicker, and set its initial value
                $("#" + channelElementId + "-config-points-color").colorPicker();
                $("#" + channelElementId + "-config-points-color").change(updateDataSeriesPlotChannelConfig);

                // Create the fillColor colorpicker, and set its initial value
                $("#" + channelElementId + "-config-points-fillColor").colorPicker();
                $("#" + channelElementId + "-config-points-fillColor").change(updateDataSeriesPlotChannelConfig);

                /* Configure the Bars options ----------------------------------------------------------------------------- */

                // Set the initial value of the show checkbox
                $("#" + channelElementId + "-config-bars-show").change(updateDataSeriesPlotChannelConfig);

                // Create the color colorpicker, and set its initial value
                $("#" + channelElementId + "-config-bars-color").colorPicker();
                $("#" + channelElementId + "-config-bars-color").change(updateDataSeriesPlotChannelConfig);

                /* Configure the Values options --------------------------------------------------------------------------- */

                // Set the initial value of the show checkbo
                $("#" + channelElementId + "-config-values-show").change(updateDataSeriesPlotChannelConfig);

                // Create the fillColor colorpicker, and set its initial value
                $("#" + channelElementId + "-config-values-fillColor").colorPicker();
                $("#" + channelElementId + "-config-values-fillColor").change(updateDataSeriesPlotChannelConfig);

                // Set the initial value of the showOnlyOnHighlight select menu and the initial visibility of the marginWidth select menu
                $("#" + channelElementId + "-config-values-showOnlyOnHighlight").msDropDown();

                /* Configure the Comments options ------------------------------------------------------------------------- */

                // Set the initial value of the show checkbox
                $("#" + channelElementId + "-config-comments-show").change(updateDataSeriesPlotChannelConfig);

                // Create the color colorpicker, and set its initial value
                $("#" + channelElementId + "-config-comments-color").colorPicker();
                $("#" + channelElementId + "-config-comments-color").change(updateDataSeriesPlotChannelConfig);

                // Create the fillColor colorpicker, and set its initial value
                $("#" + channelElementId + "-config-comments-fillColor").colorPicker();
                $("#" + channelElementId + "-config-comments-fillColor").change(updateDataSeriesPlotChannelConfig);
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
                    channel.usingDefaultStyle = false;
                    var channelElement = $(this).parents("._timeline_channel").parent();
                    var plot = grapher.plotsMap[channelElement.attr("id")];
                    var newStyle = plot.getStyle();
                    //console.log("----------------------------------------\nOLD JSON: " + JSON.stringify(newStyle,null,3));

                    // completely overwrite the existing tag filters object
                    if (typeof newStyle['filters'] === 'undefined') {
                        newStyle['filters'] = {};
                    }

                    var matchingStrategy = $("#" + channelElementId + "-photo-tags-matching-strategy").val();
                    var userSelectedTags = getUserSelectedTags();
                    newStyle['filters']["tag"] = {
                        "tags" : userSelectedTags,
                        "matchingStrategy" : matchingStrategy
                    };

                    // Display the filter settings in the channel tab
                    if (userSelectedTags.length > 0 || matchingStrategy == "untagged") {
                        var params = {
                            value:userSelectedTags.join(", "),
                            matchingString: "Any of:"
                        };
                        switch (matchingStrategy){
                            case "all":
                                params.matchingString = "All of:";
                                break;
                            case "none":
                                params.matchingString = "None of:";
                                break;
                            case "untagged":
                                params.matchingString = "Untagged";
                                params.value = "";
                                break;
                        }
                        var filterHtml = App.fetchCompiledMustacheTemplate("core/grapher/timelineTemplates.html","_timeline_channel_tab_filter_template").render(params);
                        $("#" + channelElementId + "-timeline-channel-filter").html(filterHtml).shorten();
                    } else {
                        $("#" + channelElementId + "-timeline-channel-filter").text('').hide();
                    }

                    //console.log("NEW JSON: " + JSON.stringify(newStyle,null,3));

                    plot.setStyle(newStyle);

                    // we must use the object_type_name here and not the channel_name!
                    var objectTypeOrChannelName = (typeof channel["object_type_name"] === 'undefined' ? channel["channel_name"] : channel["object_type_name"]);
                    plot.setDatasource(photoDatasource(App.getUID(),
                        channel["device_name"],
                        objectTypeOrChannelName,
                        newStyle['filters']["tag"]["tags"],
                        newStyle['filters']["tag"]["matchingStrategy"]
                    ));
                };

                $("#" + channelElementId + "-photo-tags-matching-strategy").change(updatePhotoSeriesPlotChannelConfig);
                $("#" + channelElementId + "-photo-tags-matching-strategy").change(function(){
                    // show/hide the tags text box depending on the matching strategy (hidden when the "untagged" strategy is selected)
                    var matchingStrategy = $("#" + channelElementId + "-photo-tags-matching-strategy").val();
                    $("#" + channelElementId + "-photo-tags-filter").toggle(matchingStrategy != "untagged");
                });




                $("#" + channelElementId + "-photo-tags-filter").bind('tagsChanged', updatePhotoSeriesPlotChannelConfig);
                //$("#" + channelElementId + "-photo-tags-filter").bind('tagAdded', function(){console.log('tagAdded')});
                //$("#" + channelElementId + "-photo-tags-filter").bind('tagEdited', function(){console.log('tagEdited')});
                //$("#" + channelElementId + "-photo-tags-filter").bind('tagRemoved', function(){console.log('tagRemoved')});

                // Show PhotoSeriesPlot config
                $("#" + channelElementId + " ._timeline_photo_series_plot_config").show();
            } else if (plot instanceof TimespanSeriesPlot){
                yAxis.setMaxRange(0,1);
                $("#" + channelElementId + " #" + channelElementId + "_btnShowAllY").click(function(event){
                    yAxis.setRange(0,1);
                });
            }
            grapher.applyChannelStyle(channelElementId,channel.style,true);

            // Force initial resize
            resizePlot(grapher, id, 0);

            // Update scroll area
            TOOLS.resizeHandler();

            return html;
        });
    }//Grapher.addChannel

    Grapher.prototype.applyChannelStyle = function(channelId,style,silent){
        var channel = this.channelsMap[channelId];
        var plot = this.plotsMap[channelId];
        var usingDefault = channel.usingDefaultStyle;
        style = TOOLS.clone(style);
        if (plot instanceof DataSeriesPlot){
            // First, record whether this is a Zeo plot
            var isZeo = channel["channel_name"] == "Sleep_Graph";

            // Check for styles array
            if (!style.hasOwnProperty("styles")) {
                style["styles"] = [];
            }
            // Check for highlight object
            if (!style.hasOwnProperty("highlight")) {
                style["highlight"] = {};
            }
            // Check for highlight styles array
            if (!style["highlight"].hasOwnProperty("styles")) {
                style["highlight"]["styles"] = [];
            }
            // Check for comments object
            if (!style.hasOwnProperty("comments")) {
                style["comments"] = {};
            }
            // Check for comments styles array
            if (!style["comments"].hasOwnProperty("styles")) {
                style["comments"]["styles"] = [];
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

            for (var styleTypeIndex = 0; styleTypeIndex < style["styles"].length; styleTypeIndex++) {
                var theStyle = style["styles"][styleTypeIndex];
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
            for (var highlightStyleTypeIndex = 0; highlightStyleTypeIndex < style["highlight"]["styles"].length; highlightStyleTypeIndex++) {
                var theHighlightStyle = style["highlight"]["styles"][highlightStyleTypeIndex];
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
            for (var commentsStyleTypeIndex = 0; commentsStyleTypeIndex < style["comments"]["styles"].length; commentsStyleTypeIndex++) {
                var theCommentsStyle = style["comments"]["styles"][commentsStyleTypeIndex];
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

            // don't show this section if this is a Zeo plot
            $("#" + channelId + "-config-lines").toggle(!isZeo);

            //Line Options--------------------------------------------------------------
            //set value of show lines
            $("#" + channelId + "-config-lines-show").prop("checked", linesStyle["show"] && !isZeo);

            // Set the value of the lineWidth select menu
            $("#" + channelId + " .configLineWidth a[value=" + TOOLS.parseInt(linesStyle["lineWidth"], 1) + "]").click();

            // Create the color colorpicker, and set its initial value
            $("#" + channelId + "-config-lines-color").val(typeof linesStyle["color"] === 'undefined' ? defaultColor : linesStyle["color"]);
            $("#" + channelId + "-config-lines-color").change();
            /* Configure the Points options --------------------------------------------------------------------------- */

            // don't show this section if this is a Zeo plot
            $("#" + channelId + "-config-points").toggle(!isZeo);

            // Set the initial value of the show checkbox
            $("#" + channelId + "-config-points-show").prop("checked", pointsStyle["show"] && !isZeo);

            // Set the initial value of the type select menu and the initial state of the fillColor color picker
            $("#" + channelId + " .configPointsType a[value=" + pointsStyle['type-ui'] + "]").click();
            $("#" + channelId + "-config-points-fillColor-container").toggle(pointsStyle['fill']);

            // Set the initial value of the radius select menu
            $("#" + channelId + " .configPointsRadius a[value=" + TOOLS.parseInt(pointsStyle["radius"], 2) + "]").click();

            // Create the color colorpicker, and set its initial value
            $("#" + channelId + "-config-points-color").val(typeof pointsStyle["color"] === 'undefined' ? defaultColor : pointsStyle["color"]);
            $("#" + channelId + "-config-points-color").change();

            // Create the fillColor colorpicker, and set its initial value
            $("#" + channelId + "-config-points-fillColor").val(typeof pointsStyle["fillColor"] === 'undefined' ? defaultColor : pointsStyle["fillColor"]);
            $("#" + channelId + "-config-points-fillColor").change();

            /* Configure the Bars options ----------------------------------------------------------------------------- */

            // don't show this section if this is a Zeo plot
            $("#" + channelId + "-config-bars").toggle(!isZeo);

            // Set the initial value of the show checkbox
            $("#" + channelId + "-config-bars-show").prop("checked", barsStyle["show"] && !isZeo);

            // Set the initial value of the lineWidth select menu
            $("#" + channelId + " .configBarsLineWidth a[value=" + TOOLS.parseInt(barsStyle["lineWidth"], 1) + "]").click();

            // Create the color colorpicker, and set its initial value
            $("#" + channelId + "-config-bars-color").val(typeof barsStyle["color"] === 'undefined' ? defaultColor : barsStyle["color"]);
            $("#" + channelId + "-config-bars-color").change();

            /* Configure the Values options --------------------------------------------------------------------------- */

            // Set the initial value of the show checkbox
            $("#" + channelId + "-config-values-show").prop("checked", valuesStyle["show"]);

            // Create the fillColor colorpicker, and set its initial value
            $("#" + channelId + "-config-values-fillColor").val(typeof valuesStyle["fillColor"] === 'undefined' ? defaultColor : valuesStyle["fillColor"]);
            $("#" + channelId + "-config-values-fillColor").change();

            // Set the initial value of the numberFormat select menu
            $("#" + channelId + " .configValuesNumberFormat a[value=\"" + (typeof valuesStyle["numberFormat"] === 'undefined' ? "###,##0.0##" : valuesStyle["numberFormat"]) + "\"]").click();

            // Set the initial value of the verticalOffset select menu
            $("#" + channelId + " .configValuesVerticalOffset a[value=" + TOOLS.parseInt(valuesStyle["verticalOffset"], 7) + "]").click();

            // Set the initial value of the showOnlyOnHighlight select menu and the initial visibility of the marginWidth select menu
            $("#" + channelId + " .configValuesShowOnlyOnHighlight a[value=" + showValuesOnlyOnHighlight + "]").click();
            var showValuesOnlyOnHighlightBoolean = showValuesOnlyOnHighlight == 'true';
            $("#" + channelId + "-config-values-marginWidth-label-container").toggle(!showValuesOnlyOnHighlightBoolean);
            $("#" + channelId + "-config-values-marginWidth-container").toggle(!showValuesOnlyOnHighlightBoolean);

            // Set the initial value of the marginWidth select menu
            $("#" + channelId + " .configValuesMarginWidth a[value=" + TOOLS.parseInt(valuesStyle["marginWidth"], 5) + "]").click();

            /* Configure the Comments options ------------------------------------------------------------------------- */

            // Set the initial value of the show checkbox
            $("#" + channelId + "-config-comments-show").prop("checked", commentsStyle["show"]);

            // Set the initial value of the type select menu and the initial state of the fillColor color picker
            $("#" + channelId + " .configCommentsType a[value=" + commentsStyle['type-ui'] + "]").click();

            // Set the initial value of the radius select menu
            $("#" + channelId + " .configCommentsRadius a[value=" + TOOLS.parseInt(commentsStyle["radius"],3) + "]").click();

            // Create the color colorpicker, and set its initial value
            $("#" + channelId + "-config-comments-color").val(typeof commentsStyle["color"] === 'undefined' ? defaultColor : commentsStyle["color"]);
            $("#" + channelId + "-config-comments-color").change();

            // Create the fillColor colorpicker, and set its initial value
            $("#" + channelId + "-config-comments-fillColor").val(typeof commentsStyle["fillColor"] === 'undefined' ? defaultColor : commentsStyle["fillColor"]);
            $("#" + channelId + "-config-comments-fillColor").change();
        }
        else if (plot instanceof PhotoSeriesPlot){
            // Check for filters object
            if (!style.hasOwnProperty("filters")) {
                style["filters"] = {};
            }
            // Check for filters.tag object
            if (!style["filters"].hasOwnProperty("tag")) {
                style["filters"]["tag"] = {};
            }
            // Check for filters.tag.tags array
            if (!style["filters"]["tag"].hasOwnProperty("tags")) {
                style["filters"]["tag"]["tags"] = [];
            }
            // Check for filters.tag.matchingStrategy property
            if (!style["filters"]["tag"].hasOwnProperty("matchingStrategy")) {
                style["filters"]["tag"]["matchingStrategy"] = "any";  // default to joining with OR
            }

            // Load up the existing tag filter (if any)
            var tagFilter = style["filters"]["tag"];

            // Set the initial value of the matchingStrategy select menu
            $("#" + channelId + "-photo-tags-matching-strategy").val("" + tagFilter["matchingStrategy"]);

            // seed the tag filter editor with the tags currently saved in the channel (if any)
            $("#" + channelId + "-photo-tags-filter").html("");
            if (tagFilter['tags'].length > 0) {
                $.each(tagFilter['tags'], function(index, value) {
                    var tagHtml = App.fetchCompiledMustacheTemplate("core/grapher/timelineTemplates.html","_timeline_photo_dialog_tags_editor_tag_template").render({"value" : value});
                    $("#" + channelId + "-photo-tags-filter").append(tagHtml);
                });
            } else {
                var tagHtml = App.fetchCompiledMustacheTemplate("core/grapher/timelineTemplates.html","_timeline_photo_dialog_tags_editor_tag_template").render({"value" : ""});
                $("#" + channelId + "-photo-tags-filter").append(tagHtml);
            }

            // returns the array of tags already selected for this photo
            var getUserSelectedTags = function() {
                var tags = [];
                $.each($("#" + channelId + "-photo-tags-filter .tagedit-listelement-old input"),
                    function(index, inputElement) {
                        var val = inputElement['value'];
                        if (typeof val === 'string' && val != '') {
                            tags[tags.length] = val;
                        }
                    }
                );
                return tags;
            };

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
            $("#" + channelId + "-photo-tags-filter input.tag").tagedit(tagFilterOptions);

            // Finally, trigger a call updatePhotoSeriesPlotChannelConfig() so that the grapher properly represents the config settings
            $("#" + channelId + "-photo-tags-matching-strategy").change();
        }
        else if (plot instanceof TimespanSeriesPlot){
            plot.setStyle(style);
            channel.style = style;
        }

        if (silent){
            channel.usingDefaultStyle = usingDefault;
        }
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
        VIEWS.data["v2"]["x_axis"]["min"] = grapher.dateAxis.getMin();
        VIEWS.data["v2"]["x_axis"]["max"] = grapher.dateAxis.getMax();

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

            grapher.dateAxis.setRange(view["v2"]["x_axis"]["min"],
                view["v2"]["x_axis"]["max"]);
            return;
        }
        else {
            // Reset view specific data
            grapher.channelsMap = {};
            grapher.plotsMap = {};
            grapher.plotContainersMap = {};
            grapher.plotContainers = [];

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
            grapher.cursorString = null;
            grapher.prevCursorPos = null;
            var currentMin = view["v2"]["x_axis"]["min"];
            var currentMax = view["v2"]["x_axis"]["max"];
            grapher.dateAxis.addAxisChangeListener(function(event) {
                if ((currentMin != event.min || currentMax != event.max) && grapher.currentTooltip != null)//this is to avoid processing on events where the axis bounds didn't change
                    grapher.currentTooltip.remove();
                currentMin = event.min;
                currentMax = event.max;
                if (event.cursorPosition != grapher.prevCursorPos){
                    grapher.prevCursorPos = event.cursorPosition;
                    grapher.cursorString = event.cursorPositionString;
                    grapher.clickPointString = null;
                }
                updateDataPointDisplay(grapher);
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
            for (i = l - 1; i >= 0; i--) {

                // Update min_time, max_time for each channel with latest from
                // SOURCES if available
                channel = getSourceChannelByName(yAxes[i]["device_name"], yAxes[i]["channel_name"]);
                if (!!channel && channel.hasOwnProperty("min_time")
                    && channel.hasOwnProperty("max_time")) {
                    yAxes[i]["min_time"] = channel["min_time"];
                    yAxes[i]["max_time"] = channel["max_time"];
                }

                grapher.addChannel(yAxes[i], null, true);
            }
        }
        $(window).resize();//fixes issue of no date axis when window no channels are in view.
    }

    Grapher.prototype.getCurrentTimeUnit = function(){
        var range = this.dateAxis.getMax() - this.dateAxis.getMin();
        //if (range > 364 * 24 * 3600)   Temporarily disabled
        //    return "year";
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

    function dataPointListener(grapher, pointObj, sourceInfo) {
        if (pointObj) {
            App.loadMustacheTemplate("core/grapher/timelineTemplates.html","dataPointValueLabel",function (template){
                if (sourceInfo.actionName == "highlight")
                    grapher.pointString = template.render(pointObj);
                else if (sourceInfo.actionName == "click")
                    grapher.clickPointString = template.render(pointObj);
                updateDataPointDisplay(grapher);
            });
        } else {
            grapher.pointString = null;
            updateDataPointDisplay(grapher);
        }
    }

    function updateDataPointDisplay(grapher){
        var stringToUse = "";
        if (grapher.pointString != null){
            stringToUse = grapher.pointString;
        }
        else if (grapher.clickPointString != null){
            stringToUse = grapher.clickPointString
        }
        else if (grapher.cursorString != null){
            stringToUse = grapher.cursorString;
        }
        $("#" + grapher.grapherId + "_timeline_dataPointValueLabel").html(stringToUse);
    }

    function loadLogrecMetadata(logrecId, callbacks) {
        logrecId = TOOLS.parseInt(logrecId, -1);
        if (logrecId >= 0) {

            var url = "/bodytrack/users/" + App.getUID() + "/logrecs/" + logrecId + "/get";

            TOOLS.loadJson(url, {}, callbacks);
        }
    }

    function timespanDataPointListener(grapher,plot){
        var mainContentContainer = $("#" + grapher.grapherId + "_timeline_mainContentArea");

        return function (pointObj, sourceInfo){
            var timespanObject = sourceInfo.info.timespanInfo;
            $.ajax("/api/connectors/" + timespanObject.objectType + "/data?start=" + timespanObject.start * 1000 + "&end=" + timespanObject.end * 1000 + "&value=" + encodeURIComponent(timespanObject.value),{
                success: function(facets){
                    $.ajax("/api/metadata/cities?start=" + timespanObject.start * 1000 + "&end=" + timespanObject.end * 1000,{
                        success: function(cities){
                            var plotContainer = $("#" + plot.plotContainer.getPlaceholder());
                            var position = sourceInfo.info.position;
                            var mainContentPosition = mainContentContainer.offset();
                            var plotOffset = plotContainer.offset();
                            var positionRelativeToMainContentArea = {
                                x: plotOffset.left - mainContentPosition.left + position.x,
                                y: plotOffset.top - mainContentPosition.top + position.y

                            }

                            grapher.currentTooltip = Tooltip.createTooltip(mainContentContainer,positionRelativeToMainContentArea,ListUtils.buildList(facets,cities),sourceInfo.info.color);
                            grapher.currentTooltip.onRemove = function(){
                                if (grapher.currentTooltip == this)
                                    grapher.currentTooltip = null;
                            }
                        }
                    });
                }
            });
        };
    }

    function photoDataPointListener(grapher, channel, channelElementId) {
        return function(pointObj, sourceInfo) {
            if (pointObj && sourceInfo && sourceInfo['info']) {
                function getTagFilterForChannel() {
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


                var parts = sourceInfo.info.imageId.split(".");

                PhotoUtils.showPhotoDialog(parts[0],parts[1],parts[2],pointObj.date * 1000,{
                    channelFilters: getTagFilterForChannel(),
                    filteringStrategy: $("#" + channelElementId + "-photo-tags-matching-strategy").val(),
                    photoChange: function(metadata,timestamp){
                        grapher.setTimeCursorPosition(timestamp);
                        var curRange = grapher.getRange();
                        if (timestamp < curRange.min || timestamp > curRange.max){
                            var factor = (curRange.max - curRange.min) / 2;
                            grapher.setRange(timestamp - factor,timestamp + factor);
                        }
                    }

                });
                return;
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

    Grapher.prototype.getRange = function(){
        return {min:this.dateAxis.getMin(), max:this.dateAxis.getMax()};
    }

    Grapher.prototype.getCenter = function(){
        var range = this.getRange();
        return (range.min + range.max) / 2;
    }

    Grapher.prototype.setTimeCursorPosition = function(position){
        this.dateAxis.setCursorPosition(position);
        repaintAllPlots(this);
    }

    Grapher.prototype.getTimeCursorPosition = function(){
        return this.dateAxis.getCursorPosition();
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

                var finishLoading = function(){
                    if (SOURCES.initialized){
                        if (grapher.onLoad != null) {
                            var onload = grapher.onLoad;
                            grapher.onLoad = null;
                            onload();
                        }
                        /*$.ajax("/api/timezones/mapping", {success: function(mapping) {    //Disabled for now
                            grapher.dateAxis.setTimeZoneMapping(mapping);
                        }});    */
                    }
                    else{
                        $.doTimeout(100,finishLoading);
                    }
                }

                finishLoading();
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

    Grapher.prototype.onDataUpdate = function(data){
        var channelName,deviceName;
        var shouldRefreshSources = data.bodytrackStyle != null; //refreshing sources is the only way to retrieve styles right now
        if (data.bodytrackData != null){
            for (var member in this.channelsMap){
                deviceName = this.channelsMap[member].device_name;
                channelName = this.channelsMap[member].channel_name;
                if (data.bodytrackData[deviceName] != null && data.bodytrackData[deviceName][channelName] != null){
                    var range = data.bodytrackData[deviceName][channelName];
                    this.plotsMap[member].invalidateTiles(range.start / 1000, range.end / 1000);
                }
            }
            if (!shouldRefreshSources){
                outerCheck: for (deviceName in data.bodytrackData){
                    for (channelName in data.bodytrackData[deviceName]){
                        shouldRefreshSources = getSourceChannelByName(deviceName,channelName) == null;
                        if (shouldRefreshSources)
                            break outerCheck;
                    }
                }
            }

        }
        if (shouldRefreshSources){
            var grapher = this;
            getSources(this,function(){
                if (data.bodytrackStyle == null)
                    return;
                for (var member in grapher.channelsMap){
                    if (!grapher.channelsMap[member].usingDefaultStyle)
                        continue;
                    deviceName = grapher.channelsMap[member].device_name;
                    channelName = grapher.channelsMap[member].channel_name;
                    if (data.bodytrackStyle[deviceName] != null && data.bodytrackStyle[deviceName].indexOf(channelName) >= 0){
                        var channel = getSourceChannelByName(deviceName,channelName);
                        grapher.applyChannelStyle(member,channel.style,true);
                    }

                }


            });
        }
    }

    return Grapher;
});