define(function() {

    function ConnectorSettingsHandler(connectorName) {
        this.connectorName = connectorName;
    }

    function loadSettings(apiKeyId, connector, template) {
        var bindSettingsOverride = this.bindSettings;
        var handler = this;
        $.ajax({
            url: "/api/connectors/settings/"+apiKeyId,
            success: function(settings) {
                var settingsHtml = template.render({
                    connectorName:connector.connectorName,
                    name:connector.name,
                    settings: settings
                });
                var $connectorSettingsTab = $("#connectorSettingsTab");
                $connectorSettingsTab.empty();
                $connectorSettingsTab.append(settingsHtml);
                bindSettingsOverride(settings, apiKeyId);
                var $resetSettingsButton = $("#resetSettingsButton");
                $resetSettingsButton.unbind("click");
                $resetSettingsButton.click(function() {
                    console.log("click!");
                    resetSettings(handler, apiKeyId, connector, template);
                });
            }
        });
    };

    ConnectorSettingsHandler.prototype.loadSettings = loadSettings;

    function resetSettings(handler, apiKeyId, connector, template) {
        $.ajax({
            url: "/api/connectors/settings/reset/" + apiKeyId,
            type: "POST",
            success: function(){
                handler.loadSettings(apiKeyId, connector, template);
            }
        })
    }

    function bindSettings(settings, apiKeyId) {
        console.log("ConnectorSettingsHandler.bindSettings: not yet implemented!")
    }

    ConnectorSettingsHandler.prototype.bindSettings = bindSettings;

    function saveSettings(apiKeyId, settings) {
        $.ajax({
            url: "/api/connectors/settings/" + apiKeyId,
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

    ConnectorSettingsHandler.prototype.saveSettings = saveSettings;

    return ConnectorSettingsHandler;

});