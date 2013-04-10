package com.fluxtream.updaters.quartz;

import com.fluxtream.connectors.google_latitude.LocationFacet;
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

    @Autowired
    @Qualifier("locationDatapointHandlersExecutor")
    ThreadPoolTaskExecutor executor;

    @Autowired
    BeanFactory beanFactory;

    public void processLocationDatapoints() {
        LocationFacet locationFacet;
        while((locationFacet=hasMoreDatapointsToProcess())!=null) {
            processLocationDatapoint(locationFacet);
        }
    }

    private void processLocationDatapoint(final LocationFacet locationFacet) {
        LocationDatapointHandler handler = beanFactory.getBean(LocationDatapointHandler.class);
        handler.locationFacet = locationFacet;
        try {
            executor.execute(handler);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private LocationFacet hasMoreDatapointsToProcess() {
        // select a non-processed locationdatapoint at random
        return null;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        executor.setThreadGroupName("LocationDatapointHandlers");
        executor.setThreadNamePrefix("LocationDatapointHandler-");
    }

}
