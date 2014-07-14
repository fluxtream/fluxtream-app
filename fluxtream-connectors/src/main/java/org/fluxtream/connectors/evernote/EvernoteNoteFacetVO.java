package org.fluxtream.connectors.evernote;

import org.fluxtream.core.OutsideTimeBoundariesException;
import org.fluxtream.core.TimeInterval;
import org.fluxtream.core.connectors.vos.AbstractInstantFacetVO;
import org.fluxtream.core.domain.GuestSettings;
import org.joda.time.format.ISODateTimeFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * User: candide
 * Date: 09/12/13
 * Time: 15:27
 */
public class EvernoteNoteFacetVO extends AbstractInstantFacetVO<EvernoteNoteFacet> {

    public String title;
    public String content;
    public String created;
    public String guid;
    public String notebookGuid;
    public long apiKeyId;
    public List<String> tagGuids;

    @Override
    protected void fromFacet(final EvernoteNoteFacet facet, final TimeInterval timeInterval, final GuestSettings settings)
            throws OutsideTimeBoundariesException {
        this.guid = facet.guid;
        this.notebookGuid = facet.notebookGuid;
        this.apiKeyId = facet.apiKeyId;
        if (facet.created!=null)
            created = ISODateTimeFormat.dateTimeNoMillis().print(facet.created);
        if (facet.htmlContent!=null) {
            content = facet.htmlContent;
        }
        title = facet.title;
        if (facet.getTagGuids()!=null)
            tagGuids = new ArrayList<String>(Arrays.asList(facet.getTagGuids()));
    }
}
