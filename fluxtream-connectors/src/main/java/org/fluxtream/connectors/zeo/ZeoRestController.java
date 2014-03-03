package org.fluxtream.connectors.zeo;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.fluxtream.Configuration;
import org.fluxtream.aspects.FlxLogger;
import org.fluxtream.services.ApiDataService;
import org.fluxtream.services.GuestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller()
@RequestMapping(value = "/zeo")
public class ZeoRestController {

	FlxLogger logger = FlxLogger.getLogger(ZeoRestController.class);

	@Autowired
	GuestService guestService;

	@Autowired
	Configuration env;

    @Qualifier("apiDataServiceImpl")
    @Autowired
	ApiDataService apiDataService;

	@RequestMapping(value = "/enterCredentials")
	public ModelAndView userSubscribe(HttpServletRequest request) throws IOException {
        ModelAndView mav = new ModelAndView("connectors/zeo/enterCredentials");
        return mav;
	}

    //TODO check to see if username and password are  correct.
	@RequestMapping(value = "/submitCredentials")
	public ModelAndView userSubscribed(HttpServletRequest request, HttpServletResponse response) throws Exception {
        //String email = request.getParameter("username");
        //String password = request.getParameter("password");
        //email = email.trim();
        //password = password.trim();
        //request.setAttribute("username", email);
        //List<String> required = new ArrayList<String>();
        //if (email.equals("")) {
        //    required.add("username");
        //}
        //if (password.equals("")) {
        //    required.add("password");
        //}
        //if (required.size()!=0) {
        //    request.setAttribute("required", required);
        //    return new ModelAndView("connectors/zeo/enterCredentials");
        //}
        //try{
        //    String zeoApiKey = env.get("zeoApiKey");
        //    String baseUrl = "http://api.myzeo.com:8080/zeows/api/v1/json/sleeperService/";
        //    String datesUrl = baseUrl + "getDatesWithSleepDataInRange?key=" + zeoApiKey;
        //    ZeoRestUpdater.callURL(datesUrl, email, password);
        //}
        //catch (IOException e)
        //{
        //    request.setAttribute("errorMessage", "The credentials provided are invalid");
        //    return new ModelAndView("connectors/zeo/enterCredentials");
        //}
        //
        //long guestId = AuthHelper.getGuestId();
        //
        //final Connector connector = Connector.getConnector("zeo");
        //final ApiKey apiKey = guestService.createApiKey(guestId, connector);
        //
        //guestService.setApiKeyAttribute(apiKey, "username", email);
        //guestService.setApiKeyAttribute(apiKey, "password", password);
        //
        //ModelAndView mav = new ModelAndView("connectors/zeo/success");
        //mav.addObject("guestId", guestId);
        //return mav;
        return null;
	}

}
