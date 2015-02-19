package org.fluxtream.core.api;

import com.sun.jersey.api.Responses;
import com.wordnik.swagger.annotations.*;
import org.codehaus.jackson.map.ObjectMapper;
import org.fluxtream.core.Configuration;
import org.fluxtream.core.api.models.BasicGuestModel;
import org.fluxtream.core.api.models.PostCommentModel;
import org.fluxtream.core.api.models.PostModel;
import org.fluxtream.core.auth.AuthHelper;
import org.fluxtream.core.domain.Guest;
import org.fluxtream.core.domain.GuestDetails;
import org.fluxtream.core.domain.Post;
import org.fluxtream.core.domain.PostComment;
import org.fluxtream.core.services.GuestService;
import org.fluxtream.core.services.PostsService;
import org.fluxtream.core.services.exceptions.UnauthorizedAccessException;
import org.fluxtream.core.utils.Parse;
import org.fluxtream.core.utils.RestCallException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * Created by candide on 15/01/15.
 */
@Path("/v1/posts")
@Component("RESTPostsStore")
@Scope("request")
@Api(value = "/v1/posts", description = "CRUD operations on wall posts")
public class PostsStore {

    @Autowired
    PostsService postsService;

    @Autowired
    Configuration env;

    @Autowired
    GuestService guestService;

    @Autowired
    Parse parse;

    ObjectMapper objectMapper = new ObjectMapper();

    @POST
    @Path("/")
    @ApiOperation(value = "Post a message on another user's wall")
    @ApiResponses({
            @ApiResponse(code = 403, message = "If the calling guest and the post's recipient aren't in a coach/coachee relationship"),
            @ApiResponse(code = 404, message = "If there is no Guest with username `targetGuestUsername`")
    })
    public Response createPost(@ApiParam(value="The message of the post", required=true) @FormParam("message") final String message,
                               @ApiParam(value="Target guest's username", required=true) @FormParam("to") final String targetGuestUsername) throws URISyntaxException {
        try {
            Guest targetGuest = guestService.getGuest(targetGuestUsername);
            if (targetGuest==null)
                return Responses.notFound().entity("No such guest: " + targetGuestUsername).build();
            Post post = postsService.createPost(message, targetGuest.getId());
            URI createdURI = new URI(String.format("%sapi/v1/posts/%s", env.get("homeBaseUrl"), post.getId()));
            GuestDetails guestDetails = guestService.getGuestDetails(targetGuest.getId());
            notifyUser(post, guestDetails, "New Post", AuthHelper.getGuest().getGuestName() + " just sent you a message");
            return Response.created(createdURI).build();
        } catch (UnauthorizedAccessException e) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
    }

    private void notifyUser(Post post, GuestDetails guestDetails, String title, String alert) {
        Map<String,String> notificationParams = new HashMap<String,String>();
        notificationParams.put("postId", String.valueOf(post.getId()));
        notificationParams.put("title", title);
        try {
            final List<String> installations = guestService.getDeviceIds(guestDetails.guestId);
            parse.pushNotification(new HashSet<String>(installations), alert, notificationParams);
        } catch (RestCallException e) {
            e.printStackTrace();
        }
    }

    @PUT
    @Path("/{postId}")
    @ApiOperation(value = "Edit (the text message of) an existing post")
    @ApiResponses({
            @ApiResponse(code = 403, message = "If the calling guest is not the original author of the post"),
            @ApiResponse(code = 404, message = "If there is no post with id `postId`")
    })
    public Response updatePost(@ApiParam(value="Post message", required=true) @FormParam("message") final String body,
                               @ApiParam(value="Post ID", required=true) @PathParam("postId") final long postId) throws URISyntaxException {
        try {
            Post post = postsService.updatePost(postId, body);
            if (post==null)
                return Responses.notFound().entity("No such post: " + postId).build();
            URI createdURI = new URI(String.format("%srest/v1/posts/%s", env.get("homeBaseUrl"), post.getId()));
            return Response.created(createdURI).build();
        } catch (UnauthorizedAccessException e) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
    }

    @GET
    @Path("/{postId}")
    @ApiOperation(value = "Read a post", response = PostModel.class)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses({
            @ApiResponse(code = 403, message = "If the calling guest is neither the sender nor the recipient of the post"),
            @ApiResponse(code = 404, message = "If there is no post with id `postId`")
    })
    public Response readPost(@ApiParam(value="Post ID", required=true) @PathParam("postId") final long postId,
                             @ApiParam(value="Include comments?") @QueryParam("includeComments") final boolean includeComments) throws IOException {
        try {
            Post post = postsService.getPost(postId);
            if (post==null)
                return Responses.notFound().entity("No such post: " + postId).build();
            PostModel postModel = getPostModel(post, includeComments);
            return Response.ok(objectMapper.writeValueAsString(postModel)).build();
        } catch (UnauthorizedAccessException e) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
    }

