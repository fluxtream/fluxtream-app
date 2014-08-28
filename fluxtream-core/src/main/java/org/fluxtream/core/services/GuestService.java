package org.fluxtream.core.services;

import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.domain.AbstractUserProfile;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.domain.Guest;
import org.fluxtream.core.domain.ResetPasswordToken;
import org.fluxtream.core.services.impl.ExistingEmailException;
import org.fluxtream.core.services.impl.UsernameAlreadyTakenException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface GuestService {
	public void addRole(long guestId, String role);

	public void removeRole(long guestId, String role);

	public List<Guest> getAllGuests();
	
	public Guest createGuest(String username, String firstname, String lastname, String password, String email, Guest.RegistrationMethod registrationMethod, final String appId) throws UsernameAlreadyTakenException, ExistingEmailException;

	public void eraseGuestInfo(String username) throws Exception;

    public void eraseGuestInfo(long id) throws Exception;

	public boolean isUsernameAvailable(String username);

	public void checkIn (long guestId, String ipAddress) throws IOException;

	public Guest getGuestByEmail(String email);

	public Guest getGuestById(long id);

	public Guest getGuest(String username);
	
	public void setPassword(long guestId, String password);

    public ApiKey createApiKey(long guestId, Connector connector);

	public ApiKey setApiKeyAttribute(ApiKey apiKey, String key,
			String value);

    public Map<String, String> getApiKeyAttributes(long apiKeyId);

	public String getApiKeyAttribute(ApiKey apiKey, String key);

    public ApiKey getApiKey(long apiKeyId);

    public List<ApiKey> getApiKeys(long guestId);

	public boolean hasApiKey(long guestId, Connector connector);

	public List<ApiKey> getApiKeys(long guestId, Connector connector);

    public void setApiKeyStatus(long apiKeyId, ApiKey.Status status, String stackTrace, String reason);

    public void setApiKeyToSynching(long apiKeyId, boolean synching);

    /**
     * Multiple apiKeys per connector per user are now allowed. This call is maintained for
     * backward compatibility, and we will hopefully soon be able to completely get rid of it
     * @param guestId guest id
     * @param connector connector
     * @return the first api key matching passed arguments
     */
    @Deprecated
    public ApiKey getApiKey(long guestId, Connector connector);

    public void removeApiKeys(long guestId, Connector connector);

	public void removeApiKey(long apiKeyId);

	public void saveUserProfile(long guestId, AbstractUserProfile userProfile);

    public void setApiKeySettings(long apiKeyId, Object settings);

	public ResetPasswordToken getToken(String token);

	public ResetPasswordToken createToken(long guestId);
	
	public void deleteToken(String token);

	public <T extends AbstractUserProfile> T getUserProfile(long guestId,
			Class<T> clazz);

    public void removeApiKeyAttribute(long apiKeyId, String key);

    public void setAutoLoginToken(long guestId, String s);

    public boolean checkPassword(long guestId, String currentPassword);

    void populateApiKey(long apiKeyId);

    void addDeveloperRole(Long guestId);
}
