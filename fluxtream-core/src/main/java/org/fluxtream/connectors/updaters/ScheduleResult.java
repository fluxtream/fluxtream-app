package org.fluxtream.connectors.updaters;

import java.util.List;
import org.fluxtream.connectors.Connector;
import org.fluxtream.connectors.ObjectType;
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

    @Expose
    long apiKeyId;

    @Expose
    public ResultType type = ResultType.NO_RESULT;

    private static final DateTimeFormatter fmt = ISODateTimeFormat.dateTime().withZone(DateTimeZone.forID("UTC"));

    public ScheduleResult(long apiKeyId, String connectorName, int objectTypes, ResultType resultType, long ts) {
        this.connectorName = connectorName;
        this.apiKeyId = apiKeyId;
        types = ObjectType.getObjectTypes(Connector.getConnector(connectorName), objectTypes);
        type = resultType;
        when = fmt.print(ts);
	}

	public enum ResultType {
		NO_RESULT, ALREADY_SCHEDULED,
			SCHEDULED_UPDATE_DEFERRED, SCHEDULED_UPDATE_IMMEDIATE, SYSTEM_IS_SHUTTING_DOWN
	}

}
