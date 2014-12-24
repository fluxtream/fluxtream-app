package org.fluxtream.core.connectors.vos;

import org.fluxtream.core.OutsideTimeBoundariesException;
import org.fluxtream.core.TimeInterval;
import org.fluxtream.core.domain.AbstractFacet;
import org.fluxtream.core.domain.AbstractLocalTimeFacet;
import org.fluxtream.core.domain.FacetComment;
import org.joda.time.DateTimeZone;
import org.joda.time.format.ISODateTimeFormat;

import java.util.TimeZone;

/**
 * Created by candide on 24/12/14.
 */
public class FacetCommentVO<T extends AbstractFacet> {

    public String created, updated;
    public Author author;
    public String body;

    public class Author {
        String username, firstname, lastname;
    }

    public FacetCommentVO(FacetComment facetComment, T facet, TimeInterval timeInterval) {
        if (facetComment.guest!=null) {
            author = new Author();
            author.username = facetComment.guest.username;
            author.firstname = facetComment.guest.firstname;
            author.firstname = facetComment.guest.lastname;
        }
        body = facetComment.body;
        TimeZone facetTimeZone = null;
        try {
            if (facet instanceof AbstractLocalTimeFacet) {
                AbstractLocalTimeFacet ltFacet = (AbstractLocalTimeFacet) facet;
                facetTimeZone = timeInterval.getTimeZone(ltFacet.date);
            } else {
                facetTimeZone = timeInterval.getTimeZone(facet.start);
            }
        } catch (OutsideTimeBoundariesException e) {e.printStackTrace();}
        if (facetTimeZone==null)
            facetTimeZone = TimeZone.getTimeZone("UTC");
        created = ISODateTimeFormat.time().withZone(DateTimeZone.forTimeZone(facetTimeZone)).print(facetComment.created);
        updated = ISODateTimeFormat.time().withZone(DateTimeZone.forTimeZone(facetTimeZone)).print(facetComment.updated);
    }

}
