package org.fluxtream.connectors.beddit;

import net.sf.json.JSONObject;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.message.BasicNameValuePair;
import org.fluxtream.core.Configuration;
import org.fluxtream.core.auth.AuthHelper;
import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.mvc.models.StatusModel;
import org.fluxtream.core.services.ConnectorUpdateService;
import org.fluxtream.core.services.GuestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/beddit")
public class BedditConnectorController {

    @Autowired
    GuestService guestService;

    @Autowired
    ConnectorUpdateService connectorUpdateService;

    @Autowired
    protected Configuration env;

    @RequestMapping(value = "/enterAuthInfo")
    public ModelAndView enterProvisioningURL() {
        ModelAndView mav = new ModelAndView("connectors/beddit/enterAuthInfo");
        return mav;
    }

    @RequestMapping(value = "/setAuthInfo", produces=MediaType.APPLICATION_JSON, method=RequestMethod.POST)
    @ResponseBody
    public StatusModel setAuthInfo(@RequestParam("email") final String email,
                              @RequestParam("password") final String password) {
        try{
            HttpPost login = new HttpPost("https://cloudapi.beddit.com/api/v1/auth/authorize");
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
            nameValuePairs.add(new BasicNameValuePair("grant_type", "password"));
            nameValuePairs.add(new BasicNameValuePair("username", email));
            nameValuePairs.add(new BasicNameValuePair("password", password));
            login.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            HttpResponse response = env.getHttpClient().execute(login);

            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode == HttpStatus.SC_OK){
                ResponseHandler<String> responseHandler = new BasicResponseHandler();

                //extract the data from the response
                String content = responseHandler.handleResponse(response);
                JSONObject topLevelObject = JSONObject.fromObject(content);
                String access_token = topLevelObject.getString("access_token");
                long user = topLevelObject.getLong("user");

                //create the connector and set the api key attributes
                final Connector connector = Connector.getConnector("beddit");
                final ApiKey apiKey = guestService.createApiKey(AuthHelper.getGuestId(), connector);
                guestService.setApiKeyAttribute(apiKey,"access_token", access_token);
                guestService.setApiKeyAttribute(apiKey,"userid",user + "");

                //schedule and update for the connector
                connectorUpdateService.updateConnector(apiKey, false);

                //return success
                return StatusModel.success("Successfully Authorized");
            }
            else{
                return StatusModel.failure("Invalid Credentials");
            }

        }
        catch (Exception e){
            e.printStackTrace();
            return StatusModel.failure("Something went wrong: " + e.getMessage());

        }
   }

}
