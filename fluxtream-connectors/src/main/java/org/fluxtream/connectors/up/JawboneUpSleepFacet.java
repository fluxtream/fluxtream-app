package org.fluxtream.connectors.up;

import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import org.fluxtream.core.connectors.annotations.ObjectTypeSpec;

/**
 * User: candide
 * Date: 28/01/14
 * Time: 15:15
 */
@Entity(name="Facet_JawboneUpSleep")
@NamedQueries({
      @NamedQuery(name = "up.sleep.latest", query = "SELECT facet FROM Facet_JawboneUpSleep facet WHERE facet.apiKeyId=? ORDER BY facet.start DESC LIMIT 1")
})
@ObjectTypeSpec(name = "sleep", value = 4, prettyname = "Sleep", isDateBased = true)
public class JawboneUpSleepFacet extends JawboneUpGeoFacet {

    public String title;

    public String snapshot_image;
    public long smart_alarm_fire;
    public long awake_time;
    public long asleep_time;
    public int awakenings;
    public int rem;
    public int light;
    public int deep;
    public int awake;
    public int duration;
    public int quality;

    @Lob
    public String phasesStorage;

    public JawboneUpSleepFacet(){}
    public JawboneUpSleepFacet(long apiKeyId){super(apiKeyId);}

    @Override
    protected void makeFullTextIndexable() {
    }

}
