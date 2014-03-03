package org.fluxtream.connectors.quantifiedmind;

import javax.persistence.Entity;
import org.fluxtream.connectors.annotations.ObjectTypeSpec;
import org.fluxtream.domain.AbstractFacet;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Entity(name="Facet_QuantifiedMindTest")
@ObjectTypeSpec(name = "test", value = 1, extractor=QuantifiedMindTestFacetExtractor.class, parallel=false, prettyname = "Test")
public class QuantifiedMindTestFacet extends AbstractFacet {

    public String test_name;
    public String result_name;
    public long session_timestamp;
    public double result_value;

    public QuantifiedMindTestFacet() {
        super();
    }

    public QuantifiedMindTestFacet(final long apiKeyId) {
        super(apiKeyId);
    }

    @Override
    protected void makeFullTextIndexable() {
        this.fullTextDescription = (new StringBuilder(test_name).append(" ").append(result_name)).toString();
    }
}
