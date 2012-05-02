package com.fluxtream.dashboard.dataproviders.week;

import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.Connector;
import com.fluxtream.dashboard.dataproviders.AbstractWidgetDataProvider;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.domain.GuestSettings;
import net.sf.json.JSONObject;
import org.springframework.stereotype.Component;

/**
 * <p>
 * <code>StepsDataProvider</code> does something...
 * </p>
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Component("week/steps")
public class StepsDataProvider extends AbstractWidgetDataProvider {

    @Override
    public JSONObject provideData(final long guestId, final GuestSettings settings, final TimeInterval timeInterval) {
        JSONObject steps = new JSONObject();
        ApiKey bodytrackApiKey = guestService.getApiKey(guestId, Connector.getConnector("bodytrack"));
        String user_id = bodytrackApiKey.getAttributeValue("user_id", env);
        steps.accumulate("user_id", user_id);
        addRequiredJS(steps, "steps");
        return steps;
    }
}
