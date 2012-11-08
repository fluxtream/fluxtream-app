package com.fluxtream.connectors;

import java.util.List;
import com.fluxtream.domain.ApiKey;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
public interface SyncNeededAware {

    public List<Integer> getSyncNeededObjectTypeValues(ApiKey apiKey) throws Exception;

}