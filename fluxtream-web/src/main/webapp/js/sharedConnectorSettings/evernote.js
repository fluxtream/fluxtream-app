define(["core/SharedConnectorSettingsHandler"], function(SharedConnectorSettingsHandler) {

    var EvernoteSharedConnectorSettingsHandler = new SharedConnectorSettingsHandler("evernote");

    EvernoteSharedConnectorSettingsHandler.bindSettings = function(apiKeyId, username, settings) {
        var setNotebookChecked = EvernoteSharedConnectorSettingsHandler.setNotebookChecked;
        var saveSettings = EvernoteSharedConnectorSettingsHandler.saveSettings;
        $("#evernote-sharedConnector-settings input").click(function(evt){
            var notebookGuid = $(evt.target).attr("data-guid");
            var isChecked = evt.target.checked;
            setNotebookChecked(notebookGuid, isChecked, settings);
            saveSettings(apiKeyId, username, settings);
        });
    };

    EvernoteSharedConnectorSettingsHandler.setNotebookChecked = function(notebookGuid, isChecked, settings) {
        for (var i=0; i<settings.notebooks.length; i++) {
            if (settings.notebooks[i].guid===notebookGuid){
                if (isChecked)
                    settings.notebooks[i].shared = true;
                else
                    settings.notebooks[i].shared = false;
            }
        }
    };

    return EvernoteSharedConnectorSettingsHandler;

});