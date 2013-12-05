package com.fluxtream.connectors.evernote;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import com.evernote.auth.EvernoteAuth;
import com.evernote.auth.EvernoteService;
import com.evernote.clients.ClientFactory;
import com.evernote.clients.NoteStoreClient;
import com.evernote.edam.error.EDAMErrorCode;
import com.evernote.edam.error.EDAMUserException;
import com.evernote.edam.notestore.SyncChunk;
import com.evernote.edam.type.Note;
import com.evernote.edam.type.Notebook;
import com.evernote.edam.type.Tag;
import com.fluxtream.connectors.annotations.Updater;
import com.fluxtream.connectors.updaters.AbstractUpdater;
import com.fluxtream.connectors.updaters.RateLimitReachedException;
import com.fluxtream.connectors.updaters.UpdateInfo;
import com.fluxtream.services.ApiDataService;
import com.fluxtream.services.JPADaoService;
import com.fluxtream.utils.JPAUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Component
@Updater(prettyName = "Evernote", value = 17, objectTypes ={})
public class EvernoteUpdater extends AbstractUpdater {

    private static final int MAX_ENTRIES = 200;

    @Autowired
    JPADaoService jpaDaoService;

    @Override
    protected void updateConnectorDataHistory(final UpdateInfo updateInfo) throws Exception {
        try {
            fullSync(updateInfo);
        } catch (EDAMUserException e) {
            if (e.getErrorCode()==EDAMErrorCode.RATE_LIMIT_REACHED)
                throw new RateLimitReachedException();
        }
    }

    private void fullSync(UpdateInfo updateInfo) throws Exception {
        String token = guestService.getApiKeyAttribute(updateInfo.apiKey, "accessToken");

        EvernoteAuth evernoteAuth = new EvernoteAuth(EvernoteService.SANDBOX, token);
        ClientFactory factory = new ClientFactory(evernoteAuth);
        final NoteStoreClient noteStore = factory.createNoteStoreClient();

        SyncChunk chunk = noteStore.getSyncChunk(0, MAX_ENTRIES, true);
        final List<SyncChunk> chunks = new ArrayList<SyncChunk>();
        chunks.add(chunk);
        if (chunk.getChunkHighUSN()<chunk.getUpdateCount()) {
            chunk = noteStore.getSyncChunk(chunk.getChunkHighUSN(), MAX_ENTRIES, true);
            chunks.add(chunk);
        }
        for (SyncChunk syncChunk : chunks) {
            final Iterator<String> expungedTagsIterator = syncChunk.getExpungedTagsIterator();
            while(expungedTagsIterator.hasNext())
                removeEvernoteFacet(updateInfo, EvernoteTagFacet.class, expungedTagsIterator.next());
            final Iterator<Tag> tagsIterator = syncChunk.getTagsIterator();
            while(tagsIterator.hasNext())
                createOrUpdateTag(updateInfo, tagsIterator.next());

            final Iterator<String> expungedNotebooksIterator = syncChunk.getExpungedNotebooksIterator();
            while(expungedNotebooksIterator.hasNext())
                removeNotebook(updateInfo, expungedNotebooksIterator.next());
            final Iterator<Notebook> notebooksIterator = syncChunk.getNotebooksIterator();
            while(notebooksIterator.hasNext())
                createOrUpdateNotebook(updateInfo, notebooksIterator.next());

            final Iterator<String> expungedNotesIterator = syncChunk.getExpungedNotesIterator();
            while(expungedNotesIterator.hasNext())
                removeEvernoteFacet(updateInfo, EvernoteNoteFacet.class, expungedNotesIterator.next());
            final Iterator<Note> notesIterator = syncChunk.getNotesIterator();
            while(notesIterator.hasNext())
                createOrUpdateNote(updateInfo, notesIterator.next(), noteStore);
        }
    }

