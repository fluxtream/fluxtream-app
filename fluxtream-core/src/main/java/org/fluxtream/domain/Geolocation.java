package org.fluxtream.domain;

import org.jetbrains.annotations.Nullable;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
public interface Geolocation {
    @Nullable
    Double getLatitude();

    @Nullable
    Double getLongitude();

    @Nullable
    Float getHeading();

    @Nullable
    String getHeadingRef();

    @Nullable
    Float getAltitude();

    @Nullable
    Integer getAltitudeRef();

    @Nullable
    Float getGpsPrecision();

    @Nullable
    String getGpsDatestamp();

    @Nullable
    String getGpsTimestamp();
}