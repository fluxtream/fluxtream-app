package org.fluxtream.mvc.controllers;

import org.apache.oltu.oauth2.as.request.OAuthAuthzRequest;
import org.apache.oltu.oauth2.as.request.OAuthTokenRequest;
import org.apache.oltu.oauth2.as.response.OAuthASResponse;
import org.apache.oltu.oauth2.common.error.OAuthError;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.OAuthResponse;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.apache.oltu.oauth2.common.message.types.ResponseType;
import org.apache.oltu.oauth2.common.message.types.TokenType;
import org.fluxtream.core.auth.AuthHelper;
import org.fluxtream.core.domain.Guest;
import org.fluxtream.core.domain.oauth2.Application;
import org.fluxtream.core.domain.oauth2.AuthorizationCode;
import org.fluxtream.core.domain.oauth2.AuthorizationCodeResponse;
import org.fluxtream.core.domain.oauth2.AuthorizationToken;
import org.fluxtream.core.services.OAuth2MgmtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * User: candide
 * Date: 08/04/14
 * Time: 14:26
 */
@Controller
@RequestMapping("/auth/oauth2")
public class OAuth2ProviderController {

    @Autowired
    OAuth2MgmtService oAuth2MgmtService;

    @RequestMapping(
            value = "/authorize",
            method = {RequestMethod.GET, RequestMethod.POST})
    public @ResponseBody String receiveAuthorizationCodeRequest(final HttpServletRequest request,
                                                                final HttpServletResponse response)
            throws IOException, OAuthSystemException {

        // Create the OAuth request from the HTTP request.
        OAuthAuthzRequest oauthRequest;
        try {
            oauthRequest = new OAuthAuthzRequest(request);
        }
        // The request does not conform to the RFC, so we return a HTTP 400
        // with a reason.
        catch (OAuthProblemException e) {
            // Create the OAuth response.
            OAuthResponse oauthResponse = OAuthASResponse.errorResponse(HttpServletResponse.SC_BAD_REQUEST).error(e)
                    .buildJSONMessage();

            // Set the status and return the error message.
            response.setStatus(oauthResponse.getResponseStatus());
            return oauthResponse.getBody();
        }

        // Validate that the user is requesting a "code" response type, which
        // is the only response type we accept.
        try {
            if (!ResponseType.CODE.toString().equals(oauthRequest.getResponseType())) {
                // Create the OAuth response.
                OAuthResponse oauthResponse = OAuthASResponse.errorResponse(HttpServletResponse.SC_BAD_REQUEST)
                        .setError(OAuthError.CodeResponse.UNSUPPORTED_RESPONSE_TYPE)
                        .setErrorDescription("The response type must be '" +
                                             ResponseType.CODE.toString() +
                                             "' but was instead: "
                                             + oauthRequest.getResponseType())
                        .setState(oauthRequest.getState())
                        .buildJSONMessage();

                // Set the status and return the error message.
                response.setStatus(oauthResponse.getResponseStatus());
                return oauthResponse.getBody();
            }
        }
        catch (IllegalArgumentException e) {
            // Create the OAuth response.
            OAuthResponse oauthResponse = OAuthASResponse.errorResponse(HttpServletResponse.SC_BAD_REQUEST)
                    .setError(OAuthError.CodeResponse.UNSUPPORTED_RESPONSE_TYPE)
                    .setErrorDescription("The response type is unknown: " + oauthRequest.getResponseType())
                    .setState(oauthRequest.getState())
                    .buildJSONMessage();

            // Set the status and return the error message.
            response.setStatus(oauthResponse.getResponseStatus());
            return oauthResponse.getBody();
        }

        // Make sure a redirect URI was given.
        if (oauthRequest.getRedirectURI() == null) {
            // Create the OAuth response.
            OAuthResponse oauthResponse = OAuthASResponse.errorResponse(HttpServletResponse.SC_BAD_REQUEST)
                    .setError(OAuthError.CodeResponse.INVALID_REQUEST)
                    .setErrorDescription("A redirect URI must be given.")
                    .setState(oauthRequest.getState())
                    .buildJSONMessage();

            // Set the status and return the error message.
            response.setStatus(oauthResponse.getResponseStatus());
            return oauthResponse.getBody();
        }

        // Attempt to get the third-party.
        Application application = oAuth2MgmtService.getApplicationForClientId(oauthRequest.getClientId());
        // If the third-party is unknown, reject the request.
        if (application == null) {
            // Create the OAuth response.
            OAuthResponse oauthResponse = OAuthASResponse.errorResponse(HttpServletResponse.SC_BAD_REQUEST).setError
                    (OAuthError.CodeResponse.INVALID_REQUEST).setErrorDescription(
                        "The client ID is unknown: " + oauthRequest.getClientId()
            ).setState(oauthRequest.getState()).buildJSONMessage();

            // Set the status and return the error message.
            response.setStatus(oauthResponse.getResponseStatus());
            return oauthResponse.getBody();
        }

        // Create the temporary code to be granted or rejected by the user.
        AuthorizationCode code = oAuth2MgmtService.issueAuthorizationCode(application.getId(),
                                                                          oauthRequest.getScopes(),
                                                                          oauthRequest.getState());

        // Set the redirect.
        response.sendRedirect(OAuthASResponse.authorizationResponse(request, HttpServletResponse.SC_FOUND)
                                      .setCode(code.code)
                                      .location("Authorize.html")
                                      //.setScope(scopeBuilder.toString())
                                      .setParam("name", application.name)
                                      .setParam("description", application.description)
                                      .setParam("redirectUri", oauthRequest.getRedirectURI())
                                      .buildQueryMessage().getLocationUri()
        );
        // Since we are redirecting the user, we don't need to return anything.
        return null;
    }

