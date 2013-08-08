package com.fluxtream.connectors.moves;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;
import com.fluxtream.aspects.FlxLogger;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.annotations.Updater;
import com.fluxtream.connectors.location.LocationFacet;
import com.fluxtream.connectors.updaters.AbstractUpdater;
import com.fluxtream.connectors.updaters.UpdateInfo;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.domain.AbstractLocalTimeFacet;
import com.fluxtream.services.ApiDataService;
import com.fluxtream.services.JPADaoService;
import com.fluxtream.services.MetadataService;
import com.fluxtream.utils.HttpUtils;
import com.fluxtream.utils.Utils;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * User: candide
 * Date: 17/06/13
 * Time: 16:50
 */
@Component
@Updater(prettyName = "Moves", value = 144, objectTypes = {LocationFacet.class, MovesMoveFacet.class, MovesPlaceFacet.class}, timespanResponder = MovesTimespanResponder.class)
public class MovesUpdater extends AbstractUpdater {
    static FlxLogger logger = FlxLogger.getLogger(AbstractUpdater.class);

    final static String host = "https://api.moves-app.com/api/v1";
    final static String updateDateKeyName = "lastDate";

    public static DateTimeFormatter compactDateFormat = DateTimeFormat.forPattern("yyyyMMdd");
    public static DateTimeFormatter dateStorageFormat = DateTimeFormat.forPattern("yyyy-MM-dd");
    public static DateTimeFormatter timeStorageFormat = DateTimeFormat.forPattern("yyyyMMdd'T'HHmmss'Z'");

    @Autowired
    MovesController controller;

    @Autowired
    MetadataService metadataService;

    @Autowired
    JPADaoService jpaDaoService;

    @Override
    protected void updateConnectorDataHistory(final UpdateInfo updateInfo) throws Exception {
        // Get Moves data for the range of dates starting the first profile date.  We don't need to
        // do any fixup since we're already doing a full update for the full range of days.
        String userRegistrationDate = getUserRegistrationDate(updateInfo);

        updateMovesData(updateInfo, userRegistrationDate, 0);
    }

    // Get/update moves data for the range of dates starting from the stored date of the last update.
    // Do a maximum of 7 days of fixup on earlier dates to pickup user-initiated changes
    @Override
    protected void updateConnectorData(final UpdateInfo updateInfo) throws Exception {
        // Get the date for starting the update.  This will either be a stored date from a previous run
        // of the updater or the user's registration date.
        String updateStartDate = getUpdateStartDate(updateInfo);

        updateMovesData(updateInfo, updateStartDate, 7);
    }

    public String getUserRegistrationDate(UpdateInfo updateInfo) throws Exception {
        // Check first if we already have a user registration date stored in apiKeyAttributes as userRegistrationDate.
        // userRegistrationDate is stored in storage format (yyyy-mm-dd)
        String userRegistrationKeyName = "userRegistrationDate";
        String userRegistrationDate = guestService.getApiKeyAttribute(updateInfo.apiKey,userRegistrationKeyName);

        // The first time we do this there won't be a stored userRegistrationDate yet.  In that case get the
        // registration date from a Moves API call
        if(userRegistrationDate == null) {
            long currentTime = System.currentTimeMillis();
            String accessToken = controller.getAccessToken(updateInfo.apiKey);
            String query = host + "/user/profile?access_token=" + accessToken;
            try {
                final String fetched = HttpUtils.fetch(query);
                countSuccessfulApiCall(updateInfo.apiKey, updateInfo.objectTypes, currentTime, query);
                JSONObject json = JSONObject.fromObject(fetched);
                if (!json.has("profile"))
                    throw new Exception("no profile");
                final JSONObject profile = json.getJSONObject("profile");
                if (!profile.has("firstDate"))
                    throw new Exception("no firstDate in profile");
                String compactRegistrationDate = profile.getString("firstDate");

                if(compactRegistrationDate!=null) {
                    // The format of firstDate returned by the Moves API is compact (yyyymmdd).  Convert to
                    // the storage format (yyyy-mm-dd) for consistency
                    userRegistrationDate = toStorageFormat(compactRegistrationDate);

                    // Cache registrationDate so we don't need to do an API call next time
                    guestService.setApiKeyAttribute(updateInfo.apiKey, userRegistrationKeyName, userRegistrationDate);
                }
            } catch (Exception e) {
                // Couldn't get user registration date
                StringBuilder sb = new StringBuilder("module=updateQueue component=updater action=MovesUpdater.getUserRegistrationDate")
                        .append(" message=\"exception while retrieving UserRegistrationDate\" connector=")
                        .append(updateInfo.apiKey.getConnector().toString()).append(" guestId=")
                        .append(updateInfo.apiKey.getGuestId())
                        .append(" stackTrace=<![CDATA[").append(Utils.stackTrace(e)).append("]]>");;
                logger.info(sb.toString());

                countFailedApiCall(updateInfo.apiKey, updateInfo.objectTypes, currentTime, query, Utils.stackTrace(e));
            }
        }
        return userRegistrationDate;
    }

