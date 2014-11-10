package org.fluxtream.core.domain;

import javax.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class AbstractUserProfile extends AbstractEntity {

	public long apiKeyId;

}
