package org.fluxtream.core.mvc.models;

import com.wordnik.swagger.annotations.ApiModel;
import org.codehaus.plexus.util.ExceptionUtils;

@ApiModel(value = "Beginning end end time bounds in UTC")
public class TimeBoundariesModel {

	public long start;
	public long end;

    public TimeBoundariesModel(){}

    public TimeBoundariesModel(long start, long end){
        this.start = start;
        this.end = end;
    }
	
}
