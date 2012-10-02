package com.fluxtream.domain;

import java.util.List;
import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.ObjectType;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
public interface PhotoFacetFinderStrategy {

    List<AbstractFacet> findAll(long guestId, Connector connector, ObjectType objectType, TimeInterval timeInterval);

    List<AbstractFacet> findBefore(long guestId, Connector connector, ObjectType objectType, long timeInMillis, int desiredCount);

    List<AbstractFacet> findAfter(long guestId, Connector connector, ObjectType objectType, long timeInMillis, int desiredCount);

    AbstractFacet findOldest(long guestId, Connector connector, ObjectType objectType);

    AbstractFacet findLatest(long guestId, Connector connector, ObjectType objectType);
}