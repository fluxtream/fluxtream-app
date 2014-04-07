package org.fluxtream.mvc.models;

import com.wordnik.swagger.annotations.ApiModel;

@ApiModel(value = "Beginning end end time bounds in UTC")
public class TimeBoundariesModel {

	public long start;
	public long end;
	
}
