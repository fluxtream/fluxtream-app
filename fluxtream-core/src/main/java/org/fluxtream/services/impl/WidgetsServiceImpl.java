package org.fluxtream.services.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.fluxtream.Configuration;
import org.fluxtream.domain.ApiKey;
import org.fluxtream.domain.DashboardWidget;
import org.fluxtream.domain.DashboardWidgetsRepository;
import org.fluxtream.domain.WidgetSettings;
import org.fluxtream.services.GuestService;
import org.fluxtream.services.WidgetsService;
import org.fluxtream.utils.HttpUtils;
import org.fluxtream.utils.JPAUtils;
import org.fluxtream.utils.UnexpectedHttpResponseCodeException;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Service
@Transactional(readOnly=true)
public class WidgetsServiceImpl implements WidgetsService {

    @PersistenceContext
    EntityManager em;

    @Autowired
    GuestService guestService;

    @Autowired
    Configuration env;

    @Override
    @CacheEvict(value = "officialWidgets")
    public void refreshWidgets() {
        // empty method: just let spring evict the "officialWidgets" cache
    }

    @Override
    @CacheEvict(value = "userWidgets")
    public void refreshWidgets(final long guestId) {
        // empty method: just let spring evict the "userWidgets" cache for the guestId key
    }

    @Override
    public List<DashboardWidgetsRepository> getWidgetRepositories(final long guestId) {
        final List<DashboardWidgetsRepository> repositoryList = JPAUtils.find(em, DashboardWidgetsRepository.class,
                                                                              "repositories.all", guestId);
        return repositoryList;
    }

    @Override
    @Transactional(readOnly=false)
    public void addWidgetRepositoryURL(final long guestId, final String url) {
        if (StringUtils.isEmpty(url))
            throw new RuntimeException("Null URL");
        DashboardWidgetsRepository repository = new DashboardWidgetsRepository();
        repository.guestId = guestId;
        repository.url = url;
        repository.created = new Date();
        em.persist(repository);
        refreshWidgets(guestId);
    }

    @Override
    @Transactional(readOnly=false)
    public void removeWidgetRepositoryURL(final long guestId, final String url) {
        final List<DashboardWidgetsRepository> repositories = getWidgetRepositories(guestId);
        for (final ListIterator eachRepository = repositories.listIterator(); eachRepository.hasNext(); ) {
            final DashboardWidgetsRepository repository = (DashboardWidgetsRepository)eachRepository.next();
            if (repository.url.equals(url)) {
                em.remove(repository);
                break;
            }
        }
        refreshWidgets(guestId);
    }

    @Override
    @Transactional(readOnly=false)
    public void saveWidgetSettings(final long guestId, final long dashboardId, final String widgetName, final String settingsJSON) {
        WidgetSettings settings = JPAUtils.findUnique(em, WidgetSettings.class,
                                                      "widgetSettings.byDashboardAndName",
                                                      guestId, dashboardId, widgetName);
        if (settings==null) {
            settings = new WidgetSettings();
            settings.guestId = guestId;
            settings.dashboardId = dashboardId;
            settings.widgetName = widgetName;
            settings.settingsJSON = settingsJSON;
            em.persist(settings);
        } else {
            settings.settingsJSON = settingsJSON;
            em.merge(settings);
        }
    }

    @Override
    public WidgetSettings getWidgetSettings(final long guestId, final long dashboardId, final String widgetName) {
        final WidgetSettings settings = JPAUtils.findUnique(em, WidgetSettings.class,
                                                            "widgetSettings.byDashboardAndName",
                                                            guestId, dashboardId, widgetName);
        if (settings==null) {
            saveWidgetSettings(guestId, dashboardId, widgetName, "{}");
            return getWidgetSettings(guestId, dashboardId, widgetName);
        }
        return settings;
    }

    @Override
    public void deleteWidgetSettings(final long guestId, final long dashboardId, final String widgetName) {
        JPAUtils.execute(em, "widgetSettings.delete.byDashboardAndName", guestId, dashboardId, widgetName);
    }

