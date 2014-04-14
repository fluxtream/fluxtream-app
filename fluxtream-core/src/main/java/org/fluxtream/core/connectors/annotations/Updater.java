package org.fluxtream.core.connectors.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.fluxtream.core.connectors.Connector.UpdateStrategyType;
import org.fluxtream.core.connectors.DefaultSharedConnectorFilter;
import org.fluxtream.core.connectors.SharedConnectorFilter;
import org.fluxtream.core.connectors.bodytrackResponders.AbstractBodytrackResponder;
import org.fluxtream.core.connectors.bodytrackResponders.DefaultBodytrackResponder;
import org.fluxtream.core.domain.AbstractFacet;
import org.fluxtream.core.domain.AbstractUserProfile;
import org.fluxtream.core.facets.extractors.AbstractFacetExtractor;

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
