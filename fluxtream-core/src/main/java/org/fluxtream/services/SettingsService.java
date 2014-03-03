package org.fluxtream.services;

import java.io.Serializable;
import java.util.List;
import org.fluxtream.connectors.Connector;
import org.fluxtream.domain.GuestAddress;
import org.fluxtream.domain.GuestSettings;
import org.fluxtream.domain.GuestSettings.DistanceMeasureUnit;
import org.fluxtream.domain.GuestSettings.LengthMeasureUnit;
import org.fluxtream.domain.GuestSettings.TemperatureUnit;
import org.fluxtream.domain.GuestSettings.WeightMeasureUnit;
import org.springframework.transaction.annotation.Transactional;

public interface SettingsService {

	public GuestSettings getSettings(long guestId);

    public int incrementDisplayCounter(long guestId, String messageName);

	public void setFirstname(long guestId, String firstname);

	public void setLastname(long guestId, String lastname);

	public void setWeightMeasureUnit(long guestId, WeightMeasureUnit unit);

	public void setLengthMeasureUnit(long guestId, LengthMeasureUnit unit);

	public void setDistanceMeasureUnit(long guestId, DistanceMeasureUnit unit);

	public GuestAddress addAddress(long guestId, String type, String address, double latitude,
			double longitude, long since, long until, double radius, String jsonString);

    public GuestAddress addAddress(long guestId, String type, String address, double latitude,
                           double longitude, long since, double radius, String jsonString);

    public String[] getChannelsForConnector(long guestId, Connector connector);

    public void setChannelsForConnector(long guestId, Connector connector, String[] channels);

	public List<GuestAddress> getAllAddressesForDate(long guestId, long date);

    public List<GuestAddress> getAllAddresses(long guestId);

    public List<GuestAddress> getAllAddressesOfType(long guestId, String type);

    public List<GuestAddress> getAllAddressesOfTypeForDate(long guestId, String type, long date);

    public GuestAddress getAddressById(long guestId, long id);

    public void deleteAddressById(long guestId, long id);

    public void deleteAllAddresses(long guestId);

    public void deleteAllAddressesAtDate(long guestId, long date);

    public void deleteAllAddressesOfType(long guestId, String type);

    public void deleteAllAddressesOfTypeForDate(long guestId, String type, long date);

    public GuestAddress updateAddress(long guestId, long addressId, String type, String address, Double latitude,
                              Double longitude, Long since, Long until, Double radius, String jsonString);

	public void setTemperatureUnit(long guestId, TemperatureUnit temperatureUnit);

    public void setConnectorFilterState(long guestId, String stateJSON);

    public String getConnectorFilterState(long guestId);

    public Object getConnectorSettings(long apiKeyId);

    @Transactional(readOnly=false)
    void persistConnectorSettings(long apiKeyId, Object settings, Object defaultSettings);

    public void saveConnectorSettings(long apiKeyId, String json);

    public void saveConnectorSettings(long apiKeyId, Serializable settings);

    public void resetConnectorSettings(long apiKeyId);

}
