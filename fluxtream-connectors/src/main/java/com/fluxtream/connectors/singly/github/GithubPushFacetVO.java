package com.fluxtream.connectors.singly.github;

import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.vos.AbstractFacetVO;
import com.fluxtream.connectors.vos.AbstractInstantFacetVO;
import com.fluxtream.domain.GuestSettings;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
public class GithubPushFacetVO extends AbstractInstantFacetVO<GithubPushFacet> {

    public String commitsJSON;

    public String repoName;
    public String repoURL;

    @Override
    protected void fromFacet(final GithubPushFacet facet, final TimeInterval timeInterval, final GuestSettings settings) {
        this.repoName = facet.repoName;
        this.repoURL = facet.repoURL;
        this.commitsJSON = facet.commitsJSON;
    }

}
