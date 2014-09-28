package org.fluxtream.core.domain;

import javax.persistence.MappedSuperclass;
import org.hibernate.annotations.Index;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

@MappedSuperclass
public abstract class AbstractLocalTimeFacet extends AbstractFacet {

    @Index(name = "date")
    public String date;
    public String startTimeStorage;
    public String endTimeStorage;

	public static DateTimeFormatter timeStorageFormat = DateTimeFormat.forPattern(
			"yyyy-MM-dd'T'HH:mm:ss.SSS");

    public AbstractLocalTimeFacet(final long apiKeyId) {
        super(apiKeyId);
    }

    public AbstractLocalTimeFacet() {
        super();
    }

}


