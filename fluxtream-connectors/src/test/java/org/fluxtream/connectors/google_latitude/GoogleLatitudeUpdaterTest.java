package org.fluxtream.connectors.google_latitude;

import java.io.StringReader;
import java.util.ArrayList;
import org.fluxtream.connectors.location.LocationFacet;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * User: candide
 * Date: 13/08/13
 * Time: 04:58
 */
public class GoogleLatitudeUpdaterTest {

    @Test
    public void testImportFile() throws Exception {
//        GoogleLatitudeUpdater updater = new GoogleLatitudeUpdater();
//        //final InputStream resourceAsStream = GoogleLatitudeUpdaterTest.class.getResourceAsStream("/locationhistory-unwrapped.zip");
//        final InputStream resourceAsStream = GoogleLatitudeUpdaterTest.class.getResourceAsStream("/locationhistory-wrapped.zip");
//        final File file = File.createTempFile("resource", ".temp");
//        IOUtils.copy(resourceAsStream, new FileOutputStream(file));
//        final int imported = updater.importFile(null, file);
//        assertTrue(imported==88);
    }

    @Test
    public void testParseLocation() throws Exception {
        JsonFactory jfactory = new JsonFactory();
        JsonParser jParser = jfactory.createJsonParser(new StringReader("{\n" +
                                                                        "    \"timestampMs\" : \"1376053800229\",\n" +
                                                                        "    \"latitudeE7\" : 508261000,\n" +
                                                                        "    \"longitudeE7\" : 43543660,\n" +
                                                                        "    \"accuracy\" : 10\n" +
                                                                        "  }"));
        while (jParser.nextToken()!= JsonToken.START_OBJECT);
        while (jParser.getCurrentName()==null)
            jParser.nextToken();
        GoogleLatitudeUpdater updater = new GoogleLatitudeUpdater();
        final ArrayList<LocationFacet> locations = new ArrayList<LocationFacet>();
        updater.parseLocation(null, jParser, locations);
        assertTrue(locations.size()==1);
        final LocationFacet locationFacet = locations.get(0);
        assertTrue(locationFacet.timestampMs==1376053800229l);
        assertTrue(locationFacet.latitude==50.8261000f);
        assertTrue(locationFacet.longitude==4.3543660f);
        assertTrue(locationFacet.accuracy==10);
    }
}
