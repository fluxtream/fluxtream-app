define(function() {

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
        $.ajax("/api/coaching/coaches",{
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
        });
    }

    function findUser(username) {
        $.ajax({
            url: "/api/coaching/coaches/find",
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
        $.ajax("/api/coaching/coaches/" + username,{
            success:function(coach) {
                App.loadMustacheTemplate("settingsTemplates.html","sharedConnectors",function(template){
                    updateCoachesDropdown(username);
                    var html = template.render({connectors : FlxUtils.rowsOf(coach.sharedConnectors, 3),
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
                });
            }
        });
    }

    function addCoach(username) {
        $.ajax({
            url: "/api/coaching/coaches/"+username,
            type: "POST",
            success: function(status) {
                $('#findUserModal').modal('hide');
                showCoach(username);
            }
        });
    }

    function removeCoach(username) {
        $.ajax({
            url: "/api/coaching/coaches/"+username,
            type: "DELETE",
            success: function(status) {
                $("#removeCoachButton").remove();
                $("#sharedConnectors").empty();
                updateCoachesDropdown(null);
            }
        });
    }

    function toggleSharedConnector(username, connectorName, checked) {
        if (checked)
            addSharedConnector(username, connectorName);
        else
            removeSharedConnector(username, connectorName);
    }

    function addSharedConnector(username, connectorName) {
        $.ajax({
            url: "/api/coaching/coaches/" + username + "/connectors/" + connectorName,
            data: {connectorName : connectorName},
            type: "POST",
            success: function() {console.log("OK")}
        });
    }

    function removeSharedConnector(username, connectorName) {
        $.ajax({
            url: "/api/coaching/coaches/" + username + "/connectors/" + connectorName,
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