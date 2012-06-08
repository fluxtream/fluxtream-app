package com.fluxtream.domain;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

/**
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Entity(name = "Dashboard")
@NamedQueries({
      @NamedQuery(name = "dashboards.all",
                  query = "SELECT dashboard FROM Dashboard dashboard WHERE dashboard.guestId=?"),
      @NamedQuery(name = "dashboards.maxOrder",
                  query = "SELECT max(dashboard.ordering) FROM Dashboard dashboard WHERE dashboard.guestId=?"),
      @NamedQuery(name = "dashboards.byId",
                  query = "SELECT dashboard FROM Dashboard dashboard WHERE dashboard.guestId=? AND dashboard.id=?"),
      @NamedQuery(name = "dashboards.byName",
                  query = "SELECT dashboard FROM Dashboard dashboard WHERE dashboard.guestId=? AND dashboard.name=?")
})
public class Dashboard extends AbstractEntity implements Comparable<Dashboard> {

    public Dashboard() {}

    public String name;

    public long guestId;

    public int ordering;

    public String widgetsJson;

    @Override
    public int compareTo(final Dashboard dashboard) {
        return ordering - dashboard.ordering;
    }
}