    public String getUpdateStartDate(UpdateInfo updateInfo) throws Exception
    {
        // Check first if we already have a date stored in apiKeyAttributes as updateDateKeyName.
        // In the case of a failure the updater will store the date
        // that failed and start there next time.  In the case of a successfully completed update it will store
        // the date of the last day that returned partial data
        // The updateDateKeyName attribute is stored in storage format (yyyy-mm-dd)
        String updateStartDate = guestService.getApiKeyAttribute(updateInfo.apiKey, updateDateKeyName);

        // The first time we do this there won't be an apiKeyAttribute yet.  In that case get the
        // registration date for the user and store that.
        if(updateStartDate == null) {
            updateStartDate = getUserRegistrationDate(updateInfo);

            // Store in the apiKeyAttribute for next time
            guestService.setApiKeyAttribute(updateInfo.apiKey, updateDateKeyName, updateStartDate);
        }
        return updateStartDate;
    }



    // Get/update moves data for the range of dates start
    protected void updateMovesData(final UpdateInfo updateInfo, String fullUpdateStartDate, int fixupDateNum) throws Exception {
        // Calculate the lists of days to update. Moves only updates its data for a given day when either the user
        // manually opens the application or when the phone notices that it's past midnight local time.  The former
        // action generates partial data for the day and the latter generates finalized data for that day with respect
        // to the parsing of the move and place segments and the generation of the GPS data points.  However, the
        // user is able to go back and modify things like place IDs and movemet segment types ("wlk', 'trp', or 'cyc').
        //
        // fullUpdateStartDate is the last date that we had partial data for previous time we did an update (user had opened app
        // but the phone presumably hadn't done the cross-midnight recompute (NOTE: this assumption may be flawed if our
        // inferred user timezone differs from the phone's idea of its own timezone).  We will do a full update
        // including GPS points, for that date and all future dates up to and including today as computed in the
        // timezone we currently infer for the user.
        //
        // The days prior to fullUpdateStartDate should presumably have imported
        // complete updates during a previous update so we don't need to reimport the GPS data points.  However,
        // we do need to import the move/place segments and reconcile them with our stored versions since the
        // user may have tweaked some of them.  We currently arbitrarily re-import the 7 prior days to do this
        // fixup operation.

        // getDatesSince and getDatesBefore both take their arguments and return their list of dates in storage
        // format (yyyy-mm-dd).  The list returned by getDatesSince includes the date passed in (in this case fullUpdateStartDate)
        // but getDatesBefore does not, so fullUpdateStartDate is processed as a full update.
        final List<String> fullUpdateDates = getDatesSince(fullUpdateStartDate);

        // For the dates that aren't yet completed (fullUpdateStartDate through today), createOrUpdate with trackpoints
        String maxDateWithData = createOrUpdateData(fullUpdateDates, updateInfo, true);

        // If any of these dates had data, potentially update the start date for next time
        if(maxDateWithData!=null && fullUpdateStartDate.compareTo(maxDateWithData)<0) {
            // Got non-empty data for a day more recent than fullUpdateStartDate, store for where to start next time
            guestService.setApiKeyAttribute(updateInfo.apiKey, updateDateKeyName, maxDateWithData);
        }

        // If fixupDateNum>0, do createOrUpdate without trackpoints for the fixupDateNum dates prior to
        // fullUpdateStartDate
        if(fixupDateNum>0) {
            final List<String> fixupDates = getDatesBefore(fullUpdateStartDate, fixupDateNum);
            createOrUpdateData(fixupDates, updateInfo, false);
        }
    }

    private String fetchStorylineForDate(final UpdateInfo updateInfo, final String date, final boolean withTrackpoints) throws Exception {
        long then = System.currentTimeMillis();
        String fetched = null;
        String compactDate = toCompactDateFormat(date);
        String fetchUrl = "not set yet";
        try {
            String accessToken = controller.getAccessToken(updateInfo.apiKey);
            fetchUrl = String.format(host + "/user/storyline/daily/%s?trackPoints=%s&access_token=%s",
                                            compactDate, withTrackpoints, accessToken);
            fetched = HttpUtils.fetch(fetchUrl);
            countSuccessfulApiCall(updateInfo.apiKey, updateInfo.objectTypes, then, fetchUrl);
        } catch (Exception e) {
            countFailedApiCall(updateInfo.apiKey, updateInfo.objectTypes, then, fetchUrl, Utils.stackTrace(e));
        }
        return fetched;
    }

