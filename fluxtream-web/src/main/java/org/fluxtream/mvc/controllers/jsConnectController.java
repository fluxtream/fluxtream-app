package org.fluxtream.mvc.controllers;

import org.fluxtream.core.Configuration;
import org.fluxtream.core.auth.AuthHelper;
import org.fluxtream.core.domain.Guest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import com.vanillaforums.jsConnect;

import static org.fluxtream.core.utils.Utils.hash;

/**
 * User: candide
 * Date: 14/05/14
 * Time: 17:39
 */
@Controller
public class jsConnectController {

    @Autowired
    Configuration env;

    @RequestMapping(value="/authenticate.json", produces="application/json")
    public void authenticate(final HttpServletRequest request,
                             final HttpServletResponse response) throws IOException {
        Map user = new java.util.LinkedHashMap();
        final Guest guest = AuthHelper.getGuest();
        if (guest!=null) {
            user.put("uniqueid", String.valueOf(guest.getId()));
            user.put("name", guest.getGuestName());
            user.put("email", guest.email);
            String emailHash = hash(guest.email.toLowerCase().trim()); //gravatar specifies the email should be trimmed, taken to lowercase, and then MD5 hashed
            String gravatarURL = String.format("http://www.gravatar.com/avatar/%s?s=256&d=retro", emailHash);
            user.put("photourl", gravatarURL);
        }
        String clientId = env.get("vanillaforums.client.id");
        String clientSecret = env.get("vanillaforums.client.secret");
        final String connectString = jsConnect.GetJsConnectString(user, request.getParameterMap(), clientId, clientSecret, false);
        response.getWriter().write(connectString);
    }

}
