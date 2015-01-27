define(["sharedConnectorSettings/evernote", "sharedConnectorSettings/google_calendar"],
    function(EvernoteSharedConnectorSettingsHandler, GoogleCalendarSharedConnectorSettingsHandler) {

    var sharedConnectorSettingsHandlers = {
        "evernote" : EvernoteSharedConnectorSettingsHandler,
        "google_calendar" : GoogleCalendarSharedConnectorSettingsHandler
    };

    function show(){
        App.loadMustacheTemplate("settingsTemplates.html","sharingDialog",function(template){
            var html = template.render();
            App.makeModal(html);
            $("#connectorSharingTabs a:first").tab("show");
            updateCoachesDropdown(null);
        });
    };

    function updateCoachesDropdown(username) {
        if (username==null)
            username = "< Please select a buddy >";
        $.ajax("/api/v1/buddies/trusted",{
            success:function(coaches){
            App.loadMustacheTemplate("settingsTemplates.html","coachesDropdown",function(template){
                var html = template.render({coaches : coaches, selectedCoach : username});
                $("#coachesDropdown").empty();
                $("#coachesDropdown").append(html);
            });
            }
        });
    }

    function findUserDialog() {
        App.loadMustacheTemplate("settingsTemplates.html","findUserDialog",function(template){
            var html = template.render({release : window.FLX_RELEASE_NUMBER});
            App.makeModal(html);
            $(".loading-animation").hide();
            $("#findUserField").focus();
            $("#findUserField").keypress(function(evt) {
                if (evt.which==13) {
                    findUser($("#findUserField").val());
                    $(".loading-animation").css("display", "inline");
                }
            });
            $("#findUserButton").click(function(event){
                findUser($("#findUserField").val());
                $(".loading-animation").css("display", "inline");
            });
        });
    }

    function findUser(username) {
        $.ajax({
            url: "/api/v1/buddies/find",
            data: {username : username},
            type: "POST",
            statusCode: {
                200: function(guest, statusText, jqXHR) {
                    $(".loading-animation").hide();
                    $("#findUserMessage").removeAttr("class");
                    var message = "<i class=\"icon-ok\"></i> Found User! Is " + guest["fullname"]
                        + " the person you are looking for? "
                        +  "If yes, please hit the 'Add Buddy' button below";
                    $("#findUserMessage").html(message);
                    $("#findUserMessage").addClass("alert alert-success");
                    $("#shareMyDataButton").removeClass("disabled");
                },
                404: function(jqXHR, statusText, errorThrown) {
                    $(".loading-animation").hide();
                    $("#findUserMessage").html("<i class=\"icon-exclamation-sign\"></i> " + errorThrown + ": " + jqXHR.responseText);
                    $("#findUserMessage").addClass("alert alert-error");
                    $("#shareMyDataButton").addClass("disabled");
                },
                400: function(jqXHR, statusText, errorThrown) {
                    $(".loading-animation").hide();
                    $("#findUserMessage").html("<i class=\"icon-exclamation-sign\"></i> " + errorThrown + ": " + jqXHR.responseText);
                    $("#findUserMessage").addClass("alert alert-error");
                    $("#shareMyDataButton").addClass("disabled");
                }
            }
        });
    }

    function showCoach(username) {
        $.ajax("/api/v1/buddies/trusted/" + username + "/connectors",{
            success:function(coach) {
                bindSharedConnectors(coach, username);
                bindSharedChannels(coach, username);
            }
        });
    };

    function bindSharedConnectors(coach, username) {
        App.loadMustacheTemplate("settingsTemplates.html","sharedConnectors",function(template){
            $(".remove-connector-warning").remove();
            updateCoachesDropdown(username);
            var html = template.render({connectors : coach.sharedConnectors,
                username : username});
            $("#sharedConnectors").empty().append(html);

            $("#removeCoachButton").remove();
            $("#coachesDropdown").parent().append("<a class=\"btn btn-inverse\" " +
            "id=\"removeCoachButton\">Remove Buddy " +
            "<i class=\"icon-trash\"></i></a>")
            $("#removeCoachButton").click(function(){
                removeCoach(username);
            });
            $("#sharedConnectors .sharedConnectorSettingsBtn").click(function(evt){
                var connectorName = $(evt.target).attr("data-connectorName");
                var connectorPrettyName = $(evt.target).attr("data-connectorPrettyName");
                var apiKeyId = $(evt.target).attr("data-apiKeyId");
                App.loadMustacheTemplate("connectorMgmtTemplates.html",connectorName + "-sharedConnector-settings",function(template){
                    var settingsHandler = sharedConnectorSettingsHandlers[connectorName];
                    settingsHandler.loadSettings(apiKeyId, username, connectorName, connectorPrettyName, template);
                });
            });
        });
    }

    function bindSharedChannels(coach, username) {
        App.loadMustacheTemplate("settingsTemplates.html","sharedChannels",function(template){
            $(".remove-connector-warning").remove();
            var devices = [];
            for (var name in coach["sharedChannels"]) {
                var deviceName = _.isUndefined(coach["sharedChannels"][name][0])?null:coach["sharedChannels"][name][0]["deviceName"];
                devices.push({
                    name : name,
                    deviceName: deviceName,
                    channels : coach["sharedChannels"][name]
                });
            }
            var html = template.render({devices: devices,
                username : username});
            $("#sharedChannels").empty().append(html);
            $(".device-tree-parent").unbind().click(function(){
                var deviceName = $(this).attr("data-deviceName");
                $(".device-"+deviceName).toggle();
                $(this).find("i").toggleClass("icon-chevron-down");
                $(this).find("i").toggleClass("icon-chevron-right");
            });
            $(".toggleChannelSharing").unbind().click(function(){
                var channelId = $(this).attr("data-channelId");
                toggleSharedChannel(username, channelId, this);
            });
        });
    }

    function addCoach(username) {
        $.ajax({
            url: "/api/v1/buddies/trusted/"+username,
            type: "POST",
            success: function(status) {
                $('#findUserModal').modal('hide');
                showCoach(username);
            }
        });
    }

    function removeCoach(username) {
        $.ajax({
            url: "/api/v1/buddies/trusted/"+username,
            type: "DELETE",
            success: function(status) {
                $("#removeCoachButton").remove();
                $("#sharedConnectors").empty();
                $("#sharedChannels").empty();
                updateCoachesDropdown(null);
            }
        });
    }

    function toggleSharedChannel(username, channelId, checkbox) {
        if (checkbox.checked) {
            addSharedChannel(username, channelId);
        } else {
            removeSharedChannel(username, channelId);
        }
    }

    function addSharedChannel(username, channelId) {
        $.ajax({
            url: "/api/v1/buddies/trusted/" + username + "/channels/" + channelId + "/shared",
            data: {value : true},
            type: "PUT",
            success: function() {console.log("OK")}
        });
    }

    function removeSharedChannel(username, channelId) {
        $.ajax({
            url: "/api/v1/buddies/trusted/" + username + "/channels/" + channelId + "/shared",
            data: {value : false},
            type: "PUT",
            success: function() {console.log("OK")}
        });
    }

    function toggleSharedConnector(username, connectorName, checkbox) {
        if (checkbox.checked) {
            $(".remove-connector-warning").remove();
            addSharedConnector(username, connectorName);
            $(checkbox).parent().find(".sharedConnectorSettingsBtn").show();
        } else {
            $("#sharingModal > .modal-footer").prepend('<div class="remove-connector-warning alert alert-warning">Please note that you may still have shared channels for this connector!</div>');
            removeSharedConnector(username, connectorName);
            $(checkbox).parent().find(".sharedConnectorSettingsBtn").hide();
        }
    }

    function addSharedConnector(username, connectorName) {
        $.ajax({
            url: "/api/v1/buddies/trusted/" + username + "/connectors/" + connectorName,
            data: {connectorName : connectorName},
            type: "POST",
            success: function() {console.log("OK")}
        });
    }

    function removeSharedConnector(username, connectorName) {
        $.ajax({
            url: "/api/v1/buddies/trusted/" + username + "/connectors/" + connectorName,
            type: "DELETE",
            success: function() {console.log("OK")}
        });
    }

    var SharingDialog = {};
    SharingDialog.show = show;
    SharingDialog.showCoach = showCoach;
    SharingDialog.addCoach = addCoach;
    SharingDialog.findUserDialog = findUserDialog;
    SharingDialog.toggleSharedConnector = toggleSharedConnector;
    return SharingDialog;

});