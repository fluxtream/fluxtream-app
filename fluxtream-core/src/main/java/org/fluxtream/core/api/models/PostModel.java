package org.fluxtream.core.api.models;

import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;
import org.fluxtream.core.domain.Post;
import org.joda.time.DateTimeZone;
import org.joda.time.format.ISODateTimeFormat;

import java.util.List;

/**
 * User: candide
 * Date: 27/06/14
 * Time: 21:32
 */
@ApiModel("A generic Wall Post")
public class PostModel {

    @ApiModelProperty(value = "The Post's ID", required = true)
    public long id;

    @ApiModelProperty(value = "Last time this post was edited, in ISO8601 format (yyyy-MM-dd'T'HH:mm:ss'Z')", required = true)
    public String lastUpdateTime;
    @ApiModelProperty(value = "Date/Time this post was created, in ISO8601 format (yyyy-MM-dd'T'HH:mm:ss'Z')", required = true)
    public String creationTime;
    @ApiModelProperty(value = "The text body of the post", required = true)
    public String body;
    @ApiModelProperty(value = "The originating guest of this post (only present if it is different from the calling guest)", required = false)
    public BasicGuestModel from;
    @ApiModelProperty(value = "The destination guest of this post (only present if it is different from the calling guest)", required = false)
    public BasicGuestModel to;

    @ApiModelProperty(value = "The post's comments", required = false)
    public List<PostCommentModel> comments;

    public PostModel(Post post, String referenceTimezone) {
        if (referenceTimezone==null) referenceTimezone = "GMT";
        this.id = post.getId();
        this.lastUpdateTime = ISODateTimeFormat.dateTime().withZone(DateTimeZone.forID(referenceTimezone)).print(post.lastUpdateTime);
        this.creationTime = ISODateTimeFormat.dateTime().withZone(DateTimeZone.forID(referenceTimezone)).print(post.creationTime);
        this.body = post.body;
    }

}
