package org.fluxtream.connectors.evernote;

import java.util.ArrayList;
import java.util.List;
import org.fluxtream.connectors.SharedConnectorFilter;
import org.fluxtream.domain.AbstractFacet;
import org.fluxtream.domain.SharedConnector;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.springframework.stereotype.Component;

/**
 * User: candide
 * Date: 19/02/14
 * Time: 16:42
 */
@Component
public class EvernoteSharedConnectorFilter implements SharedConnectorFilter {

    @Override
    public <T extends AbstractFacet> List<T> filterFacets(final SharedConnector sharedConnector, final List<T> facets) {
        if (sharedConnector.filterJson==null)
            return facets;
        JSONObject json = JSONObject.fromObject(sharedConnector.filterJson);
        final JSONArray notebooks = json.getJSONArray("notebooks");
        List<String> sharedNotebookGuids = new ArrayList<String>();
        for (int i=0; i<notebooks.size(); i++) {
            JSONObject notebook = notebooks.getJSONObject(i);
            boolean shared = notebook.getBoolean("shared");
            if (shared)
                sharedNotebookGuids.add(notebook.getString("guid"));
        }
        List<T> filteredFacets = new ArrayList<T>();
        for (T facet : facets) {
            if (facet instanceof EvernoteNoteFacet) {
                if(sharedNotebookGuids.contains(((EvernoteNoteFacet)facet).notebookGuid))
                    filteredFacets.add(facet);
            }
        }
        return filteredFacets;
    }

}
