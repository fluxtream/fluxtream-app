package glacier.github;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.fluxtream.core.TimeInterval;
import org.fluxtream.core.connectors.vos.AbstractInstantFacetVO;
import org.fluxtream.core.domain.GuestSettings;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
public class GithubPushFacetVO extends AbstractInstantFacetVO<GithubPushFacet> {

    public String repoName;
    public String repoURL;

    @Override
    protected void fromFacet(final GithubPushFacet facet, final TimeInterval timeInterval, final GuestSettings settings) {
        this.repoName = facet.repoName;
        this.repoURL = facet.repoURL;
        JSONArray jsonCommits = JSONArray.fromObject(facet.commitsJSON);
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<jsonCommits.size(); i++) {
            JSONObject jsonObject = jsonCommits.getJSONObject(i);
            if (i>0) sb.append(", ");
            sb.append(jsonObject.getString("message"));
        }
        this.description = sb.toString();
    }

}
