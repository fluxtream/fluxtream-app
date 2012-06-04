package com.fluxtream.connectors.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

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
    public String detailsTemplate() default "";
	
}
