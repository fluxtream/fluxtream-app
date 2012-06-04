package com.fluxtream.services;

import java.util.List;
import com.fluxtream.domain.Dashboard;
import com.fluxtream.domain.DashboardWidget;
import com.fluxtream.domain.DashboardWidgetsRepository;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
public interface DashboardService {

    public List<Dashboard> getDashboards(long guestId);

    public void addDashboard(long guestId, String dashboardName);

    public void removeDashboard(long guestId, String dashboardName);

    public void renameDashboard(long guestId, String previousDashboardName, String dashboardName);

    public void addWidget(long guestId, String dashboardName, String widgetJson);

    public void removeWidget(long guestId, String dashboardName, String widgetName);

    public void setWidgetsOrder(long guestId, String dashboardName, String[] widgetNames);

    public void setDashboardsOrder(long guestId, String[] dashboardNames);

    public List<DashboardWidget> getAvailableWidgetsList(long guestId);

    public void refreshWidgets();

    public void refreshWidgets(long guestId);

    public List<DashboardWidgetsRepository> getWidgetRepositories(long guestId);

    public void addWidgetRepositoryURL(long guestId, String url);

    public void removeWidgetRepositoryURL(long guestId, String url);

}