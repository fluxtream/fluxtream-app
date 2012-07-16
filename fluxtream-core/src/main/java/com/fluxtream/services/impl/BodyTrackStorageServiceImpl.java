package com.fluxtream.services.impl;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import com.fluxtream.Configuration;
import com.fluxtream.TimeInterval;
import com.fluxtream.TimeUnit;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.ObjectType;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.services.ApiDataService;
import com.fluxtream.services.BodyTrackStorageService;
import com.fluxtream.services.GuestService;
import com.fluxtream.services.MetadataService;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Service
@Component
public class BodyTrackStorageServiceImpl implements BodyTrackStorageService {

	static Logger LOG = Logger.getLogger(BodyTrackStorageServiceImpl.class);

	@Autowired
	Configuration env;

	@Autowired
	GuestService guestService;

    @Qualifier("apiDataServiceImpl")
    @Autowired
	ApiDataService apiDataService;

    @Qualifier("metadataServiceImpl")
    @Autowired
	MetadataService metadataService;

    @Autowired
    BodyTrackHelper bodyTrackHelper;

    @Autowired
    BeanFactory beanFactory;

    private Hashtable<String, FieldHandler> fieldHandlers = new Hashtable<String, FieldHandler>();

	@Override
	public void storeApiData(long guestId, List<AbstractFacet> facets) {

		Connector bodytrackConnector = Connector.getConnector("bodytrack");
		ApiKey bodytrackApiKey = guestService.getApiKey(guestId,
				bodytrackConnector);
		if (bodytrackApiKey == null)
			return;

		Map<String, List<AbstractFacet>> facetsByFacetName = sortFacetsByFacetName(facets);
        for (final String facetName : facetsByFacetName.keySet()) {
            storeDeviceData(guestId, facetsByFacetName, facetName);
        }

	}

	private void storeDeviceData(long guestId,
			Map<String, List<AbstractFacet>> facetsByDeviceNickname,
			String facetName) {
        String deviceName = getDeviceNickname(facetName);
		Map<String, String> channelNamesMapping = getChannelNamesMapping(facetName);
		List<AbstractFacet> deviceFacets = facetsByDeviceNickname.get(facetName);
		List<List<Object>> channelValues = extractChannelValuesFromFacets(channelNamesMapping, deviceFacets, guestId);
		//String jsonArray = makeJSONArray(channelValues, false);
		//params.put("data", jsonArray);

        bodyTrackHelper.uploadToBodyTrack(guestId, deviceName, channelNamesMapping.values(), channelValues, new HashMap<String,Object>());
    }

    private List<List<Object>> extractChannelValuesFromFacets(
            Map<String, String> channelNamesMapping,
            List<AbstractFacet> deviceFacets, final long guestId) {
		List<List<Object>> channelValues = new ArrayList<List<Object>>();
		for (AbstractFacet deviceFacet : deviceFacets) {
            List<Object> values = new ArrayList<Object>();
            values.add(deviceFacet.start/1000.0);
            for(String fieldName : channelNamesMapping.keySet()){
                try {
                    Field field;
                    String bodyTrackMappedField = channelNamesMapping.get(fieldName);
                    if (bodyTrackMappedField.startsWith("#")) {
                        String fieldHandlerName = bodyTrackMappedField.substring(1);
                        if (!fieldHandlerName.equalsIgnoreCase("NOOP")) {
                            // #! indicates a "fieldHandler"
                            if (fieldHandlerName.startsWith("!")) {
                                FieldHandler fieldHandler = getFieldHandler(fieldHandlerName);
                                fieldHandler.handleField(guestId, deviceFacet);
                            } else {
                                LOG.warn("********* NO SUPPORT FOR #fields FOR NOW **************** " + fieldHandlerName);
                            }
                            // else... it might be a "converter"... if/when there is a use-case for it
                        }
                        continue;
                    }
					field = deviceFacet.getClass().getField(fieldName);
					Object channelValue = field.get(deviceFacet);
					if (channelValue instanceof Date)
                        values.add(((Date)channelValue).getTime());
					else
                        values.add(channelValue);
				} catch (Exception e) {
					throw new RuntimeException("No such Field: " + fieldName);
				}
			}
			channelValues.add(values);
		}
		return channelValues;
	}

