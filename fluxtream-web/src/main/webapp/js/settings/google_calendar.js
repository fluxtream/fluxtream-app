define(["core/ConnectorSettingsHandler"], function(ConnectorSettingsHandler) {

    var GoogleCalendarSettingsHandler = new ConnectorSettingsHandler("google_calendar");

    GoogleCalendarSettingsHandler.bindSettings = function(container,settings, apiKeyId) {
        var calendars = settings.calendars;
        var setCalendarChecked = GoogleCalendarSettingsHandler.setCalendarChecked;
        var saveSettings = GoogleCalendarSettingsHandler.saveSettings;
        for (var i=0; i<calendars.length; i++) {
            var calendarCheckbox = getCalendarCheckbox(calendars[i].id);
            calendarCheckbox.click(function(e){
                var input = $(e.target);
                var isChecked = e.target.checked;
                var calendarId = input.val();
                setCalendarChecked(calendarId, isChecked, settings);
                saveSettings(apiKeyId, settings);
            });
        }
    }

    GoogleCalendarSettingsHandler.setCalendarChecked = function(calendarId, isChecked, settings) {
        for (var i=0; i<settings.calendars.length; i++) {
            if (settings.calendars[i].id===calendarId){
                if (!isChecked)
                    settings.calendars[i].hidden = true;
                else
                    settings.calendars[i].hidden = false;
            }
        }
    }

    function getCalendarCheckbox(id) {
        var result;
        $("#googleCalendarSettings input").each(function(jq,input){
            var value = $(input).val();
            if (value===id)
                result = $(input);
        });
        return result;
    }

    return GoogleCalendarSettingsHandler;

});