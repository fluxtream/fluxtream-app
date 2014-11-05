package org.fluxtream.core.services.impl;

import com.google.gson.Gson;
import org.apache.commons.lang.StringUtils;
import org.fluxtream.core.Configuration;
import org.fluxtream.core.aspects.FlxLogger;
import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.connectors.annotations.Updater;
import org.fluxtream.core.connectors.updaters.AbstractUpdater;
import org.fluxtream.core.connectors.updaters.SettingsAwareUpdater;
import org.fluxtream.core.connectors.updaters.UpdateFailedException;
import org.fluxtream.core.domain.*;
import org.fluxtream.core.domain.GuestSettings.DistanceMeasureUnit;
import org.fluxtream.core.domain.GuestSettings.LengthMeasureUnit;
import org.fluxtream.core.domain.GuestSettings.TemperatureUnit;
import org.fluxtream.core.domain.GuestSettings.WeightMeasureUnit;
import org.fluxtream.core.services.GuestService;
import org.fluxtream.core.services.SettingsService;
import org.fluxtream.core.utils.JPAUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.Serializable;
import java.util.List;

@Service
@Transactional(readOnly=true)
public class SettingsServiceImpl implements SettingsService {

    static FlxLogger logger = FlxLogger.getLogger(SettingsServiceImpl.class);

	@Autowired
	Configuration env;

	@Autowired
	GuestService guestService;

	@PersistenceContext
	EntityManager em;

    @Autowired
    BeanFactory beanFactory;

    Gson gson = new Gson();

	@Override
	@Transactional(readOnly = false)
	public GuestSettings getSettings(long guestId) {
		GuestSettings settings = JPAUtils.findUnique(em, GuestSettings.class,
				"settings.byGuestId", guestId);
        if (settings != null) {
            settings.config = env;
            return settings;
        }
        else {
            settings = new GuestSettings();
            settings.guestId = guestId;
            settings.createMessageDisplayCounters();
            em.persist(settings);
            settings.config = env;
            return settings;
        }
	}

    @Override
    @Transactional(readOnly=false)
    public int incrementDisplayCounter(final long guestId, final String messageName) {
        GuestSettings settings = JPAUtils.findUnique(em, GuestSettings.class,
                                                     "settings.byGuestId", guestId);
        final Integer count = settings.getMessageDisplayCounter(messageName);
        int incremented = count==null? 1 : count+1;
        if (count==null) settings.setMessageDisplayCounter(messageName, incremented);
        else settings.setMessageDisplayCounter(messageName, incremented);
        em.persist(settings);
        return incremented;
    }

    @Override
	@Transactional(readOnly = false)
	public void setFirstname(long guestId, String firstname) {
		Guest guest = guestService.getGuestById(guestId);
		guest.firstname = firstname;
	}

	@Override
	@Transactional(readOnly = false)
	public void setLastname(long guestId, String lastname) {
		Guest guest = guestService.getGuestById(guestId);
		guest.lastname = lastname;
	}

	@Override
	@Transactional(readOnly = false)
	public void setWeightMeasureUnit(long guestId, WeightMeasureUnit unit) {
		GuestSettings settings = JPAUtils.findUnique(em, GuestSettings.class,
				"settings.byGuestId", guestId);
		settings.weightMeasureUnit = unit;
	}

	@Override
    @Transactional(readOnly = false)
	public void setTemperatureUnit(long guestId, TemperatureUnit unit) {
		GuestSettings settings = JPAUtils.findUnique(em, GuestSettings.class,
				"settings.byGuestId", guestId);
		settings.temperatureUnit = unit;
	}

    @Override
    @Transactional(readOnly = false)
    public void setConnectorFilterState(final long guestId, final String stateJSON) {
         ConnectorFilterState filterState = JPAUtils.findUnique(em, ConnectorFilterState.class,
                                                                           "connectorFilterState",
                                                                           guestId);
        if (filterState==null) {
            filterState = new ConnectorFilterState();
            filterState.guestId = guestId;
            filterState.stateJSON = stateJSON;
            em.persist(filterState);
        } else {
            filterState.stateJSON = stateJSON;
            em.merge(filterState);
        }
    }

    @Override
    public String getConnectorFilterState(final long guestId) {
        ConnectorFilterState filterState = JPAUtils.findUnique(em, ConnectorFilterState.class, "connectorFilterState", guestId);
        return filterState == null ? "{}" : filterState.stateJSON;
    }

    /**
     * Returns the serialized settings for a connector instance or new settings of the right
     * type if none have been already persisted and a non-default Settings class is specified in
     * the connector's Updater (annotation)
     * @param apiKeyId
     * @return
     * @throws UpdateFailedException
     */
    @Override
    @Transactional(readOnly = false)
    public Object getConnectorSettings(final long apiKeyId) {
        ApiKey apiKey = guestService.getApiKey(apiKeyId);
        Object settings = apiKey.getSettings();
        final Class<? extends AbstractUpdater> updaterClass = apiKey.getConnector().getUpdaterClass();
        final Class<?> settingsClass = updaterClass.getAnnotation(Updater.class).settings();
        if (settings==null&& settingsClass != Updater.EmptySettings.class){
            try {
                settings = settingsClass.newInstance();
                apiKey.setSettings(settings);
                em.persist(apiKey);
                return settings;
            }
            catch (Exception e) {
                throw new RuntimeException("Could not instantiate default settings for connector " + apiKey.getConnector().getName());
            }
        }
        return settings;
    }

