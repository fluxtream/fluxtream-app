package glacier.github;

import javax.persistence.Entity;
import javax.persistence.Lob;
import org.fluxtream.core.connectors.annotations.ObjectTypeSpec;
import org.fluxtream.core.domain.AbstractFacet;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Entity(name="Facet_GithubPush")
@ObjectTypeSpec(name = "push", value = 1, extractor= GithubPushFacetExtractor.class, parallel=true, prettyname = "Test")
public class GithubPushFacet extends AbstractFacet {

    @Lob
    public String commitsJSON;

    public String repoName;
    public String repoURL;

    public GithubPushFacet() {
        super();
    }

    public GithubPushFacet(final long apiKeyId) {
        super(apiKeyId);
    }

    @Override
    protected void makeFullTextIndexable() {
    }
}
