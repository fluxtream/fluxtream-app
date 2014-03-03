package org.fluxtream.connectors.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.fluxtream.connectors.Connector.UpdateStrategyType;
import org.fluxtream.connectors.DefaultSharedConnectorFilter;
import org.fluxtream.connectors.SharedConnectorFilter;
import org.fluxtream.connectors.bodytrackResponders.AbstractBodytrackResponder;
import org.fluxtream.connectors.bodytrackResponders.DefaultBodytrackResponder;
import org.fluxtream.domain.AbstractFacet;
import org.fluxtream.domain.AbstractUserProfile;
import org.fluxtream.facets.extractors.AbstractFacetExtractor;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Updater {

    // dummy empty class meant to let settings have a default
    static class EmptySettings {}

	int value();
	
	UpdateStrategyType updateStrategyType() default UpdateStrategyType.INCREMENTAL;
	
	String prettyName();
	
	Class <? extends AbstractFacet>[] objectTypes();
	
	public Class<? extends AbstractFacetExtractor> extractor() default AbstractFacetExtractor.class;
	
	public Class<? extends AbstractUserProfile> userProfile() default AbstractUserProfile.class;
	
	public boolean hasFacets() default true;

    public String[] defaultChannels() default {};

    public Class<? extends Object> settings() default EmptySettings.class;

    public Class<? extends AbstractBodytrackResponder> bodytrackResponder() default DefaultBodytrackResponder.class;

    public int[] deleteOrder() default {-1};

    public Class<? extends SharedConnectorFilter> sharedConnectorFilter() default DefaultSharedConnectorFilter.class;

}
