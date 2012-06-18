package com.fluxtream.connectors.bodymedia;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import com.fluxtream.connectors.annotations.ObjectTypeSpec;
import org.hibernate.search.annotations.Indexed;

/**
 * <p>
 * <code>BodymediaUserFacet</code> does something...
 * </p>
 *
 * @author Prasanth Somasundar
 */
@Entity(name="Facet_BodymediaUser")
@ObjectTypeSpec(name = "user", value = 8, prettyname = "Bodymedia User", extractor = BodymediaFacetExtractor.class)
@NamedQueries({
	@NamedQuery(name = "bodymedia.user.deleteAll", query = "DELETE FROM Facet_BodymediaUser facet WHERE facet.guestId=?"),
	@NamedQuery(name = "bodymedia.user.between", query = "SELECT facet FROM Facet_BodymediaUser facet WHERE facet.guestId=? AND facet.start>=? AND facet.end<=?")
})
@Indexed
public class BodymediaUserFacet
{
    public int guestId;
    public String connector;
    public long LastSync;
    public String LatestData;
    public int Status;

    public int getGuestId()
    {
        return guestId;
    }

    public void setGuestId(final int guestId)
    {
        this.guestId = guestId;
    }

    public String getConnector()
    {
        return connector;
    }

    public void setConnector(final String connector)
    {
        this.connector = connector;
    }

    public long getLastSync()
    {
        return LastSync;
    }

    public void setLastSync(final long lastSync)
    {
        LastSync = lastSync;
    }

    public String getLatestData()
    {
        return LatestData;
    }

    public void setLatestData(final String latestData)
    {
        LatestData = latestData;
    }

    public int getStatus()
    {
        return Status;
    }

    public void setStatus(final int status)
    {
        Status = status;
    }

    public String getFacet()
    {
        return Facet;
    }

    public void setFacet(final String facet)
    {
        Facet = facet;
    }

    public String Facet;

}
