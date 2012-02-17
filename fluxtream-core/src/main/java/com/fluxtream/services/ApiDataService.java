package com.fluxtream.services;

import java.util.List;

import net.sf.json.JSONObject;

import org.dom4j.Document;

import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.ObjectType;
import com.fluxtream.connectors.updaters.UpdateInfo;
import com.fluxtream.domain.AbstractFacet;

public interface ApiDataService {

	public void cacheApiDataObject(UpdateInfo updateInfo, long start, long end,
			AbstractFacet payload) throws Exception;

	public void cacheApiDataJSON(UpdateInfo updateInfo, JSONObject jsonObject,
			long start, long end) throws Exception;

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
			ObjectType objectType, TimeInterval timeInterval);

	public void eraseApiData(long guestId, Connector api);

	public void eraseApiData(long guestId, Connector api, int objectTypes);

	public void eraseApiData(long guestId, Connector api, ObjectType objectType);

	public void eraseApiData(long guestId, Connector api, int objectTypes,
			TimeInterval timeInterval);

	public void eraseApiData(long guestId, Connector api,
			ObjectType objectType, TimeInterval timeInterval);

	public void cacheEmptyData(UpdateInfo updateInfo, long fromMidnight,
			long toMidnight);
	
	public long getNumberOfDays(long guestId);

}
