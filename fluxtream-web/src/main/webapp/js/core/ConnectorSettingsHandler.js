define(function() {

    function ConnectorSettingsHandler(connectorName) {
        this.connectorName = connectorName;
    }

    ConnectorSettingsHandler.prototype.loadSettings = function(apiKeyId, connector, template) {
        console.log("loading settings " + apiKeyId);
        var bindSettings = this.bindSettings;
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
            }
        });
    };

    ConnectorSettingsHandler.prototype.bindSettings = function() {
        console.log("ConnectorSettingsHandler.bindSettings: not yet implemented!")
    };

    return ConnectorSettingsHandler;

});