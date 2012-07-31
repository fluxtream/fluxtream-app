package com.fluxtream.connectors.singly.github;

import java.util.Date;
import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.vos.AbstractFacetVO;
import com.fluxtream.connectors.vos.AbstractInstantFacetVO;
import com.fluxtream.domain.GuestSettings;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

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
        startMinute = toMinuteOfDay(new Date(facet.start), timeInterval.timeZone);
        this.start = facet.start;
        this.repoName = facet.repoName;
        this.repoURL = facet.repoURL;
        this.commitsJSON = facet.commitsJSON;
        JSONArray jsonCommits = JSONArray.fromObject(commitsJSON);
        if (jsonCommits!=null)
            this.description = jsonCommits.size() + " commits";
    }

}
