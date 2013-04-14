package com.fluxtream.domain;

import javax.persistence.MappedSuperclass;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

@MappedSuperclass
public abstract class AbstractLocalTimeFacet extends AbstractFacet {

    public String date;

	public static DateTimeFormatter timeStorageFormat = DateTimeFormat.forPattern(
			"yyyy-MM-dd'T'HH:mm:ss.SSS");

    public AbstractLocalTimeFacet(final long apiKeyId) {
        super(apiKeyId);
    }

    public AbstractLocalTimeFacet() {
        super();
    }

}