    // getDatesSince takes argument and returns a list of dates in storage format (yyyy-mm-dd)
    private static List<String> getDatesSince(String fromDate) {
        List<String> dates = new ArrayList<String>();
        DateTime then = dateStorageFormat.withZoneUTC().parseDateTime(fromDate);
        String today = dateStorageFormat.withZoneUTC().print(System.currentTimeMillis());
        DateTime todaysTime = dateStorageFormat.withZoneUTC().parseDateTime(today);
        if (then.isAfter(todaysTime))
            throw new IllegalArgumentException("fromDate is after today");
        while (!today.equals(fromDate)) {
            dates.add(fromDate);
            then = dateStorageFormat.withZoneUTC().parseDateTime(fromDate);
            String date = dateStorageFormat.withZoneUTC().print(then.plusDays(1));
            fromDate = date;
        }
        dates.add(today);
        return dates;
    }

    private static String toStorageFormat(String date) {
        DateTime then = compactDateFormat.withZoneUTC().parseDateTime(date);
        String storageDate = dateStorageFormat.withZoneUTC().print(then);
        return storageDate;
    }

    private String toCompactDateFormat(final String date) {
        DateTime then = dateStorageFormat.withZoneUTC().parseDateTime(date);
        String compactDate = compactDateFormat.withZoneUTC().print(then);
        return compactDate;
    }


    // getDatesBefore assumes its argument is in storage format and returns a list of dates in storage format
    private List<String> getDatesBefore(String date, int nDays) {
        DateTime initialDate = dateStorageFormat.withZoneUTC().parseDateTime(date);
        List<String> dates = new ArrayList<String>();
        for (int i=0; i<nDays; i++) {
            initialDate = initialDate.minusDays(1);
            String nextDate = dateStorageFormat.withZoneUTC().print(initialDate);
            dates.add(nextDate);
        }
        return dates;
    }

    private String createOrUpdateData(List<String> dates, UpdateInfo updateInfo, boolean withTrackpoints) throws Exception {
        // Create or update the data for a list of dates.  Returns the date of the latest day with non-empty data,
        // or null if no dates had data

        // Get the user registration date for comparison.  There's no point in trying to update data from before then.
        String userRegistrationDate = getUserRegistrationDate(updateInfo);
        String maxDateWithData=null;

        try {
            for (String date : dates) {
                if(date==null || (userRegistrationDate!=null && date.compareTo(userRegistrationDate)<0)) {
                    // This date is either invalid or would be before the registration date, skip it
                    continue;
                }
                String fetched = fetchStorylineForDate(updateInfo, date, withTrackpoints);

                if(fetched!=null) {
                    final JSONArray segments = getSegments(fetched);
                    boolean dateHasData=createOrUpdateDataForDate(updateInfo, segments, date);

                    if(dateHasData && (maxDateWithData==null || maxDateWithData.compareTo(date)<0)) {
                        maxDateWithData = date;
                    }
                }
            }
        }
        catch (Exception e) {
            // Couldn't get user registration date
            StringBuilder sb = new StringBuilder("module=updateQueue component=updater action=MovesUpdater.getUserRegistrationDate")
                    .append(" message=\"exception while retrieving UserRegistrationDate\" connector=")
                    .append(updateInfo.apiKey.getConnector().toString()).append(" guestId=")
                    .append(updateInfo.apiKey.getGuestId())
                    .append(" stackTrace=<![CDATA[").append(Utils.stackTrace(e)).append("]]>");;
            logger.info(sb.toString());
        }
        return(maxDateWithData);
    }

    private boolean createOrUpdateDataForDate(final UpdateInfo updateInfo, final JSONArray segments,
                                           final String date) {
        // For a given date, iterate over the JSON array of segments returned by a call to the Moves API and
        // reconcile them with any previously persisted move and place facets for that date.
        // A given segment may be either of type move or place.  Either type has an overall
        // start and end time and may contain a list of activities.  A given segment is considered to match
        // a stored facet if the type matches (move vs place) and the start time and date match.  If a match is
        // found, the activities associated with that segment are reconciled.
        // Returns true if the date has a non empty set of data, false otherwise
        boolean dateHasData=false;

        // Check that segments and date are both non-null.  If so, continue, otherwise return false
        if(date==null || segments==null)
            return false;

        for (int j=0; j<segments.size(); j++) {
            JSONObject segment = segments.getJSONObject(j);
            if (segment.getString("type").equals("move")) {
                if(createOrUpdateMovesMoveFacet(date, segment, updateInfo)!=null)
                    dateHasData=true;
            } else if (segment.getString("type").equals("place")) {
                if(createOrUpdateMovesPlaceFacet(date, segment, updateInfo)!=null)
                    dateHasData=true;
            }
        }
        return dateHasData;
    }

