define(["core/SharedConnectorSettingsHandler"], function(SharedConnectorSettingsHandler) {

    var EvernoteSharedConnectorSettingsHandler = new SharedConnectorSettingsHandler("evernote");

    EvernoteSharedConnectorSettingsHandler.bindSettings = function(container,settings, apiKeyId) {
    };

    EvernoteSharedConnectorSettingsHandler.setNotebookChecked = function(notebookId, isChecked, settings) {
    };

    return EvernoteSharedConnectorSettingsHandler;

});