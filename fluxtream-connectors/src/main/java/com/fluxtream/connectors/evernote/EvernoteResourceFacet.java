package com.fluxtream.connectors.evernote;

import javax.persistence.Entity;
import javax.persistence.Lob;
import com.fluxtream.connectors.annotations.ObjectTypeSpec;

/**
 * User: candide
 * Date: 10/12/13
 * Time: 12:54
 */
@Entity(name="Facet_EvernoteResource")
@ObjectTypeSpec(name = "resource", value = 16, prettyname = "Resource", clientFacet = false)
public class EvernoteResourceFacet extends EvernoteFacet {

    public String noteGuid;

    @Lob
    public byte[] dataBody;
    public Integer dataSize;
    public byte[] dataBodyHash;

    @Lob
    public byte[] alternateDataBody;
    public Integer alternateDataSize;
    public byte[] alternateDataBodyHash;

    @Lob
    public byte[] recognitionDataBody;
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
