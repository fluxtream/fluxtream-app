package org.fluxtream.core.services;

import org.fluxtream.core.domain.Post;
import org.fluxtream.core.domain.PostComment;
import org.fluxtream.core.services.exceptions.UnauthorizedAccessException;

import java.util.List;

/**
 * Created by candide on 15/01/15.
 */
public interface PostsService {

    Post createPost(String body, long toGuestId) throws UnauthorizedAccessException;

    Post updatePost(long postId, String body) throws UnauthorizedAccessException;

    Post getPost(long postId) throws UnauthorizedAccessException;

    void deletePost(long postId) throws UnauthorizedAccessException;

    PostComment addComment(long postId, String body) throws UnauthorizedAccessException;

    PostComment updatePostComment(long commentId, String body) throws UnauthorizedAccessException;

    PostComment getPostComment(long commentId) throws UnauthorizedAccessException;

    void deletePostComment(long commentId) throws UnauthorizedAccessException;

    List<Post> getCoacheePosts(long coacheeId, Long before, Integer count) throws UnauthorizedAccessException;

    List<Post> getOwnPosts(Long before, Integer count);
}
