package org.fluxtream.connectors.beddit;

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
public class BedditBodytrackResponder extends AbstractBodytrackResponder {

    @Override
    public List<TimespanModel> getTimespans(long startMillis, long endMillis, ApiKey apiKey, String channelName) {
        List<TimespanModel> items = new ArrayList<TimespanModel>();
        final TimeInterval timeInterval = new SimpleTimeInterval(startMillis, endMillis, TimeUnit.ARBITRARY, TimeZone.getTimeZone("UTC"));
        Connector connector = apiKey.getConnector();
        final ObjectType sleep_type = ObjectType.getObjectType(connector, "sleep");

        String objectTypeName = apiKey.getConnector().getName() + "-" + sleep_type.getName();
        List<AbstractFacet> facets = getFacetsInTimespan(timeInterval,apiKey, sleep_type);

        if (channelName.equals("sleepStages")) {
            for (AbstractFacet facet : facets) {
                SleepFacet sleepFacet = (SleepFacet) facet;
                int lastStage = -1;
                long lastTimestamp = -1;
                for (Pair<Long,Integer> stage : sleepFacet.getSleepStages()) {
                    if (lastStage != -1) {
                        switch (lastStage) {
                            case SleepFacet.STATE_AWAY_FROM_BED:
                                items.add(new TimespanModel(lastTimestamp,stage.first,"away",objectTypeName));
                                break;
                            case SleepFacet.STATE_AWAKE:
                                items.add(new TimespanModel(lastTimestamp,stage.first,"awake",objectTypeName));
                                break;
                            case SleepFacet.STATE_ASLEEP:
                                items.add(new TimespanModel(lastTimestamp,stage.first,"asleep",objectTypeName));
                                break;
                            case SleepFacet.STATE_MEASUREMENT_GAP:
                                //we don't create an element for measurement gaps.
                                break;
                        }
                    }
                    lastTimestamp = stage.first;
                    lastStage = stage.second;
                }
                if (lastStage != -1) {
                    switch (lastStage) {
                        case SleepFacet.STATE_AWAY_FROM_BED:
                            items.add(new TimespanModel(lastTimestamp,facet.end,"away",objectTypeName));
                            break;
                        case SleepFacet.STATE_AWAKE:
                            items.add(new TimespanModel(lastTimestamp,facet.end,"awake",objectTypeName));
                            break;
                        case SleepFacet.STATE_ASLEEP:
                            items.add(new TimespanModel(lastTimestamp,facet.end,"asleep",objectTypeName));
                            break;
                        case SleepFacet.STATE_MEASUREMENT_GAP:
                            //we don't create an element for measurement gaps.
                            break;
                    }
                }
            }

        }
        else if (channelName.equals("snoringEpisodes")) {
            for (AbstractFacet facet : facets) {
                SleepFacet sleepFacet = (SleepFacet) facet;
                for (Pair<Long,Double> episode : sleepFacet.getSnoringEpisodes()) {
                    long start = episode.first;
                    long end = (long) (start + episode.second * 1000);
                    items.add(new TimespanModel(start,end,"on",objectTypeName));
                }
            }
        }

        System.out.println(channelName);
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
    public void addToDeclaredChannelMappings(ApiKey apiKey, List<ChannelMapping> channelMappings) {
        ChannelMapping sleepChannelMapping = new ChannelMapping(
                apiKey.getId(), apiKey.getGuestId(),
                ChannelMapping.ChannelType.timespan,
                ChannelMapping.TimeType.gmt,
                ObjectType.getObjectType(apiKey.getConnector(), "sleep").value(),
                apiKey.getConnector().getDeviceNickname(), "sleepStages",
                apiKey.getConnector().getDeviceNickname(), "sleepStages");
        channelMappings.add(sleepChannelMapping);
        ChannelMapping snoringEpisodesMapping = new ChannelMapping(
                apiKey.getId(), apiKey.getGuestId(),
                ChannelMapping.ChannelType.timespan,
                ChannelMapping.TimeType.gmt,
                ObjectType.getObjectType(apiKey.getConnector(), "sleep").value(),
                apiKey.getConnector().getDeviceNickname(), "snoringEpisodes",
                apiKey.getConnector().getDeviceNickname(), "snoringEpisodes");
        channelMappings.add(snoringEpisodesMapping);
    }
}