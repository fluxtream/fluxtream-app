package glacier.openpath;

import java.util.ArrayList;
import java.util.List;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;

import org.fluxtream.auth.AuthHelper;
import org.fluxtream.domain.ApiKey;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import org.fluxtream.connectors.Connector;
import org.fluxtream.services.ConnectorUpdateService;
import org.fluxtream.services.GuestService;

@Controller()
@RequestMapping("/openPath")
public class OpenPathConnectorController {

	@Autowired
	GuestService guestService;

	@Autowired
	ConnectorUpdateService connectorUpdateService;
	
	@Autowired
	BeanFactory beanFactory;

	@RequestMapping(value = "/enterCredentials")
	public ModelAndView signin(HttpServletRequest request) {
		ModelAndView mav = new ModelAndView(
				"connectors/openpath/enterCredentials");
		return mav;
	}

	@RequestMapping("/check")
	public ModelAndView check(@RequestParam("accessKey") String accessKey,
			@RequestParam("secretKey") String secretKey,
			HttpServletRequest request) throws MessagingException {
		List<String> required = new ArrayList<String>();
		accessKey = accessKey.trim();
		secretKey = secretKey.trim();
		request.setAttribute("accessKey", accessKey);
		request.setAttribute("secretKey", secretKey);
		if (accessKey.equals(""))
			required.add("accessKey");
		if (secretKey.equals(""))
			required.add("secretKey");
		if (required.size() != 0) {
			request.setAttribute("required", required);
			return new ModelAndView("connectors/openpath/enterCredentials");
		}
		long guestId = AuthHelper.getGuestId();

        final Connector connector = Connector.getConnector("instagram");
        final ApiKey apiKey = guestService.createApiKey(guestId, connector);

        guestService.setApiKeyAttribute(apiKey, "accessKey", accessKey);
        guestService.setApiKeyAttribute(apiKey, "secretKey", secretKey);

        ModelAndView mav = new ModelAndView("connectors/openpath/success");
        return mav;
	}

}
