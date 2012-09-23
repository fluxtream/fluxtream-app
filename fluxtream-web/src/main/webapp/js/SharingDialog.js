define(function() {

    function show(){
        $.ajax("/api/sharing/buddies",{
            success:function(buddies){
                App.loadMustacheTemplate("settingsTemplates.html","sharingDialog",function(template){
                    var html = template.render({buddies : buddies});
                    App.makeModal(html);
                });
            }
        });
    };

    function showConnectorsSharedWith(username) {
        console.log("hahahahahaha: " + username);
    }

    var Settings = {};
    Settings.show = show;
    Settings.showConnectorsSharedWith = showConnectorsSharedWith;
    return Settings;

});