    private PostModel getPostModel(Post post, boolean includeComments) {
        final PostModel postModel = new PostModel(post, env.get("referenceTimezone"));
        // let's locally cache the to and from guests to avoid useless database roundtrips
        Map<Long, Guest> guestCache = new HashMap<Long,Guest>();
        if (post.fromGuestId!= AuthHelper.getGuestId()) {
            final Guest guest = getGuest(guestCache, post.fromGuestId);
            final GuestDetails details = guest!=null ? guestService.getGuestDetails(guest.getId()) : null;
            postModel.from = new BasicGuestModel(guest, details);
        }
        if (post.toGuestId!= AuthHelper.getGuestId()) {
            final Guest guest = getGuest(guestCache, post.toGuestId);
            final GuestDetails details = guest!=null ? guestService.getGuestDetails(guest.getId()) : null;
            postModel.to = new BasicGuestModel(guest, details);
        }
        if (includeComments) {
            if (post.comments!=null&&post.comments.size()>0) {
                postModel.comments = new ArrayList<PostCommentModel>();
                for (PostComment postComment : post.comments) {
                    PostCommentModel comment = getPostCommentModel(guestCache, postComment);
                    postModel.comments.add(comment);
                }
            }
        }
        return postModel;
    }

    private PostCommentModel getPostCommentModel(Map<Long, Guest> guestCache, PostComment postComment) {
        PostCommentModel comment = new PostCommentModel(postComment, env.get("referenceTimezone"));
        if (postComment.fromGuestId!= AuthHelper.getGuestId()) {
            final Guest guest = getGuest(guestCache, postComment.fromGuestId);
            final GuestDetails details = guest!=null ? guestService.getGuestDetails(guest.getId()) : null;
            comment.from = new BasicGuestModel(guest, details);
        }
        if (postComment.toGuestId!= AuthHelper.getGuestId()) {
            final Guest guest = getGuest(guestCache, postComment.toGuestId);
            final GuestDetails details = guest!=null ? guestService.getGuestDetails(guest.getId()) : null;
            comment.to = new BasicGuestModel(guest, details);
        }
        return comment;
    }

    private Guest getGuest(Map<Long, Guest> guestCache, long id) {
        if (!guestCache.containsKey(id)) {
            Guest guest = guestService.getGuestById(id);
            if (guest==null) return null;
            guestCache.put(id, guest);
        }
        return guestCache.get(id);
    }

    @DELETE
    @Path("/{postId}")
    @ApiOperation(value = "Delete a post")
    @ApiResponses({
            @ApiResponse(code = 403, message = "If the calling guest is not the original author of the post"),
            @ApiResponse(code = 404, message = "If there is no post with id `postId`")
    })
    public Response deletePost(@ApiParam(value="Post ID", required=true) @PathParam("postId") final long postId) {
        try {
            Post post = postsService.getPost(postId);
            if (post==null)
                return Responses.notFound().entity("No such post: " + postId).build();
            postsService.deletePost(postId);
            return Response.noContent().build();
        } catch (UnauthorizedAccessException e) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
    }

    @POST
    @Path("/{postId}/comments")
    @ApiOperation(value = "Add a comment on a post")
    @ApiResponses({
            @ApiResponse(code = 403, message = "If the calling guest is neither the sender nor the receiver of the target post"),
            @ApiResponse(code = 404, message = "If there is no post with id `postId`")
    })
    public Response createComment(@ApiParam(value="Post Id", required=true) @PathParam("postId") final long postId,
                                  @ApiParam(value="Comment message", required=true) @FormParam("message") final String message) throws URISyntaxException {
        try {
            Post post = postsService.getPost(postId);
            if (post==null)
                return Responses.notFound().entity("No such post: " + postId).build();
            PostComment postComment = postsService.addComment(postId, message);
            notifyUser(post, guestService.getGuestDetails(postComment.toGuestId), "New Comment", AuthHelper.getGuest().getGuestName() + " just sent you a message");
            URI createdURI = new URI(String.format("%srest/v1/posts/%s/comments/%s", env.get("homeBaseUrl"), postComment.post.getId(), postComment.getId()));
            return Response.created(createdURI).build();
        } catch (UnauthorizedAccessException e) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
    }

    @PUT
    @Path("/{postId}/comments/{commentId}")
    @ApiOperation(value = "Edit an existing comment")
    @ApiResponses({
            @ApiResponse(code = 400, message = "If the comment with id `commentId` doesn't have a post with id `postId`"),
            @ApiResponse(code = 403, message = "If the calling guest is not the original author of the comment"),
            @ApiResponse(code = 404, message = "If there is no comment with id `commentId`")
    })
    public Response updateComment(@ApiParam(value="Post Id", required=true) @PathParam("postId") final long postId,
                                  @ApiParam(value="Comment ID", required=true) @PathParam("commentId") final long commentId,
                                  @ApiParam(value="Comment message", required=true) @FormParam("message") final String message) throws URISyntaxException {
        try {
            PostComment postComment = postsService.updatePostComment(commentId, message);
            if (postComment==null)
                return Responses.notFound().entity("No such comment: " + commentId).build();
            if (postComment.post.getId()!=postId)
                return Response.status(Response.Status.BAD_REQUEST).build();
            URI createdURI = new URI(String.format("%srest/v1/posts/%s/comments/%s", env.get("homeBaseUrl"), postComment.post.getId(), postComment.getId()));
            return Response.created(createdURI).build();
        } catch (UnauthorizedAccessException e) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
    }

