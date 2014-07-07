/*
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fluxtream.core.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class FlxAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private AuthenticationEntryPoint defaultEntryPoint;
    private AuthenticationEntryPoint mobileEntryPoint;

    @Autowired
    FlxRestApiEntryPoint restApiEntryPoint;

    public void commence(HttpServletRequest request,
                         HttpServletResponse response, AuthenticationException authException)
            throws IOException, ServletException {

        if (request.getRequestURI().startsWith("/api"))
            restApiEntryPoint.commence(request, response, authException);
        else if (request.getHeader("User-Agent").indexOf("Mobile") != -1) {
            mobileEntryPoint.commence(request, response, authException);
        } else
            defaultEntryPoint.commence(request, response, authException);
    }

    /**
     * EntryPoint which is used when no RequestMatcher returned true
     */
    public void setDefaultEntryPoint(AuthenticationEntryPoint defaultEntryPoint) {
        this.defaultEntryPoint = defaultEntryPoint;
    }

    public void setMobileEntryPoint(AuthenticationEntryPoint mobileEntryPoint) {
        this.mobileEntryPoint = mobileEntryPoint;
    }

}

