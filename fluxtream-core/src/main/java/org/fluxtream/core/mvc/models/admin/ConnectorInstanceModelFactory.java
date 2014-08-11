package org.fluxtream.core.mvc.models.admin;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.fluxtream.core.Configuration;
import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.domain.UpdateWorkerTask;
import org.fluxtream.core.services.ConnectorUpdateService;
import org.fluxtream.core.services.GuestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * User: candide
 * Date: 16/09/13
 * Time: 13:51
 */
@Component
public class ConnectorInstanceModelFactory {

    @Autowired
    Configuration env;

    @Autowired
    ConnectorUpdateService connectorUpdateService;

    @Autowired
    GuestService guestService;

    public Map<String,Object> createConnectorInstanceModel(ApiKey apiKey) {
        Map<String,Object> model = new HashMap<String,Object>();
        final Connector connector = apiKey.getConnector();
        final Map<String, String> attributes = guestService.getApiKeyAttributes(apiKey.getId());
        model.put("connectorName", connector.getName());
        model.put("attributes", attributes);
        String rateLimitString = (String) env.connectors.getProperty(connector.getName()
                                                                     + ".rateLimit");
        if (rateLimitString == null)
            rateLimitString = (String) env.connectors.getProperty("rateLimit");
        model.put("rateLimitSpecs", rateLimitString);
        final String auditTrail = checkForErrors(apiKey);
        ApiKey.Status status = apiKey.getStatus();
        // Treat status=null as STATUS_UP
        if(status==null) {
            status=ApiKey.Status.STATUS_UP;
        }
            model.put("status", status.toString());

        model.put("errors", status != ApiKey.Status.STATUS_UP);
        model.put("auditTrail", auditTrail!=null?auditTrail:"");
        int quota = Integer.valueOf(rateLimitString.split("/")[0]);
        long numberOfUpdates = getNumberOfUpdatesOverSpecifiedTimePeriod(apiKey.getGuestId(), connector, rateLimitString,
                                                                         model);
        model.put("isOverQuota", numberOfUpdates >= quota);
        return model;
    }

    private String checkForErrors(ApiKey apiKey) {
        Collection<UpdateWorkerTask> update = connectorUpdateService.getLastFinishedUpdateTasks(apiKey);
        if (update.size() < 1) {
            return null;
        }
        for (UpdateWorkerTask workerTask : update) {
            if (workerTask == null || workerTask.status != UpdateWorkerTask.Status.DONE) {
                if (workerTask.auditTrail != null) {
                    return workerTask.auditTrail;
                }
                else {
                    return "no audit trail";
                }
            }
        }
        return null;
    }

    private long getNumberOfUpdatesOverSpecifiedTimePeriod(final long guestId, final Connector connector, final String rateLimitString, final Map<String, Object> model) {
        int millis = Integer.valueOf(rateLimitString.split("/")[1]);
        long then = System.currentTimeMillis() - millis;
        long numberOfUpdates;
        if (rateLimitString.endsWith("/user")) {
            numberOfUpdates = connectorUpdateService
                    .getNumberOfUpdatesSince(guestId, connector.value(), then);
            model.put("numberOfUserCalls", numberOfUpdates);
        } else {
            numberOfUpdates = connectorUpdateService
                    .getTotalNumberOfUpdatesSince(connector, then);
            model.put("numberOfCalls", numberOfUpdates);
        }
        return numberOfUpdates;
    }


}
