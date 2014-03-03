package org.fluxtream.images;

import org.jetbrains.annotations.NotNull;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
public interface Image {
    @NotNull
    byte[] getBytes();

    int getWidth();

    int getHeight();
}
