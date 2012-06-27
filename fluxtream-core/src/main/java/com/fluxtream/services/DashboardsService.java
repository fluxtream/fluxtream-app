package com.fluxtream.services;

import java.util.List;
import com.fluxtream.domain.Dashboard;
import com.fluxtream.domain.DashboardWidget;
import com.fluxtream.domain.DashboardWidgetsRepository;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
public interface DashboardsService {

    public List<Dashboard> getDashboards(long guestId);

    public Dashboard getDashboard(long guestId, long dashboardId);

    public void addDashboard(long guestId, String dashboardName);

    public void removeDashboard(long guestId, long dashboardId);

    public void renameDashboard(long guestId, long previousDashboardId, String dashboardName);

    public void addWidget(long guestId, long dashboardId, String widgetName);

    public void removeWidget(long guestId, long dashboardId, String widgetName);

    public void setWidgetsOrder(long guestId, long dashboardId, String[] widgetNames);

    public void setDashboardsOrder(long guestId, long[] dashboardIds);

    public void setActiveDashboard(long guestId, long dashboardId);

}