package org.fluxtream.core.services.impl;

import org.fluxtream.core.auth.AuthHelper;
import org.fluxtream.core.domain.Post;
import org.fluxtream.core.domain.PostComment;
import org.fluxtream.core.services.BuddiesService;
import org.fluxtream.core.services.PostsService;
import org.fluxtream.core.services.exceptions.UnauthorizedAccessException;
import org.joda.time.DateTimeUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.List;

/**
 * Created by candide on 15/01/15.
 */
@Service
@Transactional(readOnly=true)
public class PostsServiceImpl implements PostsService {

    @Autowired
    BuddiesService buddiesService;

    @PersistenceContext
    EntityManager em;

    @Autowired
    BeanFactory beanFactory;

    @Override
    @Transactional(readOnly=false)
    /**
     * Post a message or recommendation on another user's wall
     * @param body Target guest's ID
     * @param targetGuestId Target guest's ID
     * @return the newly created Post
     * @throws UnauthorizedAccessException If the calling guest and the post's recipient aren't in a coach/coachee relationship
     */
    public Post createPost(final String body, final long toGuestId) throws UnauthorizedAccessException {
        final long callingGuestId = AuthHelper.getGuestId();
        if (!isCoacheeOf(toGuestId, callingGuestId)&&!isCoachOf(toGuestId, callingGuestId))
            throw new UnauthorizedAccessException();
        Post post = new Post();
        post.body = body;
        post.toGuestId = toGuestId;
        post.fromGuestId = callingGuestId;
        post.creationTime = DateTimeUtils.currentTimeMillis();
        post.lastUpdateTime = DateTimeUtils.currentTimeMillis();
        em.persist(post);
        return post;
    }

    private boolean isCoachOf(final long coachId, final long guestId) {

//        String json = beanFactory.getBean(FluxtreamApiInvocation.class)
//                .forEndpoint("/api/v1/guest/coachees")
//                .invokeSynchronously();
//        try {
//            JSONArray array = JSONArray.fromObject(json);
//        } catch (Throwable t) {
//            t.printStackTrace();
//            return new JSONArray();
//        }

//        final List<Guest> coaches = buddiesService.getCoaches(guestId);
//        for (Guest coach : coaches) {
//            if (coach.getId()==coachId)
//                return true;
//        }
//        return false;
        return true;
    }

    private boolean isCoacheeOf(final long coacheeId, final long guestId) {
//        final List<Guest> coachees = buddiesService.getCoachees(guestId);
//        for (Guest coachee : coachees) {
//            if (coachee.getId()==coacheeId)
//                return true;
//        }
//        return false;
        return true;
    }

    @Override
    @Transactional(readOnly=false)
    /**
     *
     * @param postId
     * @param body
     * @return
     * @throws UnauthorizedAccessException If the calling guest is not the original author of the post
     */
    public Post updatePost(final long postId, final String body) throws UnauthorizedAccessException {
        Post post = em.find(Post.class, postId);
        if (post.fromGuestId!=AuthHelper.getGuestId())
            throw new UnauthorizedAccessException();
        if (post==null) return null;
        post.body = body;
        post.lastUpdateTime = DateTimeUtils.currentTimeMillis();
        em.persist(post);
        return post;
    }

    @Override
    /**
     *
     * @param postId
     * @return
     * @throws UnauthorizedAccessException If the calling guest is neither the sender nor the recipient of the post
     */
    public Post getPost(final long postId) throws UnauthorizedAccessException {
        Post post = em.find(Post.class, postId);
        if (post.fromGuestId!=AuthHelper.getGuestId()&&post.toGuestId!=AuthHelper.getGuestId())
            throw new UnauthorizedAccessException();
        if (post==null) return null;
        return post;
    }

    @Override
    @Transactional(readOnly=false)
    /**
     *
     * @param postId If the calling guest is not the original author of the post
     * @throws UnauthorizedAccessException
     */
    public void deletePost(final long postId) throws UnauthorizedAccessException {
        Post post = em.find(Post.class, postId);
        if (post.fromGuestId!=AuthHelper.getGuestId())
            throw new UnauthorizedAccessException();
        if (post!=null) {
            em.remove(post);
        }
    }

