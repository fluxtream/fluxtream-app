package com.fluxtream.services;

import java.util.List;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.updaters.UpdateFailedException;
import com.fluxtream.domain.GuestAddress;
import com.fluxtream.domain.GuestSettings;
import com.fluxtream.domain.GuestSettings.DistanceMeasureUnit;
import com.fluxtream.domain.GuestSettings.LengthMeasureUnit;
import com.fluxtream.domain.GuestSettings.TemperatureUnit;
import com.fluxtream.domain.GuestSettings.WeightMeasureUnit;

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

    public Object getConnectorSettings(long apiKeyId, boolean refresh) throws UpdateFailedException;

    public void saveConnectorSettings(long apiKeyId, String json);

    public void resetConnectorSettings(long apiKeyId);

}
