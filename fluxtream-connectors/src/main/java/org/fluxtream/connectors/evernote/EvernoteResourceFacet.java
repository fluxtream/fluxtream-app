package org.fluxtream.connectors.evernote;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import org.fluxtream.core.connectors.annotations.ObjectTypeSpec;
import org.hibernate.annotations.Index;

/**
 * User: candide
 * Date: 10/12/13
 * Time: 12:54
 */
@Entity(name="Facet_EvernoteResource")
@ObjectTypeSpec(name = "resource", value = 16, prettyname = "Resource", clientFacet = false)
@NamedQueries({
      @NamedQuery(name = "evernote.resources.byApiKeyIdAndNoteGuid", query = "SELECT facet FROM Facet_EvernoteResource facet WHERE facet.apiKeyId=? AND facet.noteGuid=?")
})
public class EvernoteResourceFacet extends EvernoteFacet {

    @Index(name="noteGuid")
    public String noteGuid;

    public Integer dataSize;
    public byte[] dataBodyHash;

    public Integer alternateDataSize;
    public byte[] alternateDataBodyHash;

    public Integer recognitionDataSize;
    public byte[] recognitionDataBodyHash;

    public Short width, height;
    public String mime;

    public String sourceURL;
    public Long timestamp;
    public Double longitude, latitude, altitude;
    public String cameraMake;
    public String cameraModel;
    public String fileName;
    public String recoType;
    public Boolean isAttachment;

    public EvernoteResourceFacet() {}

    public EvernoteResourceFacet(final long apiKeyId) {
        super (apiKeyId);
    }
}