    @RequestMapping(value = "Authorize.html")
    public ModelAndView getAuthorizeForm(@RequestParam(value="code",required=true) String code,
                                         @RequestParam(value="name",required=true) String name,
                                         @RequestParam(value="description",required=true) String description,
                                         @RequestParam(value="redirectUri",required=true) String redirectUri) {
        final ModelAndView mav = new ModelAndView("oauth2/Authorize");
        mav.addObject("code", code);
        mav.addObject("name", name);
        mav.addObject("description", description);
        mav.addObject("redirectUri", redirectUri);
        return mav;
    }

    @RequestMapping(value = "/authorization", method = RequestMethod.POST)
    public void authenticateAuthorizationCodeRequest(
            @RequestParam(value = "granted", required = true) final boolean granted,
            @RequestParam(value = "redirectUri", required = true) final String redirectUri,
            @RequestParam(value = "code", required = true) final String code,
            final HttpServletRequest request,
            final HttpServletResponse response) throws IOException, OAuthSystemException
    {
        // Get the user. If the user's credentials are invalid for whatever
        // reason, an exception will be thrown and the page will echo back the
        // reason.
        Guest guest = AuthHelper.getGuest();

        // Get the authorization code.
        AuthorizationCode authCode = oAuth2MgmtService.getCode(code);

        // If the code is unknown, we cannot redirect back to the third-party
        // because we don't know who they are.
        if (authCode == null) {
            throw new RuntimeException("The authorization code is unknown.");
        }

        // Verify that the code has not yet expired.
        if (System.currentTimeMillis() > authCode.expirationTime) {
            response.sendRedirect(OAuthASResponse.errorResponse(HttpServletResponse.SC_BAD_REQUEST)
                                          .setError(OAuthError.CodeResponse.ACCESS_DENIED)
                                          .setErrorDescription("The code has expired.")
                                          .location(redirectUri).setState(authCode.state).buildQueryMessage()
                                          .getLocationUri()
            );
            return;
        }

        // Get the response if it already exists.
        AuthorizationCodeResponse codeResponse = oAuth2MgmtService.getResponse(code);

        // If the response does not exist, attempt to create a new one and
        // save it.
        if (codeResponse == null) {
            // Create the new code.
            codeResponse = new AuthorizationCodeResponse(authCode, guest.getId(), granted);

            // Store it.
            oAuth2MgmtService.storeVerification(codeResponse);
        }

        // Make sure it is being verified by the same user.
        else if (!guest.getId().equals(codeResponse.guestId)) {

            response.sendRedirect(OAuthASResponse.errorResponse(HttpServletResponse.SC_UNAUTHORIZED)
                                          .setError(OAuthError.CodeResponse.ACCESS_DENIED)
                                          .setErrorDescription("The code has already been verified by another user.")
                                          .location(redirectUri).setState(authCode.state)
                                          .buildQueryMessage()
                                          .getLocationUri()
            );
        }
        // Make sure the same grant response is being made.
        else if (granted == codeResponse.granted) {
            response.sendRedirect(OAuthASResponse.errorResponse(HttpServletResponse.SC_UNAUTHORIZED)
                                          .setError(OAuthError.CodeResponse.ACCESS_DENIED)
                                          .setErrorDescription("The user has re-submitted the same authorization code twice with competing grant values.")
                                          .location(redirectUri)
                                          .setState(authCode.state)
                                          .buildQueryMessage()
                                          .getLocationUri()
            );
        }
        // Otherwise, this is simply a repeat of the same request as before,
        // and we can simply ignore it.

        // Redirect the user back to the third-party with the authorization
        // code and state.
        response.sendRedirect(OAuthASResponse.authorizationResponse(request, HttpServletResponse.SC_OK).location(redirectUri)
                                      .setCode(authCode.code)
                                      .setParam("state", authCode.state)
                                      .buildQueryMessage()
                                      .getLocationUri()
        );
    }

