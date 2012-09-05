package com.fluxtream.services;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.fluxtream.connectors.Connector;
import com.fluxtream.domain.AbstractUserProfile;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.domain.Guest;
import com.fluxtream.domain.ResetPasswordToken;

public interface GuestService {
	public void addRole(long guestId, String role);

	public void removeRole(long guestId, String role);

	public List<Guest> getAllGuests();
	
	public Guest createGuest(String username, String firstname,
			String lastname, String password, String email) throws Exception;

	public void eraseGuestInfo(String username) throws Exception;

	public boolean isUsernameAvailable(String username);

	public void checkIn (long guestId, String ipAddress) throws IOException;

	public Guest getGuestByEmail(String email);

	public Guest getGuestById(long id);

	public Guest getGuest(String username);
	
	public void setPassword(long guestId, String password);

	public ApiKey setApiKeyAttribute(long guestId, Connector api, String key,
			String value);

	public String getApiKeyAttribute(long guestId, Connector api, String key);

    public Map<String,String> getApiKeyAttributes(long guestId, Connector api, String key);

    public List<ApiKey> getApiKeys(long guestId);

	public boolean hasApiKey(long guestId, Connector api);

	public ApiKey getApiKey(long guestId, Connector api);

	public void removeApiKey(long guestId, Connector api);

	public void saveUserProfile(long guestId, AbstractUserProfile userProfile);

	public ResetPasswordToken getToken(String token);

	public ResetPasswordToken createToken(long guestId);
	
	public void deleteToken(String token);

	public <T extends AbstractUserProfile> T getUserProfile(long guestId,
			Class<T> clazz);

}
