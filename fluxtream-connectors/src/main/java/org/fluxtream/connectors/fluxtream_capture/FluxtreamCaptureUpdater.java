package org.fluxtream.connectors.fluxtream_capture;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bodytrack.datastore.DatastoreTile;
import org.fluxtream.core.aspects.FlxLogger;
import org.fluxtream.core.auth.AuthHelper;
import org.fluxtream.core.connectors.Autonomous;
import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.connectors.annotations.Updater;
import org.fluxtream.core.connectors.fluxtream_capture.FluxtreamCapturePhotoFacet;
import org.fluxtream.core.connectors.location.LocationFacet;
import org.fluxtream.core.connectors.updaters.AbstractUpdater;
import org.fluxtream.core.connectors.updaters.ScheduleResult;
import org.fluxtream.core.connectors.updaters.UpdateInfo;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.services.ApiDataService;
import org.fluxtream.core.services.impl.BodyTrackHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletResponse;

@Component
@Controller
@Updater(prettyName = "FluxtreamCapture",
         value = 42,                                                // hat tip to Douglas Adams :-)
         objectTypes = {FluxtreamCapturePhotoFacet.class, LocationFacet.class, FluxtreamObservationFacet.class},
         defaultChannels = {"FluxtreamCapture.photo"})
public class FluxtreamCaptureUpdater extends AbstractUpdater implements Autonomous {

    static FlxLogger logger = FlxLogger.getLogger(FluxtreamCaptureUpdater.class);

    @Autowired
    BodyTrackHelper bodyTrackHelper;

    @Autowired
    CouchUpdater couchUpdater;

    @Override
    protected void updateConnectorDataHistory(final UpdateInfo updateInfo) throws Exception {
        // There's no difference between the initial history update and the incremental updates, so
        // just call updateConnectorData in either case
        updateConnectorData(updateInfo);
    }

    @Override
    protected void updateConnectorData(final UpdateInfo updateInfo) throws Exception {
        updateLocationData(updateInfo);
        couchUpdater.updateCaptureData(updateInfo, this, CouchUpdater.CouchDatabaseName.TOPICS);
        couchUpdater.updateCaptureData(updateInfo, this, CouchUpdater.CouchDatabaseName.OBSERVATIONS);
    }

    // Had trouble with conversion to BigDecimal in updateLocationData.
    // Copied this function from
    //    http://www.java2s.com/Code/Java/Data-Type/ConvertObjecttoBigDecimal.htm
    // to try to deal with the issue.
    public static BigDecimal getBigDecimal( Object value ) {
        BigDecimal ret = null;
        if( value != null ) {
            if( value instanceof BigDecimal ) {
                ret = (BigDecimal) value;
            } else if( value instanceof String ) {
                ret = new BigDecimal( (String) value );
            } else if( value instanceof BigInteger ) {
                ret = new BigDecimal( (BigInteger) value );
            } else if( value instanceof Number ) {
                ret = new BigDecimal( ((Number)value).doubleValue() );
            } else {
                throw new ClassCastException("Not possible to coerce ["+value+"] from class "+value.getClass()+" into a BigDecimal.");
            }
        }
        return ret;
    }

