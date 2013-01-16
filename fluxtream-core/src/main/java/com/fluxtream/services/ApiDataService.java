package com.fluxtream.services;

import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.ObjectType;
import com.fluxtream.connectors.google_latitude.LocationFacet;
import com.fluxtream.connectors.updaters.UpdateInfo;
import com.fluxtream.domain.AbstractFacet;
import net.sf.json.JSONObject;
import org.dom4j.Document;
import org.springframework.transaction.annotation.Transactional;

public interface ApiDataService {

    //public EntityManager getEntityManager();

	public void cacheApiDataObject(UpdateInfo updateInfo, long start, long end,
			AbstractFacet payload) throws Exception;

	public void cacheApiDataJSON(UpdateInfo updateInfo, JSONObject jsonObject,
			long start, long end) throws Exception;

    public void cacheApiDataJSON(UpdateInfo updateInfo, String json,
                                 long start, long end, int objectTypes) throws Exception;

	public void cacheApiDataJSON(UpdateInfo updateInfo, String json,
			long start, long end) throws Exception;

	public void cacheApiDataXML(UpdateInfo updateInfo, String xml, long start,
			long end) throws Exception;

	public void cacheApiDataXML(UpdateInfo updateInfo, Document xmlDocument,
			long start, long end) throws Exception;

	public void setFacetComment(long guestId, ObjectType objectType,
			long facetId, String text);

	public void setFacetComment(long guestId, Connector connector,
			long facetId, String text);

    public List<AbstractFacet> getApiDataFacets(long guestId, Connector api,
                                                ObjectType objectType, List<String> dates);

	public List<AbstractFacet> getApiDataFacets(long guestId, Connector api,
			ObjectType objectType, TimeInterval timeInterval);

    public AbstractFacet getOldestApiDataFacet(long guestId, Connector connector, ObjectType objectType);
    public AbstractFacet getLatestApiDataFacet(long guestId, Connector connector, ObjectType objectType);

    /**
     * Returns up to <code>desiredCount</code> facets which have a timestamp equal to or before the given
     * <code>timeInMillis</code>.  Returns <code>null</code> if no facets are found.
     */
    public List<AbstractFacet> getApiDataFacetsBefore(long guestId,
                                                      Connector connector,
                                                      ObjectType objectType,
                                                      long timeInMillis,
                                                      int desiredCount);

    /**
     * Returns up to <code>desiredCount</code> facets which have a timestamp equal to or after the given
     * <code>timeInMillis</code>.  Returns <code>null</code> if no facets are found.
     */
    public List<AbstractFacet> getApiDataFacetsAfter(long guestId,
                                                     Connector connector,
                                                     ObjectType objectType,
                                                     long timeInMillis,
                                                     int desiredCount);

    public AbstractFacet persistFacet(AbstractFacet facet);

    // Pass this to createOrReadModifyWrite
    public interface FacetModifier<T extends AbstractFacet> {
        // Override this with your code to either modify or create
        // a facet.  If you are passed facet != null, modify that
        // facet and return it.  If you are passed facet == null,
        // create a new facet, fill it in, and return it
        public T createOrModify(T facet);
    }

    public class FacetQuery {
        public String query;
        public Object[] args;
        // query must include reference to "e", and question marks for each arg, e.g.
        // new FacetQuery("e.guestId = ? AND e.mymeeId = ?", guestId, mymeeId);
        public FacetQuery(String query, Object... args) {
            this.query = query;
            this.args = args;
        }
    }

    public <T extends AbstractFacet> T createOrReadModifyWrite(Class<? extends AbstractFacet> facetClass, FacetQuery query, FacetModifier<T> modifier);

	public void eraseApiData(long guestId, Connector api);

	public void eraseApiData(long guestId, Connector api, int objectTypes);

	public void eraseApiData(long guestId, Connector api, ObjectType objectType);

	public void eraseApiData(long guestId, Connector api, int objectTypes,
			TimeInterval timeInterval);

	public void eraseApiData(long guestId, Connector api,
			ObjectType objectType, TimeInterval timeInterval);

    public void eraseApiData(long guestId, Connector api,
                             ObjectType objectType, List<String> dates);

	public void cacheEmptyData(UpdateInfo updateInfo, long fromMidnight,
			long toMidnight);

	public long getNumberOfDays(long guestId);

    void addGuestLocation(long guestId, LocationFacet locationResource);

}
