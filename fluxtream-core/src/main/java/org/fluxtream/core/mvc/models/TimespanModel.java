package org.fluxtream.core.mvc.models;

import org.fluxtream.core.services.impl.BodyTrackHelper;

public class TimespanModel {
    private double start;
    private double end;
    private String value;
    private String objectType;
    private BodyTrackHelper.TimespanStyle style;

    public TimespanModel(long start, long end, String value,String objectType){
        this.start = start / 1000.0;
        this.end = end / 1000.0;
        this.value = value;
        this.objectType = objectType;
    }

    public TimespanModel(long start, long end, String value,String objectType, BodyTrackHelper.TimespanStyle style){
        this(start,end,value,objectType);
        this.style = style;
    }

    public String getValue(){
        return value;
    }

    public String getObjectType(){
        return objectType;
    }

    public double getStart(){
        return start;
    }

    public void setStart(double start){
        this.start = start;
    }

    public double getEnd(){
        return end;
    }

    public void setEnd(double end){
        this.end = end;
    }
}
