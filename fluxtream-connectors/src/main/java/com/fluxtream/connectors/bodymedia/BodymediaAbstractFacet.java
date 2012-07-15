package com.fluxtream.connectors.bodymedia;

import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;
import com.fluxtream.domain.AbstractFacet;

/**
 * <p>
 * <code>BodymediaAbstractFacet</code> does something...
 * </p>
 *
 * @author Prasanth Somasundar
 */
@MappedSuperclass
public abstract class BodymediaAbstractFacet extends AbstractFacet
{
    //The date that this facet represents
    public String date;
    public long lastSync = 0;
    @Lob
    public String json;

    @Override
    protected void makeFullTextIndexable() {}
}