    // For a given date and move segment JSON, either create or update the data for a move corresponding to that segment
    private MovesMoveFacet createOrUpdateMovesMoveFacet(final String date,final JSONObject segment, final UpdateInfo updateInfo) {
        try {
            final DateTime startTime = timeStorageFormat.withZoneUTC().parseDateTime(segment.getString("startTime"));
            long start = startTime.getMillis();

            MovesMoveFacet ret = (MovesMoveFacet)
                    apiDataService.createOrReadModifyWrite(MovesMoveFacet.class,
                                                           new ApiDataService.FacetQuery(
                                                                   "e.guestId = ? AND e.date = ? AND e.start = ?",
                                                                   updateInfo.getGuestId(),
                                                                   date,
                                                                   start),
                                                           new ApiDataService.FacetModifier<MovesMoveFacet>() {
                                                               // Throw exception if it turns out we can't make sense of the observation's JSON
                                                               // This will abort the transaction
                                                               @Override
                                                               public MovesMoveFacet createOrModify(MovesMoveFacet facet, Long apiKeyId) {
                                                                   boolean needsUpdate = false;
                                                                   // Don't already have a MovesMoveFacet with this apiKeyId, start, and date.  Create one
                                                                   if (facet == null) {
                                                                       facet = new MovesMoveFacet(apiKeyId);
                                                                       facet.guestId = updateInfo.apiKey.getGuestId();
                                                                       facet.api = updateInfo.apiKey.getConnector().value();

                                                                       // Set the fields based on the JSON and create activities from scratch
                                                                       extractMoveData(date, segment, facet, updateInfo);
                                                                       needsUpdate=true;
                                                                   }
                                                                   else {
                                                                       // Already have a MovesMoveFacet with this apiKeyId, start, and date.
                                                                       // Just update from the segment info
                                                                       needsUpdate = tidyUpMoveFacet(segment, facet);
                                                                       needsUpdate |= tidyUpActivities(updateInfo, segment, facet);
                                                                   }


                                                                   // If the facet changed set timeUpdated
                                                                   if(needsUpdate) {
                                                                       facet.timeUpdated = System.currentTimeMillis();
                                                                   }

                                                                   return facet;
                                                               }
                                                           }, updateInfo.apiKey.getId());
            return ret;

        } catch (Throwable e) {
            // Couldn't makes sense of the move's JSON
            StringBuilder sb = new StringBuilder("module=updateQueue component=updater action=MovesUpdater.createOrUpdateMovesMoveFacet")
                    .append(" message=\"exception while processing move segment\" connector=")
                    .append(updateInfo.apiKey.getConnector().toString()).append(" guestId=")
                    .append(updateInfo.apiKey.getGuestId())
                    .append(" stackTrace=<![CDATA[").append(Utils.stackTrace(e)).append("]]>");;
            logger.info(sb.toString());

            return null;
        }
    }

    // For a given date and place segment JSON, either create or update the data for a place corresponding to that segment
    private MovesPlaceFacet createOrUpdateMovesPlaceFacet(final String date,final JSONObject segment, final UpdateInfo updateInfo) {
        try {
            final DateTime startTime = timeStorageFormat.withZoneUTC().parseDateTime(segment.getString("startTime"));
            long start = startTime.getMillis();

            MovesPlaceFacet ret = (MovesPlaceFacet)
                    apiDataService.createOrReadModifyWrite(MovesPlaceFacet.class,
                                                           new ApiDataService.FacetQuery(
                                                                   "e.guestId = ? AND e.date = ? AND e.start = ?",
                                                                   updateInfo.getGuestId(),
                                                                   date,
                                                                   start),
                                                           new ApiDataService.FacetModifier<MovesPlaceFacet>() {
                                                               // Throw exception if it turns out we can't make sense of the observation's JSON
                                                               // This will abort the transaction
                                                               @Override
                                                               public MovesPlaceFacet createOrModify(MovesPlaceFacet facet, Long apiKeyId) {
                                                                   boolean needsUpdate = false;
                                                                   if (facet == null) {
                                                                       facet = new MovesPlaceFacet(apiKeyId);
                                                                       facet.guestId = updateInfo.apiKey.getGuestId();
                                                                       facet.api = updateInfo.apiKey.getConnector().value();
                                                                       // Set the fields based on the JSON and create activities from scratch
                                                                       extractMoveData(date, segment, facet, updateInfo);
                                                                       extractPlaceData(segment, facet);
                                                                       needsUpdate=true;
                                                                   }
                                                                   else {
                                                                       // Already have a MovesMoveFacet with this apiKeyId, start, and date.
                                                                       // Just update from the segment info
                                                                       needsUpdate = tidyUpPlaceFacet(segment, facet);
                                                                       needsUpdate |= tidyUpActivities(updateInfo, segment, facet);
                                                                   }

                                                                   // If the facet changed set timeUpdated
                                                                   if(needsUpdate) {
                                                                       facet.timeUpdated = System.currentTimeMillis();
                                                                   }
                                                                   return facet;
                                                               }
                                                           }, updateInfo.apiKey.getId());
            return ret;

        } catch (Throwable e) {
            // Couldn't makes sense of the move's JSON
            StringBuilder sb = new StringBuilder("module=updateQueue component=updater action=MovesUpdater.createOrUpdateMovesPlaceFacet")
                    .append(" message=\"exception while processing place segment\" connector=")
                    .append(updateInfo.apiKey.getConnector().toString()).append(" guestId=")
                    .append(updateInfo.apiKey.getGuestId())
                    .append(" stackTrace=<![CDATA[").append(Utils.stackTrace(e)).append("]]>");;
            logger.info(sb.toString());

            return null;
        }
    }