    private void createOrUpdateNote(final UpdateInfo updateInfo, final Note note, final NoteStoreClient noteStore) throws Exception {
        final ApiDataService.FacetQuery facetQuery = new ApiDataService.FacetQuery(
                "e.apiKeyId=? AND e.GUID=?",
                updateInfo.apiKey.getId(), note.getGuid());
        final ApiDataService.FacetModifier<EvernoteNoteFacet> facetModifier = new ApiDataService.FacetModifier<EvernoteNoteFacet>() {
            @Override
            public EvernoteNoteFacet createOrModify(EvernoteNoteFacet facet, final Long apiKeyId) throws Exception {
                if (facet == null) {
                    facet = new EvernoteNoteFacet(updateInfo.apiKey.getId());
                    facet.GUID = note.getGuid();
                    facet.guestId = updateInfo.apiKey.getGuestId();
                    facet.api = updateInfo.apiKey.getConnector().value();
                }
                if ((facet.contentHash==null||!Arrays.equals(facet.contentHash, note.getContentHash())||
                    facet.contentLength!=note.getContentLength())){
                    noteStore.getNoteContent(note.getGuid());
                }
                facet.USN = note.getUpdateSequenceNum();
                facet.contentHash = note.getContentHash();
                facet.contentLength = note.getContentLength();
                facet.content = note.getContent();
                facet.clearTags();
                facet.addTags(StringUtils.join(note.getTagNames(), ","), ',');
                facet.title = note.getTitle();
                facet.created = note.getCreated();
                facet.updated = note.getUpdated();
                facet.notebookGUID = note.getNotebookGuid();
                return facet;
            }
        };
        // we could use the resulting value (facet) from this call if we needed to do further processing on it (e.g. passing it on to the datastore)
        apiDataService.createOrReadModifyWrite(EvernoteNoteFacet.class, facetQuery, facetModifier, updateInfo.apiKey.getId());
    }

    private void removeEvernoteFacet(final UpdateInfo updateInfo, Class<? extends EvernoteFacet> clazz, final String guid) {
        jpaDaoService.execute(String.format("DELETE FROM %s facet WHERE " +
                                            "facet.apiKeyId=%s AND facet.GUID='%s'", JPAUtils.getEntityName(clazz),
                                            updateInfo.apiKey.getId(), guid));
    }

    private void createOrUpdateNotebook(final UpdateInfo updateInfo, final Notebook notebook) {
        //To change body of created methods use File | Settings | File Templates.
    }

    private void removeNotebook(final UpdateInfo updateInfo, final String guid) {
        removeEvernoteFacet(updateInfo, EvernoteNotebookFacet.class, guid);
        jpaDaoService.execute(String.format("DELETE FROM %s facet WHERE " +
                                            "facet.apiKeyId=%s AND facet.notebookGUID='%s'",
                                            JPAUtils.getEntityName(EvernoteNoteFacet.class),
                                            updateInfo.apiKey.getId(), guid));
    }

    private void createOrUpdateTag(final UpdateInfo updateInfo, final Tag tag) throws Exception {
        final ApiDataService.FacetQuery facetQuery = new ApiDataService.FacetQuery(
                "e.apiKeyId=? AND e.GUID=?",
                updateInfo.apiKey.getId(), tag.getGuid());
        final ApiDataService.FacetModifier<EvernoteTagFacet> facetModifier = new ApiDataService.FacetModifier<EvernoteTagFacet>() {
            @Override
            public EvernoteTagFacet createOrModify(EvernoteTagFacet facet, final Long apiKeyId) {
                if (facet == null) {
                    facet = new EvernoteTagFacet(updateInfo.apiKey.getId());
                    facet.GUID = tag.getGuid();
                    facet.guestId = updateInfo.apiKey.getGuestId();
                    facet.api = updateInfo.apiKey.getConnector().value();
                }
                facet.USN = tag.getUpdateSequenceNum();
                facet.name = tag.getName();
                return facet;
            }
        };
        // we could use the resulting value (facet) from this call if we needed to do further processing on it (e.g. passing it on to the datastore)
        apiDataService.createOrReadModifyWrite(EvernoteTagFacet.class, facetQuery, facetModifier, updateInfo.apiKey.getId());
    }

    @Override
    protected void updateConnectorData(final UpdateInfo updateInfo) throws Exception {
    }

}
