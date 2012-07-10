package com.fluxtream.connectors.updaters;

import java.util.Date;
import com.google.gson.annotations.Expose;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

public class ScheduleResult {

    @Expose
    String when;

    private static final DateTimeFormatter fmt = ISODateTimeFormat.dateTime().withZone(DateTimeZone.forID("UTC"));

    public ScheduleResult(ResultType resultType, long ts) {
		type = resultType;
        when = fmt.print(ts);
	}
	
	public ScheduleResult() {}

    @Expose
	public ResultType type = ResultType.NO_RESULT;

	public enum ResultType {
		NO_RESULT, ALREADY_SCHEDULED,
			SCHEDULED_UPDATE_DEFERRED, SCHEDULED_UPDATE_IMMEDIATE
	}

}
