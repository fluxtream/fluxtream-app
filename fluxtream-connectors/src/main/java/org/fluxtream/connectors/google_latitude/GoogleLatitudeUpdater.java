package org.fluxtream.connectors.google_latitude;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.fluxtream.core.aspects.FlxLogger;
import org.fluxtream.core.connectors.Connector.UpdateStrategyType;
import org.fluxtream.core.connectors.FileUploadSupport;
import org.fluxtream.core.connectors.annotations.Updater;
import org.fluxtream.core.connectors.location.LocationFacet;
import org.fluxtream.core.connectors.updaters.AbstractUpdater;
import org.fluxtream.core.connectors.updaters.UpdateInfo;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.domain.Notification;
import org.fluxtream.core.services.ApiDataService;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Updater(prettyName = "Latitude", value = 2, objectTypes = { LocationFacet.class }, updateStrategyType = UpdateStrategyType.INCREMENTAL)
public class GoogleLatitudeUpdater extends AbstractUpdater implements FileUploadSupport {

    FlxLogger logger = FlxLogger.getLogger(GoogleLatitudeUpdater.class);

    @Autowired
    ApiDataService apiDataService;

	public GoogleLatitudeUpdater() {
		super();
	}

	@Override
	public void updateConnectorDataHistory(UpdateInfo updateInfo)
			throws Exception {
        if (guestService.getApiKeyAttribute(updateInfo.apiKey, "googleConsumerKey")!=null) {
            sendServiceDiscontinuedWarning(updateInfo);
            cleanupOldTokens(updateInfo);
        }
	}

    public void updateConnectorData(UpdateInfo updateInfo) throws Exception {
        if (guestService.getApiKeyAttribute(updateInfo.apiKey, "googleConsumerKey")!=null) {
            sendServiceDiscontinuedWarning(updateInfo);
            cleanupOldTokens(updateInfo);
        }
	}

    @Override
    public void setDefaultChannelStyles(ApiKey apiKey) {}

    private void sendServiceDiscontinuedWarning(final UpdateInfo updateInfo) {
        notificationsService.addNamedNotification(updateInfo.getGuestId(), Notification.Type.WARNING, connector().statusNotificationName(),
                                                  "Heads Up. Google recently discontinued support for their Latitude service.<br>" +
                                                  "However, Google Takeout will let you get a backup of your data that you will be able to import in Fluxtream.<br>" +
                                                  "If you choose to do this, please head to <a href=\"javascript:App.manageConnectors()\">Manage Connectors</a>,<br>" +
                                                  "go to the Google Latitude connector section and click on the upload icon <i class=\"icon-arrow-up\"></i>," +
                                                  "To track your location, we now recommend using the <a target=\"_blank\" href\"http://movesapp.com/\">Moves App<a>.");
    }

    private void cleanupOldTokens(final UpdateInfo updateInfo) {
        guestService.removeApiKeyAttribute(updateInfo.apiKey.getId(), "googleConsumerKey");
        if (guestService.getApiKeyAttribute(updateInfo.apiKey, "googleConsumerSecret")!=null)
            guestService.removeApiKeyAttribute(updateInfo.apiKey.getId(), "googleConsumerSecret");
    }

