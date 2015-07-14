package org.fluxtream.core.services.impl;

import org.apache.commons.lang.StringUtils;
import org.fluxtream.core.Configuration;
import org.fluxtream.core.SimpleTimeInterval;
import org.fluxtream.core.TimeInterval;
import org.fluxtream.core.TimeUnit;
import org.fluxtream.core.aspects.FlxLogger;
import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.connectors.ObjectType;
import org.fluxtream.core.connectors.bodytrackResponders.AbstractBodytrackResponder;
import org.fluxtream.core.connectors.updaters.AbstractUpdater;
import org.fluxtream.core.domain.*;
import org.fluxtream.core.services.*;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.lang.reflect.Field;
import java.util.*;

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

    @Autowired
	ApiDataService apiDataService;

    @Autowired
	MetadataService metadataService;

    @Autowired
    BodyTrackHelper bodyTrackHelper;

    @Autowired
    BeanFactory beanFactory;

    @PersistenceContext
    EntityManager em;

    private Hashtable<String, FieldHandler> fieldHandlers = new Hashtable<String, FieldHandler>();

	@Override
	public void storeApiData(ApiKey apiKey, List<? extends AbstractFacet> facets) {
        logStoreApiData(apiKey.getGuestId(), facets);

		Map<String, List<AbstractFacet>> facetsByFacetName = sortFacetsByFacetName(facets);
        for (final String facetName : facetsByFacetName.keySet()) {
            List<BodyTrackHelper.BodyTrackUploadResult> results = storeDeviceData(apiKey, facetsByFacetName, facetName);
            if (!results.isEmpty()){
                AbstractFacet facet = facetsByFacetName.get(facetName).get(0);
                long apiKeyId = facet.apiKeyId;
                long objectTypeId = facet.objectType;
                for (BodyTrackHelper.BodyTrackUploadResult result : results){
                    if (!(result instanceof BodyTrackHelper.ParsedBodyTrackUploadResult))
                        continue;
                    BodyTrackHelper.ParsedBodyTrackUploadResult parsedResult = (BodyTrackHelper.ParsedBodyTrackUploadResult) result;
                    dataUpdateSerivce.logBodyTrackDataUpdate(apiKey.getGuestId(),apiKeyId,objectTypeId,parsedResult);
                }
            }
        }

	}

    /**
     * Channels can be thought of as containers: sometimes they have data, some times they don't. If a guest wants
     * to share their data, these containers need to be explicitely listed, even if they are momentarily empty. This
     * method will list all channels that we know of in advance, irrespective of their containing data or not.
     * @param apiKey
     */
    @Override
    @Transactional(readOnly=false)
    public boolean mapChannels(ApiKey apiKey) {
        try {
            final Connector connector = apiKey.getConnector();

            Query saveExistingSharedChannelsQuery = em.createQuery("SELECT sc FROM SharedChannels sc WHERE sc.channelMapping.apiKeyId=? AND sc.channelMapping.creationType=?");
            saveExistingSharedChannelsQuery.setParameter(1, apiKey.getId());
            saveExistingSharedChannelsQuery.setParameter(2, ChannelMapping.CreationType.mapChannels);
            List<SharedChannel> savedSharedChannels = saveExistingSharedChannelsQuery.getResultList();

            for (SharedChannel savedSharedChannel : savedSharedChannels)
                em.remove(savedSharedChannel);

            // only delete mappings that were previously created by this method
            Query deleteExistingMappingsQuery = em.createQuery("DELETE FROM ChannelMapping mapping WHERE mapping.apiKeyId=? AND mapping.creationType=?");
            deleteExistingMappingsQuery.setParameter(1, apiKey.getId());
            deleteExistingMappingsQuery.setParameter(2, ChannelMapping.CreationType.mapChannels);
            deleteExistingMappingsQuery.executeUpdate();

            final AbstractBodytrackResponder bodytrackResponder = connector.getBodytrackResponder(beanFactory);
            if (bodytrackResponder != null) {
                final List<ChannelMapping> responderMappings = new ArrayList<ChannelMapping>();
                bodytrackResponder.addToDeclaredChannelMappings(apiKey, responderMappings);
                for (ChannelMapping responderMapping : responderMappings) {
                    responderMapping.setCreationType(ChannelMapping.CreationType.mapChannels);
                    em.persist(responderMapping);
                }
            }
            final Iterator<String> keys = env.bodytrackProperties.getKeys();
            while (keys.hasNext()) {
                String key = keys.next();
                if (key.startsWith(connector.getName()) && key.indexOf("channel_names") != -1) {
                    String[] keySplits = key.split("\\.");
                    String objectTypeName = keySplits[1];
                    final ObjectType objectType = ObjectType.getObjectType(connector, objectTypeName);
                    if (objectType == null) {
                        logger.warn("Could not find objectType named " + objectTypeName + " for connector " + connector.getName());
                        continue;
                    }
                    final String facetName = keySplits[0] + "." + objectTypeName;
                    final List<String> facetFieldChannelNames = getDatastoreChannelNames(facetName);
                    for (String facetFieldChannelName : facetFieldChannelNames) {
                        final ChannelMapping facetFieldChannelMapping = createFacetFieldChanneMapping(facetFieldChannelName, apiKey, objectType);
                        facetFieldChannelMapping.setCreationType(ChannelMapping.CreationType.mapChannels);
                        em.persist(facetFieldChannelMapping);
                    }
                    final List<FieldHandler> handlers = getFieldHandlers(facetName);
                    for (FieldHandler fieldHandler : handlers) {
                        final List<ChannelMapping> fieldHandlerMappings = new ArrayList<ChannelMapping>();
                        fieldHandler.addToDeclaredChannelMappings(apiKey, fieldHandlerMappings);
                        for (ChannelMapping fieldHandlerMapping : fieldHandlerMappings) {
                            fieldHandlerMapping.setCreationType(ChannelMapping.CreationType.mapChannels);
                            em.persist(fieldHandlerMapping);
                        }
                    }
                    break;
                }
            }

            // add photo channels
            ObjectType[] objectTypes = apiKey.getConnector().objectTypes();
            for (ObjectType objectType : objectTypes) {
                if (objectType.isImageType()) {
                    ChannelMapping.TimeType timeType = getTimeType(connector, objectType);
                    ChannelMapping photoChannelMapping = new ChannelMapping(apiKey.getId(), apiKey.getGuestId(),
                            ChannelMapping.ChannelType.photo, timeType, objectType.value(),
                            apiKey.getConnector().getDeviceNickname(), "photo",
                            apiKey.getConnector().getDeviceNickname(), "photo");
                    photoChannelMapping.setCreationType(ChannelMapping.CreationType.mapChannels);
                    em.persist(photoChannelMapping);
                }
            }

            // restore the save sharedChannels
            for (SharedChannel savedSharedChannel : savedSharedChannels) {
                ChannelMapping newMapping = getChannelMapping(apiKey.getId(), savedSharedChannel.channelMapping);
                if (newMapping!=null) {
                    SharedChannel restoredSharedChannel = new SharedChannel();
                    restoredSharedChannel.buddy = savedSharedChannel.buddy;
                    restoredSharedChannel.channelMapping = newMapping;
                    em.persist(restoredSharedChannel);
                } else {
                    String message = "Could not restore saved shared channel because a corresponding channel mapping could not be found: ";
                    System.out.println(message);
                    logger.warn(message);
                }
            }

            // finally, possibly create default ChannelStyles
            final AbstractUpdater updater = beanFactory.getBean(connector.getUpdaterClass());
            updater.setDefaultChannelStyles(apiKey);
        } catch (Throwable t) {
            System.out.println("Can't map channels");
            t.printStackTrace();
            return false;
        }
        return true;
    }

    public ChannelMapping getChannelMapping(long apiKeyId, ChannelMapping oldMapping) {
        Query query = em.createQuery("SELECT channelMapping FROM ChannelMapping channelMapping WHERE channelMapping.apiKeyId=? AND " +
                "channelMapping.deviceName=? AND channelMapping.channelName=? AND " +
                "channelMapping.internalDeviceName=? AND channelMapping.internalChannelName=?");
        query.setParameter(1, apiKeyId);
        query.setParameter(2, oldMapping.getDeviceName());
        query.setParameter(3, oldMapping.getChannelName());
        query.setParameter(4, oldMapping.getInternalDeviceName());
        query.setParameter(5, oldMapping.getInternalChannelName());
        List<ChannelMapping> mappings = query.getResultList();
        if (mappings.size()>0)
            return mappings.get(0);
        return null;
    }

    @Override
    public List<ChannelMapping> getChannelMappings(long apiKeyId) {
        Query query = em.createQuery("SELECT channelMapping FROM ChannelMapping channelMapping WHERE channelMapping.apiKeyId=?");
        query.setParameter(1, apiKeyId);
        List mappings = query.getResultList();
        return mappings;
    }

    private ChannelMapping createFacetFieldChanneMapping(String facetFieldChannelName, ApiKey apiKey, ObjectType objectType) {
        ChannelMapping.ChannelType channelType = ChannelMapping.ChannelType.data;
        if (objectType.isImageType())
            channelType = ChannelMapping.ChannelType.photo;
        Connector connector = apiKey.getConnector();
        final String deviceName = connector.getDeviceNickname();
        ChannelMapping.TimeType timeType = getTimeType(connector, objectType);
        ChannelMapping declaredMapping = new ChannelMapping(apiKey.getId(), apiKey.getGuestId(),
                channelType, timeType, objectType.value(),
                deviceName, facetFieldChannelName,
                deviceName, facetFieldChannelName);
        return declaredMapping;
    }

    private ChannelMapping.TimeType getTimeType(Connector connector, ObjectType objectType) {
        ChannelMapping.TimeType timeType = ChannelMapping.TimeType.gmt;
        if (Arrays.asList("zeo", "flickr", "fitbit").contains(connector.getName()))
            timeType = ChannelMapping.TimeType.local;
        else if (objectType!=null && objectType.facetClass()!=null &&
                AbstractLocalTimeFacet.class.isAssignableFrom(objectType.facetClass()))
            return ChannelMapping.TimeType.local;
        return timeType;
    }

    private void logStoreApiData(final long guestId, final List<? extends AbstractFacet> facets) {
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

    private List<BodyTrackHelper.BodyTrackUploadResult> storeDeviceData(ApiKey apiKey,
			Map<String, List<AbstractFacet>> facetsByDeviceNickname,
			String facetName) {
        List<BodyTrackHelper.BodyTrackUploadResult> results = new ArrayList<BodyTrackHelper.BodyTrackUploadResult>();
        String deviceName = getDeviceNickname(facetName);
        List<AbstractFacet> deviceFacets = facetsByDeviceNickname.get(facetName);

        results.add(uploadDailyData(apiKey, deviceName, deviceFacets, facetName));

        List<FieldHandler> facetFieldHandlers = getFieldHandlers(facetName);
        for (FieldHandler fieldHandler : facetFieldHandlers) {
            results.addAll(uploadIntradayData(apiKey, deviceFacets, fieldHandler));
        }
        return results;
    }

    private BodyTrackHelper.BodyTrackUploadResult uploadDailyData(ApiKey apiKey, String deviceName, List<AbstractFacet> deviceFacets, String facetName) {
        List<String> datastoreChannelNames = getDatastoreChannelNames(facetName);
        List<String> facetColumnNames = getFacetColumnNames(facetName);
        List<List<Object>> dailyDataChannelValues = getDailyDataChannelValues(deviceFacets, facetColumnNames);

        // TODO: check the status code in the BodyTrackUploadResult
        final BodyTrackHelper.BodyTrackUploadResult bodyTrackUploadResult = bodyTrackHelper.uploadToBodyTrack(apiKey, deviceName, datastoreChannelNames, dailyDataChannelValues);
        return bodyTrackUploadResult;
    }

    @Override
    @Transactional(readOnly=false)
    public void ensureDataChannelMappingsExist(ApiKey apiKey, List<String> datastoreChannelNames,
                                           final String internalDeviceName) {
        ensureChannelMappingsExist(apiKey, datastoreChannelNames, internalDeviceName, ChannelMapping.ChannelType.data, null);
    }

    @Override
    @Transactional(readOnly=false)
    public void ensurePhotoChannelMappingsExist(ApiKey apiKey, List<String> datastoreChannelNames,
                                               final String internalDeviceName, Integer objectTypeId) {
        ensureChannelMappingsExist(apiKey, datastoreChannelNames, internalDeviceName, ChannelMapping.ChannelType.photo, objectTypeId);
    }

    private void ensureChannelMappingsExist(ApiKey apiKey, List<String> datastoreChannelNames, final String internalDeviceName,
                                            final ChannelMapping.ChannelType channelType, Integer objectTypeId) {
        for (String channelName : datastoreChannelNames) {
            final TypedQuery<ChannelMapping> query = em.createQuery("SELECT mapping FROM ChannelMapping mapping WHERE mapping.apiKeyId=? AND mapping.internalDeviceName=? AND mapping.channelName=?", ChannelMapping.class);
            query.setParameter(1, apiKey.getId());
            query.setParameter(2, internalDeviceName);
            query.setParameter(3, channelName);
            final List<ChannelMapping> mappings = query.getResultList();
            if (mappings==null||mappings.size()==0) {
                ObjectType objectType = ObjectType.getObjectType(apiKey.getConnector(), objectTypeId);
                ChannelMapping.TimeType timeType = getTimeType(apiKey.getConnector(), objectType);
                ChannelMapping mapping = new ChannelMapping(apiKey.getId(), apiKey.getGuestId(),
                        channelType, timeType, objectTypeId,
                        apiKey.getConnector().getDeviceNickname(), channelName,
                        internalDeviceName!=null ? internalDeviceName : apiKey.getConnector().getDeviceNickname(), channelName);
                mapping.setCreationType(ChannelMapping.CreationType.dynamic);
                em.persist(mapping);
            }
        }
    }


    private List<BodyTrackHelper.BodyTrackUploadResult> uploadIntradayData(ApiKey apiKey, List<AbstractFacet> deviceFacets, FieldHandler fieldHandler) {
        List<BodyTrackHelper.BodyTrackUploadResult> results = new ArrayList<BodyTrackHelper.BodyTrackUploadResult>();
        for (AbstractFacet deviceFacet : deviceFacets) {
            List<BodyTrackHelper.BodyTrackUploadResult> facetResults = fieldHandler.handleField(apiKey, deviceFacet);
            if (facetResults != null)
                results.addAll(facetResults);
        }
        return results;
    }

    private FieldHandler getFieldHandler(String fieldHandlerName) {
        if (fieldHandlers.get(fieldHandlerName)==null) {
            FieldHandler fieldHandler;
            fieldHandler = (FieldHandler)beanFactory.getBean(fieldHandlerName);
            fieldHandlers.put(fieldHandlerName, fieldHandler);
        }
        return fieldHandlers.get(fieldHandlerName);
    }

    private List<String> getDatastoreChannelNames(String facetName) {
        String[] channelNamesMappings = env.bodytrackProperties.getString(facetName + ".channel_names").split(",");
        // this is to account for a very strange eisenbug where bodytrackProperties.getString() would only return the first item before the comma
        String[] stringArray = env.bodytrackProperties.getStringArray(facetName + ".channel_names");
        if (stringArray.length>channelNamesMappings.length)
            channelNamesMappings = stringArray;
        List<String> channelNames = new ArrayList<String>();
        for (String mapping : channelNamesMappings) {
            String[] terms = StringUtils.split(mapping, ":");
            if (terms[1].startsWith("#"))
                continue;
            channelNames.add(terms[0].trim());
        }
        return channelNames;
    }

    private List<String> getFacetColumnNames(String facetName) {
        String[] channelNamesMappings = env.bodytrackProperties.getString(facetName + ".channel_names").split(",");
        // this is to account for a very strange eisenbug where bodytrackProperties.getString() would only return the first item before the comma
        String[] stringArray = env.bodytrackProperties.getStringArray(facetName + ".channel_names");
        if (stringArray.length>channelNamesMappings.length)
            channelNamesMappings = stringArray;
        List<String> channelNames = new ArrayList<String>();
        for (String mapping : channelNamesMappings) {
            String[] terms = StringUtils.split(mapping, ":");
            if (terms[1].startsWith("#"))
                continue;
            channelNames.add(terms[1].trim());
        }
        return channelNames;
    }

    private List<FieldHandler> getFieldHandlers(String facetName) {
        String[] channelNamesMappings = env.bodytrackProperties.getString(facetName + ".channel_names").split(",");
        // this is to account for a very strange eisenbug where bodytrackProperties.getString() would only return the first item before the comma
        String[] stringArray = env.bodytrackProperties.getStringArray(facetName + ".channel_names");
        if (stringArray.length>channelNamesMappings.length)
            channelNamesMappings = stringArray;
        List<FieldHandler> fieldHandlers = new ArrayList<FieldHandler>();
        for (String mapping : channelNamesMappings) {
            String[] terms = StringUtils.split(mapping, ":");
            if (terms[1].startsWith("#")) {
                String handlerName = terms[1].substring(1);
                if (handlerName.equalsIgnoreCase("NOOP")||handlerName.equalsIgnoreCase("OOP"))
                    continue;
                FieldHandler fieldHandler = getFieldHandler(handlerName.substring(1));
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

    private Map<String, List<AbstractFacet>> sortFacetsByFacetName(List<? extends AbstractFacet> facets) {
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
		List<AbstractFacet> facets = apiDataService.getApiDataFacets(apiKey, null, timeInterval, null);
		storeApiData(apiKey, facets);
	}

    @Override
    public void storeInitialHistory(ApiKey apiKey, int objectTypes) {
        logger.info("module=updateQueue component=bodytrackStorageService action=storeInitialHistory" +
                " objectTypes=" + objectTypes +
                " guestId=" + apiKey.getGuestId() + " connector=" + apiKey.getConnector().getName());
        TimeInterval timeInterval = new SimpleTimeInterval(0,
                System.currentTimeMillis(), TimeUnit.ARBITRARY, TimeZone.getDefault());
        final ObjectType[] objectTypesForValue = apiKey.getConnector().getObjectTypesForValue(objectTypes);
        for (ObjectType objectType : objectTypesForValue) {
            List<AbstractFacet> facets = apiDataService.getApiDataFacets(apiKey, objectType, timeInterval, null);
            storeApiData(apiKey, facets);
        }
    }

}
