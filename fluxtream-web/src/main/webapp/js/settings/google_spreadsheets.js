define(["core/ConnectorSettingsHandler"], function(ConnectorSettingsHandler) {

  var GoogleSpreadsheetsSettingsHandler = new ConnectorSettingsHandler("google_spreadsheets");

  GoogleSpreadsheetsSettingsHandler.bindSettings = function(container,settings, apiKeyId) {
  };

  return GoogleSpreadsheetsSettingsHandler;

});