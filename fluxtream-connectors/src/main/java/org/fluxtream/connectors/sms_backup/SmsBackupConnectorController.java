package org.fluxtream.connectors.sms_backup;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import org.fluxtream.core.Configuration;
import org.fluxtream.core.auth.AuthHelper;
import org.fluxtream.core.auth.CoachRevokedException;
import org.fluxtream.core.utils.ImageUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller()
@RequestMapping("/smsBackup")
public class SmsBackupConnectorController {

    @Autowired
    Configuration env;

    @RequestMapping(value="/attachment/{apiKeyId}/{fileName}")
    public void getAttachment(@PathVariable("apiKeyId") long apiKeyId,
                            @PathVariable("fileName") String fileName,
                            @RequestParam(value="s", required=false) Integer maxSideLength,
                            HttpServletResponse response) throws IOException, CoachRevokedException {
        File file = SmsBackupUpdater.getAttachmentFile(env.targetEnvironmentProps.getString("btdatastore.db.location"),
                                            AuthHelper.getVieweeId(), apiKeyId,fileName);
        if (!file.exists()){
            response.sendError(404);
        }
        if (maxSideLength == null){
            IOUtils.copy(new FileInputStream(file), response.getOutputStream());
        }
        else{     //TODO: make sure we actually have a photo
            byte[] photoData = IOUtils.toByteArray(new FileInputStream(file));
            IOUtils.write(ImageUtils.createJpegThumbnail(photoData,maxSideLength).getBytes(),response.getOutputStream());
        }

    }


}
