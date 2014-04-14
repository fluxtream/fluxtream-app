package glacier.toodledo;

import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import org.hibernate.annotations.Index;
import org.hibernate.search.annotations.Indexed;

import org.fluxtream.core.connectors.annotations.ObjectTypeSpec;
import org.fluxtream.core.domain.AbstractFacet;

@Entity(name="Facet_ToodledoGoal")
@ObjectTypeSpec(name = "goal", value = 2, isImageType=false, prettyname = "Goals")
@NamedQueries({
	@NamedQuery(name = "toodledo.goal.all", query = "SELECT facet FROM Facet_ToodledoGoal facet WHERE facet.guestId=? ORDER BY facet.start DESC"),
	@NamedQuery(name = "toodledo.goal.deleteAll", query = "DELETE FROM Facet_ToodledoGoal facet WHERE facet.guestId=?"),
	@NamedQuery(name = "toodledo.goal.between", query = "SELECT facet FROM Facet_ToodledoGoal facet WHERE facet.guestId=? AND facet.start>=? AND facet.end<=?")
})
@Indexed
public class ToodledoGoalFacet extends AbstractFacet {
	
	@Index(name="toodledo_id")
	public long toodledo_id;
	
	public String name;
	public int level;
	public byte archived;
	public long contributes;
	
	@Lob
	public String note;

    public ToodledoGoalFacet(final long apiKeyId) {
        super(apiKeyId);
    }

    @Override
	protected void makeFullTextIndexable() {
		StringBuilder sb = new StringBuilder(name);
		if (note!=null) sb.append(" ").append(note);
		this.fullTextDescription = sb.toString();
	}
	
}
