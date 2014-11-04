package org.fluxtream.core.domain;

import com.wordnik.swagger.annotations.ApiModel;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

/**
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Entity(name = "Dashboard")
@NamedQueries({
      @NamedQuery(name = "dashboards.delete.all",
                  query = "DELETE FROM Dashboard dashboard WHERE dashboard.guestId=?"),
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

    @Index(name="guestId")
    public long guestId;

    public int ordering;

    public String widgetNames;

    @Type(type="yes_no")
    public boolean active;

    @Override
    public int compareTo(final Dashboard dashboard) {
        return ordering - dashboard.ordering;
    }
}
