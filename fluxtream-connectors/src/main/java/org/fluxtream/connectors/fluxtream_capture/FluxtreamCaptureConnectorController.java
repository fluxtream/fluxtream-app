package org.fluxtream.connectors.fluxtream_capture;

import javax.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
@Controller()
@RequestMapping("/fluxtream_capture")
public class FluxtreamCaptureConnectorController {

    @RequestMapping(value = "/about")
    public ModelAndView about(final HttpServletRequest request) {
        return new ModelAndView("connectors/fluxtream_capture/about");
    }
}