    @RequestMapping(value = "/token", method = RequestMethod.POST)
    public @ResponseBody String createAuthorizationToken(final HttpServletRequest request,
                                                         final HttpServletResponse response)
            throws OAuthSystemException, IOException
    {
        // Attempt to build an OAuth request from the HTTP request.
        OAuthTokenRequest oauthRequest;
        try {
            oauthRequest = new OAuthTokenRequest(request);
        }
        // If the HTTP request was not a valid OAuth token request, then we
        // have no other choice but to reject it as a bad request.
        catch (OAuthProblemException e) {
            // Build the OAuth response.
            OAuthResponse oauthResponse = OAuthResponse.errorResponse(HttpServletResponse.SC_BAD_REQUEST).error(e)
                    .buildJSONMessage();

            // Set the HTTP response status code from the OAuth response.
            response.setStatus(oauthResponse.getResponseStatus());

            // Return the error message.
            return oauthResponse.getBody();
        }

        // Attempt to get the client.
        Application application = oAuth2MgmtService.getApplicationForClientId(oauthRequest.getClientId());
        // If the client is unknown, respond as such.
        if (application == null) {
            // Create the OAuth response.
            OAuthResponse oauthResponse = OAuthASResponse.errorResponse(HttpServletResponse.SC_BAD_REQUEST)
                    .setError(OAuthError.TokenResponse.INVALID_CLIENT)
                    .setErrorDescription("The client is unknown: " + oauthRequest.getClientId())
                    .buildJSONMessage();

            // Set the status and return the error message.
            response.setStatus(oauthResponse.getResponseStatus());
            return oauthResponse.getBody();
        }

        // Get the given client secret.
        String applicationSecret = oauthRequest.getClientSecret();
        if (applicationSecret == null) {
            // Create the OAuth response.
            OAuthResponse oauthResponse = OAuthASResponse.errorResponse(HttpServletResponse.SC_BAD_REQUEST)
                    .setError(OAuthError.TokenResponse.INVALID_CLIENT)
                    .setErrorDescription("The client secret is required.")
                    .buildJSONMessage();

            // Set the status and return the error message.
            response.setStatus(oauthResponse.getResponseStatus());
            return oauthResponse.getBody();
        }
        // Make sure the client gave the right secret.
        else if (!applicationSecret.equals(application.sharedSecret)) {
            // Create the OAuth response.
            OAuthResponse oauthResponse = OAuthASResponse.errorResponse(HttpServletResponse.SC_BAD_REQUEST)
                    .setError(OAuthError.TokenResponse.INVALID_CLIENT)
                    .setErrorDescription("The client secret is incorrect.")
                    .buildJSONMessage();

            // Set the status and return the error message.
            response.setStatus(oauthResponse.getResponseStatus());
            return oauthResponse.getBody();
        }

        // Get the grant-type.
        GrantType grantType;
        String grantTypeString = oauthRequest.getGrantType();
        if (GrantType.AUTHORIZATION_CODE.toString().equals(grantTypeString)) {
            grantType = GrantType.AUTHORIZATION_CODE;
        }
        else if (GrantType.CLIENT_CREDENTIALS.toString().equals(grantTypeString)) {
            grantType = GrantType.CLIENT_CREDENTIALS;
        }
        else if (GrantType.PASSWORD.toString().equals(grantTypeString)) {
            grantType = GrantType.PASSWORD;
        }
        else if (GrantType.REFRESH_TOKEN.toString().equals(grantTypeString)) {
            grantType = GrantType.REFRESH_TOKEN;
        }
        else {
            // Create the OAuth response.
            OAuthResponse oauthResponse = OAuthASResponse.errorResponse(HttpServletResponse.SC_BAD_REQUEST)
                    .setError(OAuthError.TokenResponse.INVALID_GRANT)
                    .setErrorDescription("The grant type is unknown: " + grantTypeString)
                    .buildJSONMessage();
            // Set the status and return the error message.
            response.setStatus(oauthResponse.getResponseStatus());
            return oauthResponse.getBody();
        }

        // Handle the different types of token requests.
        AuthorizationToken token;
        if (GrantType.AUTHORIZATION_CODE.equals(grantType)) {
            // Attempt to get the code.
            String codeString = oauthRequest.getCode();
            if (codeString == null) {
                // Create the OAuth response.
                OAuthResponse oauthResponse = OAuthASResponse.errorResponse(HttpServletResponse.SC_BAD_REQUEST)
                        .setError(OAuthError.TokenResponse.INVALID_REQUEST)
                        .setErrorDescription("An authorization code must be given to be exchanged  for an authorization token.")
                        .buildJSONMessage();

                // Set the status and return the error message.
                response.setStatus(oauthResponse.getResponseStatus());
                return oauthResponse.getBody();
            }

            // Attempt to lookup the actual AuthorizationCode object.
            AuthorizationCode code = oAuth2MgmtService.getCode(codeString);
            // If the code doesn't exist, reject the request.
            if (code == null) {
                // Create the OAuth response.
                OAuthResponse oauthResponse = OAuthASResponse.errorResponse(HttpServletResponse.SC_BAD_REQUEST)
                        .setError(OAuthError.TokenResponse.INVALID_REQUEST)
                        .setErrorDescription("The given authorization code is unknown: " + codeString)
                        .buildJSONMessage();

                // Set the status and return the error message.
                response.setStatus(oauthResponse.getResponseStatus());
                return oauthResponse.getBody();
            }

            // Verify that the client asking for a token is the same as the one
            // that requested the code.
            if (code.applicationId != application.getId()) {
                // Create the OAuth response.
                OAuthResponse oauthResponse = OAuthASResponse.errorResponse(HttpServletResponse.SC_BAD_REQUEST)
                        .setError(OAuthError.TokenResponse.INVALID_REQUEST)
                        .setErrorDescription("This client is not allowed to reference this code: " + codeString)
                        .buildJSONMessage();

                // Set the status and return the error message.
                response.setStatus(oauthResponse.getResponseStatus());
                return oauthResponse.getBody();
            }

            // If the code has expired, reject the request.
            if (System.currentTimeMillis() > code.expirationTime) {
                // Create the OAuth response.
                OAuthResponse oauthResponse = OAuthASResponse.errorResponse(HttpServletResponse.SC_BAD_REQUEST)
                        .setError(OAuthError.TokenResponse.INVALID_REQUEST)
                        .setErrorDescription("The given authorization code has expired: " + codeString)
                        .buildJSONMessage();

                // Set the status and return the error message.
                response.setStatus(oauthResponse.getResponseStatus());
                return oauthResponse.getBody();
            }

            // Use the code to lookup the response information and error out if
            // a user has not yet verified it.
            AuthorizationCodeResponse codeResponse = oAuth2MgmtService.getResponse(code.code);
            if (codeResponse == null) {
                // Create the OAuth response.
                OAuthResponse oauthResponse = OAuthASResponse.errorResponse(HttpServletResponse.SC_BAD_REQUEST)
                        .setError(OAuthError.TokenResponse.INVALID_REQUEST)
                        .setErrorDescription("A user has not yet verified the code: " + codeString)
                        .buildJSONMessage();

                // Set the status and return the error message.
                response.setStatus(oauthResponse.getResponseStatus());
                return oauthResponse.getBody();
            }

            // Determine if the user granted access and, if not, error out.
            if (!codeResponse.granted) {
                // Create the OAuth response.
                OAuthResponse oauthResponse = OAuthASResponse.errorResponse(HttpServletResponse.SC_BAD_REQUEST)
                        .setError(OAuthError.TokenResponse.INVALID_REQUEST)
                        .setErrorDescription("The user denied the authorization: " + codeString)
                        .buildJSONMessage();

                // Set the status and return the error message.
                response.setStatus(oauthResponse.getResponseStatus());
                return oauthResponse.getBody();
            }

            // Create a new token.
            token = new AuthorizationToken(codeResponse);
        }
        // Handle a third-party refreshing an existing token.
        else if (GrantType.REFRESH_TOKEN.equals(grantType)) {
            // Get the refresh token from the request.
            String refreshToken = oauthRequest.getRefreshToken();
            if (refreshToken == null) {
                // Create the OAuth response.
                OAuthResponse oauthResponse = OAuthASResponse.errorResponse(HttpServletResponse.SC_BAD_REQUEST)
                        .setError(OAuthError.TokenResponse.INVALID_REQUEST)
                        .setErrorDescription("A refresh token must be given to be exchanged for a new authorization token.")
                        .buildJSONMessage();

                // Set the status and return the error message.
                response.setStatus(oauthResponse.getResponseStatus());
                return oauthResponse.getBody();
            }
            // Use the refresh token to lookup the actual refresh token.
            AuthorizationToken currentToken = oAuth2MgmtService.getTokenFromRefreshToken(refreshToken);
            if (currentToken == null) {
                // Create the OAuth response.
                OAuthResponse oauthResponse = OAuthASResponse.errorResponse(HttpServletResponse.SC_BAD_REQUEST)
                        .setError(OAuthError.TokenResponse.INVALID_REQUEST)
                        .setErrorDescription("The refresh token is unknown.")
                        .buildJSONMessage();

                // Set the status and return the error message.
                response.setStatus(oauthResponse.getResponseStatus());
                return oauthResponse.getBody();
            }

            // Verify that the client asking for a token is the same as the one
            // that was issued the refresh token.
            // This is probably a very serious offense and should probably
            // raise some serious red flags!
            if (!oAuth2MgmtService.getApplicationForToken(currentToken).getId().equals(application.getId())) {

                // Create the OAuth response.
                OAuthResponse oauthResponse = OAuthASResponse.errorResponse(HttpServletResponse.SC_BAD_REQUEST)
                        .setError(OAuthError.TokenResponse.INVALID_REQUEST)
                        .setErrorDescription("This token does not belong to this client.")
                        .buildJSONMessage();

                // Set the status and return the error message.
                response.setStatus(oauthResponse.getResponseStatus());
                return oauthResponse.getBody();
            }

            // Create a new authorization token from the current one.
            token = new AuthorizationToken(currentToken);
        }
        // If the grant-type is unknown, then we do not yet understand how
        // the request is built and, therefore, can do nothing more than
        // reject it via an OmhException.
        else {
            // Create the OAuth response.
            OAuthResponse oauthResponse = OAuthASResponse.errorResponse(HttpServletResponse.SC_BAD_REQUEST)
                    .setError(OAuthError.TokenResponse.UNSUPPORTED_GRANT_TYPE)
                    .setErrorDescription("The grant type must be one of '" + GrantType.AUTHORIZATION_CODE.toString() +
                        "' or '" + GrantType.REFRESH_TOKEN.toString() + "': " + grantType.toString())
                    .buildJSONMessage();

            // Set the status and return the error message.
            response.setStatus(oauthResponse.getResponseStatus());
            return oauthResponse.getBody();
        }

        // Store the new token.
        oAuth2MgmtService.storeToken(token);

        // Build the response.
        OAuthResponse oauthResponse = OAuthASResponse.tokenResponse(HttpServletResponse.SC_OK)
                .setAccessToken(token.accessToken)
                .setExpiresIn(Long.valueOf(token.getExpirationIn() / 1000).toString())
                .setRefreshToken(token.refreshToken)
                .setTokenType(TokenType.BEARER.toString())
                .buildJSONMessage();

        // Set the status.
        response.setStatus(oauthResponse.getResponseStatus());

        // Set the content-type.
        response.setContentType("application/json");

        // Add the headers.
        Map<String, String> headers = oauthResponse.getHeaders();
        for (String headerKey : headers.keySet()) {
            response.addHeader(headerKey, headers.get(headerKey));
        }

        // Return the body.
        return oauthResponse.getBody();
    }
}
