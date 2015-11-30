package org.fluxtream.core.services;

import org.fluxtream.core.domain.AbstractFacet;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.domain.ChannelMapping;

import java.util.List;

public interface BodyTrackStorageService {

    void ensureDataChannelMappingsExist(ApiKey apiKey, List<String> datastoreChannelNames, String internalDeviceName);

    void ensurePhotoChannelMappingsExist(ApiKey apiKey, List<String> datastoreChannelNames, String internalDeviceName, Integer objectTypeId);

    void storeInitialHistory(ApiKey apiKey);

    void storeInitialHistory(ApiKey apiKey, int objectTypes);

	void storeApiData(ApiKey apiKey, List<? extends AbstractFacet> facet);

    boolean mapChannels(final ApiKey apiKey);

    List<ChannelMapping> getChannelMappings(final long apiKeyId);

}
