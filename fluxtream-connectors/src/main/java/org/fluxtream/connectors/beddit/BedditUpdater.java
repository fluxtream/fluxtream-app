package org.fluxtream.connectors.beddit;


import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson.JacksonFactory;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.fluxtream.core.connectors.annotations.Updater;
import org.fluxtream.core.connectors.updaters.AbstractUpdater;
import org.fluxtream.core.connectors.updaters.UpdateFailedException;
import org.fluxtream.core.connectors.updaters.UpdateInfo;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.domain.Notification;
import org.fluxtream.core.utils.Utils;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Updater(prettyName = "Beddit", value = 352, objectTypes={SleepFacet.class})
public class BedditUpdater extends AbstractUpdater {


    @Override
    protected void updateConnectorDataHistory(UpdateInfo updateInfo) throws Exception {
        updateConnectorData(updateInfo);

    }

    @Override
    protected void updateConnectorData(UpdateInfo updateInfo) throws Exception {
       System.out.println("we should update!");

    }
}