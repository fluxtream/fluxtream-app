package com.fluxtream.connectors.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import com.fluxtream.connectors.location.LocationFacet;
import com.fluxtream.domain.DefaultPhotoFacetFinderStrategy;
import com.fluxtream.domain.PhotoFacetFinderStrategy;
import com.fluxtream.facets.extractors.AbstractFacetExtractor;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ObjectTypeSpec {

	public String name();
	public int value();
	public Class<? extends AbstractFacetExtractor> extractor() default AbstractFacetExtractor.class;
	boolean parallel() default false;
	boolean isImageType() default false;
	public String prettyname();
    boolean isDateBased() default false;
    public Class<? extends PhotoFacetFinderStrategy> photoFacetFinderStrategy() default DefaultPhotoFacetFinderStrategy.class;
    public LocationFacet.Source locationFacetSource() default LocationFacet.Source.NONE;
    public boolean isMixedType() default false;
}