    @Transactional(readOnly=false)
    private boolean tidyUpActivities(final UpdateInfo updateInfo, final JSONObject segment, final MovesFacet parentFacet) {
        // Reconcile the stored activities associated with a given parent facet with the contents of a corresponding json
        // object returned by the Moves API.  Returns true if the facet needs update, and false otherwise.
        final List<MovesActivity> movesActivities = parentFacet.getActivities();
        if (!segment.has("activities"))
            return false;
        final JSONArray activities = segment.getJSONArray("activities");
        boolean needsUpdate = false;
        // TODO: it's possible that the two lists would be of the same length
        // yet not have perfectly matching start times.  In that case
        // this function will fail to behave properly.
        if (movesActivities.size()<activities.size()) {
            // identify missing activities and add them to the facet's activities
            addMissingActivities(updateInfo, movesActivities, activities, parentFacet);
            needsUpdate = true;
        } else if (movesActivities.size()>activities.size()) {
            // find out which activities we need to remove
            removeActivities(movesActivities, activities, parentFacet);
            needsUpdate = true;
        }
        // finally, update activities that need it
        needsUpdate|=updateActivities(updateInfo, movesActivities, activities);
        return(needsUpdate);
    }

    // This function compares start times between a list of stored moves activity facets and entries in a JSON array
    // of activity objects returned by the Moves API.  Any items in the list of stored facets which do not have a
    // corresponding item in the JSON array with a matching start time are removed.
    private void removeActivities(final List<MovesActivity> movesActivities, final JSONArray activities, final MovesFacet facet) {
        withMovesActivities:for (int i=0; i<movesActivities.size(); i++) {
            final MovesActivity movesActivity = movesActivities.get(i);
            for (int j=0; i<activities.size(); i++) {
                JSONObject activityData = activities.getJSONObject(j);
                final long start = timeStorageFormat.withZoneUTC().parseDateTime(activityData.getString("startTime")).getMillis();
                if (movesActivity.start==start) {
                    continue withMovesActivities;
                }
            }
            facet.removeActivity(movesActivity);
        }
    }

    // This function reconciles a list of moves activity facets with a JSON array of activity objects returned by the
    // Moves API.  This assumes that the lists are of the same length and have matching startTimes.
    // Returns true if any modifications are made and false otherwise.
    private boolean updateActivities(final UpdateInfo updateInfo, final List<MovesActivity> movesActivities, final JSONArray activities) {
        boolean needsUpdate = false;
        // Loop over the activities JSON array returned by a recent API call to check if each has a corresponding
        // stored activity facet.  Consider a given stored activity facet and JSON item to match if their
        // start times are the same.
        for (int i=0; i<activities.size(); i++) {
            // Loop over the stored facets in movesActivities to make sure that they take into account
            // jsonActivity.  Consider a given stored activity facet and JSON item to match if
            // their start times are the same.
            JSONObject jsonActivity = activities.getJSONObject(i);
            for (int j=0; j<movesActivities.size(); j++) {
                final long start = timeStorageFormat.withZoneUTC().parseDateTime(jsonActivity.getString("startTime")).getMillis();
                final MovesActivity storedActivityFacet = movesActivities.get(j);
                if (storedActivityFacet.start==start) {
                    // Here we know that the storedActivityFacet and jsonActivity started at the same time.
                    // Check that they end at the same time and have the same type and auxilliary data.
                    needsUpdate|=updateActivity(updateInfo, storedActivityFacet, jsonActivity);
                    continue;
                }

            }
        }
        return needsUpdate;
    }

