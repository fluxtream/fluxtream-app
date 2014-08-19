package org.fluxtream.mvc.controllers;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import com.wordnik.swagger.config.ConfigFactory;
import com.wordnik.swagger.config.SwaggerConfig;
import net.sf.json.JSONObject;
import org.fluxtream.core.Configuration;
import org.fluxtream.core.aspects.FlxLogger;
import org.fluxtream.core.auth.AuthHelper;
import org.fluxtream.core.domain.Guest;
import org.fluxtream.core.services.GuestService;
import org.fluxtream.core.utils.HttpUtils;
import org.fluxtream.core.utils.UnexpectedHttpResponseCodeException;
import org.markdown4j.Markdown4jProcessor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import static org.fluxtream.core.utils.Utils.hash;

/**
 * User: candide
 * Date: 11/04/14
 * Time: 14:11
 */
@Controller
@RequestMapping("/dev")
public class DevController {

    FlxLogger logger = FlxLogger.getLogger(DevController.class);

    @Autowired
    Configuration env;

    @Autowired
    BeanFactory beanFactory;

    @Autowired
    GuestService guestService;

    @RequestMapping(value="")
    public String devIndexRedict() {
        return "redirect:/dev/";
    }

    @RequestMapping(value="/")
    public ModelAndView devIndex(HttpServletResponse response) {
        noCache(response);
        final Guest guest = AuthHelper.getGuest();
        if (guest!=null)
            return new ModelAndView("redirect:/dev/home");
        final ModelAndView mav = new ModelAndView("developer/index", "release", env.get("release"));
        mav.addObject("visibility", "public");
        return mav;
    }

    @RequestMapping("/test")
    public ModelAndView testPage(HttpServletResponse response) {
        noCache(response);
        final ModelAndView mav = new ModelAndView("developer/testPage", "release", env.get("release"));
        return mav;
    }

    @RequestMapping(value = "/home")
    public ModelAndView partnersHome(HttpServletResponse response, ModelMap model) {
        noCache(response);
        final ModelAndView mav = new ModelAndView("developer/index", "release", env.get("release"));
        String release = env.get("release");
        final Guest guest = AuthHelper.getGuest();
        model.addObject("avatarURL", getGravatarImageURL(guest));
        model.addObject("guestName", guest.getGuestName());
        model.addObject("release", release);
        mav.addObject("visibility", "partners");
        return mav;
    }

    private void noCache(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
        response.setHeader("Pragma", "no-cache"); // HTTP 1.0.
        response.setDateHeader("Expires", 0);
    }

    @RequestMapping(value = "/swapToken")
    public void obtainAccessToken(@RequestParam("code") String code,
                                  HttpServletResponse response) throws IOException, UnexpectedHttpResponseCodeException {
        Map<String,String> parameters = new HashMap<String,String>();
        parameters.put("grant_type", "authorization_code");
        parameters.put("code", code);
        parameters.put("client_id", env.get("fluxtreamDev.client.id"));
        parameters.put("client_secret", env.get("fluxtreamDev.client.secret"));
        parameters.put("redirect_uri", "somedummyfield");
        final String json = HttpUtils.fetch(env.get("homeBaseUrl") + "auth/oauth2/token", parameters);
        final JSONObject token = JSONObject.fromObject(json);
        final String accessToken = token.getString("access_token");
        response.sendRedirect(env.get("homeBaseUrl") + "swagger-ui/index.html?accessToken=" + accessToken);
    }

    @RequestMapping(value = "/partials/{partial}")
    public ModelAndView partial(@PathVariable("partial") String partial) {
        ModelAndView mav = new ModelAndView("/developer/public/partials/" + partial);
        String release = env.get("release");
        mav.addObject("release", release);
        final SwaggerConfig config = ConfigFactory.config();
        final String apiDocsURL = config.getBasePath()+"/api-docs";
        mav.addObject("apiDocsURL", apiDocsURL);
        return mav;
    }

    @RequestMapping(value = "/partials/manuals/general")
    public ModelAndView generalUserManual() throws IOException {
        return renderMarkdown("general");
    }

    @RequestMapping(value = "/partials/manuals/authorization")
    public ModelAndView authorizationUserManual() throws IOException {
        return renderMarkdown("authorization");
    }

    private ModelAndView renderMarkdown(final String documentName) throws IOException {
        ModelAndView mav = new ModelAndView("/developer/public/partials/user-manual");
        String release = env.get("release");
        final String userManualLocation = env.get("userManuals.location") + documentName + ".md";
        Markdown4jProcessor processor = new Markdown4jProcessor();
        String manual = processor.process(new File(userManualLocation));
        mav.addObject("manual", manual);
        mav.addObject("release", release);
        return mav;
    }

    @RequestMapping(value = "/partners/partials/{partial}")
    public String partnersPartial(@PathVariable("partial") String partial, ModelMap model) {
        String release = env.get("release");
        final Guest guest = AuthHelper.getGuest();
        model.addObject("guestName", guest.getGuestName());
        model.addObject("avatarURL", getGravatarImageURL(guest));
        model.addObject("release", release);
        return "/developer/partners/partials/" + partial;
    }

    private String getGravatarImageURL(Guest guest) {
        String emailHash = hash(guest.email.toLowerCase().trim()); //gravatar specifies the email should be trimmed, taken to lowercase, and then MD5 hashed
        String gravatarURL = String.format("http://www.gravatar.com/avatar/%s?s=27&d=404", emailHash);
        //HttpGet get = new HttpGet(gravatarURL);
        //int res = 0;
        //try { res = ((new DefaultHttpClient()).execute(get)).getStatusLine().getStatusCode(); }
        //catch (IOException e) {e.printStackTrace();}
        //return res==200 ? gravatarURL : null;
        return gravatarURL;
    }

}
