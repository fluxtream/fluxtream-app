define(["core/ConnectorSettingsHandler"], function(ConnectorSettingsHandler) {

    var SmsBackupSettingsHandler = new ConnectorSettingsHandler("sms_backup");

    SmsBackupSettingsHandler.bindSettings = function(container,settings, apiKeyId) {
        var that = this;
        container.find("input").blur(function(event){
            var settingId = event.delegateTarget.id;
            var newValue = event.delegateTarget.value;
            if (settings[settingId] != newValue){
                settings[settingId] = newValue;
                that.saveSettings(apiKeyId, settings);
            }
        })

    }

    return SmsBackupSettingsHandler;
});