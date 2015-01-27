package org.fluxtream.connectors.beddit;


import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.google.gdata.util.common.base.Pair;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.type.TypeFactory;
import org.fluxtream.core.connectors.annotations.ObjectTypeSpec;
import org.fluxtream.core.domain.AbstractFacet;

import javax.annotation.Nullable;
import javax.persistence.Entity;
import javax.persistence.Lob;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

@Entity(name="Facet_BedditSleep")
@ObjectTypeSpec(name = "sleep", value = 1, prettyname = "Sleep Logs", isDateBased = true)
public class SleepFacet extends AbstractFacet {

    static final int STATE_AWAY_FROM_BED = 65;
    static final int STATE_ASLEEP = 83;
    static final int STATE_AWAKE = 87;
    static final int STATE_MEASUREMENT_GAP = 71;


    public SleepFacet(){super();}
    public SleepFacet(long apiKeyId){super(apiKeyId);}

    //time the data corresponding to this facet was updated on beddit's server. Stored as seconds double to match the API format
    public double updatedTime;

    //The date for the facet. It's used to uniquely identify each entry
    public String date;

    //Target sleep amount for user in seconds
    public double sleepTimeTarget;

    //total seconds snoring
    public double snoringAmount;

    //resting heart rate measurement for the sleep session
    public double restingHeartRate;

    //average respiration rate for sleep session.
    @Nullable
    public Double respirationRate;

    //number of seconds it took to fall asleep from start
    @Nullable
    public Double timeToFallAsleep;

    //number of times away from bed during session
    public int awayCount;

    //total seconds in away state
    public double totalAwayTime;

    //total seconds in sleep state
    public double totalSleepTime;

    //total seconds in wake state
    public double totalWakeTime;

    //total time where signal was lost in seconds
    public double totalTimeNoSignal;

    public double scoreBedExits;
    public double scoreSleepAmount;
    public double scoreSnoring;
    public double scoreFallAsleepTime;
    public double scoreSleepEfficiency; //time asleep vs time in bed
    public double scoreAwakenings;

    @Lob
    public String sleepTags;

    @Lob
    public String sleepCyclesData;

    @Lob
    public String heartRateCurveData;

    @Lob
    public String sleepStagesData;

    @Lob
    public String snoringEpisodesData;

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
            return  new ObjectMapper().readValue(sleepTags, TypeFactory.defaultInstance().constructCollectionType(List.class, String.class));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

    }

    public void setSleepCycles(List<Pair<Long,Double>> sleepCycles){
        try{
            this.sleepCyclesData = new ObjectMapper().writeValueAsString(sleepCycles);
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }

    public List<Pair<Long,Double>> getSleepCycles(){
        try {
            List<Pair<Long,Double>> list = new LinkedList<Pair<Long,Double>>();
            JsonNode rootNode = new ObjectMapper().readTree(this.sleepCyclesData);
            for (Iterator<JsonNode> i = rootNode.getElements(); i.hasNext();){
                JsonNode node = i.next();
                list.add(new Pair<Long,Double>(node.get("first").asLong(),node.get("second").asDouble()));
            }
            return list;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void setHeartRateCurve(List<Pair<Long,Double>> heartRateCurve) {
        try {
            this.heartRateCurveData = new ObjectMapper().writeValueAsString(heartRateCurve);
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }

    public List<Pair<Long,Double>> getHeartRateCurve() {
        try{
            List<Pair<Long,Double>> list = new LinkedList<Pair<Long,Double>>();
            JsonNode rootNode = new ObjectMapper().readTree(this.heartRateCurveData);
            for (Iterator<JsonNode> i = rootNode.getElements(); i.hasNext();){
                JsonNode node = i.next();
                list.add(new Pair<Long,Double>(node.get("first").asLong(),node.get("second").asDouble()));
            }
            return list;
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void setSleepStages(List<Pair<Long,Integer>> sleepStages) {
        try {
            this.sleepStagesData = new ObjectMapper().writeValueAsString(sleepStages);
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }

    public List<Pair<Long,Integer>> getSleepStages() {
        try{
            List<Pair<Long,Integer>> list = new LinkedList<Pair<Long,Integer>>();
            JsonNode rootNode = new ObjectMapper().readTree(this.sleepStagesData);
            for (Iterator<JsonNode> i = rootNode.getElements(); i.hasNext();){
                JsonNode node = i.next();
                list.add(new Pair<Long,Integer>(node.get("first").asLong(),node.get("second").asInt()));
            }
            return list;
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void setSnoringEpisodes(List<Pair<Long,Double>> snoringEpisodes) {
        try {
            this.snoringEpisodesData = new ObjectMapper().writeValueAsString(snoringEpisodes);
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }

    public List<Pair<Long,Double>> getSnoringEpisodes() {
        try{
            List<Pair<Long,Double>> list = new LinkedList<Pair<Long,Double>>();
            JsonNode rootNode = new ObjectMapper().readTree(this.snoringEpisodesData);
            for (Iterator<JsonNode> i = rootNode.getElements(); i.hasNext();){
                JsonNode node = i.next();
                list.add(new Pair<Long,Double>(node.get("first").asLong(),node.get("second").asDouble()));
            }
            return list;
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }



    @Override
    protected void makeFullTextIndexable() {

    }
}

