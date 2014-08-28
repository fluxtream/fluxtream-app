package org.fluxtream.mvc.controllers;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.fluxtream.core.Configuration;
import org.fluxtream.core.aspects.FlxLogger;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.domain.ApiKeyAttribute;
import org.fluxtream.core.services.GuestService;
import org.fluxtream.core.utils.RequestUtils;
import org.joda.time.format.ISODateTimeFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class TestController {
    FlxLogger logger = FlxLogger.getLogger(TestController.class);

    @Autowired
    Configuration env;

    @Autowired
    GuestService guestService;

    @RequestMapping(value = "/test/unit")
    public ModelAndView unit(HttpServletRequest request) {
        // check that we're running locally
        if (!RequestUtils.isDev(request)) {
            return new ModelAndView("redirect:/welcome");
        }
        ModelAndView mav = new ModelAndView("test/unit");
        String release = env.get("release");
        mav.addObject("release", release);
        return mav;
    }

    @RequestMapping("/test/setAtt")
    public void testSetAttribute(HttpServletResponse resp) throws IOException {
        final ApiKey apiKey = guestService.getApiKey(136);
        for (int i=0; i<1000; i++)
            setApiKeyAttribute(apiKey);
        resp.getWriter().write("OK");
    }

    private void setApiKeyAttribute(final ApiKey apiKey) {
        guestService.setApiKeyAttribute(apiKey, ApiKeyAttribute.LAST_SYNC_TIME_KEY, ISODateTimeFormat.dateHourMinuteSecondFraction().
                withZoneUTC().print(System.currentTimeMillis()));
        guestService.setApiKeyAttribute(apiKey, ApiKeyAttribute.LAST_SYNC_TIME_KEY, ISODateTimeFormat.dateHourMinuteSecondFraction().
                withZoneUTC().print(System.currentTimeMillis()));
        guestService.setApiKeyAttribute(apiKey, ApiKeyAttribute.LAST_SYNC_TIME_KEY, ISODateTimeFormat.dateHourMinuteSecondFraction().
                withZoneUTC().print(System.currentTimeMillis()));
        guestService.setApiKeyAttribute(apiKey, ApiKeyAttribute.LAST_SYNC_TIME_KEY, ISODateTimeFormat.dateHourMinuteSecondFraction().
                withZoneUTC().print(System.currentTimeMillis()));
        guestService.setApiKeyAttribute(apiKey, ApiKeyAttribute.LAST_SYNC_TIME_KEY, ISODateTimeFormat.dateHourMinuteSecondFraction().
                withZoneUTC().print(System.currentTimeMillis()));
        guestService.setApiKeyAttribute(apiKey, ApiKeyAttribute.LAST_SYNC_TIME_KEY, ISODateTimeFormat.dateHourMinuteSecondFraction().
                withZoneUTC().print(System.currentTimeMillis()));
    }
}