    @Override
    public List<WidgetSettings> getWidgetSettings(final long guestId, final long dashboardId) {
        final List<WidgetSettings> settings = JPAUtils.find(em, WidgetSettings.class,
                                                            "widgetSettings.byDashboard",
                                                            guestId, dashboardId);
        return settings;
    }

    @Override
    public void deleteWidgetSettings(final long guestId, final long dashboardId) {
        JPAUtils.execute(em, "widgetSettings.delete.byDashboard", guestId, dashboardId);
    }

    @Override
    public List<DashboardWidget> getAvailableWidgetsList(final long guestId) {
        List<DashboardWidget> allWidgets = new ArrayList<DashboardWidget>();
        final List<DashboardWidget> userWidgets = getUserWidgets(guestId);
        allWidgets.addAll(userWidgets);
        final List<DashboardWidget> officialWidgets = getOfficialWidgets();
        allWidgets.addAll(officialWidgets);
        final List<ApiKey> keys = guestService.getApiKeys(guestId);
        List<String> userConnectorNames = new ArrayList<String>();
        for (ApiKey key : keys) {
            if(key!=null && key.getConnector()!=null && key.getConnector().getName()!=null) {
                final String connectorName = key.getConnector().getName();
                userConnectorNames.add(connectorName);
            }
        }
        List<DashboardWidget> availableWidgetsList = new ArrayList<DashboardWidget>();
        for (DashboardWidget widget : allWidgets) {
            if (widget.matchesUserConnectors(userConnectorNames, env.get("environment").equals("local"))) {
                availableWidgetsList.add(widget);
            }
        }
        return availableWidgetsList;
    }

    @Cacheable(value = "userWidgets")
    private List<DashboardWidget> getUserWidgets(final long guestId) {
        final List<DashboardWidgetsRepository> repositoryURLs = getWidgetRepositories(guestId);
        List<DashboardWidget> userWidgets = new ArrayList<DashboardWidget>();
        for (DashboardWidgetsRepository repositoryURL : repositoryURLs) {
            try{
                List<DashboardWidget> widgetsList = getWidgetsList(repositoryURL.url,false);
                userWidgets.addAll(widgetsList);
            }
            catch (Exception e){
                System.out.println(e.getMessage());
            }
        }
        return userWidgets;
    }

    @Cacheable(value = "officialWidgets")
    private List<DashboardWidget> getOfficialWidgets() {
        String mainWidgetsUrl = env.get("homeBaseUrl");
        return getWidgetsList(mainWidgetsUrl,true);
    }

    private List<DashboardWidget> getWidgetsList(String baseURL, boolean local) {
        JSONArray widgetsList = null;
        String widgetListString = null;
        try {
            widgetListString = HttpUtils.fetch(baseURL + "widgets.json");
        }
        catch (IOException e) {
            throw new RuntimeException("Could not access widgets JSON URL: " + baseURL + "widgets.json");
        }
        catch (UnexpectedHttpResponseCodeException e) {
            throw new RuntimeException("Could not access widgets JSON URL: " + baseURL + "widgets.json");
        }
        try {
            widgetsList = JSONArray.fromObject(widgetListString);
        } catch (Throwable t) {
            throw new RuntimeException("Could not parse widgets JSON (" + t.getMessage() + ")");
        }
        String widgetUrl = null;
        List<DashboardWidget> widgets = new ArrayList<DashboardWidget>();
        String manifestJSONString = null;
        try {
            for (int i=0; i<widgetsList.size(); i++) {
                String widgetName = widgetsList.getString(i);
                widgetUrl = baseURL + "widgets/" + widgetName + "/manifest.json";
                try {
                    manifestJSONString = HttpUtils.fetch(widgetUrl);
                }
                catch (UnexpectedHttpResponseCodeException e) {
                    e.printStackTrace();
                }
                JSONObject manifestJSON = null;
                try {
                    manifestJSON = JSONObject.fromObject(manifestJSONString);
                } catch (Throwable t) {
                    throw new RuntimeException("Could not parse widget manifest (" + t.getMessage() + ")");
                }
                widgets.add(new DashboardWidget(manifestJSON, (local ? "/" : baseURL) + "widgets"));
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not access widget manifest JSON URL: " + widgetUrl);
        }
        return widgets;
    }

}
