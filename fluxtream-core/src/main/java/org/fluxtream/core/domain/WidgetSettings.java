package org.fluxtream.core.domain;

import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import org.hibernate.annotations.Index;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Entity(name="WidgetSettings")
@NamedQueries( {
       @NamedQuery( name="widgetSettings.delete.all",
                    query="DELETE FROM WidgetSettings settings " +
                          "WHERE settings.guestId=?"),
       @NamedQuery( name="widgetSettings.delete.byDashboardAndName",
                    query="DELETE FROM WidgetSettings settings " +
                          "WHERE settings.guestId=? " +
                          "AND settings.dashboardId=? " +
                          "AND settings.widgetName=?"),
       @NamedQuery( name="widgetSettings.delete.byDashboard",
                    query="DELETE FROM WidgetSettings settings " +
                          "WHERE settings.guestId=? " +
                          "AND settings.dashboardId=?"),
       @NamedQuery( name="widgetSettings.byDashboardAndName",
                    query="SELECT settings FROM WidgetSettings settings " +
                          "WHERE settings.guestId=? " +
                          "AND settings.dashboardId=? " +
                          "AND settings.widgetName=?"),
       @NamedQuery( name="widgetSettings.byDashboard",
                    query="SELECT settings FROM WidgetSettings settings " +
                          "WHERE settings.guestId=? " +
                          "AND settings.dashboardId=?")
})
public class WidgetSettings extends AbstractEntity {

    public WidgetSettings() {}

    @Index(name="guest_index")
    public long guestId;

    @Index(name="dashboardId")
    public long dashboardId;

    @Index(name="widgetName")
    public String widgetName;

    @Lob
    public String settingsJSON;
}
