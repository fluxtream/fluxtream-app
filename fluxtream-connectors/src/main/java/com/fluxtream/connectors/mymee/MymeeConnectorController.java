package com.fluxtream.connectors.mymee;

import java.io.IOException;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import com.fluxtream.auth.AuthHelper;
import com.fluxtream.connectors.Connector;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.services.ConnectorUpdateService;
import com.fluxtream.services.GuestService;
import com.fluxtream.utils.HttpUtils;
import com.fluxtream.utils.UnexpectedHttpResponseCodeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller()
@RequestMapping("/mymee")
public class MymeeConnectorController {

	@Autowired
	GuestService guestService;

    @Autowired
    ConnectorUpdateService connectorUpdateService;
	
	@RequestMapping(value = "/enterFetchURL")
	public ModelAndView enterProvisioningURL() {
		ModelAndView mav = new ModelAndView("connectors/mymee/enterFetchURL");
		return mav;
	}

	@RequestMapping("/setFetchURL")
	public ModelAndView setProvisioningURL(@RequestParam("url") String url,
                                           HttpServletRequest request ) throws MessagingException
	{
		ModelAndView mav = new ModelAndView("connectors/mymee/success");
		long guestId = AuthHelper.getGuestId();
        boolean worked = false;
		try { testConnection(url); worked = true;}
		catch (Exception e) {}
		if (worked) {
            final Connector connector = Connector.getConnector("mymee");
            final ApiKey apiKey = guestService.createApiKey(guestId, connector);
			guestService.setApiKeyAttribute(apiKey,  "fetchURL", url);
            connectorUpdateService.updateConnector(apiKey, false);
			return mav;
		} else {
			request.setAttribute("errorMessage", "Sorry, the URL you provided did not work.\n" +
                                                 "Please check that you entered it correctly.");
			return new ModelAndView("connectors/mymee/enterProvisioningURL");
		}
	}

    private void testConnection(final String url) throws IOException, UnexpectedHttpResponseCodeException {
        HttpUtils.fetch(url);
    }

}
