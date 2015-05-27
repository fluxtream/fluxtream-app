package org.fluxtream.mvc.controllers;

import org.fluxtream.core.Configuration;
import org.fluxtream.core.aspects.FlxLogger;
import org.fluxtream.core.auth.AuthHelper;
import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.domain.Guest;
import org.fluxtream.core.domain.Notification.Type;
import org.fluxtream.core.services.*;
import org.fluxtream.core.utils.SecurityUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;

@Controller
public class AppController {

	FlxLogger logger = FlxLogger.getLogger(AppController.class);

	@Autowired
	Configuration env;

	@Autowired
	GuestService guestService;

	@Autowired
	ApiDataService apiDataService;

	@Autowired
	MetadataService metadataService;

	@Autowired
	NotificationsService notificationsService;

    @Autowired
    ConnectorUpdateService connectorUpdateService;

    @Autowired
    BuddiesService buddiesService;

    @Autowired
	BeanFactory beanFactory;

    @Autowired
    ErrorController errorController;

    @RequestMapping(value = { "*", "/*", "/welcome*" })
    public ModelAndView index(HttpServletRequest request,
                              HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
        response.setHeader("Pragma", "no-cache"); // HTTP 1.0.
        response.setDateHeader("Expires", 0);
        if (AuthHelper.isFullyAuthenticated())
            return new ModelAndView("redirect:/app");
        String indexPageName = "default";
        if (env.get("homepage.name")!=null)
            indexPageName = env.get("homepage.name");
        ModelAndView mav = new ModelAndView(indexPageName);
        String release = env.get("release");
        if (release != null)
            mav.addObject("release", release);
        final String facebookAppId = env.get("facebook.appId");
        if (facebookAppId !=null&&!facebookAppId.equals("xxx")) {
            mav.addObject("facebookAppId", facebookAppId);
            mav.addObject("supportsFBLogin", true);
        } else
            mav.addObject("supportsFBLogin", false);
        mav.addObject("tracker", hasTracker(request));
        return mav;
    }

    @RequestMapping(value = "handle404")
    public String handle404() {
        return "redirect:/welcome";
    }

    private boolean hasIntercom(HttpServletRequest request) {
        String intercomScriptPath = request.getSession().getServletContext().getRealPath("/WEB-INF/jsp/intercom.jsp");
        File intercomScriptFile = new File(intercomScriptPath);
        final boolean fileExists = intercomScriptFile.exists();
        final boolean hasApiKey = env.get("intercomApiKey")!=null;
        return fileExists&&hasApiKey;
    }

    private boolean hasTracker(HttpServletRequest request) {
        String trackerPath = request.getSession().getServletContext().getRealPath("/WEB-INF/jsp/tracker.jsp");
        File trackerFile = new File(trackerPath);
        final boolean fileExists = trackerFile.exists();
        return fileExists;
    }

    @RequestMapping(value = { "/snippets" })
    public ModelAndView snippets(HttpServletRequest request) {

        ModelAndView mav = new ModelAndView("snippets");
        mav.addObject("tracker", hasTracker(request));
        if (request.getSession(false) == null)
            return mav;

        Authentication auth = SecurityContextHolder.getContext()
                .getAuthentication();
        if (auth == null || !auth.isAuthenticated())
            return mav;
        mav.setViewName("snippets");

        String release = env.get("release");
        mav.addObject("release", release);
        return mav;
    }

    public ModelAndView home(HttpServletRequest request,
                             HttpServletResponse response) {
		logger.info("action=loggedIn");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
        response.setHeader("Pragma", "no-cache"); // HTTP 1.0.
        response.setDateHeader("Expires", 0);

		long guestId = AuthHelper.getGuestId();

		ModelAndView mav = new ModelAndView("redirect:main");
        mav.addObject("tracker", hasTracker(request));
        final boolean hasIntercom = hasIntercom(request);
        mav.addObject("intercom", hasIntercom);
        if (hasIntercom)
            mav.addObject("intercomApiKey", env.get("intercomApiKey"));
		if (request.getSession(false) == null)
			return mav;

		Authentication auth = SecurityContextHolder.getContext()
				.getAuthentication();
		if (auth == null || !auth.isAuthenticated())
			return mav;
		mav.setViewName("home");

        Guest guest = guestService.getGuestById(guestId);

		mav.addObject("fullname", guest.getGuestName());

        String release = env.get("release");
		request.setAttribute("guestName", guest.getGuestName());
        request.setAttribute("trustingBuddies", buddiesService.getTrustingBuddies(guestId));
        request.setAttribute("trustedBuddies", buddiesService.getTrustedBuddies(guestId));
        request.setAttribute("useMinifiedJs", Boolean.valueOf(env.get("useMinifiedJs")));
        if (env.get("forum.url")!=null)
            request.setAttribute("forumUrl", env.get("forum.url"));

		if (SecurityUtils.isDemoUser())
			request.setAttribute("demo", true);
		if (release != null)
			mav.addObject("release", release);
		return mav;
	}

