package com.fluxtream.mvc.models;

import java.util.List;

public class TimespanModel {
    double start;
    double end;
    String value;
    String objectType;

    public TimespanModel(long start, long end, String value,String objectType){
        this.start = start / 1000.0;
        this.end = end / 1000.0;
        this.value = value;
        this.objectType = objectType;
    }
}
