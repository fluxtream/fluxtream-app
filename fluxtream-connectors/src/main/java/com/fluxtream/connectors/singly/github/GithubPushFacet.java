package com.fluxtream.connectors.singly.github;

import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import com.fluxtream.connectors.annotations.ObjectTypeSpec;
import com.fluxtream.domain.AbstractFacet;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Entity(name="Facet_GithubPush")
@ObjectTypeSpec(name = "push", value = 1, extractor= GithubPushFacetExtractor.class, parallel=true, prettyname = "Test")
@NamedQueries({
    @NamedQuery(name = "github.push.deleteAll", query = "DELETE FROM Facet_GithubPush facet WHERE facet.guestId=?"),
    @NamedQuery(name = "github.push.between", query = "SELECT facet FROM Facet_GithubPush facet WHERE facet.guestId=? AND facet.start>=? AND facet.end<=?")
})
public class GithubPushFacet extends AbstractFacet {

    @Lob
    public String commitsJSON;

    public String repoName;
    public String repoURL;

    @Override
    protected void makeFullTextIndexable() {
    }
}
