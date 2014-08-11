package org.fluxtream.core.connectors.updaters;

import com.google.gson.annotations.Expose;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;
import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.connectors.ObjectType;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.util.List;

@ApiModel(value = "Update schedule info")
public class ScheduleResult {

    @Expose
    @ApiModelProperty(value="When is the update going to happen", required=true)
    public String when;

    @Expose
    @ApiModelProperty(value="The connector's 'technical' name", required=true)
    public String connectorName;

    @Expose
    @ApiModelProperty(value="The list of object types to be updated", required=true)
    public List<ObjectType> types;

    @Expose
    @ApiModelProperty(value="The ID of the API key", required=true)
    public long apiKeyId;

    @Expose
    @ApiModelProperty(value="The result of the scheduling operation", required=true, allowableValues = "NO_RESULT, ALREADY_SCHEDULED, " +
            "SCHEDULED_UPDATE_DEFERRED, SCHEDULED_UPDATE_IMMEDIATE, SYSTEM_IS_SHUTTING_DOWN")
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