    @GET
    @Path("/{postId}/comments/{commentId}")
    @ApiOperation(value = "Read a comment", response = PostCommentModel.class)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses({
            @ApiResponse(code = 400, message = "If the comment with id `commentId` doesn't have a post with id `postId`"),
            @ApiResponse(code = 403, message = "If the calling guest is neither the sender nor the receiver of the comment's post"),
            @ApiResponse(code = 404, message = "If there is no comment with id `commentId`")
    })
    public Response readComment(@ApiParam(value="Post ID", required=true) @PathParam("postId") final long postId,
                                @ApiParam(value="Comment ID", required=true) @PathParam("commentId") final long commentId) throws IOException {
        try {
            PostComment postComment = postsService.getPostComment(commentId);
            if (postComment==null)
                return Responses.notFound().entity("No such comment: " + commentId).build();
            if (postComment.post.getId()!=postId)
                return Response.status(Response.Status.BAD_REQUEST).build();
            PostCommentModel postCommentModel = getPostCommentModel(new HashMap<Long,Guest>(), postComment);
            return Response.ok(objectMapper.writeValueAsString(postCommentModel)).build();
        } catch (UnauthorizedAccessException e) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
    }

    @DELETE
    @Path("/{postId}/comments/{commentId}")
    @ApiOperation(value = "Delete a comment")
    @ApiResponses({
            @ApiResponse(code = 400, message = "If the comment with id `commentId` doesn't have a post with id `postId`"),
            @ApiResponse(code = 403, message = "If the calling guest is not the original author of the comment"),
            @ApiResponse(code = 404, message = "If there is no comment with id `commentId`")
    })
    public Response deleteComment(@ApiParam(value="Post ID", required=true) @PathParam("postId") final long postId,
                                  @ApiParam(value="Comment ID", required=true) @PathParam("commentId") final long commentId) {
        try {
            PostComment postComment = postsService.getPostComment(commentId);
            if (postComment==null)
                return Responses.notFound().entity("No such comment: " + commentId).build();
            if (postComment.post.getId()!=postId)
                return Response.status(Response.Status.BAD_REQUEST).build();
            postsService.deletePostComment(commentId);
            return Response.noContent().build();
        } catch (UnauthorizedAccessException e) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
    }

    @GET
    @Path("/all/{coacheeUsername}")
    @ApiOperation(value = "Retrieve a coachee's wall. Only the posts to and from the calling coach will be shown.",
            response = PostModel.class, responseContainer = "Array")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses({
            @ApiResponse(code = 403, message = "If the calling guest is not a coach of the guest with username `coacheeUsername`"),
            @ApiResponse(code = 404, message = "If there is no coachee with that username")
    })
    public Response getCoacheeWall(@ApiParam(value="The username of the coachee", required=true) @PathParam("coacheeUsername") final String coacheeUsername,
                                   @ApiParam(value="Include comments?") @QueryParam("includeComments") final boolean includeComments,
                                   @ApiParam(value="The id of the last post that was part of the previous query", required=false) @QueryParam(value="before") Long before,
                                   @ApiParam(value="The wanted number of posts", required=false) @QueryParam(value="count") Integer count) throws IOException {
        Guest coachee = guestService.getGuest(coacheeUsername);
        if (coachee==null)
            return Responses.notFound().entity("No such coachee: " + coacheeUsername).build();
        try {
            List<Post> posts = postsService.getCoacheePosts(coachee.getId(), before, count);
            if (posts.size()==0)
                return Response.ok("[]").build();
            List<PostModel> postModels = getPostModels(includeComments, posts);
            return Response.ok(objectMapper.writeValueAsString(postModels)).build();
        } catch (UnauthorizedAccessException e) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
    }

    @GET
    @Path("/all")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Retrieve the calling guest's wall.", response = PostModel.class, responseContainer="Array")
    public Response getOwnWall(@ApiParam(value="Include comments?") @QueryParam("includeComments") final boolean includeComments,
                               @ApiParam(value="The id of the last post that was part of the previous query", required=false) @QueryParam(value="before") Long before,
                               @ApiParam(value="The wanted number of posts", required=false) @QueryParam(value="count") Integer count) throws IOException {
        List<Post> posts = postsService.getOwnPosts(before, count);
        if (posts.size()==0)
            return Response.ok("[]").build();
        List<PostModel> postModels = getPostModels(includeComments, posts);
        return Response.ok(objectMapper.writeValueAsString(postModels)).build();
    }

    private List<PostModel> getPostModels(boolean includeComments, List<Post> posts) {
        List<PostModel> postModels = new ArrayList<PostModel>();
        for (Post post : posts) {
            PostModel model = getPostModel(post, includeComments);
            postModels.add(model);
        }
        return postModels;
    }

}