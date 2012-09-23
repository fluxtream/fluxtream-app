package com.fluxtream.domain;

import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import org.hibernate.annotations.Index;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Entity(name = "SharedConnectors")
public class SharedConnector extends AbstractEntity {

    @ManyToOne
    public SharingBuddy buddy;

    @Index(name = "connectorName")
    public String connectorName;

    @Lob
    public String filterJson;

}
