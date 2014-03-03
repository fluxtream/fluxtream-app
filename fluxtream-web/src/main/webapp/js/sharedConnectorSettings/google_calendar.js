define(["core/SharedConnectorSettingsHandler"], function(SharedConnectorSettingsHandler) {

    var GoogleCalendarSharedConnectorSettingsHandler = new SharedConnectorSettingsHandler("google_calendar");

    GoogleCalendarSharedConnectorSettingsHandler.bindSettings = function(apiKeyId, username, settings) {
        var setCalendarChecked = GoogleCalendarSharedConnectorSettingsHandler.setCalendarChecked;
        var saveSettings = GoogleCalendarSharedConnectorSettingsHandler.saveSettings;
        $("#google_calendar-sharedConnector-settings input").click(function(evt){
            var calendarId = $(evt.target).attr("data-calendarId");
            var isChecked = evt.target.checked;
            setCalendarChecked(calendarId, isChecked, settings);
            saveSettings(apiKeyId, username, settings);
        });
    };

    GoogleCalendarSharedConnectorSettingsHandler.setCalendarChecked = function(calendarId, isChecked, settings) {
        for (var i=0; i<settings.calendars.length; i++) {
            if (settings.calendars[i].id===calendarId){
                if (isChecked)
                    settings.calendars[i].shared = true;
                else
                    settings.calendars[i].shared = false;
            }
        }
    };

    return GoogleCalendarSharedConnectorSettingsHandler;

});