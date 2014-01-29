package com.fluxtream.connectors.up;

import java.util.List;
import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import com.fluxtream.connectors.annotations.ObjectTypeSpec;
import com.fluxtream.domain.AbstractFacet;

/**
 * User: candide
 * Date: 28/01/14
 * Time: 15:15
 */
@Entity(name="Facet_JawboneUpSleep")
@NamedQueries({
      @NamedQuery(name = "up.sleep.latest", query = "SELECT facet FROM Facet_JawboneUpSleep facet WHERE facet.apiKeyId=? ORDER BY facet.start DESC LIMIT 1")
})
@ObjectTypeSpec(name = "sleep", value = 4, prettyname = "Sleep")
public class JawboneUpSleepFacet extends AbstractFacet {

    public String xid;
    public String title;
    public int sub_type;
    public long time_created;
    public long time_completed;
    public String date;
    public double place_lat;
    public double place_long;
    public int place_acc;
    public String place_name;
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
    public String tz;

    @ElementCollection(fetch= FetchType.EAGER)
    @CollectionTable(
            name = "JawboneUpSleepPhase",
            joinColumns = @JoinColumn(name="SleepRecordID")
    )
    public List<JawboneUpMovesHourlyTotals> hourlyTotals;

    public JawboneUpSleepFacet(){}
    public JawboneUpSleepFacet(long apiKeyId){super(apiKeyId);}

    @Override
    protected void makeFullTextIndexable() {
    }

}
