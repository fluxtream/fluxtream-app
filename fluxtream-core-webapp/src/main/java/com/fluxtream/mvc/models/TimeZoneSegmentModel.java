package com.fluxtream.mvc.models;

import java.util.TimeZone;

public class TimeZoneSegmentModel {
    public String timeZone;
    public transient TimeZone tz;
    public long start;
    public long end;
}
