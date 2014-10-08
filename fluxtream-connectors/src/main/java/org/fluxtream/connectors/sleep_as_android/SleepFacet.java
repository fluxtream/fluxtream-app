package org.fluxtream.connectors.sleep_as_android;


import com.google.gdata.util.common.base.Pair;
import org.fluxtream.core.connectors.annotations.ObjectTypeSpec;
import org.fluxtream.core.domain.AbstractFacet;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Lob;
import java.util.ArrayList;
import java.util.List;

// note: in this class we used ArrayList<T> for lists instead of the generic List<T> structure since ArrayList<T>
// implements serializable interface but List<T> does not!

@Entity(name="Facet_SleepAsAndroidSleep")
@ObjectTypeSpec(name = "sleep", value = 1, prettyname = "Sleep Logs")
public class SleepFacet extends AbstractFacet {

    public SleepFacet(){super();}
    public SleepFacet(long apiKeyId){super(apiKeyId);}

    //The number of sleep cycles went through
    public int cycles;
    //0.0 - 1.0 where 0 means no deep sleep and 1 means all deep sleep
    public double ratioDeepSleep;

    //0.0-5.0 rating out of 5 of the sleep quality
    public double rating;

    //how many seconds was there snroing
    public int snoringSeconds;

    //what the noise level was like
    public double noiseLevel;

    //the actigaph is evenly sampled over the duration of the sleep. each value represents activity level (no upper bound)
    @Lob
    public ArrayList<Double> actiGraph;

    //Tags associated with this sleep
    @Lob
    public ArrayList<String> sleepTags;

    //Labels different events that occur throughout the night
    @Lob
    public ArrayList<Pair<String,Long>> eventLabels;


    @Override
    protected void makeFullTextIndexable() {

    }
}
