package com.fluxtream.connectors.quantifiedmind;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import com.fluxtream.connectors.annotations.ObjectTypeSpec;
import com.fluxtream.connectors.lastfm.LastFmFacetExtractor;
import com.fluxtream.domain.AbstractFacet;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Entity(name="Facet_QuantifiedMindTest")
@ObjectTypeSpec(name = "test", value = 1, extractor=QuantifiedMindTestFacetExtractor.class, parallel=false, prettyname = "Test")
@NamedQueries({
    @NamedQuery(name = "quantifiedmind.test.deleteAll", query = "DELETE FROM Facet_QuantifiedMindTest facet WHERE facet.guestId=?"),
    @NamedQuery(name = "quantifiedmind.test.between", query = "SELECT facet FROM Facet_QuantifiedMindTest facet WHERE facet.guestId=? AND facet.start>=? AND facet.end<=?")
})
public class QuantifiedMindTestFacet extends AbstractFacet {

    public String test_name;
    public String result_name;
    public long session_timestamp;
    public double result_value;

    @Override
    protected void makeFullTextIndexable() {
        this.fullTextDescription = (new StringBuilder(test_name).append(" ").append(result_name)).toString();
    }
}
