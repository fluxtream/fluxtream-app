package org.fluxtream.connectors.misfit;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
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
import org.joda.time.format.ISODateTimeFormat;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

/**
 * Created by candide on 24/02/15.
 */
@Component
public class MisfitBodytrackResponder extends AbstractBodytrackResponder {

    @Override
    public List<TimespanModel> getTimespans(long startMillis, long endMillis, ApiKey apiKey, String channelName) {
        List<TimespanModel> items = new ArrayList<TimespanModel>();
        final TimeInterval timeInterval = new SimpleTimeInterval(startMillis, endMillis, TimeUnit.ARBITRARY, TimeZone.getTimeZone("UTC"));

        int sleepObjectTypeValue = ObjectType.getObjectTypeValue(MisfitSleepFacet.class);
        final ObjectType sleepObjectType = ObjectType.getObjectType(Connector.getConnector("misfit"), sleepObjectTypeValue);
        List<AbstractFacet> facets = getFacetsInTimespanOrderedByEnd(timeInterval, apiKey, sleepObjectType);
        for (AbstractFacet facet : facets){
            MisfitSleepFacet sleepFacet = (MisfitSleepFacet) facet;
            if (StringUtils.isEmpty(sleepFacet.sleepDetails))
                continue;
            JSONArray sleepPhases = JSONArray.fromObject(sleepFacet.sleepDetails);
            for (int i=0; i<sleepPhases.size(); i++) {
                JSONObject sleepPhase = sleepPhases.getJSONObject(i);
                long start = ISODateTimeFormat.dateTimeNoMillis().parseMillis(sleepPhase.getString("datetime"));
                int phase = sleepPhase.getInt("value");
                long end = start + 30;
                if (i<sleepPhases.size()-1) {
                    JSONObject nextSleepPhase = sleepPhases.getJSONObject(i + 1);
                    end = ISODateTimeFormat.dateTimeNoMillis().parseMillis(nextSleepPhase.getString("datetime"));
                }
                final TimespanModel timespanModel = new TimespanModel(start, end-1, toPhaseString(phase), "misfit-sleep");
//                simpleMergeAddTimespan(items,timespanModel,startMillis,endMillis);
                items.add(timespanModel);
            }
        }
        return items;
    }

    private String toPhaseString(final int phase) {
        switch(phase) {
            case 1:
                return "wake";
            case 2:
                return "light";
            case 3:
                return "deep";
            default:
                return "wake";
        }
    }

    @Override
    public List<AbstractFacetVO<AbstractFacet>> getFacetVOs(GuestSettings guestSettings, ApiKey apiKey, String objectTypeName, long start, long end, String value) {
        TimeInterval timeInterval = metadataService.getArbitraryTimespanMetadata(apiKey.getGuestId(), start, end).getTimeInterval();

        int sleepObjectTypeValue = ObjectType.getObjectTypeValue(MisfitSleepFacet.class);
        final ObjectType sleepObjectType = ObjectType.getObjectType(Connector.getConnector("misfit"), sleepObjectTypeValue);
        List<AbstractFacet> facets = getFacetsInTimespan(timeInterval, apiKey, sleepObjectType);

        List<AbstractFacetVO<AbstractFacet>> facetVOsForFacets = getFacetVOsForFacets(facets, timeInterval, guestSettings);
        return facetVOsForFacets;
    }

    @Override
    public void addToDeclaredChannelMappings(ApiKey apiKey, List<ChannelMapping> mappings) {
        ChannelMapping sleepChannelMapping = new ChannelMapping(
                apiKey.getId(), apiKey.getGuestId(),
                ChannelMapping.ChannelType.timespan,
                ChannelMapping.TimeType.gmt,
                ObjectType.getObjectType(apiKey.getConnector(), "sleep").value(),
                apiKey.getConnector().getDeviceNickname(), "sleep",
                apiKey.getConnector().getDeviceNickname(), "sleep");
        mappings.add(sleepChannelMapping);
    }
}
