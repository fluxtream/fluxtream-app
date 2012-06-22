package com.fluxtream.domain;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import com.fluxtream.connectors.updaters.UpdateInfo;

@Entity(name="ScheduledUpdate")
@NamedQueries ( {
	@NamedQuery(name = "scheduledUpdates.delete.all",
			query = "DELETE FROM ScheduledUpdate updt WHERE updt.guestId=?"),
	@NamedQuery(name = "scheduledUpdates.delete.byApi",
			query = "DELETE FROM ScheduledUpdate updt WHERE updt.guestId=? AND updt.connectorName=?"),
	@NamedQuery(name = "scheduledUpdates.delete.byApiAndObjectType",
			query = "DELETE FROM ScheduledUpdate updt WHERE updt.guestId=? AND updt.connectorName=? AND updt.objectTypes=?"),
	@NamedQuery(name = "scheduledUpdates.delete.byStatus",
			query = "DELETE FROM ScheduledUpdate updt WHERE updt.status=?"),
	@NamedQuery( name="scheduledUpdates.byStatus",
			query="SELECT updt FROM ScheduledUpdate updt WHERE updt.status=? AND updt.timeScheduled<?"),
	@NamedQuery( name="scheduledUpdates.exists",
		query="SELECT updt FROM ScheduledUpdate updt WHERE (updt.status=? OR updt.status=?) AND updt.guestId=? " +
				"AND updt.updateType=? AND updt.objectTypes=? " +
				"AND updt.connectorName=?"),
	@NamedQuery( name="scheduledUpdates.completed",
		query="SELECT updt FROM ScheduledUpdate updt WHERE updt.status=? " +
				"AND updt.guestId=? " +
				"AND updt.updateType=? AND updt.objectTypes=? " +
				"AND updt.connectorName=?")
})
public class ScheduledUpdate extends AbstractEntity {

	public String connectorName;
	public Status status = Status.SCHEDULED;
	public long timeScheduled;
		
	public static enum Status { SCHEDULED, IN_PROGRESS, DONE, FAILED };
	public UpdateInfo.UpdateType updateType;
	
	public long guestId;
	public int objectTypes;
	public int retries;
	public String jsonParams;
	
	public ScheduledUpdate() {}
	public ScheduledUpdate(ScheduledUpdate other) {
		connectorName = other.connectorName;
		status = other.status;
		timeScheduled = other.timeScheduled;
		updateType = other.updateType;
		guestId = other.guestId;
		objectTypes = other.objectTypes;
		retries = other.retries;
	}
	
	public long getGuestId() { return guestId; }
	public int getObjectTypes() { return objectTypes; }
	public UpdateInfo.UpdateType getUpdateType() { return updateType; }
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(guestId);
		sb.append("/");
		sb.append(connectorName);
		sb.append("/");
		sb.append(objectTypes);
		return sb.toString();
	}
	
	
}