    // This function reconciles a given moves activity facet with a JSON activity objects returned by the
    // Moves API.  This assumes that the args have already been confirmed to have matching startTimes.
    // Returns true if any modifications are made and false otherwise.
    private boolean updateActivity(final UpdateInfo updateInfo,
                                   final MovesActivity movesActivity,
                                   final JSONObject activityData) {
        boolean needsUpdate = false;
        final long end = timeStorageFormat.withZoneUTC().parseDateTime(activityData.getString("endTime")).getMillis();
        if (movesActivity.end!=end) {
            needsUpdate = true;
            movesActivity.endTimeStorage = AbstractLocalTimeFacet.timeStorageFormat.print(end);
            movesActivity.end = end;
        }

        final String activity = activityData.getString("activity");
        if (!movesActivity.activity.equals(activity)) {
            needsUpdate = true;
            movesActivity.activity = activity;
        }

        if ((activityData.has("steps")&&movesActivity.steps==null)||
            (activityData.has("steps")&&movesActivity.steps!=activityData.getInt("steps"))) {
            needsUpdate = true;
            movesActivity.steps = activityData.getInt("steps");
        }
        if (activityData.getInt("distance")!=movesActivity.distance) {
            needsUpdate = true;
            movesActivity.distance = activityData.getInt("distance");
        }
        if (activityData.has("trackPoints")) {
            if (movesActivity.activityURI!=null) {
                // Anne: I removed the check since there should be no problem with inserting
                // location points which already exist.  TODO: see if we can do better here

                // note: we don't needs to set needsUpdate to true here as the location data is
                // not stored with the facet per se, it is stored separately in the LocationFacets table
                //final long stored = jpaDaoService.executeCount("SELECT count(facet) FROM " +
                //                                          JPAUtils.getEntityName(LocationFacet.class) +
                //                                          " facet WHERE facet.source=" + LocationFacet.Source.MOVES.ordinal() +
                //                                          " AND facet.uri='" + movesActivity.activityURI + "'");
                //final JSONArray trackPoints = activityData.getJSONArray("trackPoints");
                //if (stored!=trackPoints.size()) {
                //    jpaDaoService.execute("DELETE facet FROM " +
                //                          JPAUtils.getEntityName(LocationFacet.class) +
                //                          " facet WHERE facet.source=" + LocationFacet.Source.MOVES +
                //                          " AND facet.uri='" + movesActivity.activityURI + "'");
                    extractTrackPoints(movesActivity.activityURI, activityData, updateInfo);
                //}
            } else {
                needsUpdate = true; // adding an activityURI means an update is needed
                // Generate a URI of the form '{wlk,cyc,trp}/UUID'.  The activity field must be set before calling createActivityURI
                movesActivity.activityURI = createActivityURI(movesActivity);
                extractTrackPoints(movesActivity.activityURI, activityData, updateInfo);
            }
        }
        return needsUpdate;
    }

    private String createActivityURI(final MovesActivity movesActivity) {
        // Generate a URI of the form '{wlk,cyc,trp}/UUID'.  The activity field must be set before calling createActivityURI
        if(movesActivity.activity!=null) {
            return(movesActivity.activity + "/" + UUID.randomUUID().toString());
        }
        else {
            return null;
        }
    }
    private void addMissingActivities(final UpdateInfo updateInfo,
                                      final List<MovesActivity> movesActivities,
                                      final JSONArray activities,
                                      final MovesFacet parentFacet) {
        // Loop over the activities JSON array returned by a recent API call to check if each has a corresponding
        // stored activity facet.  Consider a given stored activity facet and JSON item to match if their
        // start times are the same.
        withApiActivities:for (int i=0; i<activities.size(); i++) {
            JSONObject jsonActivity = activities.getJSONObject(i);

            // Loop over the stored facets in movesActivities to make sure that they take into account
            // jsonActivity.  Consider a given stored activity facet and JSON item to match if
            // their start times are the same.
            for (int j=0; j<movesActivities.size(); j++) {
                final long start = timeStorageFormat.withZoneUTC().parseDateTime(jsonActivity.getString("startTime")).getMillis();
                MovesActivity storedActivityFacet = movesActivities.get(j);
                if (storedActivityFacet.start==start) {
                    // Here we know that storedActivityFacet and jsonActivity started at the same time.
                    // A later call to updateActivities will check that they end at the same time,
                    // have the same type and auxilliary data.  Don't worry about that here.
                    continue withApiActivities;
                }
            }

            // There was no stored activity facet matching the same startTime as jsonActivity.  Extract
            // the fields from jsonActivityfrom into a new facet and add it to our parent facet.
            // Use the same date for the activities as is stored with the parent.  This is the date
            // used to make the request to the Moves API.
            final MovesActivity activity = extractActivity(parentFacet.date,updateInfo, jsonActivity);
            parentFacet.addActivity(activity);
        }
    }

