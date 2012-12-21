package com.fluxtream.connectors.fluxtream_capture;

import java.io.File;
import com.fluxtream.Configuration;
import com.fluxtream.services.ApiDataService;
import com.fluxtream.services.JPADaoService;
import com.google.gson.Gson;
import org.apache.log4j.Logger;
import org.bodytrack.datastore.FilesystemKeyValueStore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * <p>
 * <code>FluxtreamCapturePhotoStore</code> enables managment of Fluxtream Capture photos.
 * </p>
 *
 * @author Chris Bartley (bartley@cmu.edu)
 */
@Component
public final class FluxtreamCapturePhotoStore {

    private static final Logger LOG = Logger.getLogger(FluxtreamCapturePhotoStore.class);
    private static final Logger LOG_DEBUG = Logger.getLogger("Fluxtream");

    public enum Operation {
        CREATED("created"), UPDATED("updated");

        private final String name;

        Operation(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public interface OperationResult<T> {
        @NotNull
        Operation getOperation();

        @NotNull
        T getData();
    }

    public interface Photo {
        byte[] getPhotoBytes();

        /**
         * Returns the timestamp, in millis, that the photo was last updated.  Returns <code>null</code> if unknown.
         */
        @Nullable
        Long getLastUpdatedTimestamp();

        /**
         * A {@link String} representation of the unique identifier for this photo.  Useful for logging and messages.
         */
        @NotNull
        String getIdentifier();
    }

    @Autowired
    private ApiDataService apiDataService;

    @Autowired
    protected JPADaoService jpaDaoService;

    @Autowired
    private Configuration env;

    private final Gson gson = new Gson();

    private FluxtreamCapturePhotoStore() {
        // private to prevent instantiation
    }

    /**
     * Returns the photo specified by the given <code>photoStoreKey</code> or <code>null</code> if no such photo exists.
     * This method assumes that the caller has already performed authentication and authorization.
     */
    @Nullable
    public Photo getPhoto(@Nullable final String photoStoreKey) throws StorageException {
        if (photoStoreKey != null) {
            final byte[] bytes = getFilesystemKeyValueStore().get(photoStoreKey);

            if (bytes != null) {
                return new Photo() {
                    @Override
                    public byte[] getPhotoBytes() {
                        return bytes;
                    }

                    @Override
                    public Long getLastUpdatedTimestamp() {
                        return null;
                    }

                    @NotNull
                    @Override
                    public String getIdentifier() {
                        return photoStoreKey;
                    }
                };
            }
        }

        return null;
    }

    /**
     * Returns the photo thumbnail specified by the given <code>photoId</code> or <code>null</code> if no such photo
     * exists. This method assumes that the caller has already performed authentication and authorization.
     */
    @Nullable
    public Photo getPhotoThumbnail(final long photoId, final int thumbnailIndex) {
        final FluxtreamCapturePhotoFacet photoFacet = jpaDaoService.findOne("fluxtream_capture.photo.byId", FluxtreamCapturePhotoFacet.class, photoId);

        if (photoFacet != null) {
            return new Photo() {
                @Override
                public byte[] getPhotoBytes() {
                    return (thumbnailIndex == 1) ? photoFacet.getThumbnailLarge() : photoFacet.getThumbnailSmall();
                }

                @Override
                public Long getLastUpdatedTimestamp() {
                    return photoFacet.timeUpdated;
                }

                @NotNull
                @Override
                public String getIdentifier() {
                    return photoId + "/" + thumbnailIndex;
                }
            };
        }

        return null;
    }

    /**
     * Saves the given photo for the given user to both the database and the Fluxtream Capture key-value photo store.
     * Returns an {@link OperationResult} if the photo was created or updated (see the
     * {@link OperationResult#getOperation()} method to determine which occurred), or throws a
     * {@link FluxtreamCapturePhotoStoreException} exception if the save/update failed.
     *
     * @throws InvalidDataException if the photo or its metadata is <code>null</code>, empty, or in some way invalid
     * @throws StorageException if the save/update fails to either the photo key-value store or the database
     * @throws UnsupportedImageFormatException if the save/update fails because the given image is not of a supported format
     */
    @SuppressWarnings("ConstantConditions")
    public OperationResult<FluxtreamCapturePhoto> saveOrUpdatePhoto(final long guestId, @NotNull final byte[] photoBytes, @NotNull final String jsonMetadata) throws StorageException, InvalidDataException, UnsupportedImageFormatException {
        if (LOG_DEBUG.isDebugEnabled()) {
            LOG_DEBUG.debug("FluxtreamCapturePhotoStore.savePhoto(" + guestId + ", " + photoBytes.length + ", " + jsonMetadata + ")");
        }

        // do simple null and empty validation
        if (photoBytes == null || photoBytes.length <= 0 || jsonMetadata == null || jsonMetadata.length() <= 0) {
            final String message = "Photo upload failed because the byte array and JSON metadata for the photo must both be non-null and non-empty";
            LOG.error("FluxtreamCapturePhotoStore.saveOrUpdatePhoto(): " + message);
            throw new InvalidDataException(message);
        }

        // Go ahead and try to create the FilesystemKeyValueStore.  This is a simple operation, so, if it's going
        // to fail, then it's better to fail now rather than after spending a lot of effort creating the photo hash
        // and thumbnails.
        final FilesystemKeyValueStore keyValueStore = getFilesystemKeyValueStore();

        // Attempt to parse the JSON metadata
        final PhotoUploadMetadata metadata;
        try {
            metadata = gson.fromJson(jsonMetadata, PhotoUploadMetadata.class);
        }
        catch (Exception e) {
            final String message = "Photo upload failed because an Exception occurred while trying to parse the photo metadata";
            LOG.error("FluxtreamCapturePhotoStore.saveOrUpdatePhoto(): " + message);
            throw new InvalidDataException(message);
        }

        // Validate the JSON metadata
        if (metadata == null || !metadata.isValid()) {
            final String message = "Photo upload failed because the JSON metadata is null or invalid";
            LOG.error("FluxtreamCapturePhotoStore.saveOrUpdatePhoto(): " + message);
            throw new InvalidDataException(message);
        }

        // Now that we know the metadata is valid, we convert the capture time from seconds to milliseconds
        final long captureTimeMillisUtc = (long)(metadata.capture_time_secs_utc * 1000);

        // Create the FluxtreamCapturePhoto (this validates the photo, generates the hash and thumbnails, etc.)
        final FluxtreamCapturePhoto photo;
        try {
            photo = new FluxtreamCapturePhoto(guestId, photoBytes, captureTimeMillisUtc);
        }
        catch (UnsupportedImageFormatException e) {
            LOG.error("FluxtreamCapturePhotoStore.saveOrUpdatePhoto(): Photo upload failed because an UnsupportedOperationException occurred while trying to create the FluxtreamCapturePhoto");
            throw e;
        }
        catch (Exception e) {
            final String message = "Photo upload failed because an Exception occurred while trying to create the FluxtreamCapturePhoto";
            LOG.error("FluxtreamCapturePhotoStore.saveOrUpdatePhoto(): " + message, e);
            throw new InvalidDataException(message, e);
        }

        // Now that we have the key-value store created and everything appears to be valid, we can go ahead and
        // insert the photo into the photo key-value store, but only if the key doesn't already exist
        final String photoStoreKey = photo.getPhotoStoreKey();
        if (!keyValueStore.hasKey(photoStoreKey)) {
            if (!keyValueStore.set(photoStoreKey, photo.getPhotoBytes())) {
                final String message = "Photo upload failed because the photo could not be saved to the key-value store";
                LOG.error("FluxtreamCapturePhotoStore.saveOrUpdatePhoto(): " + message);
                throw new StorageException(message);
            }
        }

        // The photo is in the key-value store, so try to save or update to the DB
        final PhotoCreatorOrModifier photoCreatorOrModifier = new PhotoCreatorOrModifier(photo);
        final FluxtreamCapturePhotoFacet photoFacet;
        try {
            photoFacet = apiDataService.createOrReadModifyWrite(FluxtreamCapturePhotoFacet.class, photoCreatorOrModifier.getFacetFinderQuery(), photoCreatorOrModifier);
        }
        catch (Exception e) {
            final String message = "Photo upload failed because an Exception occurred while writing the photo to the database";
            LOG.error("FluxtreamCapturePhotoStore.saveOrUpdatePhoto(): " + message, e);
            throw new StorageException(message, e);
        }

        if (photoFacet == null) {
            // attempt to remove the photo from the key-value store
            keyValueStore.delete(photoStoreKey);

            final String message = "Upload failed because the ApiDataService failed to save the facet and returned null";
            LOG.error("FluxtreamCapturePhotoStore.saveOrUpdatePhoto(): " + message);
            throw new StorageException(message);
        }

        // If we got this far, then we know everything succeeded, so simply return the boolean to indicate whether
        // the photo was created or updated
        if (LOG_DEBUG.isInfoEnabled() || LOG.isInfoEnabled()) {
            final String message = "FluxtreamCapturePhotoStore.saveOrUpdatePhoto(): photo [" + photoFacet.getHash() + "] " + (photoCreatorOrModifier.wasCreated() ? "saved" : "updated") + " sucessfully for user [" + guestId + "]";
            LOG.info(message);
            LOG_DEBUG.info(message);
        }

        return new OperationResult<FluxtreamCapturePhoto>() {
            @NotNull
            @Override
            public Operation getOperation() {
                return photoCreatorOrModifier.wasCreated() ? Operation.CREATED : Operation.UPDATED;
            }

            @NotNull
            @Override
            public FluxtreamCapturePhoto getData() {
                return photo;
            }
        };
    }

    @NotNull
    private FilesystemKeyValueStore getFilesystemKeyValueStore() throws StorageException {
        try {
            final File keyValueStoreLocation = new File(env.targetEnvironmentProps.getString("btdatastore.db.location"));
            return new FilesystemKeyValueStore(keyValueStoreLocation);
        }
        catch (IllegalArgumentException e) {
            final String message = "The photo key-value store could not be created";
            LOG.error("FluxtreamCapturePhotoStore.getFilesystemKeyValueStore(): " + message, e);
            throw new StorageException(message, e);
        }
    }

    private static class PhotoUploadMetadata {
        private double capture_time_secs_utc = -1;

        public boolean isValid() {
            return capture_time_secs_utc >= 0;
        }
    }

    private static final class PhotoCreatorOrModifier implements ApiDataService.FacetModifier<FluxtreamCapturePhotoFacet> {
        @NotNull
        private final ApiDataService.FacetQuery facetFinderQuery;
        @NotNull
        private final FluxtreamCapturePhoto photo;
        private boolean wasCreated;

        public PhotoCreatorOrModifier(@NotNull final FluxtreamCapturePhoto photo) {
            this.photo = photo;
            facetFinderQuery = new ApiDataService.FacetQuery("e.guestId = ? AND e.hash = ? AND e.start = ?", photo.getGuestId(), photo.getPhotoHash(), photo.getCaptureTimeMillisUtc());
        }

        @Override
        public FluxtreamCapturePhotoFacet createOrModify(final FluxtreamCapturePhotoFacet existingFacet) {

            if (existingFacet == null) {
                wasCreated = true;
                return new FluxtreamCapturePhotoFacet(photo);
            }
            else {
                wasCreated = false;

                // We already have this photo, so we don't need to do anything other than update the timeUpdated field
                // and return the existingFacet we found
                existingFacet.timeUpdated = System.currentTimeMillis();

                return existingFacet;
            }
        }

        public boolean wasCreated() {
            return wasCreated;
        }

        @NotNull
        public ApiDataService.FacetQuery getFacetFinderQuery() {
            return facetFinderQuery;
        }
    }

    public static abstract class FluxtreamCapturePhotoStoreException extends Exception {
        protected FluxtreamCapturePhotoStoreException(final String s) {
            super(s);
        }

        protected FluxtreamCapturePhotoStoreException(final String s, final Throwable throwable) {
            super(s, throwable);
        }
    }

    public static class StorageException extends FluxtreamCapturePhotoStoreException {
        protected StorageException(final String s) {
            super(s);
        }

        protected StorageException(final String s, final Throwable throwable) {
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

    public static final class UnsupportedImageFormatException extends FluxtreamCapturePhotoStoreException {
        protected UnsupportedImageFormatException(final String s) {
            super(s);
        }

        protected UnsupportedImageFormatException(final String s, final Throwable throwable) {
            super(s, throwable);
        }
    }
}
