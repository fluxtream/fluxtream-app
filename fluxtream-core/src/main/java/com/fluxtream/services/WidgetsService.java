package com.fluxtream.services;

import java.util.List;
import com.fluxtream.domain.DashboardWidget;
import com.fluxtream.domain.DashboardWidgetsRepository;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
public interface WidgetsService {

    public List<DashboardWidget> getAvailableWidgetsList(long guestId);

    public void refreshWidgets();

    public void refreshWidgets(long guestId);

    public List<DashboardWidgetsRepository> getWidgetRepositories(long guestId);

    public void addWidgetRepositoryURL(long guestId, String url);

    public void removeWidgetRepositoryURL(long guestId, String url);

}