package com.fluxtream.services.impl;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import java.util.ArrayList;
import java.util.List;

import com.fluxtream.connectors.Connector;
import com.fluxtream.domain.ConnectorFilterState;
import com.sun.jersey.core.util.StringIgnoreCaseKeyComparator;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fluxtream.Configuration;
import com.fluxtream.domain.Guest;
import com.fluxtream.domain.GuestAddress;
import com.fluxtream.domain.GuestSettings;
import com.fluxtream.domain.GuestSettings.DistanceMeasureUnit;
import com.fluxtream.domain.GuestSettings.LengthMeasureUnit;
import com.fluxtream.domain.GuestSettings.TemperatureUnit;
import com.fluxtream.domain.GuestSettings.WeightMeasureUnit;
import com.fluxtream.services.GuestService;
import com.fluxtream.services.SettingsService;
import com.fluxtream.utils.JPAUtils;

@Transactional(readOnly = true)
@Service
public class SettingsServiceImpl implements SettingsService {

	@Autowired
	Configuration env;

	@Autowired
	GuestService guestService;

	@PersistenceContext
	EntityManager em;

	@Override
	@Transactional(readOnly = false)
	public GuestSettings getSettings(long guestId) {
		GuestSettings settings = JPAUtils.findUnique(em, GuestSettings.class,
				"settings.byGuestId", guestId);
        if (settings != null) {
            return settings;
        }
        else {
            settings = new GuestSettings();
            settings.guestId = guestId;
            em.persist(settings);
            return settings;
        }
	}

	@Override
	@Transactional(readOnly = false)
	public void setFirstname(long guestId, String firstname) {
		Guest guest = guestService.getGuestById(guestId);
		guest.firstname = firstname;
		em.merge(guest);
	}

	@Override
	@Transactional(readOnly = false)
	public void setLastname(long guestId, String lastname) {
		Guest guest = guestService.getGuestById(guestId);
		guest.lastname = lastname;
		em.merge(guest);
	}

	@Override
	@Transactional(readOnly = false)
	public void setWeightMeasureUnit(long guestId, WeightMeasureUnit unit) {
		GuestSettings settings = JPAUtils.findUnique(em, GuestSettings.class,
				"settings.byGuestId", guestId);
		settings.weightMeasureUnit = unit;
		em.merge(settings);
	}

	@Override
    @Transactional(readOnly = false)
	public void setTemperatureUnit(long guestId, TemperatureUnit unit) {
		GuestSettings settings = JPAUtils.findUnique(em, GuestSettings.class,
				"settings.byGuestId", guestId);
		settings.temperatureUnit = unit;
		em.merge(settings);
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
        ConnectorFilterState filterState = JPAUtils.findUnique(em, ConnectorFilterState.class,
                                                               "connectorFilterState",
                                                               guestId);
        return filterState == null ? "{}" : filterState.stateJSON;
    }

    @Override
	@Transactional(readOnly = false)
	public void setLengthMeasureUnit(long guestId, LengthMeasureUnit unit) {
		GuestSettings settings = JPAUtils.findUnique(em, GuestSettings.class,
				"settings.byGuestId", guestId);
		settings.lengthMeasureUnit = unit;
		em.merge(settings);
	}

	@Override
	@Transactional(readOnly = false)
	public void setDistanceMeasureUnit(long guestId, DistanceMeasureUnit unit) {
		GuestSettings settings = JPAUtils.findUnique(em, GuestSettings.class,
				"settings.byGuestId", guestId);
		settings.distanceMeasureUnit = unit;
		em.merge(settings);
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
        String savedChannels = guestService.getApiKeyAttribute(guestId, connector, "channels");
        String[] channels;
        if (savedChannels == null){
            channels = connector.getDefaultChannels();
        }
        else{
            channels = savedChannels.split(",");
        }
        return channels;
    }

    @Override
    @Transactional(readOnly = false)
    public void setChannelsForConnector(final long guestId, final Connector connector, String[] channels) {
        guestService.setApiKeyAttribute(guestId,connector,"channels",StringUtils.join(channels,","));
    }

    @Override
    public void setChannelsForConenctor(final long guestId, final Connector connector, final List<String> channels) {
        setChannelsForConnector(guestId,connector,(String[]) channels.toArray());
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
