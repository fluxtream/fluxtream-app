package org.fluxtream.connectors.sleep_as_android;


import com.google.gdata.util.common.base.Pair;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.type.TypeFactory;
import org.fluxtream.core.connectors.annotations.ObjectTypeSpec;
import org.fluxtream.core.domain.AbstractFacet;

import javax.persistence.Entity;
import javax.persistence.Lob;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
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

    //comments that aren't tags on the sleep
    @Lob
    public String sleepComment;

    //the actigaph is evenly sampled over the duration of the sleep. each value represents activity level (no upper bound)
    @Lob
    public String actiGraph;

    //Tags associated with this sleep
    @Lob
    public String sleepTags;

    //Labels different events that occur throughout the night
    @Lob
    public String eventLabels;

    public void setActiGraph(List<Double> actiGraph){
        try{
            this.actiGraph = new ObjectMapper().writeValueAsString(actiGraph);
        }
        catch (IOException e){
            e.printStackTrace();
        }

    }

    public List<Double> getActiGraph(){
        try {
            if (actiGraph!=null)
                return  new ObjectMapper().readValue(actiGraph, TypeFactory.defaultInstance().constructCollectionType(List.class, Double.class));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    public void setSleepTags(List<String> sleepTags){
        try{
            this.sleepTags = new ObjectMapper().writeValueAsString(sleepTags);
        }
        catch (IOException e){
            e.printStackTrace();
        }

    }

    public List<String> getSleepTags(){
        try {
            if (sleepTags!=null)
                return  new ObjectMapper().readValue(sleepTags, TypeFactory.defaultInstance().constructCollectionType(List.class, String.class));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    public void setEventLabels(List<Pair<String,Long>> eventLabels){
        try{
            this.eventLabels = new ObjectMapper().writeValueAsString(eventLabels);
        }
        catch (IOException e){
            e.printStackTrace();
        }

    }

    public List<Pair<String,Long>> getEventLabels(){
        if (this.eventLabels==null) return null;
        try {
            List<Pair<String,Long>> list = new LinkedList<Pair<String,Long>>();
            JsonNode rootNode = new ObjectMapper().readTree(this.eventLabels);
            for (Iterator<JsonNode> i = rootNode.getElements(); i.hasNext();){
                JsonNode node = i.next();
                list.add(new Pair<String,Long>(node.get("first").asText(),node.get("second").asLong()));
            }
            return list;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    @Override
    protected void makeFullTextIndexable() {

    }
}