    @RequestMapping(value = "/checkIn")
    public ModelAndView checkIn(HttpServletRequest request,
                                HttpServletResponse response) throws IOException, NoSuchAlgorithmException, URISyntaxException {
        final Guest guest = AuthHelper.getGuest();
        if (!hasTimezoneCookie(request)|| guest ==null)
            return new ModelAndView("redirect:/welcome");
        long guestId = guest.getId();
        checkIn(request, guestId);
        final HttpSessionRequestCache requestCache = new HttpSessionRequestCache();
        SavedRequest savedRequest =
                requestCache.getRequest(request, response);
        if (savedRequest!=null) {
            final String redirectUrl = savedRequest.getRedirectUrl();
            requestCache.removeRequest(request, response);
            return new ModelAndView("redirect:" + redirectUrl);
        }
        return new ModelAndView("redirect:/app");
    }

    @RequestMapping(value = "/mobile/checkIn")
    public ModelAndView mobileCheckIn(HttpServletRequest request,
                                      HttpServletResponse response) throws IOException, NoSuchAlgorithmException, URISyntaxException {
        final Guest guest = AuthHelper.getGuest();
        long guestId = guest.getId();
        checkIn(request, guestId);
        final HttpSessionRequestCache requestCache = new HttpSessionRequestCache();
        SavedRequest savedRequest =
                requestCache.getRequest(request, response);
        if (savedRequest!=null) {
            final String redirectUrl = savedRequest.getRedirectUrl();
            requestCache.removeRequest(request, response);
            return new ModelAndView("redirect:" + redirectUrl);
        }
        return new ModelAndView("redirect:/app");
    }

	@RequestMapping(value = { "/app*", "/app/**" })
	public ModelAndView welcomeHome(HttpServletRequest request, HttpServletResponse response)
			throws IOException, NoSuchAlgorithmException {
		if (!hasTimezoneCookie(request)|| AuthHelper.getGuest()==null)
			return new ModelAndView("redirect:/welcome?signIn");
        SavedRequest savedRequest =
                new HttpSessionRequestCache().getRequest(request, response);
        if (savedRequest!=null) {
            final String redirectUrl = savedRequest.getRedirectUrl();
            return new ModelAndView(redirectUrl);
        }
        return home(request, response);
    }

    @RequestMapping(value = "/app/tokenRenewed/{connectorName}")
    public String tokenRenewed(@PathVariable("connectorName") String connectorName) {
        final Connector connector = Connector.getConnector(connectorName);
        String message = "You have successfully renewed your "
                         + connector.prettyName()
                         + " authentication tokens. We will now update your data.";
        return welcomeBack(connectorName, message);
    }

	@RequestMapping(value = "/app/from/{connectorName}")
	public String home(@PathVariable("connectorName") String connectorName) {
        final Connector connector = Connector.getConnector(connectorName);
        String message = "<img class=\"loading-animation\" src=\"/static/img/loading.gif\"/>You have successfully added a new connector: "
                         + connector.prettyName()
                         + ". Your data is now being retrieved. "
                         + "It may take a little while until it becomes visible.";
        return welcomeBack(connectorName, message);
	}

    public String welcomeBack(String connectorName, String message) {
        long guestId = AuthHelper.getGuestId();
        final Connector connector = Connector.getConnector(connectorName);
        notificationsService.addNamedNotification(guestId, Type.INFO, connector.statusNotificationName(), message);
        ApiKey apiKey = guestService.getApiKey(guestId, connector);
        connectorUpdateService.updateConnector(apiKey, true);
        return "redirect:/app";
    }

    private void checkIn(HttpServletRequest request, long guestId)
        throws IOException {
		String remoteAddr = request.getHeader("X-Forwarded-For");
		if (remoteAddr == null)
			remoteAddr = request.getRemoteAddr();
        initializeWithTimeZone(request, guestId);
		guestService.checkIn(guestId, remoteAddr);
        connectorUpdateService.updateAllConnectors(guestId, false);
	}
	
	private boolean hasTimezoneCookie(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		for (Cookie cookie : cookies) {
			if (cookie.getName().equalsIgnoreCase("timeZone")) {
				return true;
			}
		}
		return false;
	}

	private void initializeWithTimeZone(HttpServletRequest request, long guestId) {
		Cookie[] cookies = request.getCookies();
		String timeZone = null, date = null;
		for (Cookie cookie : cookies) {
			if (cookie.getName().equalsIgnoreCase("timeZone")) {
				timeZone = cookie.getValue();
			} else if (cookie.getName().equalsIgnoreCase("date")) {
				date = cookie.getValue();
			}
		}

		metadataService.setTimeZone(guestId, date, timeZone);
	}
}
