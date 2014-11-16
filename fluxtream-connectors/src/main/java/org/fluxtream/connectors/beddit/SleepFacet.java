package org.fluxtream.connectors.beddit;


import org.fluxtream.core.connectors.annotations.ObjectTypeSpec;
import org.fluxtream.core.domain.AbstractFacet;

import javax.persistence.Entity;

@Entity(name="Facet_BedditSleep")
@ObjectTypeSpec(name = "sleep", value = 1, prettyname = "Sleep Logs")
public class SleepFacet extends AbstractFacet {

    public SleepFacet(){super();}
    public SleepFacet(long apiKeyId){super(apiKeyId);}

    //time the data corresponding to this facet was updated on beddit's server
    public double updatedTime;

    //Target sleep amount for user in seconds
    public double sleepTimeTarget;

    //total seconds snoring
    public double snoringAmount;

    //resting heart rate measurement for the sleep session
    public double restingHeartRate;

    //average respiration rate for sleep session.
    public double respirationRate;

    //number of seconds it took to fall asleep from start
    public double timeToFallAsleep;

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

    @Override
    protected void makeFullTextIndexable() {

    }
}