    protected void updateLocationData(final UpdateInfo updateInfo) throws Exception {
        // Get the user's UID from the updateInfo object
        Long uid = updateInfo.getGuestId();

        // In the future, get this info from the api key, for now hard code
        String deviceNickname = "FluxtreamCapture";

        // Get the start date either from the time range info or from the stored apiKeyAttributes.
        // Set the end date to be today + 1 to cover the case of people in timezones which are later
        // than the timezone of the server where it's already the next day
        Double start = getUpdateStartTime(updateInfo, uid, deviceNickname, "Latitude");
        Double end = getUpdateEndTime(updateInfo, uid, deviceNickname, "Latitude");

        // This is the list of channels to get tiles for for generating location facets
        // The code below assumes that Latitude is always the first entry, so make sure not to change
        // that.  Other entries are in arbitrary order but should match the names used below.
        String chNames[]={"Latitude","Longitude","HorizontalAccuracy"};

        // Check to make sure there's data there.  Will get null for start/end time if no data
        if(start==null || end==null || start>=end) {
            StringBuilder sb = new StringBuilder("module=updateQueue component=updater action=FluxtreamCaptureUpdater.updateLocationData")
                                                            .append(" message=\"no location data to update\" connector=")
                                                            .append(updateInfo.apiKey.getConnector().toString())
                                                            .append(" guestId=").append(updateInfo.apiKey.getGuestId());
            logger.info(sb.toString());
            return;
        }
        else {
            StringBuilder sb = new StringBuilder("module=updateQueue component=updater action=FluxtreamCaptureUpdater.updateLocationData")
                    .append(" message=\"will update location\" connector=")
                    .append(updateInfo.apiKey.getConnector().toString())
                    .append(" start=").append(start)
                    .append(" end=").append(end)
                    .append(" guestId=").append(updateInfo.apiKey.getGuestId());
            logger.info(sb.toString());
        }

        // Compute the tile level to get ~5 min/bin.  This means that if data comes in faster than 1 per 5 minutes
        // we will work with averaged data.  The FluxtreamCapture app nominally records location 1/10 mins, so in
        // general we should get all the datapoints at this level.
        int level = DatastoreTile.min_delta_to_level(300.0);
        long startOffset = DatastoreTile.unixtime_at_level_to_offset(start,level);
        long endOffset = DatastoreTile.unixtime_at_level_to_offset(end,level);
        double currentTime = start;
        List<LocationFacet> locationList = new ArrayList<LocationFacet>();;

        try {
            //  Loop from start to end, fetching tiles at each offset which falls between start and end
            for(long offset=startOffset; offset<=endOffset; offset++){
                // Compute the last time we're looking for in this set of tiles
                double endTileTime = Math.min(DatastoreTile.offset_at_level_to_unixtime(offset+1,level),end);
                // Fetch the tile for this level from each of the channels we need
                Map<String,BodyTrackHelper.GetTileResponse> tiles = new HashMap<String, BodyTrackHelper.GetTileResponse>();
                // Store the index of the next item >= currentTime in each tile
                Map<String,Integer> nextIndex = new HashMap<String, Integer>();

                for(String chName : chNames) {
                    BodyTrackHelper.GetTileResponse tile = bodyTrackHelper.fetchTileObject(uid,deviceNickname,chName, level,offset);
                    if(tile==null) {
                        StringBuilder sb = new StringBuilder("module=updateQueue component=updater action=FluxtreamCaptureUpdater.updateLocationData")
                                                                                            .append(" message=\"failed to retrieve tile\" connector=")
                                                                                            .append(updateInfo.apiKey.getConnector().toString())
                                                                                            .append(" channelName=").append(chName)
                                                                                            .append(" level=").append(level)
                                                                                            .append(" offset=").append(offset)
                                                                                            .append(" guestId=").append(updateInfo.apiKey.getGuestId());
                       logger.info(sb.toString());
                       return;
                    }
                    // Did get a tile, make sure it's like we expect: each row of data starts with
                    // time, mean
                    if(tile.fields.length<2 || !tile.fields[0].contentEquals("time") || !tile.fields[1].contentEquals("mean")) {
                        StringBuilder sb = new StringBuilder("module=updateQueue component=updater action=FluxtreamCaptureUpdater.updateLocationData")
                                                                                                                    .append(" message=\"malformed tile: expected fields to start with time,mean\" connector=")
                                                                                                                    .append(updateInfo.apiKey.getConnector().toString())
                                                                                                                    .append(" channelName=").append(chName)
                                                                                                                    .append(" level=").append(level)
                                                                                                                    .append(" offset=").append(offset)
                                                                                                                    .append(" guestId=").append(updateInfo.apiKey.getGuestId());
                        logger.info(sb.toString());
                        return;
                    }
                    // store in map
                    tiles.put(chName,tile);

                    // Scan forward in the list of datapoints for this tile to find the first index we're interested in
                    for(int i=0;i<tile.data.length; i++) {
                        Object [] row = tile.data[i];
                        if(row.length >= 2 && (getBigDecimal(row[0]).doubleValue() >= currentTime)) {
                            nextIndex.put(chName,i);
                            break;
                        }
                    }
                }

                if(tiles.size()!=chNames.length || nextIndex.size()!=chNames.length) {
                    StringBuilder sb = new StringBuilder("module=updateQueue component=updater action=FluxtreamCaptureUpdater.updateLocationData")
                                                                                        .append(" message=\"skipping tile: no data in range\" connector=")
                                                                                        .append(updateInfo.apiKey.getConnector().toString())
                                                                                        .append(" level=").append(level)
                                                                                        .append(" offset=").append(offset)
                                                                                        .append(" guestId=").append(updateInfo.apiKey.getGuestId());
                   logger.info(sb.toString());
                   continue;
                }

                // Make sure the tiles for all the channels have the same number of datapoints and
                // the same initial index
                int latLength = tiles.get("Latitude").data.length;
                int latIndex = nextIndex.get("Latitude");
                for(int i=1;i<chNames.length;i++){
                    if(tiles.get(chNames[i]).data.length!=latLength || nextIndex.get(chNames[i])!=latIndex) {
                        StringBuilder sb = new StringBuilder("module=updateQueue component=updater action=FluxtreamCaptureUpdater.updateLocationData")
                                                                                                                .append(" message=\"skipping tile: times do not match\" connector=")
                                                                                                                .append(updateInfo.apiKey.getConnector().toString())
                                                                                                                .append(" channelName=").append(chNames[i])
                                                                                                                .append(" level=").append(level)
                                                                                                                .append(" offset=").append(offset)
                                                                                                                .append(" latLength=").append(latLength)
                                                                                                                .append(" latIndex=").append(latIndex)
                                                                                                                .append(" guestId=").append(updateInfo.apiKey.getGuestId());
                        logger.info(sb.toString());
                        continue;
                    }
                }

                // Process timestamps in these tiles that is >= currentTime

                BodyTrackHelper.GetTileResponse latTile=tiles.get("Latitude");
                BodyTrackHelper.GetTileResponse lonTile=tiles.get("Longitude");
                BodyTrackHelper.GetTileResponse accTile=tiles.get("HorizontalAccuracy");
                for(int index = latIndex; index<latLength; index++) {
                    // Compute the next timestamp as the minimum next timestamp across the channels
                    Double thisTime = null;

                    if(latTile.data[index][0] instanceof BigDecimal) {
                        thisTime = ((BigDecimal)latTile.data[index][0]).doubleValue();
                    } else if(latTile.data[index][0] instanceof Integer) {
                        thisTime = ((Integer)latTile.data[index][0]).doubleValue();
                    }

                    if(thisTime>endTileTime)
                        break;

                    Float latitude = null;
                    Float longitude = null;
                    Integer accuracy = null;

                    if(latTile.data[index][1] instanceof BigDecimal) {
                        latitude = ((BigDecimal)latTile.data[index][1]).floatValue();
                    } else if(latTile.data[index][1] instanceof Integer) {
                        latitude = ((Integer)latTile.data[index][1]).floatValue();
                    }
                    if(lonTile.data[index][1] instanceof BigDecimal) {
                        longitude = ((BigDecimal)lonTile.data[index][1]).floatValue();
                    } else if(lonTile.data[index][1] instanceof Integer) {
                        longitude = ((Integer)lonTile.data[index][1]).floatValue();
                    }
                    if(accTile.data[index][1] instanceof BigDecimal) {
                        accuracy = ((BigDecimal)accTile.data[index][1]).intValue();
                    } else if(accTile.data[index][1] instanceof Integer) {
                        accuracy = ((Integer)accTile.data[index][1]).intValue();
                    }

                    if(latitude==null || longitude==null || accuracy==null || latitude<DatastoreTile.TILE_BREAK_THRESH || longitude<DatastoreTile.TILE_BREAK_THRESH || accuracy<DatastoreTile.TILE_BREAK_THRESH ) {
                        continue;
                    }
                    // Process location
                    LocationFacet locationFacet = new LocationFacet(updateInfo.apiKey.getId());
                    locationFacet.source=LocationFacet.Source.FLUXTREAM_CAPTURE;
                    locationFacet.api = updateInfo.apiKey.getConnector().value();
                    locationFacet.start = locationFacet.end = locationFacet.timestampMs = (long)(thisTime*1000.0);
                    locationFacet.latitude = latitude;
                    locationFacet.longitude = longitude;
                    locationFacet.accuracy = accuracy;

                    // Push the new location facet onto the list.  The list will get batch processed either when it
                    // gets beyond a certain length or when the try block exists, either through normal completion or
                    // through catching an exception
                    locationList.add(locationFacet);
                    currentTime=thisTime;
                }
                // Check if we should process the location list yet.  Only process if we have >= 100 points.
                // If we exit the loop with fewer than that, the call in the finally block will take care of the
                // rest.
                if(locationList.size()>=100) {
                    apiDataService.addGuestLocations(uid, locationList);
                    locationList.clear();
                    // Update the stored value that controls when we will start updating next time
                    updateStartTime(updateInfo,"Latitude",currentTime);
                }
            }
        }
        catch (Exception e) {
            StringBuilder sb = new StringBuilder("module=updateQueue component=updater action=FluxtreamCaptureUpdater.updateLocationData")
                                                .append(" message=\"exception while retrieving history\" connector=")
                                                .append(updateInfo.apiKey.getConnector().toString())
                                               .append(" guestId=").append(updateInfo.apiKey.getGuestId())
                                                .append(" currentTime=").append(currentTime);
           logger.info(sb.toString());

           // Rethrow the error so that this task gets rescheduled
           throw e;
        }
        finally {
            // Process any remaining facets in locationList
            if(locationList.size()>0) {
                apiDataService.addGuestLocations(uid, locationList);
            }
            // Update the stored value that controls when we will start updating next time
            updateStartTime(updateInfo,"Latitude",currentTime);
        }

    }

