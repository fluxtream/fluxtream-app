define(function() {

    function SharedConnectorSettingsHandler(connectorName) {
        this.connectorName = connectorName;
    }

    function loadSettings(apiKeyId, username, connectorName, connectorPrettyName, template) {
        $.ajax({
            url: "/api/coaching/sharedConnector/"+apiKeyId+"/"+username,
            success: function(settings) {
                var settingsHtml = template.render({
                    connectorName: connectorName,
                    connectorPrettyName: connectorPrettyName,
                    settings: settings
                });
                App.makeModal(settingsHtml);
            },
            error: function(){
                console.error("blahblahblah!")
            }
        });
    };

    SharedConnectorSettingsHandler.prototype.loadSettings = loadSettings;

    function saveSettings(apiKeyId, username, settings) {
        $.ajax({
            url: "/api/coaching/sharedConnector/" + apiKeyId + "/" + username,
            type: "post",
            data: {json : JSON.stringify(settings)},
            success: function(status){
                console.log(status);
                if(!status.result) {
                    alert("Oops, we could not save your settings:" + status.message);
                }
            }
        });
    }

    SharedConnectorSettingsHandler.prototype.saveSettings = saveSettings;

    return SharedConnectorSettingsHandler;

});