    @Override
    @Transactional(readOnly=false)
    public void persistConnectorSettings(final long apiKeyId, final Object settings, final Object defaultSettings) {
        ApiKey apiKey = guestService.getApiKey(apiKeyId);
        apiKey.setSettings(settings);
        apiKey.setDefaultSettings(defaultSettings);
        em.persist(apiKey);
    }

    @Override
    @Transactional(readOnly=false)
    public void saveConnectorSettings(final long apiKeyId, final String json) {
        ApiKey apiKey = guestService.getApiKey(apiKeyId);
        final Updater updaterAnnotation = apiKey.getConnector().getUpdaterClass().getAnnotation(Updater.class);
        StringBuilder sb = new StringBuilder("module=connectors component=settingsServiceImpl action=saveConnectorSettings")
                .append(" apiKeyId=").append(apiKeyId);
        if (updaterAnnotation.settings()==Updater.EmptySettings.class){
            logger.warn(sb.append(" message=\"no settings class has been specified for \"" + apiKey.getConnector().getName()));
            return;
        }
        try {
            final Object settings = gson.fromJson(json, updaterAnnotation.settings());
            guestService.setApiKeySettings(apiKeyId, settings);
            final AbstractUpdater updater = beanFactory.getBean(apiKey.getConnector().getUpdaterClass());
            ((SettingsAwareUpdater)updater).connectorSettingsChanged(apiKeyId, settings);
        }
        catch (Exception e) {
            logger.warn(sb.append(" message=\"unexpected exception when saving connector settings for connector \"" + apiKey.getConnector().getName()));
            return;
        }
    }

    /**
     * Reset connector settings to the defaults if they have been specified, or a new instance of
     * the settings class for this connector, if one exists
     * @param apiKeyId
     */
    @Override
    @Transactional(readOnly=false)
    public void saveConnectorSettings(final long apiKeyId, final Serializable settings){
        saveConnectorSettings(apiKeyId, gson.toJson(settings));
    }

    @Override
    @Transactional(readOnly=false)
    public void resetConnectorSettings(final long apiKeyId) {
        ApiKey apiKey = guestService.getApiKey(apiKeyId);
        final Class<? extends AbstractUpdater> updaterClass = apiKey.getConnector().getUpdaterClass();
        Object defaultSettings = apiKey.getDefaultSettings();
        if (defaultSettings!=null)
            apiKey.setSettings(defaultSettings);
        else {
            final Class<?> settingsClass = updaterClass.getAnnotation(Updater.class).settings();
            if (settingsClass != Updater.EmptySettings.class){
                try {
                    defaultSettings = settingsClass.newInstance();
                    apiKey.setSettings(defaultSettings);
                }
                catch (Exception e) {
                    throw new RuntimeException("Could not instantiate default settings for connector " + apiKey.getConnector().getName());
                }
            }
        }
        if (SettingsAwareUpdater.class.isAssignableFrom(updaterClass)) {
            final AbstractUpdater updater = beanFactory.getBean(apiKey.getConnector().getUpdaterClass());
            ((SettingsAwareUpdater)updater).connectorSettingsChanged(apiKeyId, defaultSettings);
        }
        em.persist(apiKey);
    }

    @Override
	@Transactional(readOnly = false)
	public void setLengthMeasureUnit(long guestId, LengthMeasureUnit unit) {
		GuestSettings settings = JPAUtils.findUnique(em, GuestSettings.class,
				"settings.byGuestId", guestId);
		settings.lengthMeasureUnit = unit;
	}

	@Override
	@Transactional(readOnly = false)
	public void setDistanceMeasureUnit(long guestId, DistanceMeasureUnit unit) {
		GuestSettings settings = JPAUtils.findUnique(em, GuestSettings.class,
				"settings.byGuestId", guestId);
		settings.distanceMeasureUnit = unit;
	}

	@Override
	@Transactional(readOnly=false)
	public GuestAddress addAddress(long guestId, String type, String add, double latitude,
			double longitude, long since, long until, double radius, String jsonString) {
		GuestAddress address = new GuestAddress();
		address.guestId = guestId;
		address.address = add;
		address.latitude = latitude;
		address.longitude = longitude;
		address.since = since;
        address.until = until;
        address.type = type;
        address.radius = radius;
		address.jsonStorage = jsonString;
        if (!isAddressValid(address)) {
            throw new RuntimeException("invalid address");
        }
		em.persist(address);
        return address;
	}

