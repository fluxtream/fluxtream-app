package com.fluxtream.connectors.up;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import com.fluxtream.SimpleTimeInterval;
import com.fluxtream.TimeInterval;
import com.fluxtream.TimeUnit;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.ObjectType;
import com.fluxtream.connectors.bodytrackResponders.AbstractBodytrackResponder;
import com.fluxtream.connectors.vos.AbstractFacetVO;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.domain.GuestSettings;
import com.fluxtream.mvc.models.TimespanModel;
import com.fluxtream.services.impl.BodyTrackHelper;
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
        List<AbstractFacet> facets = getFacetsInTimespan(timeInterval, apiKey, sleepObjectType);
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
                items.add(moveTimespanModel);
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

}
