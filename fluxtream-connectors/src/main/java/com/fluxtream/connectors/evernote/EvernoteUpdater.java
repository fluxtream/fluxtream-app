package com.fluxtream.connectors.evernote;

import java.util.List;
import com.evernote.auth.EvernoteAuth;
import com.evernote.auth.EvernoteService;
import com.evernote.clients.ClientFactory;
import com.evernote.clients.NoteStoreClient;
import com.evernote.edam.type.Notebook;
import com.fluxtream.connectors.annotations.Updater;
import com.fluxtream.connectors.updaters.AbstractUpdater;
import com.fluxtream.connectors.updaters.UpdateInfo;
import org.springframework.stereotype.Component;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Component
@Updater(prettyName = "Evernote", value = 17, objectTypes ={})
public class EvernoteUpdater extends AbstractUpdater {

    @Override
    protected void updateConnectorDataHistory(final UpdateInfo updateInfo) throws Exception {

        String token = guestService.getApiKeyAttribute(updateInfo.apiKey, "accessToken");

        EvernoteAuth evernoteAuth = new EvernoteAuth(EvernoteService.SANDBOX, token);
        ClientFactory factory = new ClientFactory(evernoteAuth);
        final NoteStoreClient noteStore = factory.createNoteStoreClient();

        List<Notebook> notebooks = noteStore.listNotebooks();
        System.out.println(notebooks);
    }

    @Override
    protected void updateConnectorData(final UpdateInfo updateInfo) throws Exception {
    }

}
