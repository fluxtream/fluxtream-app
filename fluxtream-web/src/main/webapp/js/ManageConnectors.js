define(["core/grapher/BTCore",
        "settings/google_calendar", "settings/evernote", "settings/sms_backup"],function(BodyTrack,
        GoogleCalendarSettingsHandler, EvernoteSettingsHandler, SmsBackupSettingsHandler) {

    var connectors;

    /**
     * list settings handlers here in order for them to be able to be looked up by connectorName
     */
    var settingsHandlers = {
        "google_calendar" : GoogleCalendarSettingsHandler,
        "evernote" : EvernoteSettingsHandler,
        "sms_backup" : SmsBackupSettingsHandler
    };

    function show(){
        $.ajax("/api/connectors/installed",{
            success: function(data){
                dataLoaded(data,false);
            }
        });
    }

    //NOTE: updateContents is rather hacky, to prevent images being reloaded dozens of times a version of the row without
    //an image is loaded and used and only the version with an image is used if there is no row for a certain connector
    //also each cell is updated upon the html being different so unless the single row template matches up perfectly
    //with the original full dialog template, the first time through all the cells will be forced to update. However,
    //after that it should in theory properly only update when a change happens in each cell
    function updateContents(){
        $.ajax("/api/connectors/installed",{
            success: function(data){
                if (hidden)
                    return;
                App.loadMustacheTemplate("connectorMgmtTemplates.html","connectorSingleRowNoImage",function(noImageTemplate){
                    App.loadMustacheTemplate("connectorMgmtTemplates.html","connectorSingleRow",function(imageTemplate){
                        for (var i = 0; i < data.length; i++){
                            if (data[i].manageable){
                                var row = $("#connector-" + data[i].connectorName);
                                var params = getConnectorParams(data[i]);
                                var html = $(noImageTemplate.render(params));
                                if (row.length == 0){
                                    $("#connectorInfoTable").append(imageTemplate.render(params));
                                }
                                else{
                                    var rowChildren = row.children();
                                    var htmlChildren = html.children();
                                    for (var j = 0; j < rowChildren.length; j++){
                                        if ($(htmlChildren[j]).attr("nocompare") != "true" && $(rowChildren[j]).html() != $(htmlChildren[j]).html()){
                                            $(rowChildren[j]).html($(htmlChildren[j]).html());
                                        }
                                    }
                                    if (row.attr("class") != html.attr("class")){
                                        row.attr("class",html.attr("class") == null ? "" : html.attr("class"));
                                    }
                                }
                            }
                        }
                        var rows = $("#connectorInfoTable tr");
                        for (var i = 0; i < rows.length; i++){
                            var found = false;
                            for (var j = 0; !found && j < data.length; j++){
                                found = rows[i].id == "connector-" + data[j].connectorName;
                            }
                            if (!found){
                                $(rows[i]).remove();
                            }
                        }
                        bindDialog();
                    });
                });
            }
        })
    }

    function getConnectorParams(data){
        var params = {};
        for (var member in data){
            switch (member){
                default:
                    params[member] = data[member];
                    break;
                case "latestData":
                case "lastSync":
                    var formatted = App.formatDate(data[member],true);
                    if (formatted == "Present")
                        formatted = member == "lastSync" ? "Never" : "No Data";
                    else if (member == "latestData"){
                        var state = App.apps.calendar.toState(App.apps.calendar.currentTabName,"date",new Date(data[member]));
                        params.latestDataCalendarState = state.tabName + "/" + state.tabState;
                    }
                    params[member] = formatted;
                    break;
            }
        }
        var config = App.getConnectorConfig(data.connectorName);
        var hasTimelineSettings = typeof(config.hasTimelineSettings)!="undefined"&&config.hasTimelineSettings;
        var hasGeneralSettings = typeof(config.hasGeneralSettings)!="undefined"&&config.hasGeneralSettings;
        params.hasSettings = hasTimelineSettings||hasGeneralSettings;
        return params;
    }

    function dataLoaded(data,update){
        connectors = data;
        $(window).off("scroll.manageConnectorsHeader");
        App.loadMustacheTemplate("connectorMgmtTemplates.html","manageConnectors",function(template){
            var params = [];
            for (var i = 0; i < data.length; i++){
                if (!data[i].manageable)
                    continue;
                var config = App.getConnectorConfig(data[i].connectorName);
                var hasTimelineSettings = typeof(config.hasTimelineSettings)!="undefined"&&config.hasTimelineSettings;
                var hasGeneralSettings = typeof(config.hasGeneralSettings)!="undefined"&&config.hasGeneralSettings;
                params[i] = getConnectorParams(data[i])
                params[i].hasSettings = hasTimelineSettings||hasGeneralSettings;
            }
            var html = template.render({connectors:params});
            if (update){
                var scrollTop = $("#modal .modal-body").scrollTop();
                $("#modal").html($(html).html());
                $("#modal .modal-body").scrollTop(scrollTop);
            }
            else{
                App.makeModal(html);
                $("#modal .modal-body").scroll(function(event){
                    var scrollTop = $("#modal .modal-body").scrollTop();
                    $("#modal .modal-body .topHeader").width($("#modal .modal-body table").width());
                    if (scrollTop < 48){
                        $("#modal .modal-body .topHeaderHolder").removeClass("floating");
                        $("#modal .modal-body .placeholder").addClass("hidden");
                        $("#modal .modal-body .topHeaderHolder").width($("#modal .modal-body table").width());
                        $("#modal .modal-body .topHeader").css("margin-left",0);
                    }
                    else{
                        $("#modal .modal-body .topHeaderHolder").addClass("floating");
                        $("#modal .modal-body .placeholder").removeClass("hidden");
                        $("#modal .modal-body .topHeaderHolder").width($("#modal .modal-body").width() - 15);
                        $("#modal .modal-body .topHeader").css("margin-left",-$("#modal .modal-body").scrollLeft());
                    }
                });
                $(window).on("resize.manageConnectorsHeader",function(){
                    $("#modal .modal-body").scroll();
                });
            }
            bindDialog();
        });
    }

    var hidden;

    function bindDialog(){
        hidden = false;
         for (var i = 0; i < connectors.length; i++){
             bindConnector(connectors[i], i);
         }
        var syncAllBtn = $("#sync-all");
        syncAllBtn.click(function(){
            setAllToSyncing();
            event.preventDefault();
            $.ajax("/api/sync/all",{
                type:"POST"
            });
        });
        $.doTimeout("manageConnectorsUpdater", 10000, function(){
            updateContents();
            return true;
        });
        $("#modal").unbind("hide");
        $("#modal").on("hide",function(){
            $(window).off("resize.manageConnectorsHeader");
            hidden = true;
            $.doTimeout("manageConnectorsUpdater");
            App.activeApp.renderState(App.state.getState(App.activeApp.name),true);//force refresh of the current app state
        })
    }

    function bindConnector(connector, i){
        var deleteBtn = $("#remove-" + connector.connectorName);
        deleteBtn.click({index:i}, function(event){
            event.preventDefault();
            confirmDelete(event.data.index);
        });
        var syncNowBtn = $("#syncNow-" + connector.connectorName);
        syncNowBtn.click(function(event){
            event.preventDefault();
            setToSyncing(connector.connectorName)
            $.ajax("/api/sync/" + connector.connectorName,{
                type:"POST"
            });
        });
        var uploadBtn = $(".upload-" + connector.connectorName);
        uploadBtn.off("click");
        uploadBtn.click(function(event){
            event.preventDefault();
            App.loadMustacheTemplate("connectorMgmtTemplates.html","uploadDialog",function(template){
                handleSubmitForm(template, connector);
            });
        });
        var viewDataBtn = $("#viewUpdates-" + connector.connectorName);
        // remove previously bound handler
        viewDataBtn.off("click");
        viewDataBtn.click(function(event){
            event.preventDefault();
            App.loadMustacheTemplate("connectorMgmtTemplates.html","viewUpdates",function(template){
                viewUpdates(template, connector);
            });
        });
        var settingsBtn = $("#settings-" + connector.connectorName);
        // remove previously bound handler
        settingsBtn.off("click");
        settingsBtn.click(function(event){
            event.preventDefault();
            connectorSettings(connector);
        });
        var renewBtn = $("#renew-" + connector.connectorName);
        renewBtn.off("click");
        renewBtn.click(function(event){
            event.preventDefault();
            confirmRenew(connector);
        });
    }

    function connectorSettings(connector){
        BodyTrack.SOURCES.getAvailableList(function(sources){
            var source = null;
            for (var i = 0; i < sources.length; i++){
                // Here is where we decide whether a given device name from the
                // datastore (sources name) belongs to this connector or not
                // This is a nasty hack, but for the time being consider any
                // device name to be a match if it matches either connector name
                // or connector.connectorName.  This is to work around the problem
                // that lowercase device names need to be used for the timerange channels
                // to work, and connector.name fields are mostly camelcase. In the future we need to be
                // able to be more general about this.  A current example where this
                // is a problem is that FluxtreamCapture also includes PolarStrap.
                // We will need to make modifications to respect items in the ChannelMapping table
                // for mapping device names to their connectors and make multiple instances of
                // a given connector type work.
                if (sources[i].name == connector.name ||
                    sources[i].name == connector.connectorName){
                    source = sources[i];
                    break;
                }
            }
            var channelNames = [];
            for (i = 0; source != null && i < source.channels.length; i++){
                channelNames[channelNames.length] = {name: source.name + source.channels[i].name,
                    displayName: source.name + "." + source.channels[i].name
                };
            }
            App.loadMustacheTemplate("connectorMgmtTemplates.html","settings",function(template){
                var config = App.getConnectorConfig(connector.connectorName);
                config.hasTimelineSettings = typeof(config.hasTimelineSettings)!="undefined"&&config.hasTimelineSettings;
                config.hasGeneralSettings = typeof(config.hasGeneralSettings)!="undefined"&&config.hasGeneralSettings
                App.makeModal(template.render({
                    connectorName:connector.connectorName,
                    name:connector.name,
                    config: config
                }));
                if (config.hasGeneralSettings) {
                    $("#generalSettingsLink").click(function(){
                        showGeneralSettings(connector, source);
                        $("#generalSettingsLink").parent().toggleClass("active");
                        $("#timelineSettingsLink").parent().toggleClass("active");
                    });
                    showGeneralSettings(connector, source);
                    $("#generalSettingsLink").parent().toggleClass("active");
                }
                if (config.hasTimelineSettings) {
                    $("#timelineSettingsLink").click(function(){
                        showTimelineSettings(connector, channelNames, source);
                        $("#generalSettingsLink").parent().toggleClass("active");
                        $("#timelineSettingsLink").parent().toggleClass("active");
                    });
                    if (!config.hasGeneralSettings) {
                        showTimelineSettings(connector, channelNames, source);
                        $("#timelineSettingsLink").parent().toggleClass("active");
                    }
                }
            });
        });
    }

    function showTimelineSettings(connector, channelNames, source) {
        console.log(connector.channels);
        App.loadMustacheTemplate("connectorMgmtTemplates.html","channel-settings",function(template){
            var settingsHtml = template.render({
                connectorName:connector.connectorName,
                name:connector.name,
                channelNames: channelNames
            });

            $("#connectorSettingsTab").empty();
            $("#connectorSettingsTab").append(settingsHtml);

            for (var i = 0; i < connector.channels.length; i++){
                var name = connector.channels[i];
                if (name == "")
                    break;
                var index = name.substring(0,name.indexOf(".")) + name.substring(name.indexOf(".") + 1);
                $("#" + index + "-checkbox")[0].checked = true;
            }

            $("#" + connector.connectorName + "SettingsDialog input").click(function(event){
                var channelList = "";
                connector.channels = [];
                for (var i = 0; source != null && i < source.channels.length; i++){
                    if ($("#" + source.name + source.channels[i].name + "-checkbox")[0].checked){
                        connector.channels.push(source.name + "." + source.channels[i].name);
                        if (channelList == "")
                            channelList = source.name + "." + source.channels[i].name;
                        else {
                            channelList += "," + source.name + "." + source.channels[i].name;
                        }
                    }
                }
                $.ajax({
                    url:"/api/connectors/" + connector.name + "/channels",
                    type:"POST",
                    data:{channels:channelList}
                })
            });
            $("#resetSettingsButton").hide();
        });

    }

    function showGeneralSettings(connector, source) {
        App.loadMustacheTemplate("connectorMgmtTemplates.html",connector.connectorName + "-settings",function(template){
            var settingsHandler = settingsHandlers[connector.connectorName];
            settingsHandler.loadSettings(connector.apiKeyId, connector, template);
            $("#resetSettingsButton").show();
        });
    }

    function viewUpdates(template, connector) {
        var connectorName = connector.connectorName.charAt(0).toUpperCase() + connector.connectorName.slice(1);
        $.ajax({
            url:"/api/updates/" + connector.connectorName + "?page=0&pageSize=50",
            success: function(updates) {
                for (var i=0; i<updates.length; i++)
                    updates[i].time = App.formatDate(updates[i].ts, true);
                var html = template.render({connectorName : connectorName,
                                            updates : updates});

                App.makeModal(html);
            }
        });
    }

    function handleSubmitForm(template, connector) {
        connector.uploadMessage= "We support upload of zip encoded json-formatted location history files as generated from " +
                                 "<a target=\"_blank\" href=\"https://www.google.com/takeout/#custom:latitude\"> Google Takeout </a><br>" +
                                 "<div class=\"alert alert-info\"><strong>Heads up!</strong> Please do not open the zip file provided by Google.  Just upload it as-is!</div>";
        var html = template.render({connector:connector});
        App.makeModal(html);
        var submitFileUploadForm = $("#submitFileUploadForm");
        console.log(submitFileUploadForm);
        submitFileUploadForm.click(function(event){
            event.stopImmediatePropagation();
            var formData = new FormData($("#fileUploadForm")[0]);
            $.ajax({
                url: "/upload/",
                method: "POST",
                data: formData,
                processData: false,
                contentType: false,
                success: function(response){
                    var status;
                    try { status = JSON.parse(response); }
                    catch(err) { alert("Couldn't upload data:" + err); }
                    if (status.result==="OK") {
                        $("#uploadModal").modal("hide");
                        App.activeApp.renderState(App.state.getState(App.activeApp.name),true);
                    }
                    else {
                        if (typeof(status.stackTrace)!="undefined")
                            console.log(status.stackTrace);
                        alert("Could upload data: " + status.message);
                    }
                }
            });
            console.log("we should send this file now...");
        });
    }


    function setToSyncing(connectorName){
        var row = $("#connector-" + connectorName);
        if (row.hasClass("nowSynchro"))
            return;
        row.addClass("nowSynchro");
        var syncLED = $("#syncLED-" + connectorName);
        syncLED.removeClass("syncLED-yes");
        syncLED.removeClass("syncLED-no");
        syncLED.addClass("syncLED-waiting");
        syncLED.hide();
        syncLED.parent().html("<span class=\"syncLED-waiting\">" +
                     "<i class=\"icon-refresh icon-spin\" style=\"font-size:30px\"></i>" +
                     "</span>");
        var lastSync = $("#lastSync-" + connectorName);
        lastSync.html("Now synchronizing");
        var syncNowBtn = $("#syncNow-" + connectorName);
        syncNowBtn.css("display","none");
    }

    function setAllToSyncing(){
        for (var i = 0; i < connectors.length; i++)
            setToSyncing(connectors[i].connectorName);
    }

    function confirmDelete(index){
        App.loadMustacheTemplate("connectorMgmtTemplates.html","deleteConnectorConfirm",function(template){
            var html = template.render(connectors[index]);

            $("body").append(html);
            $("#deleteConnectorConfirm").modal();

            $("#deleteConnectorConfirm").css("zIndex","1052");

            $("#deleteConnectorConfirm").on("hidden",function(){
                $("#deleteConnectorConfirm").remove();
            });

            var backdrops = $(".modal-backdrop");
            $(backdrops[backdrops.length - 1]).css("zIndex","1051");

            var confirmDelete = $("#confirmRemoveConnectorBtn");
            var cancelDelete = $("#cancelRemoveConnectorBtn");

            cancelDelete.click(function() {
                $("#deleteConnectorConfirm").modal("hide");
            });

            confirmDelete.click(function(){
                $.ajax({
                    url: "/api/connectors/" + connectors[index].connectorName,
                    type:"DELETE",
                    success: function() {
                        updateContents();
                        $("#deleteConnectorConfirm").modal("hide");
                    },
                    error: function() {
                        $("#deleteConnectorConfirm").modal("hide");
                    }
                });
            });
        });

    }

    function confirmRenew(connector) {
        App.loadMustacheTemplate("connectorMgmtTemplates.html","renewConnectorConfirm",function(template){
            var html = template.render(connector);

            $("body").append(html);
            $("#renewConnectorConfirm").modal();

            $("#renewConnectorConfirm").css("zIndex","1052");

            $("#renewConnectorConfirm").on("hidden",function(){
                $("#renewConnectorConfirm").remove();
            });

            var backdrops = $(".modal-backdrop");
            $(backdrops[backdrops.length - 1]).css("zIndex","1051");

            var confirmRenew = $("#confirmRenewConnectorBtn");
            var cancelRenew = $("#cancelRenewConnectorBtn");

            cancelRenew.click(function() {
                $("#renewConnectorConfirm").modal("hide");
            });

            confirmRenew.click(function(){
                $.ajax({
                    url: "/api/connectors/renew/" + connector.apiKeyId,
                    type:"POST",
                    success: function(result) {
                        console.log("redirect to: " + result.redirectTo);
                        window.location = result.redirectTo;
                        $("#renewConnectorConfirm").modal("hide");
                    },
                    error: function() {
                        $("#renewConnectorConfirm").modal("hide");
                    }
                });
            });
        });

    }

    var ManageConnectors = {};
    ManageConnectors.show = show;
    return ManageConnectors;
});
