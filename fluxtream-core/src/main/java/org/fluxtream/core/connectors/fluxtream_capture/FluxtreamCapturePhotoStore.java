package org.fluxtream.core.connectors.fluxtream_capture;

import java.io.File;
import org.fluxtream.core.Configuration;
import org.fluxtream.core.images.ImageType;
import org.fluxtream.core.services.ApiDataService;
import org.fluxtream.core.services.JPADaoService;
import org.fluxtream.core.utils.ImageUtils;
import com.google.gson.Gson;
import org.fluxtream.core.aspects.FlxLogger;
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

    private static final FlxLogger LOG = FlxLogger.getLogger(FluxtreamCapturePhotoStore.class);
    private static final FlxLogger LOG_DEBUG = FlxLogger.getLogger("Fluxtream");

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

        @Nullable
        Long getDatabaseRecordId();

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

        /**
         * Returns the {@link ImageType} for this photo.
         */
        @NotNull
        ImageType getImageType();
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

                    @NotNull
                    @Override
                    public ImageType getImageType() {
                        // Try to read the image type.  If we can't for some reason, then just lie and say it's a JPEG.
                        // This really should never happen, but it's good to check for it anyway and log a warning if it
                        // happens.
                        ImageType imageType = ImageUtils.getImageType(bytes);
                        if (imageType == null) {
                            imageType = ImageType.JPEG;
                            LOG.warn("FluxtreamCapturePhotoStore.getImageType(): Could not determine the media type for photo [" + getIdentifier() + "]!  Defaulting to [" + imageType.getMediaType() + "]");
                        }

                        return imageType;
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
    public Photo getPhotoThumbnail(final long uid, final long photoId, final int thumbnailIndex) {
        final FluxtreamCapturePhotoFacet photoFacet = jpaDaoService.findOne("fluxtream_capture.photo.byId", FluxtreamCapturePhotoFacet.class, uid, photoId);

        if (photoFacet != null) {
            return new Photo() {
                @Override
                public byte[] getPhotoBytes() {
                    return photoFacet.getThumbnail(thumbnailIndex);
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

                @NotNull
                @Override
                public ImageType getImageType() {
                    return ImageType.JPEG;   // thumbnails are always JPEGs
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
    public OperationResult<FluxtreamCapturePhoto> saveOrUpdatePhoto(final long guestId, @NotNull final byte[] photoBytes, @NotNull final String jsonMetadata, final Long apiKeyId) throws StorageException, InvalidDataException, UnsupportedImageFormatException {
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
        final FluxtreamCapturePhoto.PhotoUploadMetadata metadata;
        try {
            metadata = gson.fromJson(jsonMetadata, FluxtreamCapturePhoto.PhotoUploadMetadata.class);
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

        // Create the FluxtreamCapturePhoto (this validates the photo, generates the hash and thumbnails, etc.)
        final FluxtreamCapturePhoto photo;
        try {
            photo = new FluxtreamCapturePhoto(guestId, photoBytes, metadata);
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
            photoFacet = apiDataService.createOrReadModifyWrite(FluxtreamCapturePhotoFacet.class, photoCreatorOrModifier.getFacetFinderQuery(), photoCreatorOrModifier, apiKeyId);
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

            @Nullable
            @Override
            public Long getDatabaseRecordId() {
                return photoFacet.getId();
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
        public FluxtreamCapturePhotoFacet createOrModify(final FluxtreamCapturePhotoFacet existingFacet, final Long apiKeyId) {

            if (existingFacet == null) {
                wasCreated = true;
                return new FluxtreamCapturePhotoFacet(photo, apiKeyId);
            }
            else {
                wasCreated = false;

                // We already have this photo, so we don't need to do anything other than update the timeUpdated field.
                // We ignore the comments and tags fields here because the client should use the metadata set method
                // instead.
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
