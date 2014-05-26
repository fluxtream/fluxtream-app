package org.fluxtream.mvc.controllers;

import org.fluxtream.core.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * User: candide
 * Date: 26/05/14
 * Time: 20:12
 */
@Controller
public class CorsController {

    @Autowired
    Configuration env;

    @RequestMapping(value = "/cors", method= RequestMethod.POST, produces = "text/html; charset=utf-8")
    public void getCorsHeaders(HttpServletRequest request,
                               HttpServletResponse response) {
    }

}
