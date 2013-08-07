package com.fluxtream.mvc.models;

public class TimespanModel {
    double start;
    double end;
    String value;

    public TimespanModel(long start, long end, String value){
        this.start = start / 1000.0;
        this.end = end / 1000.0;
        this.value = value;
    }

    public TimespanModel(long start, long end){
        this(start,end,"on");
    }

}