    private FieldHandler getFieldHandler(String fieldHandlerName) {
        fieldHandlerName = fieldHandlerName.substring(1);
        if (fieldHandlers.get(fieldHandlerName)==null) {
            FieldHandler fieldHandler;
            fieldHandler = (FieldHandler)beanFactory.getBean(fieldHandlerName);
            fieldHandlers.put(fieldHandlerName, fieldHandler);
        }
        return fieldHandlers.get(fieldHandlerName);
    }

    private String makeJSONArray(Collection<String> values, boolean addQuotes) {
		StringBuilder sb = new StringBuilder("[");
		Iterator<String> eachChannelName = values.iterator();
		for (int i=0; eachChannelName.hasNext(); i++) {
			String channelName = eachChannelName.next();
			if (i>0) sb.append(",");
			if (addQuotes)
				sb.append("\"").append(channelName).append("\"");
			else
				sb.append(channelName);
		}
		sb.append("]");
		return sb.toString();
	}

	private Map<String,String> getChannelNamesMapping(String facetName) {
		String[] channelNamesMappings = env.bodytrackProperties.getStringArray(facetName + ".channel_names");
		Map<String,String> mappings = new HashMap<String,String>();
		for (String mapping : channelNamesMappings) {
			String[] terms = StringUtils.split(mapping, ":");
            if (terms[1].startsWith("#")) {
                String converterName = terms[1].substring(1);
                if (converterName.equalsIgnoreCase("NOOP")||converterName.equalsIgnoreCase("OOP"))
                    continue;
                String bodytrackChannelName = getFieldHandler(converterName).getBodytrackChannelName();
                mappings.put(terms[0], "#!" + bodytrackChannelName);
            } else
    			mappings.put(terms[0], terms[1]);
		}
		return mappings;
	}

	private Map<String, List<AbstractFacet>> sortFacetsByFacetName(List<AbstractFacet> facets) {
		Map<String, List<AbstractFacet>> facetsByDeviceNickname = new HashMap<String, List<AbstractFacet>>();
		for (AbstractFacet facet : facets) {
			Connector connector = Connector.fromValue(facet.api);
			String connectorAndObjectType = connector.getName();
			if (connector.objectTypes()!=null&&connector.objectTypes().length>0) {
				connectorAndObjectType += "." + ObjectType.getObjectType(connector,
					facet.objectType).getName();
			}
			String deviceNickname = getDeviceNickname(connectorAndObjectType);
			if (deviceNickname==null) {
//				logger.info("No Device Nickname for " + connectorAndObjectType);
				continue;
			}
			if (facetsByDeviceNickname.get(connectorAndObjectType)==null)
				facetsByDeviceNickname.put(connectorAndObjectType, new ArrayList<AbstractFacet>());
			facetsByDeviceNickname.get(connectorAndObjectType).add(facet);
		}
		return facetsByDeviceNickname;
	}

	private String getDeviceNickname(String connectorAndObjectType) {
		Iterator<String> keys = env.bodytrackProperties.getKeys();
		while (keys.hasNext()) {
			String key = (String) keys.next();
			if (key.startsWith(connectorAndObjectType)) {
				if (key.endsWith("dev_nickname"))
					return (String) env.bodytrackProperties.getProperty(key);
			}
		}
		return null;
	}

	@Override
	public void storeInitialHistory(long guestId, String connectorName) {
		TimeInterval timeInterval = new TimeInterval(0,
				System.currentTimeMillis(), TimeUnit.DAY, TimeZone.getDefault());
		List<AbstractFacet> facets = apiDataService.getApiDataFacets(guestId,
				Connector.getConnector(connectorName), null, timeInterval);
		storeApiData(guestId, facets);
	}

}
