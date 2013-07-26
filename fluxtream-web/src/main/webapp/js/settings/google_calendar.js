define(["core/ConnectorSettingsHandler"], function(ConnectorSettingsHandler) {

    var GoogleCalendarSettingsHandler = new ConnectorSettingsHandler("google_calendar");

    GoogleCalendarSettingsHandler.bindSettings = function() {
        console.log("and this is the override bindSettings function");
    }

    return GoogleCalendarSettingsHandler;

});