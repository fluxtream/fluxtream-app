define(["core/ConnectorSettingsHandler"], function(ConnectorSettingsHandler) {

    var EvernoteSettingsHandler = new ConnectorSettingsHandler("evernote");

    EvernoteSettingsHandler.bindSettings = function(container,settings, apiKeyId) {
        var notebooks = settings.notebooks;
        var setNotebookChecked = EvernoteSettingsHandler.setNotebookChecked;
        var setNotebookColor = EvernoteSettingsHandler.setNotebookColor;
        var saveSettings = EvernoteSettingsHandler.saveSettings;
        for (var i=0; i<notebooks.length; i++) {
            var notebookCheckbox = $("#notebook-hcb-"+notebooks[i].guid);
            notebookCheckbox.click(function(e){
                var input = $(e.target);
                var isChecked = e.target.checked;
                var notebookId = input.val();
                setNotebookChecked(notebookId, isChecked, settings);
                saveSettings(apiKeyId, settings);
            });
            var colorInput = $("#notebook-c-"+notebooks[i].guid);
            colorInput.change({guid:notebooks[i].guid},function(e){
                var input = $(e.target);
                setNotebookColor(e.data.guid, input.val(), settings);
                saveSettings(apiKeyId, settings);
            });
        }
    };

    EvernoteSettingsHandler.setNotebookColor = function(notebookId, color, settings) {
        for (var i=0; i<settings.notebooks.length; i++) {
            if (settings.notebooks[i].guid===notebookId){
                settings.notebooks[i].backgroundColor = color;
            }
        }
    };

    EvernoteSettingsHandler.setNotebookChecked = function(notebookId, isChecked, settings) {
        for (var i=0; i<settings.notebooks.length; i++) {
            if (settings.notebooks[i].guid===notebookId){
                if (!isChecked)
                    settings.notebooks[i].hidden = true;
                else
                    settings.notebooks[i].hidden = false;
            }
        }
    };

    return EvernoteSettingsHandler;

});