    @Override
    @Transactional(readOnly=false)
    public GuestAddress addAddress(long guestId, String type, String add, double latitude,
                           double longitude, long since, double radius, String jsonString) {
        GuestAddress address = new GuestAddress();
        address.guestId = guestId;
        address.address = add;
        address.latitude = latitude;
        address.longitude = longitude;
        address.since = since;
        address.type = type;
        address.radius = radius;
        address.jsonStorage = jsonString;
        em.persist(address);
        return address;
    }

    @Override
    public String[] getChannelsForConnector(final long guestId, final Connector connector) {
        ConnectorChannelSet channelSet =  JPAUtils.findUnique(em, ConnectorChannelSet.class, "connectorChannelSet.byApi", guestId, connector.value());
        String[] channels;
        if (channelSet == null){
            channels = connector.getDefaultChannels();
        }
        else{
            channels = channelSet.channels.split(",");
        }
        return channels;
    }

    @Override
    @Transactional(readOnly = false)
    public void setChannelsForConnector(final long guestId, final Connector connector, String[] channels) {
        ConnectorChannelSet channelSet =  JPAUtils.findUnique(em, ConnectorChannelSet.class, "connectorChannelSet.byApi", guestId, connector.value());
        if (channelSet==null) {
            channelSet = new ConnectorChannelSet();
            channelSet.guestId = guestId;
            channelSet.api = connector.value();
            channelSet.channels = StringUtils.join(channels,",");
            em.persist(channelSet);
        } else {
            channelSet.channels = StringUtils.join(channels,",");
            em.merge(channelSet);
        }
    }

    @Override
	public List<GuestAddress> getAllAddressesForDate(long guestId, long date) {
		return JPAUtils.find(em, GuestAddress.class, "address.when", guestId, date, date);
	}

    @Override
    public List<GuestAddress> getAllAddresses(long guestId){
        return JPAUtils.find(em,GuestAddress.class,"addresses.byGuestId",guestId);

    }

    @Override
    public List<GuestAddress> getAllAddressesOfType(long guestId, String type){
        return JPAUtils.find(em,GuestAddress.class,"addresses.byType", guestId, type);
    }

    @Override
    public List<GuestAddress> getAllAddressesOfTypeForDate(long guestId, String type, long date){
        return JPAUtils.find(em,GuestAddress.class,"addresses.byType.when", guestId, type, date, date);
    }

    //delete functions are currently unimplemented

    @Override
    @Transactional(readOnly=false)
    public void deleteAddressById(long guestId, long id){
        GuestAddress address = em.find(GuestAddress.class,id);
        if (address.guestId != guestId) {
            throw new RuntimeException("Cannot delete address you don't have ownership of.");
        }
        deleteAddress(address);
    }

    @Override
    @Transactional(readOnly=false)
    public void deleteAllAddresses(long guestId){
        deleteAddresses(getAllAddresses(guestId));
    }

    @Override
    @Transactional(readOnly=false)
    public void deleteAllAddressesAtDate(long guestId, long date){
        deleteAddresses(getAllAddressesForDate(guestId, date));
    }

    @Override
    @Transactional(readOnly=false)
    public void deleteAllAddressesOfType(long guestId, String type){
        deleteAddresses(getAllAddressesOfType(guestId, type));
    }

    @Override
    @Transactional(readOnly=false)
    public void deleteAllAddressesOfTypeForDate(long guestId, String type, long date){
        deleteAddresses(getAllAddressesOfTypeForDate(guestId,type,date));
    }

    @Transactional(readOnly=false)
    private void deleteAddresses(List<GuestAddress> addresses){
        for (GuestAddress address : addresses) {
            em.remove(address);
        }
    }

    @Transactional(readOnly=false)
    private void deleteAddress(GuestAddress address){
        em.remove(address);
    }

    @Override
    public GuestAddress getAddressById(long guestId, long id){
        GuestAddress address = em.find(GuestAddress.class,id);
        if (address.guestId == guestId) {
            return address;
        }
        return null;
    }

    @Override
    @Transactional(readOnly=false)
    public GuestAddress updateAddress(long guestId, long addressId, String type, String address, Double latitude,
                              Double longitude, Long since, Long until, Double radius, String jsonString){
        GuestAddress add = getAddressById(guestId,addressId);
        if (address != null) {
            add.address = address;
        }
        if (type != null) {
            add.type = type;
        }
        if (latitude != null) {
            add.latitude = latitude;
        }
        if (longitude != null) {
            add.longitude = longitude;
        }
        if (since != null) {
            add.since = since;
        }
        if (until != null) {
            add.until = until;
        }
        if (jsonString != null) {
            add.jsonStorage = jsonString;
        }
        if (radius != null) {
            add.radius = radius;
        }
        if (!isAddressValid(add)){
            em.refresh(add);
            throw new RuntimeException("invalid address");
        }
        return add;
    }

    private boolean isAddressValid(GuestAddress address){
        if (address.until < address.since) {
            return false;
        }
        return true;
    }

	/*@Override //saving for reference
	public void removeAddress(long guestId, long addressId) {
		GuestAddress add = em.find(GuestAddress.class, addressId);
		if (add.guestId==guestId)
			em.remove(add);
	}*/

}
