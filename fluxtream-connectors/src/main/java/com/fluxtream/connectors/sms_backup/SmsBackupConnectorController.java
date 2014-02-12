package com.fluxtream.connectors.sms_backup;

import java.util.ArrayList;
import java.util.List;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import com.fluxtream.auth.AuthHelper;
import com.fluxtream.connectors.Connector;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.services.ConnectorUpdateService;
import com.fluxtream.services.GuestService;
import com.fluxtream.services.SettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller()
@RequestMapping("/smsBackup")
public class SmsBackupConnectorController {

}
