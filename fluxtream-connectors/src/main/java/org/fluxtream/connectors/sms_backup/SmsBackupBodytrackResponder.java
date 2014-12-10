package org.fluxtream.connectors.sms_backup;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import org.fluxtream.core.SimpleTimeInterval;
import org.fluxtream.core.TimeInterval;
import org.fluxtream.core.TimeUnit;
import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.connectors.ObjectType;
import org.fluxtream.core.connectors.bodytrackResponders.AbstractBodytrackResponder;
import org.fluxtream.core.connectors.bodytrackResponders.BaseBodytrackResponder;
import org.fluxtream.core.connectors.vos.AbstractFacetVO;
import org.fluxtream.core.domain.AbstractFacet;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.domain.ChannelMapping;
import org.fluxtream.core.domain.GuestSettings;
import org.fluxtream.core.mvc.models.TimespanModel;
import org.springframework.stereotype.Component;

@Component
public class SmsBackupBodytrackResponder extends BaseBodytrackResponder {

    @Override
    public void addToDeclaredChannelMappings(ApiKey apiKey, List<ChannelMapping> channelMappings) {
        ChannelMapping.addToDeclaredMappings(apiKey, 3, apiKey.getConnector().getDeviceNickname(), "data", channelMappings);
    }

}
