package com.fluxtream.connectors.evernote;

import java.util.Arrays;
import java.util.Iterator;
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
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;
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

    private final String LAST_UPDATE_COUNT = "lastUpdateCount";
    private static final String LAST_SYNC_TIME = "lastSyncTime";

    @Autowired
    JPADaoService jpaDaoService;

    @Override
    protected void updateConnectorDataHistory(final UpdateInfo updateInfo) throws Exception {
        try {
            performSync(updateInfo);
        } catch (EDAMUserException e) {
            if (e.getErrorCode()==EDAMErrorCode.RATE_LIMIT_REACHED)
                throw new RateLimitReachedException();
        }
    }

    @Override
    protected void updateConnectorData(final UpdateInfo updateInfo) throws Exception {
        try {
            performSync(updateInfo);
        } catch (EDAMUserException e) {
            if (e.getErrorCode()==EDAMErrorCode.RATE_LIMIT_REACHED)
                throw new RateLimitReachedException();
        }
    }

    private void performSync(UpdateInfo updateInfo) throws Exception {
        String token = guestService.getApiKeyAttribute(updateInfo.apiKey, "accessToken");

        EvernoteAuth evernoteAuth = new EvernoteAuth(EvernoteService.SANDBOX, token);
        ClientFactory factory = new ClientFactory(evernoteAuth);
        final NoteStoreClient noteStore = factory.createNoteStoreClient();

        final String lastUpdateCountAtt = guestService.getApiKeyAttribute(updateInfo.apiKey, LAST_UPDATE_COUNT);
        int lastUpdateCount = 0;
        if (lastUpdateCountAtt!=null)
            lastUpdateCount = Integer.valueOf(lastUpdateCountAtt);

        SyncChunk chunk = noteStore.getSyncChunk(lastUpdateCount, MAX_ENTRIES, true);
        updateSyncChunk(updateInfo, noteStore, chunk);
        if (chunk.getChunkHighUSN()<chunk.getUpdateCount()) {
            chunk = noteStore.getSyncChunk(chunk.getChunkHighUSN(), MAX_ENTRIES, true);
            updateSyncChunk(updateInfo, noteStore, chunk);
            int serviceLastUpdateCount = chunk.getUpdateCount();
            final long serviceLastSyncTime = chunk.getCurrentTime();
            guestService.setApiKeyAttribute(updateInfo.apiKey, LAST_UPDATE_COUNT, String.valueOf(serviceLastUpdateCount));
            guestService.setApiKeyAttribute(updateInfo.apiKey, LAST_SYNC_TIME, String.valueOf(serviceLastSyncTime));
        }
    }

    private void updateSyncChunk(final UpdateInfo updateInfo, final NoteStoreClient noteStore, final SyncChunk chunk) throws Exception {
        final Iterator<String> expungedTagsIterator = chunk.getExpungedTagsIterator();
        while(expungedTagsIterator.hasNext())
            removeEvernoteFacet(updateInfo, EvernoteTagFacet.class, expungedTagsIterator.next());
        final Iterator<Tag> tagsIterator = chunk.getTagsIterator();
        while(tagsIterator.hasNext())
            createOrUpdateTag(updateInfo, tagsIterator.next());

        final Iterator<String> expungedNotebooksIterator = chunk.getExpungedNotebooksIterator();
        while(expungedNotebooksIterator.hasNext())
            removeNotebook(updateInfo, expungedNotebooksIterator.next());
        final Iterator<Notebook> notebooksIterator = chunk.getNotebooksIterator();
        while(notebooksIterator.hasNext())
            createOrUpdateNotebook(updateInfo, notebooksIterator.next());

        final Iterator<String> expungedNotesIterator = chunk.getExpungedNotesIterator();
        while(expungedNotesIterator.hasNext())
            removeEvernoteFacet(updateInfo, EvernoteNoteFacet.class, expungedNotesIterator.next());
        final Iterator<Note> notesIterator = chunk.getNotesIterator();
        while(notesIterator.hasNext())
            createOrUpdateNote(updateInfo, notesIterator.next(), noteStore);
    }

    private void createOrUpdateNote(final UpdateInfo updateInfo, final Note note, final NoteStoreClient noteStore) throws Exception {
        final ApiDataService.FacetQuery facetQuery = new ApiDataService.FacetQuery(
                "e.apiKeyId=? AND e.guid=?",
                updateInfo.apiKey.getId(), note.getGuid());
        final ApiDataService.FacetModifier<EvernoteNoteFacet> facetModifier = new ApiDataService.FacetModifier<EvernoteNoteFacet>() {
            @Override
            public EvernoteNoteFacet createOrModify(EvernoteNoteFacet facet, final Long apiKeyId) throws Exception {
                if (facet == null) {
                    facet = new EvernoteNoteFacet(updateInfo.apiKey.getId());
                    extractCommonFacetData(facet, updateInfo);
                    facet.guid = note.getGuid();
                }
                if ((facet.contentHash==null||!Arrays.equals(facet.contentHash, note.getContentHash())||
                    facet.contentLength!=note.getContentLength())||
                    facet.USN<note.getUpdateSequenceNum()){
                }
                Note freshlyRetrievedNote = noteStore.getNote(note.getGuid(), true, false, false, false);
                facet.timeUpdated = System.currentTimeMillis();
                if (freshlyRetrievedNote.isSetUpdateSequenceNum())
                    facet.USN = freshlyRetrievedNote.getUpdateSequenceNum();
                if (freshlyRetrievedNote.isSetContentHash())
                    facet.contentHash = freshlyRetrievedNote.getContentHash();
                if (freshlyRetrievedNote.isSetContentLength())
                    facet.contentLength = freshlyRetrievedNote.getContentLength();
                if (freshlyRetrievedNote.isSetContent())
                    facet.content = freshlyRetrievedNote.getContent();
                facet.clearTags();
                if (freshlyRetrievedNote.isSetTagNames())
                    facet.addTags(StringUtils.join(freshlyRetrievedNote.getTagNames(), ","), ',');

                if (freshlyRetrievedNote.isSetTitle())
                    facet.title = freshlyRetrievedNote.getTitle();
                if (freshlyRetrievedNote.isSetCreated())
                    facet.created = freshlyRetrievedNote.getCreated();
                if (freshlyRetrievedNote.isSetUpdated())
                    facet.updated = freshlyRetrievedNote.getUpdated();
                if (freshlyRetrievedNote.isSetDeleted())
                    facet.deleted = freshlyRetrievedNote.getDeleted();
                if (freshlyRetrievedNote.isSetNotebookGuid())
                    facet.notebookGuid = freshlyRetrievedNote.getNotebookGuid();
                if (freshlyRetrievedNote.isSetActive())
                    facet.active = freshlyRetrievedNote.isActive();

                if (freshlyRetrievedNote.isSetResources()) {
                    ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
                    String json = ow.writeValueAsString(freshlyRetrievedNote.getResources());
                    facet.resourcesStorage = json;
                }

                return facet;
            }
        };
        // we could use the resulting value (facet) from this call if we needed to do further processing on it (e.g. passing it on to the datastore)
        apiDataService.createOrReadModifyWrite(EvernoteNoteFacet.class, facetQuery, facetModifier, updateInfo.apiKey.getId());
    }

    private void removeEvernoteFacet(final UpdateInfo updateInfo, Class<? extends EvernoteFacet> clazz, final String guid) {
        jpaDaoService.execute(String.format("DELETE FROM %s facet WHERE " + "facet.apiKeyId=%s AND facet.guid='%s'", JPAUtils.getEntityName(clazz), updateInfo.apiKey.getId(), guid));
    }

    private void createOrUpdateNotebook(final UpdateInfo updateInfo, final Notebook notebook) throws Exception {
        final ApiDataService.FacetQuery facetQuery = new ApiDataService.FacetQuery(
                "e.apiKeyId=? AND e.guid=?",
                updateInfo.apiKey.getId(), notebook.getGuid());
        final ApiDataService.FacetModifier<EvernoteNotebookFacet> facetModifier = new ApiDataService.FacetModifier<EvernoteNotebookFacet>() {
            @Override
            public EvernoteNotebookFacet createOrModify(EvernoteNotebookFacet facet, final Long apiKeyId) throws Exception {
                if (facet == null) {
                    facet = new EvernoteNotebookFacet(updateInfo.apiKey.getId());
                    extractCommonFacetData(facet, updateInfo);
                    facet.guid = notebook.getGuid();
                }
                facet.timeUpdated = System.currentTimeMillis();
                if (notebook.isSetUpdateSequenceNum())
                    facet.USN = notebook.getUpdateSequenceNum();
                if (notebook.isSetName())
                    facet.name = notebook.getName();
                if (notebook.isSetServiceCreated())
                    facet.serviceCreated = notebook.getServiceCreated();
                if (notebook.isSetServiceUpdated())
                    facet.serviceUpdated = notebook.getServiceUpdated();
                if (notebook.isSetPublishing())
                    facet.publishing = notebook.getPublishing();
                if (notebook.isSetPublished())
                    facet.published = notebook.isPublished();
                if (notebook.isSetStack())
                    facet.stack = notebook.getStack();
                if (notebook.isSetDefaultNotebook())
                    facet.defaultNotebook = notebook.isDefaultNotebook();
                //omitting business information: contact, businessNotebook
                //omitting sharedNotebooks
                return facet;
            }
        };
        // we could use the resulting value (facet) from this call if we needed to do further processing on it (e.g. passing it on to the datastore)
        apiDataService.createOrReadModifyWrite(EvernoteNotebookFacet.class, facetQuery, facetModifier, updateInfo.apiKey.getId());
    }

    private void removeNotebook(final UpdateInfo updateInfo, final String guid) {
        removeEvernoteFacet(updateInfo, EvernoteNotebookFacet.class, guid);
        jpaDaoService.execute(String.format("DELETE FROM %s facet WHERE " + "facet.apiKeyId=%s AND facet.notebookGuid='%s'", JPAUtils.getEntityName(EvernoteNoteFacet.class), updateInfo.apiKey.getId(), guid));
    }

    private void createOrUpdateTag(final UpdateInfo updateInfo, final Tag tag) throws Exception {
        final ApiDataService.FacetQuery facetQuery = new ApiDataService.FacetQuery(
                "e.apiKeyId=? AND e.guid=?",
                updateInfo.apiKey.getId(), tag.getGuid());
        final ApiDataService.FacetModifier<EvernoteTagFacet> facetModifier = new ApiDataService.FacetModifier<EvernoteTagFacet>() {
            @Override
            public EvernoteTagFacet createOrModify(EvernoteTagFacet facet, final Long apiKeyId) {
                if (facet == null) {
                    facet = new EvernoteTagFacet(updateInfo.apiKey.getId());
                    extractCommonFacetData(facet, updateInfo);
                    facet.guid = tag.getGuid();
                }
                facet.timeUpdated = System.currentTimeMillis();
                if (tag.isSetUpdateSequenceNum())
                    facet.USN = tag.getUpdateSequenceNum();
                if (tag.isSetName())
                    facet.name = tag.getName();
                return facet;
            }
        };
        // we could use the resulting value (facet) from this call if we needed to do further processing on it (e.g. passing it on to the datastore)
        apiDataService.createOrReadModifyWrite(EvernoteTagFacet.class, facetQuery, facetModifier, updateInfo.apiKey.getId());
    }

}
