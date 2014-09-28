define(function() {

    function ConnectorSettingsHandler(connectorName) {
        this.connectorName = connectorName;
    }

    function loadSettings(apiKeyId, connector, template) {
        var handler = this;
        $.ajax({
            url: "/api/v1/connectors/settings/"+apiKeyId,
            success: function(settings) {
                var settingsHtml = template.render({
                    connectorName:connector.connectorName,
                    name:connector.name,
                    settings: settings
                });
                var $connectorSettingsTab = $("#connectorSettingsTab");
                $connectorSettingsTab.empty();
                $connectorSettingsTab.append(settingsHtml);
                handler.bindSettings($connectorSettingsTab,settings, apiKeyId);
                var $resetSettingsButton = $("#resetSettingsButton");
                $resetSettingsButton.unbind("click");
                $resetSettingsButton.click(function() {
                    resetSettings(handler, apiKeyId, connector, template);
                });
            },
            error: function(){
                console.error("blahblahblah!")
            }
        });
    };

    ConnectorSettingsHandler.prototype.loadSettings = loadSettings;

    function resetSettings(handler, apiKeyId, connector, template) {
        $.ajax({
            url: "/api/v1/connectors/settings/reset/" + apiKeyId,
            type: "POST",
            success: function(){
                handler.loadSettings(apiKeyId, connector, template);
            }
        })
    }

    function bindSettings(settings, apiKeyId) {
        console.warn("ConnectorSettingsHandler.bindSettings: not yet implemented!")
    }

    ConnectorSettingsHandler.prototype.bindSettings = bindSettings;

    function saveSettings(apiKeyId, settings) {
        $.ajax({
            url: "/api/v1/connectors/settings/" + apiKeyId,
            type: "post",
            data: {json : JSON.stringify(settings)},
            error: function(jqXHR, statusText, errorThrown) {
                var errorMessage = errorThrown + ": " + jqXHR.responseText;
                console.log(errorMessage);
                alert("Could upload data: " + jqXHR.responseText);
            }
        });
    }

    ConnectorSettingsHandler.prototype.saveSettings = saveSettings;

    return ConnectorSettingsHandler;

});