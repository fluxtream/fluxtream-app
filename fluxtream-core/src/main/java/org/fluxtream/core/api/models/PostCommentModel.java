package org.fluxtream.core.api.models;

import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;
import org.fluxtream.core.domain.PostComment;
import org.joda.time.DateTimeZone;
import org.joda.time.format.ISODateTimeFormat;

/**
 * User: candide
 * Date: 28/06/14
 * Time: 18:27
 */
@ApiModel("A Post comment")
public class PostCommentModel {

    @ApiModelProperty(value = "The comment's ID", required = true)
    public long id;
    @ApiModelProperty(value = "Last time this comment was edited, in ISO8601 format (yyyy-MM-dd'T'HH:mm:ss'Z')", required = true)
    public String lastUpdateTime;
    @ApiModelProperty(value = "Date/Time this comment was created, in ISO8601 format (yyyy-MM-dd'T'HH:mm:ss'Z')", required = true)
    public String creationTime;
    @ApiModelProperty(value = "The text body of the comment", required = true)
    public String body;
    @ApiModelProperty(value = "The originating guest of this comment (only present if it is different from the calling guest)", required = false)
    public BasicGuestModel from;
    @ApiModelProperty(value = "The destination guest of this comment (only present if it is different from the calling guest)", required = false)
    public BasicGuestModel to;

    public PostCommentModel(PostComment postComment, String referenceTimezone) {
        if (referenceTimezone==null) referenceTimezone = "GMT";
        this.id = postComment.getId();
        this.lastUpdateTime = ISODateTimeFormat.dateTime().withZone(DateTimeZone.forID(referenceTimezone)).print(postComment.lastUpdateTime);
        this.creationTime = ISODateTimeFormat.dateTime().withZone(DateTimeZone.forID(referenceTimezone)).print(postComment.creationTime);
        this.body = postComment.body;
    }

}
