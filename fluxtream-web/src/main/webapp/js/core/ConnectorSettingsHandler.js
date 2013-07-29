define(function() {

    function ConnectorSettingsHandler(connectorName) {
        this.connectorName = connectorName;
    }

    function loadSettings(apiKeyId, connector, template) {
        console.log("loading settings " + apiKeyId);
        $.ajax({
            url: "/api/connectors/settings/"+apiKeyId,
            success: function(settings) {
                console.log(settings);
                var settingsHtml = template.render({
                    connectorName:connector.connectorName,
                    name:connector.name,
                    settings: settings
                });
                $("#connectorSettingsTab").empty();
                $("#connectorSettingsTab").append(settingsHtml);
                bindSettings();
                $("#resetSettingsButton").unbind("click");
                $("#resetSettingsButton").click(function() {
                    resetSettings(apiKeyId, connector, template);
                });
            }
        });
    };

    ConnectorSettingsHandler.prototype.loadSettings = loadSettings;

    function resetSettings(apiKeyId, connector, template) {
        $.ajax({
            url: "/api/connectors/settings/reset/" + apiKeyId,
            type: "POST",
            success: function(){
                loadSettings(apiKeyId, connector, template);
            }
        })
    }

    function bindSettings() {
        console.log("ConnectorSettingsHandler.bindSettings: not yet implemented!")
    };

    ConnectorSettingsHandler.prototype.bindSettings = bindSettings;

    return ConnectorSettingsHandler;

});