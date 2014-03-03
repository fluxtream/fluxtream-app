package org.fluxtream.mvc.controllers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.UUID;
import javax.servlet.http.HttpServletResponse;
import org.fluxtream.auth.AuthHelper;
import org.fluxtream.connectors.Connector;
import org.fluxtream.connectors.FileUploadSupport;
import org.fluxtream.connectors.updaters.AbstractUpdater;
import org.fluxtream.domain.ApiKey;
import org.fluxtream.domain.Guest;
import org.fluxtream.domain.Notification;
import org.fluxtream.mvc.models.StatusModel;
import org.fluxtream.services.GuestService;
import org.fluxtream.services.NotificationsService;
import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;
import org.codehaus.plexus.util.ExceptionUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

/**
 * User: candide
 * Date: 08/08/13
 * Time: 08:34
 */
@Controller
@RequestMapping(value = "/upload")
public class UploadController {

    Gson gson = new Gson();

    @Autowired
    NotificationsService notificationsService;

    @Autowired
    GuestService guestService;

    @Autowired
    @Qualifier("FileUploadWorker")
    ThreadPoolTaskExecutor executor;

    @Autowired
    BeanFactory beanFactory;


    @RequestMapping(value = "/addConnector", method = RequestMethod.POST)
    public void addUploadConnector(@RequestParam("connectorName") String connectorName,
                                   HttpServletResponse response) throws IOException {
        final Connector connector = Connector.getConnector(connectorName);
        final StatusModel statusModel = new StatusModel(true, "Successfully added upload-only connector \"" + connectorName + "\"");
        final Guest guest = AuthHelper.getGuest();

        guestService.createApiKey(guest.getId(), connector);

        notificationsService.addNotification(guest.getId(), Notification.Type.INFO, "You just added an upload-only connector for " + connector.prettyName() + "<br>" +
                                                                              "Please head to <a href=\"javascript:App.manageConnectors()\">Manage Connectors</a>, " +
                                                                              "identify your new connector, and upload your data from there.");
        response.getWriter().write(gson.toJson(statusModel));
    }

    @RequestMapping("/")
    public void upload(MultipartHttpServletRequest request,
                       HttpServletResponse response,
                       @RequestParam("apiKeyId") long apiKeyId) throws IOException {
        try {
            Iterator<String> itr =  request.getFileNames();

            MultipartFile mpf = request.getFile(itr.next());
            final String originalFilename = mpf.getOriginalFilename();
            System.out.println(originalFilename +" uploaded!");

            final String randomFilename = UUID.randomUUID().toString() + "-";
            final File f = File.createTempFile(randomFilename, ".unknown");

            IOUtils.copy(mpf.getInputStream(), new FileOutputStream(f));

            final long guestId = AuthHelper.getGuestId();
            final ApiKey apiKey = guestService.getApiKey(apiKeyId);
            if (apiKey.getGuestId()!=guestId)
                throw new RuntimeException("Attempt to upload file associated to another user's " +
                                           "ApiKey! apiKeyId=" + apiKeyId +
                                           ", guestId="  + AuthHelper.getGuestId());


            final Connector connector = apiKey.getConnector();
            executor.execute( new Runnable() {

                @Override
                public void run() {
                    final AbstractUpdater bean = beanFactory.getBean(connector.getUpdaterClass());
                    FileUploadSupport handler = (FileUploadSupport) bean;
                    try {
                        handler.importFile(apiKey, f);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                        notificationsService.addExceptionNotification(guestId, Notification.Type.ERROR, "There was a problem importing your " + connector.prettyName() + " data.<br>" +
                                                                                                        "Please check the type and format of your file.", ExceptionUtils.getStackTrace(e));
                    }
                }
            });
            notificationsService.addNotification(guestId, Notification.Type.INFO,
                                                 "We are busy importing your " + connector.prettyName() + " data.<br>" +
                                                 "You should be able to see it shortly.");

            final String json = gson.toJson(new StatusModel(true, "File was successfully uploaded: " + originalFilename));
            response.getWriter().write(json);

        } catch (Throwable e) {
            final String json = gson.toJson(new StatusModel(false, "Couldn't parse your file: " + ExceptionUtils.getStackTrace(e)));
            response.getWriter().write(json);
            e.printStackTrace();
        }
    }

}
