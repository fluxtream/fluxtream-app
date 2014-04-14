package org.fluxtream.core.api;

import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.fluxtream.core.auth.AuthHelper;
import org.fluxtream.core.mvc.models.StatusModel;
import org.fluxtream.core.services.ApiDataService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * User: candide
 * Date: 02/07/13
 * Time: 14:11
 */
@Path("/comments")
@Component("RESTCommentsController")
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
    @Produces({MediaType.APPLICATION_JSON})
    public StatusModel setComment(@PathParam("facetType") String facetType,
                                  @PathParam("facetId") long facetId,
                                  @FormParam("comment") String comment) {
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
            return new StatusModel(false, e.getMessage());
        }
        return new StatusModel(true, "Comment was set");
    }

    /**
     * Set a facet's comment field to null
     * @param facetType the facet's type as formatted in the digest, i.e. connector name + (optional) "-" + object type name
     * @param facetId the id in the facet table
     * @return
     */
    @DELETE
    @Path("/{facetType}/{facetId}")
    @Produces({MediaType.APPLICATION_JSON})
    public StatusModel deleteComment(@PathParam("facetType") String facetType,
                                     @PathParam("facetId") long facetId) {
        final long guestId = AuthHelper.getGuestId();
        String connectorName, objectTypeName = null;
        if (facetType.indexOf("-")!=-1)
            connectorName = facetType;
        else {
            final String[] splits = StringUtils.split(facetType, "-");
            connectorName = splits[0];
            objectTypeName = splits[1];
        }
        try {
            apiDataService.deleteComment(connectorName, objectTypeName, guestId, facetId);
        } catch (RuntimeException e) {
            return new StatusModel(false, e.getMessage());
        }
        return new StatusModel(true, "Comment was removed");
    }

}
