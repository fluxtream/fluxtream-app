package com.fluxtream.connectors.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.fluxtream.connectors.vos.AbstractFacetVOCollection;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface JsonFacetCollection {

	@SuppressWarnings("rawtypes")
	Class<? extends AbstractFacetVOCollection> value();
	
	String name() default "default";
	
}
