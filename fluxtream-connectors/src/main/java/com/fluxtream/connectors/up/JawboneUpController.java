package com.fluxtream.connectors.up;

import com.fluxtream.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * User: candide
 * Date: 26/01/14
 * Time: 09:56
 */
@Controller
@RequestMapping(value = "/up")
public class JawboneUpController {

    @Autowired
    Configuration env;


}
