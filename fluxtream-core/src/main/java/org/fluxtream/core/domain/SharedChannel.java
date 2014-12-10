package org.fluxtream.core.domain;

import javax.persistence.*;
import java.io.Serializable;

/**
 * Created by candide on 04/12/14.
 */
@Entity(name = "SharedChannels")
@NamedQueries({
        @NamedQuery(name="sharedChannel.byTrustedBuddyId", query="SELECT sharedChannel FROM SharedChannels sharedChannel WHERE sharedChannel.buddy.guestId=? AND sharedChannel.buddy.buddyId=?"),
        @NamedQuery (name="sharedChannel.byApiKeyId", query="SELECT sharedChannel FROM SharedChannels sharedChannel WHERE sharedChannel.buddy.guestId=? AND sharedChannel.buddy.buddyId=? AND sharedChannel.channelMapping.apiKeyId=?"),
        @NamedQuery (name="sharedChannel.byBuddyAndChannelMapping", query="SELECT sharedChannel FROM SharedChannels sharedChannel WHERE sharedChannel.buddy.guestId=? AND sharedChannel.buddy.buddyId=? AND sharedChannel.channelMapping.id=?")
})
public class SharedChannel extends AbstractEntity implements Serializable {

    @ManyToOne(fetch= FetchType.EAGER)
    public TrustedBuddy buddy;

    @OneToOne(fetch= FetchType.EAGER)
    public ChannelMapping channelMapping;

    public SharedChannel() {}

    public SharedChannel(TrustedBuddy buddy, ChannelMapping mapping) {
        this.buddy = buddy;
        this.channelMapping = mapping;
    }

}