    @Override
    public int importFile(final ApiKey apiKey, final File f) throws Exception {
        try {
            ZipFile zipFile = new ZipFile(f);
            final Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                final ZipEntry zipEntry = entries.nextElement();
                if (zipEntry.isDirectory()) continue;
                if (zipEntry.getName().endsWith("LocationHistory.json"))
                    return parseLocations(apiKey, zipFile.getInputStream(zipEntry));
            }
            throw new RuntimeException("Couldn't find LocationHistory.json in the uploaded zip file");
        }
        catch (Exception e) {
            notificationsService.addNamedNotification(apiKey.getGuestId(), Notification.Type.WARNING, connector().statusNotificationName(),
                                                      "Failed to import Google Latitude zip file, error is:<br>" +
                                                      e.getMessage());
            throw (e);
        }
    }

    private int parseLocations(final ApiKey apiKey, final InputStream inputStream) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonFactory jfactory = mapper.getJsonFactory();
        JsonParser jParser = jfactory.createJsonParser(inputStream);

        JsonToken token;
        List<LocationFacet> locations = new ArrayList<LocationFacet>();

        getToLocationData(jParser);

        int nLocations = 0;
        while ((token = jParser.nextToken())!=null) {
            if (token==JsonToken.START_OBJECT) {
                jParser.nextToken();
                parseLocation(apiKey, jParser, locations);
                if (locations.size()==1000) {
                    nLocations += 1000;
                    boolean connectorIsLive = persistLocations(apiKey, locations);
                    if (!connectorIsLive)
                        break;
                    locations.clear();
                }
            }
        }
        nLocations += locations.size();
        if (apiKey!=null) persistLocations(apiKey, locations);
        return nLocations;
    }

    private boolean persistLocations(ApiKey apiKey, List<LocationFacet> locations) {
        if (guestService.getApiKey(apiKey.getId())!=null){
            apiDataService.addGuestLocations(apiKey.getGuestId(), locations);
            return true;
        } else {
            return false;
        }
    }

    private void getToLocationData(final JsonParser jParser) throws IOException {
        // The start of the LocationHistory.json file looks like this:
        // {
        //  "somePointsHidden" : true,
        //  "locations" : [ {
        //    "timestampMs" : "1380841104348",

        // Get to the first open brace
        while (jParser.nextToken()!=JsonToken.START_OBJECT);
        // Go forward until currentName is set
        while (jParser.getCurrentName()==null)
            jParser.nextToken();

        // Go forward until we reach the "locations" array
        while (!jParser.getCurrentName().equals("locations"))
            jParser.nextToken();
    }

    void parseLocation(final ApiKey apiKey, final JsonParser jParser, final List<LocationFacet> locations) throws IOException {
        LocationFacet locationFacet = new LocationFacet();
        if (apiKey!=null) {
            locationFacet.apiKeyId = apiKey.getId();
            locationFacet.guestId = apiKey.getGuestId();
        }

        locationFacet.timeUpdated = System.currentTimeMillis();
        locationFacet.source = LocationFacet.Source.GOOGLE_LATITUDE;

        do {
            String fieldName = jParser.getCurrentName();
            final JsonToken jsonToken = jParser.nextValue();
            if (jsonToken.isScalarValue()) {
                if (fieldName.equals("timestampMs")) {
                    long ts = Long.valueOf(jParser.getText());
                    locationFacet.timestampMs = ts;
                    locationFacet.start = ts;
                    locationFacet.end = ts;
                    locationFacet.api = 2;
                } else if (fieldName.equals("accuracy")) {
                    int accuracy = jParser.getIntValue();
                    locationFacet.accuracy = accuracy;
                } else if (fieldName.equals("altitude")) {
                    int altitude = jParser.getIntValue();
                    locationFacet.altitude = altitude;
                } else if (fieldName.equals("heading")) {
                    int heading = jParser.getIntValue();
                    locationFacet.heading = heading;
                } else if (fieldName.equals("latitudeE7")) {
                    int lat = jParser.getIntValue();
                    locationFacet.latitude = lat/1E7f;
                } else if (fieldName.equals("longitudeE7")) {
                    int lon = jParser.getIntValue();
                    locationFacet.longitude = lon/1E7f;
                } else if (fieldName.equals("velocity")) {
                    int speed = jParser.getIntValue();
                    locationFacet.speed = speed;
                }
            } else {
                final JsonNode junk = jParser.readValueAsTree();
                logger.info("we are not interested in this junk: " + junk.toString());
            }
        } while (jParser.nextToken() != JsonToken.END_OBJECT);
        locations.add(locationFacet);
    }

}
