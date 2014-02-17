package com.fluxtream.connectors.sms_backup;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.fluxtream.Configuration;
import com.fluxtream.auth.AuthHelper;
import com.fluxtream.auth.CoachRevokedException;
import com.fluxtream.connectors.Connector;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.services.ConnectorUpdateService;
import com.fluxtream.services.GuestService;
import com.fluxtream.services.SettingsService;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller()
@RequestMapping("/smsBackup")
public class SmsBackupConnectorController {

    @Autowired
    Configuration env;

    @RequestMapping(value="/attachment/{apiKeyId}/{fileName}")
    public void getAttachment(@PathVariable("apiKeyId") long apiKeyId,
                            @PathVariable("fileName") String fileName,
                            HttpServletResponse response) throws IOException, CoachRevokedException {
        File file = SmsBackupUpdater.getAttachmentFile(env.targetEnvironmentProps.getString("btdatastore.db.location"),
                                            AuthHelper.getVieweeId(), apiKeyId,fileName);
        if (!file.exists()){
            response.sendError(404);
        }
        IOUtils.copy(new FileInputStream(file), response.getOutputStream());

    }


}
