package glacier.nikeplus;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import org.fluxtream.services.GuestService;

@Controller()
@RequestMapping("/nikeplus")
public class NikePlusConnectorController {

	@Autowired
	GuestService guestService;
	
	@RequestMapping(value = "/enterUsername")
	public ModelAndView signin(
			HttpServletRequest request) {
		ModelAndView mav = new ModelAndView("connectors/nikeplus/enterUsername");
		return mav;
	}

	@RequestMapping("/setUsername")
	public ModelAndView check(
		@RequestParam("username") String username,
		HttpServletRequest request ) throws MessagingException
	{
		//ModelAndView mav = new ModelAndView("connectors/nikeplus/success");
		//long guestId = AuthHelper.getGuestId();
		//boolean worked = false;
		//try { worked = (new NikePlusHelper(username)).testConnection(); }
		//catch (Exception e) {}
		//if (worked) {
        //
         //   final Connector connector = Connector.getConnector("nikeplus");
         //   final ApiKey apiKey = guestService.createApiKey(guest.getId(), connector);
        //
		//	guestService.setApiKeyAttribute(apiKey, "username", username);
		//	return mav;
		//} else {
		//	request.setAttribute("errorMessage", "Sorry, you must have entered wrong credentials.\nPlease try again.");
		//	return new ModelAndView("connectors/nikeplus/enterUsername");
		//}
        return null;
	}
	

}