    @Transactional(readOnly=false)
    private boolean tidyUpPlaceFacet(final JSONObject segment, final MovesPlaceFacet place) {
        boolean needsUpdating = false;

        // Check for change in the end time
        final DateTime endTime = timeStorageFormat.withZoneUTC().parseDateTime(segment.getString("endTime"));
        if(place.end != endTime.getMillis()) {
            System.out.println(place.start + ": endTime changed");
            needsUpdating = true;
            place.end = endTime.getMillis();
        }

        // Check for change in the place data
        JSONObject placeData = segment.getJSONObject("place");
        if (placeData.has("id")&&place.placeId==null) {
            System.out.println(place.start + ": now the place has an id");
            needsUpdating = true;
            place.placeId = placeData.getLong("id");
        }
        // update the place type
        String previousPlaceType = place.type;
        if (!placeData.getString("type").equals(place.type)) {
            System.out.println(place.start + ": place type has changed");
            needsUpdating = true;
            place.type = placeData.getString("type");
        }
        if (placeData.has("name")&&
            (place.name==null || !place.name.equals(placeData.getString("name")))) {
            System.out.println(place.start + ": place name has changed");
            needsUpdating = true;
            place.name = placeData.getString("name");
        }

        // if the place wasn't identified previously, store its fourquare info now
        if (!previousPlaceType.equals("foursquare")&&
            place.type.equals("foursquare")){
            System.out.println(place.start + ": storing foursquare info");
            needsUpdating = true;
            place.foursquareId = placeData.getString("foursquareId");
        }
        JSONObject locationData = placeData.getJSONObject("location");
        float lat = (float) locationData.getDouble("lat");
        float lon = (float) locationData.getDouble("lon");
        if (Math.abs(lat-place.latitude)>0.0001 || Math.abs(lon-place.longitude)>0.0001) {
            System.out.println(place.start + ": lat/lon have changed");
            needsUpdating = true;
            place.latitude = lat;
            place.longitude = lon;
        }
        return (needsUpdating);
    }

    @Transactional(readOnly=false)
    private boolean tidyUpMoveFacet(final JSONObject segment, final MovesFacet moveFacet) {
        boolean needsUpdating = false;

        // Check for change in the end time
        final DateTime endTime = timeStorageFormat.withZoneUTC().parseDateTime(segment.getString("endTime"));
        if(moveFacet.end != endTime.getMillis()) {
            needsUpdating = true;
            moveFacet.end = endTime.getMillis();
        }

        return(needsUpdating);
    }

    private JSONArray getSegments(final String json) {
        JSONArray jsonArray = JSONArray.fromObject(json);
        JSONObject dayFacetData = jsonArray.getJSONObject(0);
        JSONArray segments = dayFacetData.getJSONArray("segments");
        return segments;
    }

    private List<AbstractFacet> extractFacets(UpdateInfo updateInfo, String json) throws Exception {
        List<AbstractFacet> facets = new ArrayList<AbstractFacet>();
        JSONArray jsonArray = JSONArray.fromObject(json);
        for (int i=0; i<jsonArray.size(); i++) {
            JSONObject dayFacetData = jsonArray.getJSONObject(i);
            String date = dayFacetData.getString("date");
            String dateStorage = toStorageFormat(date);
            JSONArray segments = dayFacetData.getJSONArray("segments");
            for (int j=0; j<segments.size(); j++) {
                JSONObject segment = segments.getJSONObject(j);
                if (segment.getString("type").equals("move")) {
                    facets.add(createOrUpdateMovesMoveFacet(dateStorage, segment, updateInfo));
                    // Old version:
//                    facets.add(extractMove(dateStorage, segment, updateInfo));
                } else if (segment.getString("type").equals("place")) {
                    facets.add(createOrUpdateMovesPlaceFacet(dateStorage, segment, updateInfo));
                    // Old version:
//                    facets.add(extractPlace(dateStorage, segment, updateInfo));
                }
            }
        }
        return facets;
    }

    private MovesPlaceFacet extractPlace(final String date, final JSONObject segment, UpdateInfo updateInfo) {
        MovesPlaceFacet facet = new MovesPlaceFacet(updateInfo.apiKey.getId());
        facet.guestId=updateInfo.getGuestId();
        facet.date=date;
        extractMoveData(date, segment, facet, updateInfo);
        extractPlaceData(segment, facet);
        return facet;
    }

    private void extractPlaceData(final JSONObject segment, final MovesPlaceFacet facet) {
        JSONObject placeData = segment.getJSONObject("place");
        if (placeData.has("id"))
            facet.placeId = placeData.getLong("id");
        facet.type = placeData.getString("type");
        if (placeData.has("name"))
            facet.name = placeData.getString("name");
        else {
            // ask google
        }
        if (facet.type.equals("foursquare"))
            facet.foursquareId = placeData.getString("foursquareId");
        JSONObject locationData = placeData.getJSONObject("location");
        facet.latitude = (float) locationData.getDouble("lat");
        facet.longitude = (float) locationData.getDouble("lon");
    }

