package org.fluxtream.core.api;

import com.google.gson.Gson;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import org.fluxtream.core.OutsideTimeBoundariesException;
import org.fluxtream.core.SimpleTimeInterval;
import org.fluxtream.core.TimeInterval;
import org.fluxtream.core.TimeUnit;
import org.fluxtream.core.auth.AuthHelper;
import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.domain.Guest;
import org.fluxtream.core.metadata.DayMetadata;
import org.fluxtream.core.mvc.models.PhotoModel;
import org.fluxtream.core.mvc.models.StatusModel;
import org.fluxtream.core.services.GuestService;
import org.fluxtream.core.services.MetadataService;
import org.fluxtream.core.services.PhotoService;
import org.fluxtream.core.services.SettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.SortedSet;

@Path("/guest/{username}/photo")
@Component("RESTPhotoStore")
@Api(value = "/guest/{username}/photo", description = "Retrieve the user's photos")
@Scope("request")
public class PhotoStore {

    private Gson gson = new Gson();

    @Autowired
    SettingsService settingsService;

    @Autowired
    GuestService guestService;

    @Autowired
    PhotoService photoService;

    @Autowired
    MetadataService metadataService;

    @GET
    @Path("/date/{date}")
    @Produces({MediaType.APPLICATION_JSON})
    @ApiOperation(value = "Get the user's photos for a specific date", responseContainer = "array", response = PhotoModel.class)
    public String getPhotosForDate(@ApiParam(value="Username (must be currently logged in user's username)", required=true) @PathParam("username") String username,
                                   @ApiParam(value="Date (yyyy-mm-dd)", required=true) @PathParam("date") String date){
        try{
            Guest guest = guestService.getGuest(username);
            if (AuthHelper.getGuest().getId()!=guest.getId())
                throw new RuntimeException("Attempt to access another user's photos");
            DayMetadata dayMeta = metadataService.getDayMetadata(guest.getId(), date);
            return gson.toJson(getPhotos(guest, dayMeta.getTimeInterval()));
        } catch (Exception e){
            StatusModel result = new StatusModel(false, "Could not get guest addresses: " + e.getMessage());
            return gson.toJson(result);
        }
    }

    @GET
    @Path("/week/{year}/{week}")
    @Produces({MediaType.APPLICATION_JSON})
    @ApiOperation(value = "Get the user's photos for a specific date", responseContainer = "array", response = PhotoModel.class)
    public String getPhotosForWeek(@ApiParam(value="Username (must be currently logged in user's username)", required=true) @PathParam("username") String username,
                                   @ApiParam(value="Year", required=true) @PathParam("year") int year,
                                   @ApiParam(value="Week", required=true) @PathParam("week") int week){
        try{
            Calendar c = Calendar.getInstance();
            c.set(Calendar.YEAR,year);
            c.set(Calendar.WEEK_OF_YEAR,week);
            c.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            Guest guest = guestService.getGuest(username);
            if (AuthHelper.getGuest().getId()!=guest.getId())
                throw new RuntimeException("Attempt to access another user's photos");
            DecimalFormat datePartFormat = new DecimalFormat("00");
            DayMetadata dayMetaStart = metadataService.getDayMetadata(guest.getId(), year + "-" + datePartFormat.format(c.get(Calendar.MONTH) + 1) +
                                                                                          "-" + datePartFormat.format(c.get(Calendar.DAY_OF_MONTH)));
            int newDay = c.get(Calendar.DAY_OF_YEAR) + 6;
            if (newDay > (isLeapYear(year) ? 366 : 365)){
                newDay -= isLeapYear(year) ? 366 : 365;
                year += 1;
                c.set(Calendar.YEAR,year);
            }
            c.set(Calendar.DAY_OF_YEAR,newDay);
            DayMetadata dayMetaEnd = metadataService.getDayMetadata(guest.getId(), year + "-" + datePartFormat.format(c.get(Calendar.MONTH) + 1) +
                                                                                          "-" + datePartFormat.format(c.get(Calendar.DAY_OF_MONTH)));
            return gson.toJson(getPhotos(guest, new SimpleTimeInterval(dayMetaStart.start,dayMetaEnd.end,TimeUnit.WEEK,dayMetaStart.getTimeInterval().getMainTimeZone())));
        } catch (Exception e){
            StatusModel result = new StatusModel(false, "Could not get photos: " + e.getMessage());
            return gson.toJson(result);
        }
    }

    @GET
    @Path("/year/{year}")
    @Produces({MediaType.APPLICATION_JSON})
    @ApiOperation(value = "Get the user's photos for an entire year", responseContainer = "array", response = PhotoModel.class)
    public String getPhotosForYear(@ApiParam(value="Username (must be currently logged in user's username)", required=true) @PathParam("username") String username,
                                   @ApiParam(value="Year", required=true) @PathParam("year") int year){
        try{

            Guest guest = guestService.getGuest(username);
            if (AuthHelper.getGuest().getId()!=guest.getId())
                throw new RuntimeException("Attempt to access another user's photos");
            DayMetadata dayMetaStart = metadataService.getDayMetadata(guest.getId(), year + "-01-01");

            DayMetadata dayMetaEnd = metadataService.getDayMetadata(guest.getId(), year + "-12-31");
            return gson.toJson(getPhotos(guest, new SimpleTimeInterval(dayMetaStart.start,dayMetaEnd.end,TimeUnit.YEAR,dayMetaStart.getTimeInterval().getMainTimeZone())));
        } catch (Exception e){
            StatusModel result = new StatusModel(false, "Could not get photos: " + e.getMessage());
            return gson.toJson(result);
        }

    }

    private boolean isLeapYear(int year){
        if (year % 400 == 0)
            return true;
        if (year % 100 == 0)
            return false;
        return year % 4 == 0;
    }

    private List<PhotoModel> getPhotos(Guest guest, TimeInterval timeInterval)
            throws ClassNotFoundException, IllegalAccessException, InstantiationException, OutsideTimeBoundariesException
    {
        final SortedSet<PhotoService.Photo> photos = photoService.getPhotos(guest.getId(), timeInterval, Connector.getConnector("fluxtream_capture").prettyName(), "photo", null);

        List<PhotoModel> photoModels = new ArrayList<PhotoModel>();
        for (final PhotoService.Photo photo : photos) {
            photoModels.add(new PhotoModel(photo.getAbstractPhotoFacetVO()));
        }

        return photoModels;
    }
}
