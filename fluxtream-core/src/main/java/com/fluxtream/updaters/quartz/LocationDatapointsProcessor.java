package com.fluxtream.updaters.quartz;

import java.util.List;
import com.fluxtream.connectors.location.LocationFacet;
import com.fluxtream.services.ApiDataService;
import com.fluxtream.services.JPADaoService;
import com.fluxtream.utils.JPAUtils;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

/**
 * User: candide
 * Date: 10/04/13
 * Time: 17:28
 */
@Component
public class LocationDatapointsProcessor implements InitializingBean {

    Logger logger = Logger.getLogger(LocationDatapointsProcessor.class);

    @Autowired
    @Qualifier("locationDatapointHandlersExecutor")
    ThreadPoolTaskExecutor executor;

    @Autowired
    BeanFactory beanFactory;

    @Autowired
    ApiDataService apiDataService;

    @Autowired
    JPADaoService jpaDaoService;

    public void processLocationDatapoints() {
        final List<LocationFacet> locationFacets = getUnprocessedLocationDatapoints();
        for (LocationFacet facet : locationFacets) {
            processLocationDatapoint(facet);
        }
    }

    private void processLocationDatapoint(final LocationFacet locationFacet) {
        try {
            executor.execute(new Runnable() {
                @Override
                @Trace(dispatcher=true,skipTransactionTrace=true)
                public void run() {
                    NewRelic.setTransactionName(null, "locationDatapointHandler.processLocation");
                    apiDataService.processLocation(locationFacet);
                }
            });
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        executor.setThreadGroupName("LocationDatapointHandlers");
        executor.setThreadNamePrefix("LocationDatapointHandler-");
    }

    public List<LocationFacet> getUnprocessedLocationDatapoints() {
        final String entityName = JPAUtils.getEntityName(LocationFacet.class);
        final List<LocationFacet> locationFacets = jpaDaoService.executeQuery("SELECT facet from " + entityName + " facet WHERE facet.processed = false", LocationFacet.class);
        logger.info("module=locationDatapointsProcessor component=processor unprocessedDatapoints=" + locationFacets.size());
        return locationFacets;
    }

}
