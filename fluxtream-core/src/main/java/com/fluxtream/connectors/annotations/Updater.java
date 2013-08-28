package com.fluxtream.connectors.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import com.fluxtream.connectors.Connector.UpdateStrategyType;
import com.fluxtream.connectors.bodytrackResponders.AbstractBodytrackResponder;
import com.fluxtream.connectors.bodytrackResponders.DefaultBodytrackResponder;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.domain.AbstractUserProfile;
import com.fluxtream.facets.extractors.AbstractFacetExtractor;

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

}
