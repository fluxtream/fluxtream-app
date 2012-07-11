package com.fluxtream.connectors.updaters;

import java.util.Date;
import java.util.List;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.ObjectType;
import com.google.gson.annotations.Expose;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

public class ScheduleResult {

    @Expose
    String when;

    @Expose
    String connectorName;

    @Expose
    List<ObjectType> types;

    private static final DateTimeFormatter fmt = ISODateTimeFormat.dateTime().withZone(DateTimeZone.forID("UTC"));

    public ScheduleResult(String connectorName, int objectTypes, ResultType resultType, long ts) {
        this.connectorName = connectorName;
        types = ObjectType.getObjectTypes(Connector.getConnector(connectorName), objectTypes);
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
