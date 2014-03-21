package org.fluxtream.services.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.fluxtream.Configuration;
import org.fluxtream.domain.ApiKey;
import org.fluxtream.domain.Dashboard;
import org.fluxtream.domain.DashboardWidget;
import org.fluxtream.services.DashboardsService;
import org.fluxtream.services.GuestService;
import org.fluxtream.services.WidgetsService;
import org.fluxtream.utils.JPAUtils;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Service
@Transactional(readOnly=true)
public class DashboardsServiceImpl implements DashboardsService {

    @PersistenceContext
    EntityManager em;

    @Autowired
    GuestService guestService;

    @Autowired
    Configuration env;

    @Autowired(required=false)
    @Qualifier("widgets")
    PropertiesConfiguration widgetProperties;

    @Autowired
    WidgetsService widgetsService;

    @Override
    @Transactional(readOnly=false)
    public List<Dashboard> getDashboards(final long guestId) {
        List<Dashboard> dashboards = JPAUtils.find(em, Dashboard.class, "dashboards.all",
                                                   guestId);
        if (dashboards.size()==0) {
            Dashboard dashboard = createDashboard(guestId, "Untitled Dashboard");
            dashboard.active = true;
            addDefaultWidgets(guestId, dashboard);
            em.persist(dashboard);
        }
        return dashboards;
    }

    @Override
    public Dashboard getDashboard(final long guestId, final long dashboardId) {
        return getDashboardById(guestId, dashboardId);
    }

    @Transactional(readOnly=false)
    private void addDefaultWidgets(final long guestId, Dashboard dashboard) {
        List<ApiKey> keys = guestService.getApiKeys(guestId);
        Iterator<ApiKey> eachKey = keys.iterator();
        List<String> widgetNames = new ArrayList<String>();
        while (eachKey.hasNext()) {
            ApiKey key = eachKey.next();
            if(key!=null && key.getConnector()!=null && key.getConnector().getName()!=null) {
                String defaultWidgetKey = key.getConnector().getName() + ".defaultWidget";
                String widgetName = widgetProperties.getString(defaultWidgetKey);
                if (widgetName!=null) {
                    widgetNames.add(widgetName);
                }
            }
        }
        dashboard.widgetNames = StringUtils.join(widgetNames, ",");
        em.persist(dashboard);
    }

    @Transactional(readOnly=false)
    private Dashboard createDashboard(final long guestId, final String dashboardName) {
        Dashboard dashboard = new Dashboard();
        dashboard.name = dashboardName;
        dashboard.guestId = guestId;
        dashboard.widgetNames = "";
        long maxOrder = JPAUtils.count(em, "dashboards.maxOrder", guestId);
        dashboard.ordering = (int)maxOrder+1;
        em.persist(dashboard);
        return dashboard;
    }

    @Override
    public void addDashboard(final long guestId, final String dashboardName) {
        createDashboard(guestId, dashboardName);
    }

    @Override
    @Transactional(readOnly=false)
    public void removeDashboard(final long guestId, final long dashboardId) {
        Dashboard dashboard = getDashboardById(guestId, dashboardId);
        em.remove(dashboard);
        widgetsService.deleteWidgetSettings(guestId, dashboardId);
    }

    @Override
    @Transactional(readOnly=false)
    public void renameDashboard(final long guestId, final long previousDashboardId, final String dashboardName) {
        Dashboard dashboard = getDashboardById(guestId, previousDashboardId);
        dashboard.name = dashboardName;
        em.persist(dashboard);
    }

    private Dashboard getDashboardById(long guestId, long dashboardId) {
        Dashboard dashboard = JPAUtils.findUnique(em, Dashboard.class, "dashboards.byId", guestId, dashboardId);
        return dashboard;
    }

    @Override
    @Transactional(readOnly=false)
    public void addWidget(final long guestId, final long dashboardId, final String widgetName) {
        Dashboard dashboard = getDashboardById(guestId, dashboardId);
        final String[] namesArray = StringUtils.split(dashboard.widgetNames, ",");
        List<String> widgetNames = new ArrayList<String>();
        for (String s : namesArray) widgetNames.add(s);
        final List<DashboardWidget> availableWidgetsList = widgetsService.getAvailableWidgetsList(guestId);
        for (DashboardWidget dashboardWidget : availableWidgetsList) {
            if (dashboardWidget.WidgetName.equals(widgetName)) {
                widgetNames.add(widgetName);
                break;
            }
        }
        dashboard.widgetNames = StringUtils.join(widgetNames, ",");
        em.persist(dashboard);
    }

    @Override
    @Transactional(readOnly=false)
    public void removeWidget(final long guestId, final long dashboardId, final String widgetName) {
        Dashboard dashboard = getDashboardById(guestId, dashboardId);
        final String[] namesArray = StringUtils.split(dashboard.widgetNames, ",");
        List<String> widgetNames = new ArrayList<String>();
        for (String s : namesArray) widgetNames.add(s);
        widgetNames.remove(widgetName);
        dashboard.widgetNames = StringUtils.join(widgetNames, ",");
        em.persist(dashboard);
        widgetsService.deleteWidgetSettings(guestId, dashboardId, widgetName);
    }

    @Override
    @Transactional(readOnly=false)
    public void setWidgetsOrder(final long guestId, final long dashboardId, final String[] widgetNames) {
        Dashboard dashboard = getDashboardById(guestId, dashboardId);
        dashboard.widgetNames = StringUtils.join(widgetNames, ",");
        em.persist(dashboard);
    }

    private JSONObject getWidgetByName(JSONArray widgetsJson, final String widgetName) {
        for (int i=0; i<widgetsJson.size(); i++) {
            JSONObject widgetJson = widgetsJson.getJSONObject(i);
            if (widgetJson.getString("name").equals(widgetName)) {
                return widgetJson;
            }
        }
        return null;
    }

    @Override
    @Transactional(readOnly=false)
    public void setDashboardsOrder(final long guestId, final long[] dashboardIds) {
        for(int i=0; i<dashboardIds.length; i++) {
            final Dashboard dashboard = getDashboardById(guestId, dashboardIds[i]);
            dashboard.ordering = i;
            em.persist(dashboard);
        }
    }

    @Override
    @Transactional(readOnly=false)
    public void setActiveDashboard(final long guestId, final long dashboardId) {
        final List<Dashboard> dashboards = JPAUtils.find(em, Dashboard.class, "dashboards.all", guestId);
        for (Dashboard dashboard : dashboards) {
            dashboard.active = false;
            if (dashboard.getId() == dashboardId) {
                dashboard.active = true;
            }
            em.persist(dashboard);
        }
    }
}
