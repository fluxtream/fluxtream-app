define(function() {

    function show(){
        App.loadMustacheTemplate("settingsTemplates.html","sharingDialog",function(template){
            var html = template.render();
            App.makeModal(html);
            updateBuddiesDropdown(null);
        });
    };

    function updateBuddiesDropdown(username) {
        if (username==null)
            username = "< No Buddy Selected >";
        $.ajax("/api/sharing/buddies",{
            success:function(buddies){
            App.loadMustacheTemplate("settingsTemplates.html","buddiesDropdown",function(template){
                var html = template.render({buddies : buddies, selectedBuddy : username});
                $("#buddiesDropdown").empty();
                $("#buddiesDropdown").append(html);
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
            url: "/api/sharing/buddies/find",
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

    function showBuddy(username) {
        $.ajax("/api/sharing/buddies/" + username,{
            success:function(buddy) {
                App.loadMustacheTemplate("settingsTemplates.html","sharedConnectors",function(template){
                    updateBuddiesDropdown(username);
                    var html = template.render({connectors : FlxUtils.rowsOf(buddy.sharedConnectors, 3),
                                                username : username});
                    $("#sharedConnectors").empty();
                    $("#sharedConnectors").append(html);
                    $("#removeBuddyButton").remove();
                    $("#buddiesDropdown").parent().append("<a class=\"btn btn-inverse\" " +
                                                          "id=\"removeBuddyButton\">Remove Buddy " +
                                                          "<i class=\"icon-trash\"></i></a>")
                    $("#removeBuddyButton").click(function(){
                        removeBuddy(username);
                    });
                });
            }
        });
    }

    function addBuddy(username) {
        $.ajax({
            url: "/api/sharing/buddies/"+username,
            type: "POST",
            success: function(status) {
                $('#findUserModal').modal('hide');
                showBuddy(username);
            }
        });
    }

    function removeBuddy(username) {
        $.ajax({
            url: "/api/sharing/buddies/"+username,
            type: "DELETE",
            success: function(status) {
                $("#removeBuddyButton").remove();
                $("#sharedConnectors").empty();
                updateBuddiesDropdown(null);
            }
        });
    }

    function toggleSharedConnector(username, connectorName, checked) {
        if (checked)
            addSharedConnector(username, connectorName);
        else
            removeSharedConnector(username, connectorName);
        console.log(username + "/" + connectorName + "/" + checked);
    }

    function addSharedConnector(username, connectorName) {
        $.ajax({
            url: "/api/sharing/buddies/" + username + "/connectors/" + connectorName,
            data: {connectorName : connectorName},
            type: "POST",
            success: function() {console.log("OK")}
        });
    }

    function removeSharedConnector(username, connectorName) {
        $.ajax({
            url: "/api/sharing/buddies/" + username + "/connectors/" + connectorName,
            type: "DELETE",
            success: function() {console.log("OK")}
        });
    }

    var SharingDialog = {};
    SharingDialog.show = show;
    SharingDialog.showBuddy = showBuddy;
    SharingDialog.addBuddy = addBuddy;
    SharingDialog.findUserDialog = findUserDialog;
    SharingDialog.toggleSharedConnector = toggleSharedConnector;
    return SharingDialog;

});