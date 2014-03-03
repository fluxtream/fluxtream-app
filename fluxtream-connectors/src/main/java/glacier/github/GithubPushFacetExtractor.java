package glacier.github;

import java.util.ArrayList;
import java.util.List;
import org.fluxtream.ApiData;
import org.fluxtream.connectors.ObjectType;
import org.fluxtream.connectors.updaters.UpdateInfo;
import org.fluxtream.domain.AbstractFacet;
import org.fluxtream.facets.extractors.AbstractFacetExtractor;
import org.fluxtream.services.GuestService;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Component
public class GithubPushFacetExtractor extends AbstractFacetExtractor {

    @Autowired
    GuestService guestService;

    DateTimeFormatter dateTimeFormatter =
            DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    @Override
    public List<AbstractFacet> extractFacets(final UpdateInfo updateInfo, final ApiData apiData,
                                             final ObjectType objectType) throws Exception {
        List<AbstractFacet> facets = new ArrayList<AbstractFacet>();

        String login = guestService.getApiKeyAttribute(apiData.updateInfo.apiKey, "login");

        JSONArray eventsArray = JSONArray.fromObject(apiData.json);
        for(int i=0; i<eventsArray.size(); i++) {
            JSONObject eventJson = eventsArray.getJSONObject(i);
            JSONObject eventData = eventJson.getJSONObject("data");
            if (eventData==null) continue;
            if (eventData.getString("type").equals("DataReceivedEvent")) {
                GithubPushFacet facet = new GithubPushFacet(apiData.updateInfo.apiKey.getId());

                JSONObject payload = eventData.getJSONObject("payload");

                if (payload==null) continue;

                JSONObject actor = eventData.getJSONObject("actor");

                if (actor==null||!actor.getString("login").equals(login))
                    continue;

                this.extractCommonFacetData(facet, apiData);
                String timestamp = eventData.getString("created_at");
                facet.start = dateTimeFormatter.parseDateTime(timestamp).getMillis();
                facet.end = facet.start;

                if (payload.has("commits")) {
                    final JSONArray commits = payload.getJSONArray("commits");
                    facet.commitsJSON = commits.toString();
                } else
                    facet.commitsJSON = "{}";


                JSONObject repo = eventData.getJSONObject("repo");

                if (repo!=null) {
                    facet.repoName = repo.getString("name");
                    facet.repoURL = repo.getString("url");
                }

                facets.add(facet);

            }
        }

        return facets;
    }
}
