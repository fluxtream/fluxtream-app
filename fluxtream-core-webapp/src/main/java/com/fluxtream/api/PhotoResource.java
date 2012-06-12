package com.fluxtream.api;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;
import java.util.TimeZone;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import com.fluxtream.TimeInterval;
import com.fluxtream.TimeUnit;
import com.fluxtream.connectors.ObjectType;
import com.fluxtream.connectors.vos.AbstractFacetVO;
import com.fluxtream.connectors.vos.AbstractInstantFacetVO;
import com.fluxtream.connectors.vos.AbstractPhotoFacetVO;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.domain.Guest;
import com.fluxtream.domain.metadata.City;
import com.fluxtream.domain.metadata.DayMetadataFacet;
import com.fluxtream.mvc.controllers.ControllerHelper;
import com.fluxtream.mvc.models.PhotoModel;
import com.fluxtream.mvc.models.StatusModel;
import com.fluxtream.services.ApiDataService;
import com.fluxtream.services.GuestService;
import com.fluxtream.services.MetadataService;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Path("/guest/{username}/photo")
@Component("photoApi")
@Scope("request")
public class PhotoResource {

    private Gson gson = new Gson();

    @Autowired
    private ApiDataService apiDataService;

    @Autowired
    GuestService guestService;

    @Autowired
    MetadataService metadataService;

    @GET
    @Path("/date/{date}")
    @Produces({MediaType.APPLICATION_JSON})
    public String getPhotosForDate(@PathParam("username") String username, @PathParam("date") String date){
        try{
            Guest guest = guestService.getGuest(username);
            DayMetadataFacet dayMeta = metadataService.getDayMetadata(guest.getId(), date, true);
            return gson.toJson(getPhotos(guest, dayMeta.getTimeInterval()));
        } catch (Exception e){
            StatusModel result = new StatusModel(false, "Could not get guest addresses: " + e.getMessage());
            return gson.toJson(result);
        }
    }

    @GET
    @Path("/week/{year}/{week}")
    @Produces({MediaType.APPLICATION_JSON})
    public String getPhotosForWeek(@PathParam("username") String username, @PathParam("year") int year, @PathParam("week") int week){
        try{
            Calendar c = Calendar.getInstance();
            c.set(Calendar.YEAR,year);
            c.set(Calendar.WEEK_OF_YEAR,week);
            c.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            Guest guest = guestService.getGuest(username);
            DecimalFormat datePartFormat = new DecimalFormat("00");
            DayMetadataFacet dayMetaStart = metadataService.getDayMetadata(guest.getId(), year + "-" + datePartFormat.format(c.get(Calendar.MONTH) + 1) +
                                                                                          "-" + datePartFormat.format(c.get(Calendar.DAY_OF_MONTH)), true);
            int newDay = c.get(Calendar.DAY_OF_YEAR) + 6;
            if (newDay > (isLeapYear(year) ? 366 : 365)){
                newDay -= isLeapYear(year) ? 366 : 365;
                year += 1;
                c.set(Calendar.YEAR,year);
            }
            c.set(Calendar.DAY_OF_YEAR,newDay);
            DayMetadataFacet dayMetaEnd = metadataService.getDayMetadata(guest.getId(), year + "-" + datePartFormat.format(c.get(Calendar.MONTH) + 1) +
                                                                                          "-" + datePartFormat.format(c.get(Calendar.DAY_OF_MONTH)), true);
            return gson.toJson(getPhotos(guest, new TimeInterval(dayMetaStart.start,dayMetaEnd.end,TimeUnit.WEEK,TimeZone.getTimeZone(dayMetaStart.timeZone))));
        } catch (Exception e){
            StatusModel result = new StatusModel(false, "Could not get guest addresses: " + e.getMessage());
            return gson.toJson(result);
        }
    }

    @GET
    @Path("/year/{year}")
    @Produces({MediaType.APPLICATION_JSON})
    public String getPhotosForYear(@PathParam("username") String username, @PathParam("year") int year){
        try{

            Guest guest = guestService.getGuest(username);
            DayMetadataFacet dayMetaStart = metadataService.getDayMetadata(guest.getId(), year + "-01-01", true);

            DayMetadataFacet dayMetaEnd = metadataService.getDayMetadata(guest.getId(), year + "-12-31", true);
            return gson.toJson(getPhotos(guest, new TimeInterval(dayMetaStart.start,dayMetaEnd.end,TimeUnit.WEEK,TimeZone.getTimeZone(dayMetaStart.timeZone))));
        } catch (Exception e){
            StatusModel result = new StatusModel(false, "Could not get guest addresses: " + e.getMessage());
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


    private List<PhotoModel> getPhotos(Guest guest, TimeInterval timeInterval) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        List<ApiKey> userKeys = guestService.getApiKeys(guest.getId());
        List<AbstractFacet> facets = new ArrayList<AbstractFacet>();
        for (ApiKey key : userKeys){
            if (!key.getConnector().hasImageObjectType())
                continue;
            ObjectType[] objectTypes = key.getConnector().objectTypes();
            if (objectTypes == null)
                facets.addAll(apiDataService.getApiDataFacets(guest.getId(), key.getConnector(), null, timeInterval));
            else
                for (ObjectType objectType : objectTypes)
                    facets.addAll(apiDataService.getApiDataFacets(guest.getId(),key.getConnector(),objectType,timeInterval));
        }
        List<PhotoModel> photos = new ArrayList<PhotoModel>();
        for (AbstractFacet facet : facets) {
            Class<? extends AbstractFacetVO<AbstractFacet>> jsonFacetClass = AbstractFacetVO.getFacetVOClass(facet);
            AbstractInstantFacetVO<AbstractFacet> facetVo = (AbstractInstantFacetVO<AbstractFacet>) jsonFacetClass.newInstance();
            facetVo.extractValues(facet, timeInterval, null);
            photos.add(new PhotoModel((AbstractPhotoFacetVO) facetVo));
        }
        Collections.sort(photos,new Comparator<PhotoModel>(){
            public int compare(PhotoModel o1, PhotoModel o2){
                return (int) (o1.timeTaken - o2.timeTaken);
            }

        });
        return photos;
    }
}
