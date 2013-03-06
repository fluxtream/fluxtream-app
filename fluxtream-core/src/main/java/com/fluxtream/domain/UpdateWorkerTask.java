package com.fluxtream.domain;

import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import com.fluxtream.connectors.updaters.UpdateInfo;

@Entity(name="ScheduledUpdate")
@NamedQueries ( {
	@NamedQuery( name = "updateWorkerTasks.delete.all",
		query = "DELETE FROM ScheduledUpdate updt " +
                "WHERE updt.guestId=?"),
	@NamedQuery( name = "updateWorkerTasks.delete.byApi",
		query = "DELETE FROM ScheduledUpdate updt " +
                "WHERE updt.guestId=? " +
                "AND ((updt.connectorName=? AND updt.apiKeyId iS NULL) OR updt.apiKeyId=?)" +
                "AND updt.updateType!=?"),
    @NamedQuery( name = "updateWorkerTasks.deleteAll.byApi",
                 query = "DELETE FROM ScheduledUpdate updt " +
                         "WHERE updt.guestId=? " +
                         "AND ((updt.connectorName=? AND updt.apiKeyId iS NULL) OR updt.apiKeyId=?)"),
    @NamedQuery( name = "updateWorkerTasks.deleteAll.byApiAndObjectType",
                 query = "DELETE FROM ScheduledUpdate updt " +
                         "WHERE updt.guestId=? " +
                         "AND ((updt.connectorName=? AND updt.apiKeyId iS NULL) OR updt.apiKeyId=?) " +
                         "AND updt.objectTypes=?"),
    @NamedQuery( name = "updateWorkerTasks.delete.byApiAndObjectType",
		query = "DELETE FROM ScheduledUpdate updt " +
                "WHERE updt.guestId=? " +
                "AND ((updt.connectorName=? AND updt.apiKeyId iS NULL) OR updt.apiKeyId=?) " +
                "AND updt.objectTypes=?" +
                "AND updt.updateType!=?"),
	@NamedQuery( name = "updateWorkerTasks.byStatus",
		query = "SELECT updt FROM ScheduledUpdate updt " +
                "WHERE updt.status=? " +
                "AND updt.serverUUID=? " +
                "AND updt.timeScheduled<?"),
    @NamedQuery( name = "updateWorkerTasks.isScheduledOrInProgress",
        query = "SELECT updt FROM ScheduledUpdate updt " +
                "WHERE (updt.status=0 " +
                    "OR updt.status=1) " +
                "AND updt.guestId=? " +
                "AND ((updt.connectorName=? AND updt.apiKeyId iS NULL) OR updt.apiKeyId=?)"),
    @NamedQuery( name = "updateWorkerTasks.withObjectTypes.isScheduled",
		query = "SELECT updt FROM ScheduledUpdate updt " +
                "WHERE (updt.status=? OR updt.status=?) " +
                "AND updt.guestId=? " +
			    "AND updt.objectTypes=? AND ((updt.connectorName=? AND updt.apiKeyId iS NULL) OR updt.apiKeyId=?) " +
                "ORDER BY updt.timeScheduled DESC"),
	@NamedQuery( name = "updateWorkerTasks.completed",
		query = "SELECT updt FROM ScheduledUpdate updt " +
                "WHERE updt.status=? " +
				"AND updt.guestId=? " +
				"AND updt.updateType=? " +
                "AND updt.objectTypes=? " +
				"AND ((updt.connectorName=? AND updt.apiKeyId iS NULL) OR updt.apiKeyId=?)"),
    @NamedQuery( name = "updateWorkerTasks.isInProgressOrScheduledBefore",
        query = "SELECT updt FROM ScheduledUpdate updt " +
                "WHERE (updt.status=1 " +
                    "OR (updt.status=0 " +
                        "AND updt.timeScheduled<?))" +
                "AND updt.guestId=? " +
                "AND ((updt.connectorName=? AND updt.apiKeyId iS NULL) OR updt.apiKeyId=?) "),
    @NamedQuery( name = "updateWorkerTasks.getLastFinishedTask",
        query = "SELECT updt FROM ScheduledUpdate updt " +
                "WHERE updt.timeScheduled<? " +
                "AND (updt.status=2 " +
                    "OR updt.status=3 " +
                    "OR updt.status=4) " +
                "AND updt.guestId=? " +
                "AND ((updt.connectorName=? AND updt.apiKeyId iS NULL) OR updt.apiKeyId=?) " +
                "ORDER BY updt.timeScheduled DESC")
})
public class UpdateWorkerTask extends AbstractEntity {

    public static class AuditTrailEntry {

        public AuditTrailEntry(final Date date, final String reason, final String nextAction, String stackTrace) {
            this.date = date;
            this.reason = reason;
            this.nextAction = nextAction;
            this.stackTrace = stackTrace;
        }
        public AuditTrailEntry(final Date date, final String reason, final String nextAction) {
            this.date = date;
            this.reason = reason;
            this.nextAction = nextAction;
        }
        public Date date;
        public String reason;
        public String stackTrace;
        public String nextAction;
    }

    public String serverUUID;
    public Long apiKeyId;
	public String connectorName;
	public Status status = Status.SCHEDULED;

    @Lob
    public String auditTrail;

	public long timeScheduled;

	public static enum Status { SCHEDULED, IN_PROGRESS, DONE, FAILED, STALLED}

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

    public void addAuditTrailEntry(AuditTrailEntry auditTrailEntry) {
        if (auditTrail==null) auditTrail = "";
        StringBuilder sb = new StringBuilder(auditTrail);
        sb.append("\\n").append(auditTrailEntry.date.toString())
                .append(" - reason: ")
                .append(auditTrailEntry.reason)
                .append(" - next action: ")
                .append(auditTrailEntry.nextAction);
        if (auditTrailEntry.stackTrace!=null)
                sb.append("stackTracke: \n")
                  .append(auditTrailEntry.stackTrace);
        this.auditTrail = sb.toString();
    }
	
	public long getGuestId() { return guestId; }
	public int getObjectTypes() { return objectTypes; }

    public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(guestId);
		sb.append("/");
		sb.append(connectorName);
		sb.append("/");
		sb.append(objectTypes);
		return sb.toString();
	}
	
	
}