    public Double getChannelMinTime(final BodyTrackHelper.SourceInfo srcInfo, final String channelName){
        if(srcInfo!=null && srcInfo.info!=null && srcInfo.info.channels!=null && srcInfo.info.channels.size()>0) {
            for(BodyTrackHelper.Channel ch : srcInfo.info.channels) {
                if(ch.name !=null && ch.name.equals(channelName)) {
                    return(ch.min_time);
                }
            }
        }
        return null;
    }

    public Double getChannelMaxTime(final BodyTrackHelper.SourceInfo srcInfo, final String channelName){
        if (srcInfo != null && srcInfo.info != null && srcInfo.info.channels != null && srcInfo.info.channels.size() > 0) {
            for (BodyTrackHelper.Channel ch : srcInfo.info.channels) {
                if (ch.name != null && ch.name.equals(channelName)) {
                    return (ch.max_time);
                }
            }
        }
        return null;
    }

    public BodyTrackHelper.SourceInfo getSourceInfo(final UpdateInfo updateInfo, final long uid, final String deviceName) {
        // Check to see if we already got it
        BodyTrackHelper.SourceInfo srcInfo =(BodyTrackHelper.SourceInfo)updateInfo.getContext("srcInfo");

        if(srcInfo==null) {
            // Haven't gotten it yet
            srcInfo = bodyTrackHelper.getSourceInfoObject(uid, deviceName);

            // Cache it for next time
            updateInfo.setContext("srcInfo",srcInfo);
        }
        return srcInfo;
    }