    private MovesMoveFacet extractMove(final String date, final JSONObject segment, final UpdateInfo updateInfo) {
        MovesMoveFacet facet = new MovesMoveFacet(updateInfo.apiKey.getId());
        facet.guestId=updateInfo.getGuestId();
        facet.date=date;
        extractMoveData(date, segment, facet, updateInfo);
        return facet;
    }

    private void extractMoveData(final String date, final JSONObject segment, final MovesFacet facet, UpdateInfo updateInfo) {
        facet.date = date;
        // The times given by Moves are absolute GMT, not local time
        final DateTime startTime = timeStorageFormat.withZoneUTC().parseDateTime(segment.getString("startTime"));
        final DateTime endTime = timeStorageFormat.withZoneUTC().parseDateTime(segment.getString("endTime"));
        facet.start = startTime.getMillis();
        facet.end = endTime.getMillis();
        facet.date=date;
        extractActivities(date, segment, facet, updateInfo);
    }

    private void extractActivities(final String date, final JSONObject segment, final MovesFacet facet, UpdateInfo updateInfo) {
        if (!segment.has("activities"))
            return;
        final JSONArray activities = segment.getJSONArray("activities");
        for (int i=0; i<activities.size(); i++) {
            JSONObject activityData = activities.getJSONObject(i);
            MovesActivity activity = extractActivity(date, updateInfo, activityData);
            facet.addActivity(activity);
        }
    }

    private MovesActivity extractActivity(final String date, final UpdateInfo updateInfo, final JSONObject activityData) {
        MovesActivity activity = new MovesActivity();
        activity.activity = activityData.getString("activity");
        // Generate a URI of the form '{wlk,cyc,trp}/UUID'.  The activity field must be set before calling createActivityURI
        activity.activityURI = createActivityURI(activity);

        final DateTime startTime = timeStorageFormat.withZoneUTC().parseDateTime(activityData.getString("startTime"));
        final DateTime endTime = timeStorageFormat.withZoneUTC().parseDateTime(activityData.getString("endTime"));

        // Note that unlike everywhere else in the sysetm, startTimeStorage and endTimeStorage here are NOT local times.
        // They are in GMT.
        activity.startTimeStorage = AbstractLocalTimeFacet.timeStorageFormat.print(startTime);
        activity.endTimeStorage = AbstractLocalTimeFacet.timeStorageFormat.print(endTime);

        activity.start = startTime.getMillis();
        activity.end = endTime.getMillis();

        // The date we use here is the date which we used to request this activity from the Moves API
        activity.date = date;

        if (activityData.has("steps"))
            activity.steps = activityData.getInt("steps");
        activity.distance = activityData.getInt("distance");
        extractTrackPoints(activity.activityURI, activityData, updateInfo);
        return activity;
    }

    private void extractTrackPoints(final String activityId, final JSONObject activityData, UpdateInfo updateInfo) {
        // Check if we actually have trackPoints in this activity data.  If not, return now.
        if(!activityData.has("trackPoints")) {
            return;
        }
        final JSONArray trackPoints = activityData.getJSONArray("trackPoints");
        List<LocationFacet> locationFacets = new ArrayList<LocationFacet>();
        // timeZone is computed based on first location for each batch of trackPoints
        TimeZone timeZone = null;
        Connector connector = Connector.getConnector("moves");
        for (int i=0; i<trackPoints.size(); i++) {
            JSONObject trackPoint = trackPoints.getJSONObject(i);
            LocationFacet locationFacet = new LocationFacet(updateInfo.apiKey.getId());
            locationFacet.latitude = (float) trackPoint.getDouble("lat");
            locationFacet.longitude = (float) trackPoint.getDouble("lon");
            // The two lines below would calculate the timezone if we cared, but the
            // timestamps from Moves are already in GMT, so don't mess with the timezone
            //if (timeZone==null)
            //    timeZone = metadataService.getTimeZone(locationFacet.latitude, locationFacet.longitude);
            final DateTime time = timeStorageFormat.withZoneUTC().parseDateTime(trackPoint.getString("time"));
            locationFacet.timestampMs = time.getMillis();
            locationFacet.api = connector.value();
            locationFacet.start = locationFacet.timestampMs;
            locationFacet.end = locationFacet.timestampMs;
            locationFacet.source = LocationFacet.Source.MOVES;
            locationFacet.apiKeyId = updateInfo.apiKey.getId();
            locationFacet.uri = activityId;
            locationFacets.add(locationFacet);
        }
        apiDataService.addGuestLocations(updateInfo.getGuestId(), locationFacets);
    }


}

