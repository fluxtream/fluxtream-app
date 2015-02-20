package org.fluxtream.connectors.fluxtream_capture;

import org.fluxtream.core.connectors.annotations.ObjectTypeSpec;
import org.fluxtream.core.domain.AbstractFacet;

import javax.persistence.Entity;

/**
 * Created by candide on 16/02/15.
 */
@Entity(name="Facet_FluxtreamCaptureTopic")
@ObjectTypeSpec(name = "topic", value = 4, isImageType=false, parallel=false, prettyname = "Topic", clientFacet = false)
public class FluxtreamTopicFacet extends AbstractFacet {

    // NotNull
    public String fluxtreamId;

    public String name;
    public int topicNumber;

    public FluxtreamTopicFacet() {}

    public FluxtreamTopicFacet(long apiKeyId) {
        super(apiKeyId);
    }

    @Override
    protected void makeFullTextIndexable() {

    }
}