    public Double getUpdateEndTime(final UpdateInfo updateInfo, final long uid, final String deviceName, String channelName) throws Exception {
        BodyTrackHelper.SourceInfo srcInfo = getSourceInfo(updateInfo,uid,deviceName);

        // Parse out the max_time for this channel
        return getChannelMaxTime(srcInfo, channelName);
    }


    public Double getUpdateStartTime(final UpdateInfo updateInfo, final long uid, final String deviceName, final String channelName) throws Exception {
        ApiKey apiKey = updateInfo.apiKey;

        // The updateStartDate for a given object type is stored in the apiKeyAttributes
        // as FluxtreamCapture.<channelName>.updateStartDate.  In the case of a failure the updater will store the date
        // that failed and start there next time.  In the case of a successfully completed update it will store
        // the timestamp of the last datapoint from the datastore on that channel.
        String updateKeyName = "FluxtreamCapture." + channelName + ".updateStartTime";
        String updateStartTimeStr = guestService.getApiKeyAttribute(apiKey, updateKeyName);
        Double updateStartTime=null;

        // The first time we do this there won't be an apiKeyAttribute yet.  In that case get the
        // start time from the datastore info call
        if(updateStartTimeStr == null) {
            BodyTrackHelper.SourceInfo srcInfo = getSourceInfo(updateInfo,uid,deviceName);

            // Parse out the min_time for this channel
            updateStartTime = getChannelMinTime(srcInfo, channelName);

            if(updateStartTime!=null) {
                // Store in the apiKeyAttribute for next time
                guestService.setApiKeyAttribute(updateInfo.apiKey, updateKeyName, updateStartTime.toString());
            }
            return updateStartTime;
        }

        // Did have a stored updateStartTime.  Parse it
        try{
            return Double.valueOf(updateStartTimeStr);
        } catch (IllegalArgumentException e){
            return null;
        }
    }

    // Update the start time where we will begin during the next update cycle.
    public void updateStartTime(final UpdateInfo updateInfo, final String channelName, final Double nextUpdateStartTime) throws Exception {
        // The updateStartDate for a given object type is stored in the apiKeyAttributes
        // as FluxtreamCapture.<channelName>.updateStartDate.  In the case of a failure the updater will store the date
        // that failed and start there next time.  In the case of a successfully completed update it will store
        // the timestamp of the last datapoint from the datastore on that channel.
        String updateKeyName = "FluxtreamCapture." + channelName + ".updateStartTime";

        if(nextUpdateStartTime!=null)
            guestService.setApiKeyAttribute(updateInfo.apiKey, updateKeyName, nextUpdateStartTime.toString());
    }

    @Override
    public void setDefaultChannelStyles(ApiKey apiKey) {}

    @RequestMapping(value="/fluxtream_capture/notify", method = RequestMethod.POST)
    public void couchSyncFinished(HttpServletResponse response) throws IOException {

        ApiKey apiKey = guestService.getApiKey(AuthHelper.getGuestId(), Connector.getConnector("fluxtream_capture"));

        ScheduleResult scheduleResult = connectorUpdateService.scheduleUpdate(apiKey,
                2,
                UpdateInfo.UpdateType.PUSH_TRIGGERED_UPDATE,
                System.currentTimeMillis());

        response.getWriter().write("Schedule result: " + scheduleResult.type);
    }

}

