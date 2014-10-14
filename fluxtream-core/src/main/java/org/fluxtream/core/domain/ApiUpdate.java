package org.fluxtream.core.domain;

import com.google.gson.annotations.Expose;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;

import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

@Entity(name="ApiUpdates")
@NamedQueries ( {
	@NamedQuery(name = "apiUpdates.count.all.since",
		query = "SELECT COUNT(updt) FROM ApiUpdates updt " +
                "WHERE updt.api=? AND updt.ts>?"),
	@NamedQuery(name = "apiUpdates.count.byGuest.since",
		query = "SELECT COUNT(updt) FROM ApiUpdates updt WHERE updt.guestId=? AND updt.api=? AND updt.ts>?"),
	@NamedQuery(name = "apiUpdates.count.all",
		query = "SELECT COUNT(updt) FROM ApiUpdates updt WHERE updt.api=?"),
	@NamedQuery(name = "apiUpdates.count.byGuest",
		query = "SELECT COUNT(updt) FROM ApiUpdates updt WHERE updt.guestId=? AND updt.api=?"),
	@NamedQuery(name = "apiUpdates.delete.all",
		query = "DELETE FROM ApiUpdates updt WHERE updt.guestId=?"),
	@NamedQuery(name = "apiUpdates.delete.byApiKey",
		query = "DELETE FROM ApiUpdates updt WHERE updt.apiKeyId=?"),
    @NamedQuery( name="apiUpdates.last.paged",
                 query="SELECT updt FROM ApiUpdates updt WHERE updt.apiKeyId=? ORDER BY updt.ts DESC"),
    @NamedQuery( name="apiUpdates.last",
		query="SELECT updt FROM ApiUpdates updt WHERE updt.apiKeyId=? ORDER BY updt.ts DESC LIMIT 10"),
	@NamedQuery( name="apiUpdates.last.successful.byApi",
		query="SELECT updt FROM ApiUpdates updt WHERE updt.apiKeyId=? and updt.success=true ORDER BY updt.ts DESC"),
	@NamedQuery( name="apiUpdates.last.successful.byApiAndObjectTypes",
		query="SELECT updt FROM ApiUpdates updt WHERE updt.apiKeyId=? and updt.objectTypes=? and updt.success=true ORDER BY updt.ts DESC")})

public class ApiUpdate extends AbstractEntity {

    /**
     * Legacy (we need it for the existing user data)
     */
	@Index(name="guestId")
	public long guestId;

    @Expose
	@Index(name="ts")
	public long ts;

    @Expose
    @Index(name="elapsed")
	public long elapsed;

    @Index(name="apiKeyId")
    public Long apiKeyId;

    @Index(name="httpResponseCode")
    @Expose
    public Integer httpResponseCode;

    @Expose
    public String reason;

    /**
     * Legacy (we need it for the existing user data)
     */
	@Index(name="api")
	public int api;

    @Expose
	@Index(name="objectTypes")
	public int objectTypes;

    @Expose
    @Index(name="success")
	@Type(type="yes_no")
	public boolean success;

    @Lob
    @Expose
    public String query;
}
