package com.fluxtream.connectors.evernote;

import java.util.Date;
import com.fluxtream.OutsideTimeBoundariesException;
import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.vos.AbstractInstantFacetVO;
import com.fluxtream.domain.GuestSettings;
import org.codehaus.jackson.annotate.JsonRawValue;
import org.joda.time.format.ISODateTimeFormat;

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

    @JsonRawValue
    public String resources;

    @Override
    protected void fromFacet(final EvernoteNoteFacet facet, final TimeInterval timeInterval, final GuestSettings settings)
            throws OutsideTimeBoundariesException {
        this.guid = facet.guid;
        startMinute = toMinuteOfDay(new Date(facet.start), timeInterval.getTimeZone(facet.start));
        if (facet.created!=null)
            created = ISODateTimeFormat.basicDateTimeNoMillis().print(facet.created);
        if (facet.htmlContent!=null) {
            content = facet.htmlContent;
        }
        title = facet.title;
    }
}
