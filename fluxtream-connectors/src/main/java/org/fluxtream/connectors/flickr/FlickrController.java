package org.fluxtream.connectors.flickr;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.servlet.http.HttpServletRequest;
import org.fluxtream.core.Configuration;
import org.fluxtream.core.auth.AuthHelper;
import org.fluxtream.core.connectors.Connector;
import org.fluxtream.connectors.controllers.ControllerSupport;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.domain.Guest;
import org.fluxtream.core.domain.Notification;
import org.fluxtream.core.services.GuestService;
import org.fluxtream.core.services.NotificationsService;
import org.apache.commons.io.IOUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.fluxtream.core.utils.HttpUtils.fetch;
import static org.fluxtream.core.utils.Utils.hash;

@Controller
@RequestMapping(value="/flickr")
public class FlickrController {

    private static final String FLICKR_RENEWTOKEN_APIKEYID = "flickr.renewtoken.apiKeyId";
    @Autowired
	GuestService guestService;
	
	@Autowired
	Configuration env;

    @Autowired
    NotificationsService notificationsService;
	
	@RequestMapping(value = "/token")
	public String getToken(HttpServletRequest request)
		throws NoSuchAlgorithmException
	{
		String api_key = env.get("flickrConsumerKey");
		String api_sig = env.get("flickrConsumerSecret") + "api_key" + api_key + "permsread";
		api_sig = hash(api_sig);
        final String validRedirectUrl = env.get("flickr.validRedirectURL");
        if (!validRedirectUrl.startsWith(ControllerSupport.getLocationBase(request, env))) {
            final long guestId = AuthHelper.getGuestId();
            final String validRedirectBase = getBaseURL(validRedirectUrl);

            notificationsService.addNamedNotification(guestId, Notification.Type.WARNING, Connector.getConnector("flickr").statusNotificationName(),
                                                      "Adding a Flickr connector only works when logged in through " + validRedirectBase +
                                                      ".  You are logged in through " + ControllerSupport.getLocationBase(request, env) +
                                                      ".<br>Please re-login via the supported URL or inform your Fluxtream administrator " +
                                                      "that the flickr.validRedirectURL setting does not match your needs.");
            return "redirect:/app";
        }
        if (request.getParameter("apiKeyId") != null)
            request.getSession().setAttribute(FLICKR_RENEWTOKEN_APIKEYID,
                                              request.getParameter("apiKeyId"));
        String loginUrl = "https://flickr.com/services/auth/" +
			"?api_key=" + api_key + "&perms=read&api_sig=" + api_sig;
		return "redirect:" + loginUrl;
	}

    public static String getBaseURL(String url) {
        try {
            URI uri = new URI(url);
            StringBuilder rootURI = new StringBuilder(uri.getScheme()).append("://").append(uri.getHost());
            if (uri.getPort() != -1) {
                rootURI.append(":" + uri.getPort());
            }
            return (rootURI.toString());
        }
        catch (URISyntaxException e) {
            return null;
        }
    }

    @RequestMapping(value = "/upgradeToken")
	public String upgradeToken(HttpServletRequest request) throws NoSuchAlgorithmException, DocumentException, IOException {
		String api_key = env.get("flickrConsumerKey");
		String frob = request.getParameter("frob");
		
		Map<String,String> params = new HashMap<String,String>();
		params.put("method", "flickr.auth.getToken");
		params.put("api_key", api_key);
		params.put("frob", frob);

        String api_sig = sign(params);
		
		String getTokenUrl = "https://api.flickr.com/services/rest/" +
			"?method=flickr.auth.getToken&api_key=" + api_key + "&frob=" + frob + "&api_sig=" + api_sig;

		Guest guest = AuthHelper.getGuest();

        String authToken = null;
        try {
            authToken = fetch(getTokenUrl);
        }
        catch (Exception e) {
            e.printStackTrace();
            notificationsService.addNamedNotification(AuthHelper.getGuestId(),
                                                      Notification.Type.ERROR, Connector.getConnector("flickr").statusNotificationName(),
                                                      "Oops, we could not link your Flickr account<br>" +
                                                      "Please contact your administrator.");
            return "redirect:/app";
        }

        StringReader stringReader = new StringReader(authToken);
        StringBuilder sb = new StringBuilder();
        final List<String> responseLines = IOUtils.readLines(stringReader);
        sb.append("<root>");
        for (int i=1; i<responseLines.size(); i++)
            sb.append(responseLines.get(i));
        sb.append("</root>");

        SAXReader reader = new SAXReader();
        Document document = reader.read(new StringReader(authToken));

		Element user = (Element) document.selectSingleNode("rsp/auth/user");
		
		String username = user.attributeValue("username");

		String nsid = user.attributeValue("nsid");
		String fullname = user.attributeValue("fullname");
		String token = document.selectSingleNode("rsp/auth/token/text()").getStringValue();
		
		Connector flickrConnector = Connector.getConnector("flickr");

        ApiKey apiKey;
        if (request.getSession().getAttribute(FLICKR_RENEWTOKEN_APIKEYID)!=null) {
            final String apiKeyIdString = (String)request.getSession().getAttribute(FLICKR_RENEWTOKEN_APIKEYID);
            long apiKeyId = Long.valueOf(apiKeyIdString);
            apiKey = guestService.getApiKey(apiKeyId);
        } else
            apiKey = guestService.createApiKey(guest.getId(), flickrConnector);

        guestService.populateApiKey(apiKey.getId());
		guestService.setApiKeyAttribute(apiKey,  "username", username);
		guestService.setApiKeyAttribute(apiKey,  "token", token);
		guestService.setApiKeyAttribute(apiKey,  "nsid", nsid);
		guestService.setApiKeyAttribute(apiKey,  "fullname", fullname);

        if (request.getSession().getAttribute(FLICKR_RENEWTOKEN_APIKEYID)!=null) {
            request.getSession().removeAttribute(FLICKR_RENEWTOKEN_APIKEYID);
            return "redirect:/app/tokenRenewed/flickr";
        }
		return "redirect:/app/from/"+flickrConnector.getName();
	}
	
	String sign(Map<String,String> parameters) throws NoSuchAlgorithmException {
		String toSign = env.get("flickrConsumerSecret");
	    SortedSet<String> eachKey= new TreeSet<String>(parameters.keySet());
		for (String key : eachKey)
			toSign += key + parameters.get(key);
		String sig = hash(toSign);
		return sig;
	}
	
	String getConsumerKey() {
		return env.get("flickrConsumerKey");
	}

	String getConsumerSecret() {
		return env.get("flickrConsumerSecret");
	}
	
}
