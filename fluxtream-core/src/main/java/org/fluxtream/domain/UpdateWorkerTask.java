package org.fluxtream.domain;

import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import org.fluxtream.connectors.updaters.UpdateInfo;
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
                "AND updt.status!=?"),
    @NamedQuery( name = "updateWorkerTasks.delete.scheduledByApi",
   		query = "DELETE FROM UpdateWorkerTask updt " +
                   "WHERE updt.apiKeyId=? " +
                   "AND updt.status=0"),
    @NamedQuery( name = "updateWorkerTasks.delete.scheduledByApiAndObjectType",
                  query = "DELETE FROM UpdateWorkerTask updt " +
                          "WHERE updt.apiKeyId= (?1) " +
                          "AND updt.status=0 " +
                          "AND updt.objectTypes= (?2)"),
    @NamedQuery( name = "updateWorkerTasks.delete.scheduledAndHistoryByApiAndObjectType",
                 query = "DELETE FROM UpdateWorkerTask updt " +
                         "WHERE updt.apiKeyId= (?1) " +
                         "AND (updt.status=0 OR updt.updateType=2) " +
                         "AND updt.objectTypes= (?2)"),
    @NamedQuery( name = "updateWorkerTasks.deleteAll.byApi",
                 query = "DELETE FROM UpdateWorkerTask updt " +
                         "WHERE updt.apiKeyId=?"),
	@NamedQuery( name = "updateWorkerTasks.byStatus",
		query = "SELECT updt FROM UpdateWorkerTask updt " +
                "WHERE updt.status=?1 " +
                "AND updt.timeScheduled<?2"),
    @NamedQuery( name = "updateWorkerTasks.byStatus.andName",
   		query = "SELECT updt FROM UpdateWorkerTask updt " +
                   "WHERE updt.status=?1 AND connectorName=?2 " +
                   "AND updt.timeScheduled<?3"),
    @NamedQuery( name = "updateWorkerTasks.all.synching",
                 query = "SELECT updt FROM UpdateWorkerTask updt " +
                         "WHERE updt.status=1 AND updt.serverUUID IN (?1)"),
    @NamedQuery( name = "updateWorkerTasks.all.scheduled",
                 query = "SELECT updt FROM UpdateWorkerTask updt " +
                         "WHERE updt.status=0"),
    @NamedQuery( name = "updateWorkerTasks.isScheduledOrInProgress",
        query = "SELECT updt FROM UpdateWorkerTask updt " +
                "WHERE (updt.status=0 OR " +
                "      (updt.status=1 AND updt.serverUUID IN (?1))) " +
                "AND updt.apiKeyId=?2"),
    @NamedQuery( name = "updateWorkerTasks.withObjectTypes",
                 query = "SELECT updt FROM UpdateWorkerTask updt " +
                         "WHERE updt.objectTypes=?1 AND updt.apiKeyId=?2 AND" +
                         "(updt.status in (0,2,3) OR " +
                         "(updt.status=1 AND updt.serverUUID IN (?3)))" +
                         "ORDER BY updt.timeScheduled DESC"),
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

    // The STALLED status should no longer be used
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
        //if (auditTrail==null) auditTrail = "";
        // always reset the audit trail to an empty string to accumulating too much cruft in the database
        if (auditTrail==null) auditTrail = "";
        //StringBuilder sb = new StringBuilder(auditTrail);
        StringBuilder sb = new StringBuilder();
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
