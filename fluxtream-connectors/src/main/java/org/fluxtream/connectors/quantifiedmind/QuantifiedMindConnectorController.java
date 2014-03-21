package org.fluxtream.connectors.quantifiedmind;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import org.fluxtream.Configuration;
import org.fluxtream.connectors.Connector;
import org.fluxtream.connectors.controllers.ControllerSupport;
import org.fluxtream.domain.ApiKey;
import org.fluxtream.domain.Guest;
import org.fluxtream.auth.AuthHelper;
import org.fluxtream.services.GuestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller()
@RequestMapping("/quantifiedmind")
public class QuantifiedMindConnectorController {

	@Autowired
	GuestService guestService;

    @Autowired
    Configuration env;

	@RequestMapping(value = "/getTokenDialog")
	public ModelAndView enterUsername(HttpServletRequest request) {
		ModelAndView mav = new ModelAndView("connectors/quantifiedmind/getToken");
        mav.addObject("redirect_url", ControllerSupport.getLocationBase(request, env) + "quantifiedmind/setToken");
        return mav;
	}

    @RequestMapping(value = "/setToken")
    public String getToken(@RequestParam("token") String token,
                           @RequestParam("username") String username) throws IOException {
        Guest guest = AuthHelper.getGuest();
        final Connector connector = Connector.getConnector("quantifiedmind");
        final ApiKey apiKey = guestService.createApiKey(guest.getId(), connector);
        guestService.setApiKeyAttribute(apiKey,
                                        "token", token);
        guestService.setApiKeyAttribute(apiKey,
                                        "username", username);

        return "redirect:/app/from/quantifiedmind";
    }

}
