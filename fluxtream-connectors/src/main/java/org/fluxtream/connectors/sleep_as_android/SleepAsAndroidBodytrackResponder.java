package org.fluxtream.connectors.sleep_as_android;

import com.google.gdata.util.common.base.Pair;
import org.fluxtream.core.SimpleTimeInterval;
import org.fluxtream.core.TimeInterval;
import org.fluxtream.core.TimeUnit;
import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.connectors.ObjectType;
import org.fluxtream.core.connectors.bodytrackResponders.AbstractBodytrackResponder;
import org.fluxtream.core.connectors.vos.AbstractFacetVO;
import org.fluxtream.core.domain.AbstractFacet;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.domain.ChannelMapping;
import org.fluxtream.core.domain.GuestSettings;
import org.fluxtream.core.mvc.models.TimespanModel;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

@Component
public class SleepAsAndroidBodytrackResponder extends AbstractBodytrackResponder {
    @Override
    public List<TimespanModel> getTimespans(long startMillis, long endMillis, ApiKey apiKey, String channelName) {
        List<TimespanModel> items = new ArrayList<TimespanModel>();
        final TimeInterval timeInterval = new SimpleTimeInterval(startMillis, endMillis, TimeUnit.ARBITRARY, TimeZone.getTimeZone("UTC"));
        Connector connector = apiKey.getConnector();
        final ObjectType sleep_type = ObjectType.getObjectType(connector, "sleep");

        String objectTypeName = apiKey.getConnector().getName() + "-" + sleep_type.getName();
        List<AbstractFacet> facets = getFacetsInTimespan(timeInterval,apiKey, sleep_type);

        for (AbstractFacet facet : facets) {
            SleepFacet sleepFacet = (SleepFacet) facet;
            TimespanModel buildingLightModel = null;
            TimespanModel buildingDeepModel = null;
            TimespanModel buildingRemModel = null;
            //since we know these are sorted by time we won't need to worry about an end before a start
            for (Pair<String,Long> eventLabel : sleepFacet.getEventLabels()) {
                String s = eventLabel.getFirst();
                if (s.equals("DEEP_START")) {
                    buildingDeepModel = new TimespanModel(eventLabel.getSecond(),eventLabel.getSecond(),"deep",objectTypeName);
                }
                else if (s.equals("DEEP_END")) {
                    buildingDeepModel.setEnd(eventLabel.getSecond()/1000);
                    items.add(buildingDeepModel);
                    buildingDeepModel = null;
                }
                else if (s.equals("LIGHT_START")) {
                    buildingLightModel = new TimespanModel(eventLabel.getSecond(),eventLabel.getSecond(),"light",objectTypeName);
                }
                else if (s.equals("LIGHT_END")) {
                    buildingLightModel.setEnd(eventLabel.getSecond()/1000);
                    items.add(buildingLightModel);
                    buildingLightModel = null;
                }
                else if (s.equals("REM_START")) {
                    buildingRemModel = new TimespanModel(eventLabel.getSecond(),eventLabel.getSecond(),"rem",objectTypeName);
                }
                else if (s.equals("REM_END")) {
                    buildingRemModel.setEnd(eventLabel.getSecond()/1000);
                    items.add(buildingRemModel);
                    buildingRemModel = null;
                }
            }
        }
        return items;
    }

    @Override
    public List<AbstractFacetVO<AbstractFacet>> getFacetVOs(GuestSettings guestSettings, ApiKey apiKey, String objectTypeName, long start, long end, String value) {
        Connector connector = apiKey.getConnector();

        TimeInterval timeInterval = metadataService.getArbitraryTimespanMetadata(apiKey.getGuestId(), start, end).getTimeInterval();

        final ObjectType recent_track = ObjectType.getObjectType(connector, "sleep");

        List<AbstractFacet> facets = getFacetsInTimespan(timeInterval, apiKey, recent_track);

        return getFacetVOsForFacets(facets,timeInterval,guestSettings);
    }

    @Override
    public void addToDeclaredChannelMappings(ApiKey apiKey, List<ChannelMapping> mappings) {
        ChannelMapping movesDataChannelMapping = new ChannelMapping(
                apiKey.getId(), apiKey.getGuestId(),
                ChannelMapping.ChannelType.timespan,
                ChannelMapping.TimeType.gmt,
                ObjectType.getObjectType(apiKey.getConnector(), "sleep").value(),
                apiKey.getConnector().getDeviceNickname(), "sleep",
                apiKey.getConnector().getDeviceNickname(), "sleep");
        mappings.add(movesDataChannelMapping);
    }
}
