package com.fluxtream.connectors.fluxtream_capture;

import com.fluxtream.services.ApiDataService;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * <p>
 * <code>FluxtreamCapturePhotoStore</code> enables managment of Fluxtream Capture photos.
 * </p>
 *
 * @author Chris Bartley (bartley@cmu.edu)
 */
@Component
@Scope(value = "singleton")
public final class FluxtreamCapturePhotoStore {

    private static final Logger LOG = Logger.getLogger("Fluxtream");

    public static class FluxtreamCapturePhotoStoreException extends Exception {
        protected FluxtreamCapturePhotoStoreException(final String s) {
            super(s);
        }

        protected FluxtreamCapturePhotoStoreException(final String s, final Throwable throwable) {
            super(s, throwable);
        }
    }

    public static final class InvalidDataException extends FluxtreamCapturePhotoStoreException {
        protected InvalidDataException(final String s) {
            super(s);
        }

        protected InvalidDataException(final String s, final Throwable throwable) {
            super(s, throwable);
        }
    }

    @Autowired
    protected ApiDataService apiDataService;

    private final Gson gson = new Gson();

    private FluxtreamCapturePhotoStore() {
        // private to prevent instantiation
    }

    /**
     * Saves the given photo for the given user to both the database and the Fluxtream Capture key-value photo store.
     * Returns <code>true</code> if the photo was saved, <code>false</code> if it was updated, or throws a
     * {@link FluxtreamCapturePhotoStoreException} exception if the save/update failed.
     *
     * @throws InvalidDataException if the <code>photoBytes</code> is <code>null</code> or empty or
     * if the <code>jsonMetadata</code> is <code>null</code> or invalid
     * @throws FluxtreamCapturePhotoStoreException if the save/update fails for any other reason
     */
    @SuppressWarnings("ConstantConditions")
    public boolean saveOrUpdatePhoto(final long guestId,
                                     @NotNull final byte[] photoBytes,
                                     @NotNull final String jsonMetadata) throws FluxtreamCapturePhotoStoreException {
        LOG.debug("FluxtreamCapturePhotoStore.savePhoto(" + guestId + ", " + photoBytes.length + ", " + jsonMetadata + ")");

        if (photoBytes != null && photoBytes.length > 0 && jsonMetadata != null && jsonMetadata.length() > 0) {
            PhotoUploadMetadata metadata;
            try {
                metadata = gson.fromJson(jsonMetadata, PhotoUploadMetadata.class);

                if (metadata != null && metadata.isValid()) {
                    LOG.debug("BodyTrackController.savePhoto(): metadata.capture_time = " + metadata.capture_time);

                    final String photoHash = FluxtreamCapturePhotoFacet.computeHash(photoBytes);
                    final long captureTimeMillis = metadata.capture_time;
                    final boolean[] wasCreated = new boolean[1];
                    final FluxtreamCapturePhotoFacet photoFacet = apiDataService.createOrReadModifyWrite(FluxtreamCapturePhotoFacet.class,
                                                                                                         new ApiDataService.FacetQuery("e.guestId = ? AND e.hash = ? AND e.start = ?", guestId, photoHash, captureTimeMillis),
                                                                                                         new ApiDataService.FacetModifier<FluxtreamCapturePhotoFacet>() {
                        @Override
                        public FluxtreamCapturePhotoFacet createOrModify(final FluxtreamCapturePhotoFacet existingFacet) {

                            if (existingFacet == null) {
                                wasCreated[0] = true;
                                return new FluxtreamCapturePhotoFacet(guestId, photoBytes, captureTimeMillis, photoHash);
                            }
                            else {
                                wasCreated[0] = false;

                                // we already have this photo, so we don't need to do
                                // anything other than return the existingFacet we found
                                return existingFacet;
                            }
                        }
                    });

                    if (photoFacet == null) {
                        final String message = "Upload failed because the ApiDataService failed to save the facet and returned null";
                        LOG.error("FluxtreamCapturePhotoStore.saveOrUpdatePhoto(): " + message);
                        throw new FluxtreamCapturePhotoStoreException(message);
                    }
                    else {
                        LOG.debug("FluxtreamCapturePhotoStore.saveOrUpdatePhoto(): photo [" + photoFacet.hash + "] " + (wasCreated[0] ? "saved" : "updated") + " sucessfully for user [" + guestId + "]");
                        return wasCreated[0];
                    }
                }
                else {
                    LOG.error("FluxtreamCapturePhotoStore.saveOrUpdatePhoto(): Upload failed because the metadata is null or invalid");
                    throw new InvalidDataException("The JSON metadata is invalid");
                }
            }
            catch (JsonSyntaxException e) {
                LOG.error("FluxtreamCapturePhotoStore.saveOrUpdatePhoto(): JsonSyntaxException while trying to parse the photo metadata: " + e);
                throw new InvalidDataException("The JSON metadata is invalid", e);
            }
            catch (Exception e) {
                final String message = "Upload failed because because an exception occurred while trying to save the photo";
                LOG.error("FluxtreamCapturePhotoStore.saveOrUpdatePhoto(): " + message, e);
                throw new FluxtreamCapturePhotoStoreException(message, e);
            }
        }
        else {
            throw new InvalidDataException("The byte array and JSON metadata for the photo must both be non-null and non-empty");
        }
    }

    private static class PhotoUploadMetadata {
        private long capture_time = -1;

        public boolean isValid() {
            return capture_time >= 0;
        }
    }
}
