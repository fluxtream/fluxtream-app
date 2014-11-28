package org.fluxtream.connectors.up;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
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
import org.fluxtream.core.services.impl.BodyTrackHelper;
import net.sf.json.JSONArray;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * User: candide
 * Date: 12/02/14
 * Time: 17:21
 */
@Component
public class JawboneUpBodytrackResponder extends AbstractBodytrackResponder {

    @Autowired
    BodyTrackHelper bodyTrackHelper;

    @Override
    public List<TimespanModel> getTimespans(final long startMillis, final long endMillis, final ApiKey apiKey, final String channelName) {
        List<TimespanModel> items = new ArrayList<TimespanModel>();
        final TimeInterval timeInterval = new SimpleTimeInterval(startMillis, endMillis, TimeUnit.ARBITRARY, TimeZone.getTimeZone("UTC"));

        int sleepObjectTypeValue = ObjectType.getObjectTypeValue(JawboneUpSleepFacet.class);
        final ObjectType sleepObjectType = ObjectType.getObjectType(Connector.getConnector("up"), sleepObjectTypeValue);
        List<AbstractFacet> facets = getFacetsInTimespanOrderedByEnd(timeInterval, apiKey, sleepObjectType);
        for (AbstractFacet facet : facets){
            JawboneUpSleepFacet sleepFacet = (JawboneUpSleepFacet) facet;
            if (StringUtils.isEmpty(sleepFacet.phasesStorage))
                continue;
            JSONArray sleepPhases = JSONArray.fromObject(sleepFacet.phasesStorage);
            for (int i=0; i<sleepPhases.size(); i++) {
                JSONArray sleepPhase = sleepPhases.getJSONArray(i);
                long start = sleepPhase.getLong(0);
                int phase = sleepPhase.getInt(1);
                long end = start + 30;
                if (i<sleepPhases.size()-1) {
                    JSONArray nextSleepPhase = sleepPhases.getJSONArray(i+1);
                    end = nextSleepPhase.getLong(0);
                }
                final TimespanModel moveTimespanModel = new TimespanModel(start*1000, end*1000-1, toPhaseString(phase), "up-sleep");
                simpleMergeAddTimespan(items,moveTimespanModel,startMillis,endMillis);
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
    public List<AbstractFacetVO<AbstractFacet>> getFacetVOs(final GuestSettings guestSettings, final ApiKey apiKey, final String objectTypeName,
                                                            final long start, final long end, final String value) {

        TimeInterval timeInterval = metadataService.getArbitraryTimespanMetadata(apiKey.getGuestId(), start, end).getTimeInterval();

        int sleepObjectTypeValue = ObjectType.getObjectTypeValue(JawboneUpSleepFacet.class);
        final ObjectType sleepObjectType = ObjectType.getObjectType(Connector.getConnector("up"), sleepObjectTypeValue);
        List<AbstractFacet> facets = getFacetsInTimespan(timeInterval, apiKey, sleepObjectType);

        List<AbstractFacetVO<AbstractFacet>> facetVOsForFacets = getFacetVOsForFacets(facets, timeInterval, guestSettings);
        return facetVOsForFacets;
    }

    @Override
    public void addToDeclaredChannelMappings(final ApiKey apiKey, final List<ChannelMapping> channelMappings) {
        ChannelMapping sleepChannelMapping = new ChannelMapping(
                apiKey.getId(), apiKey.getGuestId(),
                ChannelMapping.ChannelType.timespan,
                ChannelMapping.TimeType.gmt,
                ObjectType.getObjectType(apiKey.getConnector(), "sleep").value(),
                apiKey.getConnector().getDeviceNickname(), "sleep",
                apiKey.getConnector().getDeviceNickname(), "sleep");
        channelMappings.add(sleepChannelMapping);
    }

}
