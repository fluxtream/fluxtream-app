package com.fluxtream.domain;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import com.fluxtream.connectors.updaters.UpdateInfo;
import com.google.gson.annotations.Expose;

@Entity(name="ScheduledUpdate")
@NamedQueries ( {
	@NamedQuery(name = "updateWorkerTasks.delete.all",
			query = "DELETE FROM ScheduledUpdate updt WHERE updt.guestId=?"),
	@NamedQuery(name = "updateWorkerTasks.delete.byApi",
			query = "DELETE FROM ScheduledUpdate updt WHERE updt.guestId=? AND updt.connectorName=?"),
	@NamedQuery(name = "updateWorkerTasks.delete.byApiAndObjectType",
			query = "DELETE FROM ScheduledUpdate updt WHERE updt.guestId=? AND updt.connectorName=? AND updt.objectTypes=?"),
	@NamedQuery(name = "updateWorkerTasks.delete.byStatus",
			query = "DELETE FROM ScheduledUpdate updt WHERE updt.status=?"),
	@NamedQuery( name="updateWorkerTasks.byStatus",
			query="SELECT updt FROM ScheduledUpdate updt WHERE updt.status=? AND updt.timeScheduled<?"),
	@NamedQuery( name="updateWorkerTasks.exists",
		query="SELECT updt FROM ScheduledUpdate updt WHERE (updt.status=? OR updt.status=?) AND updt.guestId=? " +
				"AND updt.updateType=? AND updt.objectTypes=? " +
				"AND updt.connectorName=?"),
	@NamedQuery( name="updateWorkerTasks.completed",
		query="SELECT updt FROM ScheduledUpdate updt WHERE updt.status=? " +
				"AND updt.guestId=? " +
				"AND updt.updateType=? AND updt.objectTypes=? " +
				"AND updt.connectorName=?")
})
public class UpdateWorkerTask extends AbstractEntity {

	public String connectorName;
	public Status status = Status.SCHEDULED;

    @Expose
	public long timeScheduled;
		
	public static enum Status { SCHEDULED, IN_PROGRESS, DONE, FAILED };
	public UpdateInfo.UpdateType updateType;
	
	public long guestId;
	public int objectTypes;
	public int retries;
	public String jsonParams;
	
	public UpdateWorkerTask() {}
	public UpdateWorkerTask(UpdateWorkerTask other) {
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
