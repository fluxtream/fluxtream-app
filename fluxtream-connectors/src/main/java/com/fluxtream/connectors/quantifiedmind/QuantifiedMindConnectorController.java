package com.fluxtream.connectors.quantifiedmind;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import com.fluxtream.Configuration;
import com.fluxtream.connectors.Connector;
import com.fluxtream.domain.Guest;
import com.fluxtream.mvc.controllers.AuthHelper;
import com.fluxtream.services.GuestService;
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
	public ModelAndView enterUsername(
			HttpServletRequest request) {
		ModelAndView mav = new ModelAndView("connectors/quantifiedmind/getToken");
        mav.addObject("redirect_url", env.get("homeBaseUrl") + "quantifiedmind/setToken");
        return mav;
	}

    @RequestMapping(value = "/setToken")
    public String getToken(@RequestParam("token") String token,
                                 @RequestParam("username") String username) throws IOException {
        Guest guest = AuthHelper.getGuest();
        guestService.setApiKeyAttribute(guest.getId(), Connector.getConnector("quantifiedmind"),
                                        "token", token);
        guestService.setApiKeyAttribute(guest.getId(), Connector.getConnector("quantifiedmind"),
                                        "username", username);

        return "redirect:/app/from/quantifiedmind";
    }

}
