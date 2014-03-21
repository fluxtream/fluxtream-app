package glacier.github;

import java.util.Date;
import org.fluxtream.TimeInterval;
import org.fluxtream.connectors.vos.AbstractInstantFacetVO;
import org.fluxtream.domain.GuestSettings;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
public class GithubPushFacetVO extends AbstractInstantFacetVO<GithubPushFacet> {

    public String repoName;
    public String repoURL;

    @Override
    protected void fromFacet(final GithubPushFacet facet, final TimeInterval timeInterval, final GuestSettings settings) {
        startMinute = toMinuteOfDay(new Date(facet.start), timeInterval.getMainTimeZone());
        this.start = facet.start;
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
