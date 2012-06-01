package com.fluxtream.services.impl;

import java.awt.geom.QuadCurve2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.domain.Dashboard;
import com.fluxtream.services.DashboardService;
import com.fluxtream.services.GuestService;
import com.fluxtream.utils.JPAUtils;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Transactional(readOnly = true)
@Service
public class DashboardServiceImpl implements DashboardService {

    @PersistenceContext
    EntityManager em;

    @Autowired
    GuestService guestService;

    @Autowired
    @Qualifier("widgets")
    PropertiesConfiguration widgetProperties;

    @Override
    public List<Dashboard> getDashboards(final long guestId) {
        List<Dashboard> dashboards = JPAUtils.find(em, Dashboard.class, "dashboards.all",
                                          guestId);
        if (dashboards.size()==0) {
            Dashboard dashboard = createDashboard(guestId, "Untitled Dashboard");
            addDefaultWidgets(guestId, dashboard);
        }
        return dashboards;
    }

    @Transactional(readOnly=false)
    private void addDefaultWidgets(final long guestId, Dashboard dashboard) {
        List<ApiKey> keys = guestService.getApiKeys(guestId);
        Iterator<ApiKey> eachKey = keys.iterator();
        List<JSONObject> widgetsJson = new ArrayList<JSONObject>();
        while (eachKey.hasNext()) {
            ApiKey key = eachKey.next();
            String defaultWidgetKey = key.getConnector().getName() + ".defaultWidget";
            String widgetName = widgetProperties.getString(defaultWidgetKey);
            if (widgetName!=null) {
                JSONObject widgetJson = new JSONObject();
                widgetJson.accumulate("name", widgetName);
            }
        }
        String jsonWidgets = widgetsJson.toString();
        dashboard.widgetsJson = jsonWidgets;
        em.persist(dashboard);
    }

    @Transactional(readOnly=false)
    private Dashboard createDashboard(final long guestId, final String dashboardName) {
        Dashboard dashboard = new Dashboard();
        dashboard.name = dashboardName;
        dashboard.guestId = guestId;
        dashboard.widgetsJson = "[]";
        int maxOrder = JPAUtils.execute(em, "dashboards.maxOrder");
        dashboard.order = maxOrder+1;
        em.persist(dashboard);
        return dashboard;
    }

    @Override
    public void addDashboard(final long guestId, final String dashboardName) {
        createDashboard(guestId, dashboardName);
    }

    @Override
    @Transactional(readOnly=false)
    public void renameDashboard(final long guestId, final String previousDashboardName, final String dashboardName) {
        Dashboard dashboard = getDashboardByName(guestId, previousDashboardName);
        dashboard.name = dashboardName;
        em.persist(dashboard);
    }

    private Dashboard getDashboardByName(long guestId, String dashboardName) {
        Dashboard dashboard = JPAUtils.findUnique(em, Dashboard.class, "dashboards.byName", guestId, dashboardName);
        return dashboard;
    }

    @Override
    @Transactional(readOnly=false)
    public void addWidget(final long guestId, final String dashboardName, final String widgetJson) {
        Dashboard dashboard = getDashboardByName(guestId, dashboardName);
        JSONArray widgetsJson = JSONArray.fromObject(dashboard.widgetsJson);
        widgetsJson.add(JSONObject.fromObject(widgetJson));
        em.persist(dashboard);
    }

    @Override
    @Transactional(readOnly=false)
    public void removeWidget(final long guestId, final String dashboardName, final String widgetName) {
        Dashboard dashboard = getDashboardByName(guestId, dashboardName);
        JSONArray widgetsJson = JSONArray.fromObject(dashboard.widgetsJson);
        for (int i=0; i<widgetsJson.size(); i++) {
            JSONObject widgetJson = widgetsJson.getJSONObject(i);
            if (widgetJson.getString("name").equals(widgetName)) {
                widgetsJson.remove(widgetJson);
                break;
            }
        }
        dashboard.widgetsJson = widgetsJson.toString();
        em.persist(dashboard);
    }

    @Override
    public void setWidgetsOrder(final long guestId, final String dashboardName, final String[] widgetNames) {
        Dashboard dashboard = getDashboardByName(guestId, dashboardName);
        JSONArray widgetsJson = JSONArray.fromObject(dashboard.widgetsJson);
        JSONArray reorderedWidgetsJson = new JSONArray();
        for (String widgetName : widgetNames) {
            JSONObject widgetJson = getWidgetByName(widgetsJson, widgetName);
            reorderedWidgetsJson.add(widgetJson);
        }
        dashboard.widgetsJson = reorderedWidgetsJson.toString();
        em.persist(dashboard);
    }

    private JSONObject getWidgetByName(JSONArray widgetsJson, final String widgetName) {
        for (int i=0; i<widgetsJson.size(); i++) {
            JSONObject widgetJson = widgetsJson.getJSONObject(i);
            if (widgetJson.getString("name").equals(widgetName))
                return widgetJson;
        }
        return null;
    }

    @Override
    @Transactional(readOnly=false)
    public void setDashboardsOrder(final long guestId, final String[] dashboardNames) {
        List<Dashboard> dashboards = JPAUtils.find(em, Dashboard.class, "dashboards.all",
                                                   guestId);
        for(int i=0; i<dashboardNames.length; i++) {
            dashboards.get(i).name = dashboardNames[i];
            em.persist(dashboards.get(i));
        }
    }
}