    @Override
    @Transactional(readOnly=false)
    /**
     *
     * @param postId
     * @param body
     * @return
     * @throws UnauthorizedAccessException If the calling guest is neither the sender nor the receiver of the target post
     */
    public PostComment addComment(final long postId, final String body) throws UnauthorizedAccessException {
        Post post = em.find(Post.class, postId);
        final long authorGuestId = AuthHelper.getGuestId();
        if (post.fromGuestId!= authorGuestId &&post.toGuestId!= authorGuestId)
            throw new UnauthorizedAccessException();
        PostComment comment = new PostComment();
        comment.fromGuestId = authorGuestId;
        comment.toGuestId = post.fromGuestId==authorGuestId
                ? post.toGuestId
                : post.fromGuestId;
        comment.body = body;
        comment.creationTime = DateTimeUtils.currentTimeMillis();
        comment.lastUpdateTime = DateTimeUtils.currentTimeMillis();
        comment.post = post;
        em.persist(comment);
        post.comments.add(comment);
        post.lastUpdateTime = comment.lastUpdateTime;
        em.persist(post);
        return comment;
    }

    @Override
    @Transactional(readOnly=false)
    /**
     *
     * @param commentId
     * @param body
     * @return
     * @throws UnauthorizedAccessException If the calling guest is not the original author of the comment
     */
    public PostComment updatePostComment(final long commentId, final String body) throws UnauthorizedAccessException {
        PostComment comment = em.find(PostComment.class, commentId);
        if (comment==null)
            return null;
        if (comment.fromGuestId!=AuthHelper.getGuestId())
            throw new UnauthorizedAccessException();
        comment.body = body;
        comment.lastUpdateTime = DateTimeUtils.currentTimeMillis();
        em.persist(comment);
        Post post = em.find(Post.class, comment.post.getId());
        post.lastUpdateTime = comment.lastUpdateTime;
        return comment;
    }

    @Override
    /**
     *
     * @param commentId
     * @return
     * @throws UnauthorizedAccessException If the calling guest is neither the sender nor the receiver of the comment's post
     */
    public PostComment getPostComment(final long commentId) throws UnauthorizedAccessException {
        PostComment comment = em.find(PostComment.class, commentId);
        if (comment==null)
            return null;
        if (comment.fromGuestId!=AuthHelper.getGuestId()&&comment.toGuestId!=AuthHelper.getGuestId())
            throw new UnauthorizedAccessException();
        return comment;
    }

    @Override
    @Transactional(readOnly=false)
    /**
     *
     * @param postId
     * @throws UnauthorizedAccessException If the calling guest is not the original author of the comment
     */
    public void deletePostComment(final long commentId) throws UnauthorizedAccessException {
        PostComment comment = em.find(PostComment.class, commentId);
        if (comment!=null) {
            comment.post.comments.remove(comment);
            em.remove(comment);
        }
    }

    @Override
    /**
     *
     * @param coacheeId
     * @return
     * @throws UnauthorizedAccessException If the calling guest is not a coach of the guest with username `coacheeUsername`
     */
    public List<Post> getCoacheePosts(final long coacheeId, Long before, Integer count) throws UnauthorizedAccessException {
        final long coachId = AuthHelper.getGuestId();
        if (!isCoachOf(coachId, coacheeId))
            throw new UnauthorizedAccessException();

        TypedQuery<Post> query = em.createQuery("SELECT post FROM Post post " +
                        "WHERE (post.fromGuestId=? AND post.toGuestId=?) OR (post.fromGuestId=? AND post.toGuestId=?) " +
                        "ORDER BY post.lastUpdateTime DESC",
                Post.class);
        if (before!=null)
            query = em.createQuery("SELECT post FROM Post post " +
                            "WHERE (post.fromGuestId=? AND post.toGuestId=?) OR (post.fromGuestId=? AND post.toGuestId=?) " +
                            "AND post.id<?" +
                            "ORDER BY post.lastUpdateTime DESC",
                    Post.class);
        query.setParameter(1, coachId);
        query.setParameter(2, coacheeId);
        query.setParameter(3, coacheeId);
        query.setParameter(4, coachId);
        if (before!=null)
            query.setParameter(5, before);
        if (count!=null)
            query.setMaxResults(count);
        List<Post> posts = query.getResultList();
        return posts;
    }

    @Override
    /**
     *
     * @return
     */
    public List<Post> getOwnPosts(Long before, Integer count) {
        TypedQuery<Post> query = em.createQuery("SELECT post FROM Post post WHERE post.fromGuestId=? OR post.toGuestId=? ORDER BY post.lastUpdateTime DESC",
                Post.class);
        if (before!=null)
            query = em.createQuery("SELECT post FROM Post post WHERE (post.fromGuestId=? OR post.toGuestId=?) " +
                            "AND post.id<? ORDER BY post.lastUpdateTime DESC",
                    Post.class);
        final long guestId = AuthHelper.getGuestId();
        query.setParameter(1, guestId);
        query.setParameter(2, guestId);
        if (before!=null)
            query.setParameter(3, before);
        if (count!=null)
            query.setMaxResults(count);
        List<Post> posts = query.getResultList();
        return posts;
    }

}
