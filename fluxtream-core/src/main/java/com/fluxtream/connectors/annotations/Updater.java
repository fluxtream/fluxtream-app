package com.fluxtream.connectors.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.fluxtream.connectors.Connector.UpdateStrategyType;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.domain.AbstractUserProfile;
import com.fluxtream.facets.extractors.AbstractFacetExtractor;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Updater {
	
	int value();
	
	UpdateStrategyType updateStrategyType() default UpdateStrategyType.INCREMENTAL;
	
	String prettyName();
	
	Class <? extends AbstractFacet>[] objectTypes();
	
	public Class<? extends AbstractFacetExtractor> extractor() default AbstractFacetExtractor.class;
	
	public Class<? extends AbstractUserProfile> userProfile() default AbstractUserProfile.class;
	
	public boolean hasFacets() default true;
	
	public String[] additionalParameters() default {};

    public String[] defaultChannels() default {};

    public boolean isManageable() default true;
	
}
