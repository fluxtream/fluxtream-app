package org.fluxtream.core.api;

import com.google.gson.Gson;
import org.fluxtream.core.mvc.models.StatusModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * <p>
 * <code>JsonResponseHelper</code> helps create JSON {@link Response}s.  The response body consists of a JSON
 * representation of an appropriate {@link StatusModel}.
 * </p>
 *
 * @author Chris Bartley (bartley@cmu.edu)
 */
@Component
public final class JsonResponseHelper {

    private final Gson gson = new Gson();

    /**
     * Creates a {@link Response} with an HTTP 400 Bad Request status code and a JSON body consisting of a failure
     * {@link StatusModel} containing the given <code>message</code>.
     *
     * @see StatusModel#failure(String)
     */
    @NotNull
    public Response badRequest(@Nullable final String message) {
        return buildResponse(Response.Status.BAD_REQUEST, message);
    }

    /**
     * Creates a {@link Response} with an HTTP 404 Not Found status code and a JSON body consisting of a failure
     * {@link StatusModel} containing the given <code>message</code>.
     *
     * @see StatusModel#failure(String)
     */
    @NotNull
    public Response notFound(@Nullable final String message) {
        return buildResponse(Response.Status.NOT_FOUND, message);
    }

    /**
     * Creates a {@link Response} with an HTTP 403 Forbidden status code and a JSON body consisting of a failure
     * {@link StatusModel} containing the given <code>message</code>.
     *
     * @see StatusModel#failure(String)
     */
    @NotNull
    public Response forbidden(@Nullable final String message) {
        return buildResponse(Response.Status.FORBIDDEN, message);
    }

    /**
     * Creates a {@link Response} with an HTTP 415 Unsupported Media Type status code and a JSON body consisting of a
     * failure {@link StatusModel} containing the given <code>message</code>.
     *
     * @see StatusModel#failure(String)
     */
    @NotNull
    public Response unsupportedMediaType(@Nullable final String message) {
        return buildResponse(Response.Status.UNSUPPORTED_MEDIA_TYPE, message);
    }

    /**
     * Creates a {@link Response} with an HTTP 500 Internal Server Error status code and a JSON body consisting of a
     * failure {@link StatusModel} containing the given <code>message</code>.
     *
     * @see StatusModel#failure(String)
     */
    @NotNull
    public Response internalServerError(@Nullable final String message) {
        return buildResponse(Response.Status.INTERNAL_SERVER_ERROR, message);
    }

    @NotNull
    private Response buildResponse(@NotNull final Response.Status status, @NotNull final String message) {
        return Response.status(status).entity(message).build();
    }

    /**
     * Creates a {@link Response} with an HTTP 200 OK status code and a JSON body consisting of a success
     * {@link StatusModel} containing the given <code>message</code> and <code>payload</code>.
     *
     * @see StatusModel#success(String, Object)
     */
    @NotNull
    public Response ok(@Nullable final Object payload) {
        return buildResponse(Response.Status.OK, payload);
    }

    @NotNull
    private Response buildResponse(@NotNull final Response.Status status, @NotNull final Object object) {
        return Response.status(status).entity(gson.toJson(object)).type(MediaType.APPLICATION_JSON).build();
    }
}
