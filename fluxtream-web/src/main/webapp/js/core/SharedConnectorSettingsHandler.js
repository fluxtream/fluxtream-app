define(function() {

    function SharedConnectorSettingsHandler(connectorName) {
        this.connectorName = connectorName;
    }

    function loadSettings(apiKeyId, username, connectorName, connectorPrettyName, template) {
        var handler = this;
        $.ajax({
            url: "/api/v1/buddies/trusted/sharedConnector/"+apiKeyId+"/"+username,
            success: function(settings) {
                var settingsHtml = template.render({
                    connectorName: connectorName,
                    connectorPrettyName: connectorPrettyName,
                    settings: settings
                });
                App.makeModal(settingsHtml);
                handler.bindSettings(apiKeyId, username, settings);
            },
            error: function(jqXHR, statusText, errorThrown) {
                var errorMessage = errorThrown + ": " + jqXHR.responseText;
                console.log(errorMessage);
            }
        });
    };

    SharedConnectorSettingsHandler.prototype.loadSettings = loadSettings;

    function saveSettings(apiKeyId, username, settings) {
        $.ajax({
            url: "/api/v1/buddies/trusted/sharedConnector/" + apiKeyId + "/" + username,
            type: "post",
            data: {json : JSON.stringify(settings)},
            error: function(jqXHR, statusText, errorThrown) {
                var errorMessage = errorThrown + ": " + jqXHR.responseText;
                console.log(errorMessage);
                alert("Could not save settings: " + errorMessage);
            }
        });
    }

    SharedConnectorSettingsHandler.prototype.saveSettings = saveSettings;

    return SharedConnectorSettingsHandler;

});