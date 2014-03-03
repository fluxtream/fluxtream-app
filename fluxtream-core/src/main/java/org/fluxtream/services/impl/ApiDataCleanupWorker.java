package org.fluxtream.services.impl;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.fluxtream.domain.ApiKey;
import org.fluxtream.services.ApiDataService;
import org.fluxtream.services.ConnectorUpdateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * User: candide
 * Date: 12/04/13
 * Time: 17:57
 */
@Component
@Scope("prototype")
public class ApiDataCleanupWorker implements Runnable {

    @Autowired
    ApiDataService apiDataService;

    @Autowired
    ConnectorUpdateService connectorUpdateService;

    private ApiKey apiKey;

    @PersistenceContext
    EntityManager em;

    public void run() {
        connectorUpdateService.flushUpdateWorkerTasks(apiKey, true);
        apiDataService.eraseApiData(apiKey);
    }

    public void setApiKey(final ApiKey apiKey) {
        this.apiKey = apiKey;
    }

}
