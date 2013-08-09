package com.fluxtream.mvc.controllers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.fluxtream.auth.AuthHelper;
import com.fluxtream.connectors.Connector;
import com.fluxtream.domain.Guest;
import com.fluxtream.domain.Notification;
import com.fluxtream.mvc.models.StatusModel;
import com.fluxtream.services.GuestService;
import com.fluxtream.services.NotificationsService;
import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
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

    @RequestMapping(value = "/form/{connectorName}")
    public String uploadForm(@PathVariable("connectorName") String connectorName,
                             HttpServletRequest request) {
        final Connector connector = Connector.getConnector(connectorName);
        ResourceBundle res = ResourceBundle.getBundle("messages/connectors");
        String message = res.getString(connectorName + ".upload");
        request.setAttribute("message", message);
        request.setAttribute("connector", connector);
        return "upload";
    }

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
    public void upload(MultipartHttpServletRequest request, @RequestParam("connectorName") String connectorName) throws IOException {

        System.out.println(connectorName);
        try {
            Iterator<String> itr =  request.getFileNames();

            MultipartFile mpf = request.getFile(itr.next());
            System.out.println(mpf.getOriginalFilename() +" uploaded!");

            final String randomFilename = UUID.randomUUID().toString();
            File f = File.createTempFile(randomFilename, "unknown");

            IOUtils.copy(mpf.getInputStream(), new FileOutputStream(f));

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
