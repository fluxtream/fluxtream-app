package org.fluxtream.core.mvc.models;

import java.util.TimeZone;

public class TimeZoneSegmentModel {
    public String name;
    public int offset;
    public boolean usesDST;
    public transient TimeZone tz;
    public long start;
    public long end;
}
