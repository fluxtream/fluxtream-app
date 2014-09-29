package org.fluxtream.core.api;

import com.wordnik.swagger.annotations.*;
import org.apache.commons.lang.StringUtils;
import org.fluxtream.core.auth.AuthHelper;
import org.fluxtream.core.services.ApiDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * User: candide
 * Date: 02/07/13
 * Time: 14:11
 */
@Path("/v1/comments")
@Component("RESTCommentsController")
@Api(value = "/comments", description = "Set/delete user comments on individual facets")
@Scope("request")
public class CommentsController {

    @Autowired
    ApiDataService apiDataService;

    /**
     * Set a facet's comment field
     * @param facetType the facet's type as formatted in the digest, i.e. connector name + (optional) "-" + object type name
     * @param facetId the id in the facet table
     * @param comment the text to set the facet's comment to
     * @return
     */
    @POST
    @Path("/{facetType}/{facetId}")
    @Produces({MediaType.TEXT_PLAIN})
    @ApiOperation(value = "Set a facet's user comment")
    @ApiResponses({
            @ApiResponse(code=200, message = "Comment was set")
    })
    public Response setComment(@ApiParam(value="The type (<connectorName>-<objectTypeName>) of the facet", required=true) @PathParam("facetType") String facetType,
                                  @ApiParam(value="Facet ID", required=true) @PathParam("facetId") long facetId,
                                  @ApiParam(value="Comment", required=true) @FormParam("comment") String comment) {
        final long guestId = AuthHelper.getGuestId();
        String connectorName, objectTypeName = null;
        if (facetType.indexOf("-")==-1)
            connectorName = facetType;
        else {
            final String[] splits = StringUtils.split(facetType, "-");
            connectorName = splits[0];
            objectTypeName = splits[1];
        }
        try {
            apiDataService.setComment(connectorName, objectTypeName, guestId, facetId, comment);
        } catch (RuntimeException e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
        return Response.ok("Comment was set").build();
    }

    /**
     * Set a facet's comment field to null
     * @param facetType the facet's type as formatted in the digest, i.e. connector name + (optional) "-" + object type name
     * @param facetId the id in the facet table
     * @return
     */
    @DELETE
    @Path("/{facetType}/{facetId}")
    @ApiOperation(value = "Delete a facet's user comment", response = String.class)
    @ApiResponses({
            @ApiResponse(code=200, message = "Comment was removed")
    })
    @Produces({MediaType.TEXT_PLAIN})
    public Response deleteComment(@ApiParam(value="The type (<connectorName>-<objectTypeName>) of the facet", required=true) @PathParam("facetType") String facetType,
                                     @ApiParam(value="Facet ID", required=true) @PathParam("facetId") long facetId) {
        final long guestId = AuthHelper.getGuestId();
        String connectorName, objectTypeName = null;
        if (facetType.indexOf("-")==-1)
            connectorName = facetType;
        else {
            final String[] splits = StringUtils.split(facetType, "-");
            connectorName = splits[0];
            objectTypeName = splits[1];
        }
        try {
            apiDataService.deleteComment(connectorName, objectTypeName, guestId, facetId);
        } catch (RuntimeException e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
        return Response.ok("Comment was removed").build();
    }

}
