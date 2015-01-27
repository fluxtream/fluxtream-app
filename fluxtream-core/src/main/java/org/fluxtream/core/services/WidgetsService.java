package org.fluxtream.core.services;

import java.net.MalformedURLException;
import java.util.List;
import org.fluxtream.core.domain.DashboardWidget;
import org.fluxtream.core.domain.DashboardWidgetsRepository;
import org.fluxtream.core.domain.WidgetSettings;
import org.springframework.cache.annotation.Cacheable;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
public interface WidgetsService {

    @Cacheable(value = "userWidgets")
    public List<DashboardWidget> getAvailableWidgetsList(long guestId);

    public void refreshWidgets(long guestId);

    public List<DashboardWidgetsRepository> getWidgetRepositories(long guestId);

    public void addWidgetRepositoryURL(long guestId, String url);

    public void removeWidgetRepositoryURL(long guestId, String url);

    public void saveWidgetSettings(long guestId, long dashboardId, String widgetname, String settingsJSON);

    public WidgetSettings getWidgetSettings(long guestId, long dashboardId, String widgetname);

    public void deleteWidgetSettings(long guestId, long dashboardId, String widgetname);

    public List<WidgetSettings> getWidgetSettings(long guestId, long dashboardId);

    public void deleteWidgetSettings(long guestId, long dashboardId);

}