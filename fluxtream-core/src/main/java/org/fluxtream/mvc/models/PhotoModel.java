package org.fluxtream.mvc.models;

import org.fluxtream.connectors.vos.AbstractPhotoFacetVO;

public class PhotoModel {
    public String photoUrl;
    public String thumbnailUrl;
    public long timeTaken;
    public String textDescription;
    public String type = "photo";

    public PhotoModel(AbstractPhotoFacetVO facet){
        photoUrl = facet.photoUrl;
        thumbnailUrl = facet.getThumbnail(0);
        timeTaken = facet.start;
        textDescription = facet.description;
    }
}
