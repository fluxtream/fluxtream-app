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
        $.ajax("/api/v1/coaching/coaches",{
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
            url: "/api/v1/coaching/coaches/find",
            data: {username : username},
            type: "POST",
            success: function(status) {
                $(".loading-animation").hide();
                $("#findUserMessage").removeAttr("class");
                if (status.result=="OK") {
                    var message = "<i class=\"icon-ok\"></i> " + status.message + " Is " + status.payload.fullname
                                      + " the person you're looking for? "
                        +  "If yes, please hit the 'Add Buddy' button below";
                    $("#findUserMessage").html(message);
                    $("#findUserMessage").addClass("alert alert-success");
                    $("#shareMyDataButton").removeClass("disabled");
                } else {
                    $("#findUserMessage").html("<i class=\"icon-exclamation-sign\"></i> " + status.message);
                    $("#findUserMessage").addClass("alert alert-error");
                    $("#shareMyDataButton").addClass("disabled");
                }
            }
        });
    }

    function showCoach(username) {
        $.ajax("/api/v1/coaching/coaches/" + username,{
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
            url: "/api/v1/coaching/coaches/"+username,
            type: "POST",
            success: function(status) {
                $('#findUserModal').modal('hide');
                showCoach(username);
            }
        });
    }

    function removeCoach(username) {
        $.ajax({
            url: "/api/v1/coaching/coaches/"+username,
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
            url: "/api/v1/coaching/coaches/" + username + "/connectors/" + connectorName,
            data: {connectorName : connectorName},
            type: "POST",
            success: function() {console.log("OK")}
        });
    }

    function removeSharedConnector(username, connectorName) {
        $.ajax({
            url: "/api/v1/coaching/coaches/" + username + "/connectors/" + connectorName,
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