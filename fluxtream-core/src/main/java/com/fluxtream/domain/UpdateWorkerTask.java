package com.fluxtream.domain;

import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import com.fluxtream.connectors.updaters.UpdateInfo;
import org.hibernate.annotations.Index;

@Entity(name="UpdateWorkerTask")
@NamedQueries ( {
	@NamedQuery( name = "updateWorkerTasks.delete.all",
		query = "DELETE FROM UpdateWorkerTask updt " +
                "WHERE updt.guestId=?"),
    @NamedQuery( name = "updateWorkerTasks.cleanup.byApi",
                 query = "DELETE FROM UpdateWorkerTask updt " +
                         "WHERE updt.apiKeyId=? " +
                         "AND updt.status>1 " +
                         "AND updt.updateType!=?"),
	@NamedQuery( name = "updateWorkerTasks.delete.byApi",
		query = "DELETE FROM UpdateWorkerTask updt " +
                "WHERE updt.apiKeyId=? " +
                "AND updt.updateType!=?"),
    @NamedQuery( name = "updateWorkerTasks.deleteAll.byApi",
                 query = "DELETE FROM UpdateWorkerTask updt " +
                         "WHERE updt.apiKeyId=?"),
    @NamedQuery( name = "updateWorkerTasks.deleteAll.byApiAndObjectType",
                 query = "DELETE FROM UpdateWorkerTask updt " +
                         "WHERE updt.apiKeyId=? " +
                         "AND updt.objectTypes=?"),
    @NamedQuery( name = "updateWorkerTasks.delete.byApiAndObjectType",
		query = "DELETE FROM UpdateWorkerTask updt " +
                "WHERE updt.apiKeyId=? " +
                "AND updt.objectTypes=?" +
                "AND updt.updateType!=?"),
	@NamedQuery( name = "updateWorkerTasks.byStatus",
		query = "SELECT updt FROM UpdateWorkerTask updt " +
                "WHERE updt.status=?1 " +
                "AND updt.timeScheduled<?2"),
    @NamedQuery( name = "updateWorkerTasks.isScheduledOrInProgress",
        query = "SELECT updt FROM UpdateWorkerTask updt " +
                "WHERE (updt.status=0 OR " +
                "      (updt.status=1 AND updt.serverUUID IN (?1))) " +
                "AND updt.apiKeyId=?2"),
    @NamedQuery( name = "updateWorkerTasks.withObjectTypes.isScheduled",
		query = "SELECT updt FROM UpdateWorkerTask updt " +
                "WHERE (updt.status=0 OR " +
                "      (updt.status=1 AND updt.serverUUID IN (?1))) " +
			    "AND updt.objectTypes=?2 AND updt.apiKeyId=?3 " +
                "ORDER BY updt.timeScheduled DESC"),
	@NamedQuery( name = "updateWorkerTasks.completed",
		query = "SELECT updt FROM UpdateWorkerTask updt " +
                "WHERE updt.status=? " +
				"AND updt.updateType=? " +
                "AND updt.objectTypes=? " +
				"AND updt.apiKeyId=?"),
    @NamedQuery( name = "updateWorkerTasks.isInProgressOrScheduledBefore",
        query = "SELECT updt FROM UpdateWorkerTask updt " +
                "WHERE ((updt.status=1 AND updt.serverUUID IN (?2))" +
                    "OR (updt.status=0 " +
                        "AND updt.timeScheduled<?1))" +
                "AND updt.apiKeyId=?3 "),
    @NamedQuery( name = "updateWorkerTasks.getLastFinishedTask",
        query = "SELECT updt FROM UpdateWorkerTask updt " +
                "WHERE updt.timeScheduled<? " +
                "AND (updt.status=2 " +
                    "OR updt.status=3 " +
                    "OR updt.status=4) " +
                "AND updt.apiKeyId=? " +
                "ORDER BY updt.timeScheduled DESC")
})
public class UpdateWorkerTask extends AbstractEntity {

    public static class AuditTrailEntry {

        public AuditTrailEntry(final Date date, final String serverUUID) {
            this.date = date;
            this.serverUUID = serverUUID;
        }
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
        public String serverUUID;
    }

    @Index(name="serverUUID_index")
    public String serverUUID;
    @Index(name="apiKeyId_index")
    public Long apiKeyId;
    @Index(name="connectorName_index")
	public String connectorName;

    @Index(name="status_index")
	public Status status = Status.SCHEDULED;

    @Lob
    public String auditTrail;

    @Index(name="timeScheduled_index")
	public long timeScheduled;

	public static enum Status { SCHEDULED, IN_PROGRESS, DONE, FAILED, STALLED}

    @Index(name="updateType_index")
    public UpdateInfo.UpdateType updateType;

    @Index(name="guestId_index")
	public long guestId;
    @Index(name="objectTypes_index")
	public int objectTypes;
    @Index(name="retries_index")
	public int retries;
	public String jsonParams;
	
	public UpdateWorkerTask() {
    }

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
        sb.append("\\n").append(auditTrailEntry.date.toString());
        if (auditTrailEntry.serverUUID!=null) {
            sb.append(" - claimed by " + auditTrailEntry.serverUUID);
        } else {
            sb.append(" - reason: ")
                .append(auditTrailEntry.reason)
                .append(" - next action: ")
                .append(auditTrailEntry.nextAction);
            if (auditTrailEntry.stackTrace!=null)
                sb.append("\nstackTrack: \n")
                  .append(auditTrailEntry.stackTrace);
        }
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
