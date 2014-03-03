package org.fluxtream.domain;

import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Entity(name = "DashboardWidgetsRepository")
@NamedQueries({
      @NamedQuery(name = "repositories.all",
                  query = "SELECT repository FROM DashboardWidgetsRepository repository WHERE repository.guestId=?")
})
public class DashboardWidgetsRepository extends AbstractEntity {

    public long guestId;

    public String url;

    public Date created;

}
