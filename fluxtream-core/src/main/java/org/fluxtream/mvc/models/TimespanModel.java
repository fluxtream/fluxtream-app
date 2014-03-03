package org.fluxtream.mvc.models;

import org.fluxtream.services.impl.BodyTrackHelper;

public class TimespanModel {
    double start;
    double end;
    String value;
    String objectType;
    BodyTrackHelper.TimespanStyle style;

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
}
