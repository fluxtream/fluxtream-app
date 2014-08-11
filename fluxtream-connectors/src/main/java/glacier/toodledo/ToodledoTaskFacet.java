package glacier.toodledo;

import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import org.hibernate.annotations.Index;
import org.hibernate.search.annotations.Indexed;

import org.fluxtream.core.connectors.annotations.ObjectTypeSpec;
import org.fluxtream.core.domain.AbstractFacet;

@Entity(name="Facet_ToodledoTask")
@ObjectTypeSpec(name = "task", value = 1, isImageType=false, prettyname = "Tasks")
@NamedQueries({
	@NamedQuery(name = "toodledo.task.byToodledoId", query = "SELECT facet FROM Facet_ToodledoTask facet WHERE facet.guestId=? AND facet.toodledo_id=?"),
	@NamedQuery(name = "toodledo.task.all", query = "SELECT facet FROM Facet_ToodledoTask facet WHERE facet.guestId=? ORDER BY facet.start DESC"),
	@NamedQuery(name = "toodledo.task.deleteAll", query = "DELETE FROM Facet_ToodledoTask facet WHERE facet.guestId=?"),
	@NamedQuery(name = "toodledo.task.between", query = "SELECT facet FROM Facet_ToodledoTask facet WHERE facet.guestId=? AND facet.start>=? AND facet.end<=?")
})
@Indexed
public class ToodledoTaskFacet extends AbstractFacet {

	@Index(name="toodledo_id")
	public long toodledo_id;
	public String title;
	public String tag;
	public long folder;
	public long context;
	public long goal;
	public long location;
	public long parent;
	public int children;
	public int _order;
	public long duedate;
	public byte duedatemod;
	public long startdate;
	public long duetime;
	public long starttime;
	public int remind;
	public String _repeat;
	public int repeatfrom;
	public byte status;
	public int _length;
	public byte _priority;
	public byte star;
	public long modified;
	public long completed;
	public long added;
	public long _timer;
	public long timeron;
	@Lob
	public String note;
	public String meta;

    public ToodledoTaskFacet(final long apiKeyId) {
        super(apiKeyId);
    }

    @Override
	protected void makeFullTextIndexable() {
		StringBuilder sb = new StringBuilder(title);
		if (note!=null) sb.append(" ").append(note);
		if (tag!=null) sb.append(" ").append(tag);
		this.fullTextDescription = sb.toString();
	}
	
}
