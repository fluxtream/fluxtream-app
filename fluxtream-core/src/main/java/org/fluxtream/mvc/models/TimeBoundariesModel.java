package org.fluxtream.mvc.models;

public class TimeBoundariesModel {

	public long start;
	public long end;

    public TimeBoundariesModel(){}

    public TimeBoundariesModel(long start, long end){
        this.start = start;
        this.end = end;
    }
	
}
