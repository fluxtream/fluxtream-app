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
                App.loadMustacheTemplate("settingsTemplates.html","sharedConnectors",function(template){
                    updateCoachesDropdown(username);
                    var html = template.render({connectors : coach.sharedConnectors,
                                                username : username});
                    $("#sharedConnectors").empty();
                    $("#sharedConnectors").append(html);
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
                updateCoachesDropdown(null);
            }
        });
    }

    function toggleSharedConnector(username, connectorName, checkbox) {
        if (checkbox.checked) {
            addSharedConnector(username, connectorName);
            $(checkbox).parent().find(".sharedConnectorSettingsBtn").show();
        } else {
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