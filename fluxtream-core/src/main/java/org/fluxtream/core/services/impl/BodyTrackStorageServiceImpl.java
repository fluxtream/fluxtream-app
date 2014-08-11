package org.fluxtream.core.services.impl;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import org.fluxtream.core.Configuration;
import org.fluxtream.core.SimpleTimeInterval;
import org.fluxtream.core.TimeInterval;
import org.fluxtream.core.TimeUnit;
import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.connectors.ObjectType;
import org.fluxtream.core.domain.AbstractFacet;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.services.ApiDataService;
import org.fluxtream.core.services.BodyTrackStorageService;
import org.fluxtream.core.services.DataUpdateService;
import org.fluxtream.core.services.GuestService;
import org.fluxtream.core.services.MetadataService;
import org.apache.commons.lang.StringUtils;
import org.fluxtream.core.aspects.FlxLogger;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Service
@Component
public class BodyTrackStorageServiceImpl implements BodyTrackStorageService {

    static FlxLogger logger = FlxLogger.getLogger(BodyTrackStorageServiceImpl.class);

	@Autowired
	Configuration env;

    @Autowired
    private DataUpdateService dataUpdateSerivce;

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
        logStoreApiData(guestId, facets);

		Map<String, List<AbstractFacet>> facetsByFacetName = sortFacetsByFacetName(facets);
        for (final String facetName : facetsByFacetName.keySet()) {
            List<BodyTrackHelper.BodyTrackUploadResult> results = storeDeviceData(guestId, facetsByFacetName, facetName);
            if (!results.isEmpty()){
                AbstractFacet facet = facetsByFacetName.get(facetName).get(0);
                long apiKeyId = facet.apiKeyId;
                long objectTypeId = facet.objectType;
                for (BodyTrackHelper.BodyTrackUploadResult result : results){
                    if (!(result instanceof BodyTrackHelper.ParsedBodyTrackUploadResult))
                        continue;
                    BodyTrackHelper.ParsedBodyTrackUploadResult parsedResult = (BodyTrackHelper.ParsedBodyTrackUploadResult) result;
                    dataUpdateSerivce.logBodyTrackDataUpdate(guestId,apiKeyId,objectTypeId,parsedResult);

                }
            }
        }

	}

    private void logStoreApiData(final long guestId, final List<AbstractFacet> facets) {
        StringBuilder sb = new StringBuilder("module=updateQueue component=bodytrackStorageService action=storeApiData")
                .append(" guestId=").append(guestId);
        if (facets.size()>0) {
            try {
                String connectorName = Connector.fromValue(facets.get(0).api).getName();
                sb.append(" connector=" + connectorName);
            } catch (Throwable t) {
                sb.append(" message=\"could not figure out connector name...\"");
            }
        }
        logger.info(sb.toString());
    }

    private List<BodyTrackHelper.BodyTrackUploadResult> storeDeviceData(long guestId,
			Map<String, List<AbstractFacet>> facetsByDeviceNickname,
			String facetName) {
        List<BodyTrackHelper.BodyTrackUploadResult> results = new ArrayList<BodyTrackHelper.BodyTrackUploadResult>();
        String deviceName = getDeviceNickname(facetName);
        List<AbstractFacet> deviceFacets = facetsByDeviceNickname.get(facetName);

        results.add(uploadDailyData(guestId, deviceName, deviceFacets, facetName));

        List<FieldHandler> facetFieldHandlers = getFieldHandlers(facetName);
        for (FieldHandler fieldHandler : facetFieldHandlers) {
            results.addAll(uploadIntradayData(guestId, deviceFacets, fieldHandler));
        }
        return results;
    }

    private BodyTrackHelper.BodyTrackUploadResult uploadDailyData(long guestId, String deviceName, List<AbstractFacet> deviceFacets, String facetName) {
        List<String> datastoreChannelNames = getDailyDatastoreChannelNames(facetName);
        List<String> facetColumnNames = getFacetColumnNames(facetName);
        List<List<Object>> dailyDataChannelValues = getDailyDataChannelValues(deviceFacets, facetColumnNames);

        // TODO: check the status code in the BodyTrackUploadResult
        return bodyTrackHelper.uploadToBodyTrack(guestId, deviceName, datastoreChannelNames, dailyDataChannelValues);
    }

    private List<BodyTrackHelper.BodyTrackUploadResult> uploadIntradayData(long guestId, List<AbstractFacet> deviceFacets, FieldHandler fieldHandler) {
        List<BodyTrackHelper.BodyTrackUploadResult> results = new ArrayList<BodyTrackHelper.BodyTrackUploadResult>();
        for (AbstractFacet deviceFacet : deviceFacets)
            results.addAll(fieldHandler.handleField(guestId, deviceFacet));
        return results;
    }

    private FieldHandler getFieldHandler(String fieldHandlerName) {
        String HandlerName = fieldHandlerName.substring(1);
        if (fieldHandlers.get(HandlerName)==null) {
            FieldHandler fieldHandler;
            fieldHandler = (FieldHandler)beanFactory.getBean(HandlerName);
            fieldHandlers.put(HandlerName, fieldHandler);
        }
        return fieldHandlers.get(HandlerName);
    }

    private List<String> getDailyDatastoreChannelNames(String facetName) {
        String[] channelNamesMappings = env.bodytrackProperties.getStringArray(facetName + ".channel_names");
        List<String> channelNames = new ArrayList<String>();
        for (String mapping : channelNamesMappings) {
            String[] terms = StringUtils.split(mapping, ":");
            if (terms[1].startsWith("#"))
                continue;
            channelNames.add(terms[0]);
        }
        return channelNames;
    }

    private List<String> getFacetColumnNames(String facetName) {
        String[] channelNamesMappings = env.bodytrackProperties.getStringArray(facetName + ".channel_names");
        List<String> channelNames = new ArrayList<String>();
        for (String mapping : channelNamesMappings) {
            String[] terms = StringUtils.split(mapping, ":");
            if (terms[1].startsWith("#"))
                continue;
            channelNames.add(terms[1]);
        }
        return channelNames;
    }

    private List<FieldHandler> getFieldHandlers(String facetName) {
        String[] channelNamesMappings = env.bodytrackProperties.getStringArray(facetName + ".channel_names");
        List<FieldHandler> fieldHandlers = new ArrayList<FieldHandler>();
        for (String mapping : channelNamesMappings) {
            String[] terms = StringUtils.split(mapping, ":");
            if (terms[1].startsWith("#")) {
                String handlerName = terms[1].substring(1);
                if (handlerName.equalsIgnoreCase("NOOP")||handlerName.equalsIgnoreCase("OOP"))
                    continue;
                FieldHandler fieldHandler = getFieldHandler(handlerName);
                fieldHandlers.add(fieldHandler);
            }
        }
        return fieldHandlers;
    }

    private List<List<Object>> getDailyDataChannelValues(
            List<AbstractFacet> deviceFacets,
            List<String> dailyDataChannelNames) {
        List<List<Object>> channelValues = new ArrayList<List<Object>>();
        for (AbstractFacet deviceFacet : deviceFacets) {
            Iterator<String> eachFieldName = dailyDataChannelNames.iterator();
            List<Object> values = new ArrayList<Object>();
            values.add(deviceFacet.start / 1000.0);
            while (eachFieldName.hasNext()) {
                String fieldName = eachFieldName.next();
                try {
                    Field field;
                    field = deviceFacet.getClass().getField(fieldName);
                    Object channelValue = field.get(deviceFacet);
                    if (channelValue instanceof java.util.Date) {
                        values.add(((java.util.Date)channelValue).getTime());
                    }
                    else {
                        values.add(channelValue);
                    }
                }
                catch (Exception e) {
                    throw new RuntimeException("No such Field: " + fieldName);
                }
            }
            channelValues.add(values);
        }
        return channelValues;
    }

    private Map<String, List<AbstractFacet>> sortFacetsByFacetName(List<AbstractFacet> facets) {
		Map<String, List<AbstractFacet>> facetsByDeviceNickname = new HashMap<String, List<AbstractFacet>>();
		for (AbstractFacet facet : facets) {
			Connector connector = Connector.fromValue(facet.api);
			String connectorAndObjectType = connector.getName();
			if (connector.objectTypes()!=null&&connector.objectTypes().length>0) {
                ObjectType objectType = ObjectType.getObjectType(connector, facet.objectType);
				if(objectType !=null) {
                    connectorAndObjectType += "." + objectType.getName();
                }
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
			String key = keys.next();
			if (key.startsWith(connectorAndObjectType)) {
				if (key.endsWith("dev_nickname"))
					return (String) env.bodytrackProperties.getProperty(key);
			}
		}
		return null;
	}

	@Override
	public void storeInitialHistory(ApiKey apiKey) {
        logger.info("module=updateQueue component=bodytrackStorageService action=storeInitialHistory" +
                    " guestId=" + apiKey.getGuestId() + " connector=" + apiKey.getConnector().getName());
		TimeInterval timeInterval = new SimpleTimeInterval(0,
				System.currentTimeMillis(), TimeUnit.ARBITRARY, TimeZone.getDefault());
		List<AbstractFacet> facets = apiDataService.getApiDataFacets(apiKey, null, timeInterval);
		storeApiData(apiKey.getGuestId(), facets);
	}

}
