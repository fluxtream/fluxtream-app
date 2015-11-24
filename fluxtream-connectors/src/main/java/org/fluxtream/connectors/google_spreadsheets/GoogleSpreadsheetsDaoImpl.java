package org.fluxtream.connectors.google_spreadsheets;

import org.fluxtream.core.connectors.updaters.UpdateInfo;
import org.fluxtream.core.services.JPADaoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.List;

/**
 * Created by candide on 12/11/15.
 */
@Component
@Transactional(readOnly = true)
public class GoogleSpreadsheetsDaoImpl implements GoogleSpreadsheetsDao {

    @PersistenceContext
    EntityManager em;

    @Autowired
    JPADaoService jpaDaoService;

    @Transactional(readOnly = false)
    public void removeDocument(long id) {
        Query nativeQuery = em.createNativeQuery("DELETE cell FROM Facet_GoogleSpreadsheetCell cell join Facet_GoogleSpreadsheetRow row on cell.row_id=row.id WHERE row.document_id=?");
        nativeQuery.setParameter(1, id);
        nativeQuery.executeUpdate();
        nativeQuery = em.createNativeQuery("DELETE row FROM Facet_GoogleSpreadsheetRow row join Facet_GoogleSpreadsheetDocument doc on row.document_id=doc.id WHERE doc.id=?");
        nativeQuery.setParameter(1, id);
        nativeQuery.executeUpdate();
        GoogleSpreadsheetsDocumentFacet documentFacet = em.find(GoogleSpreadsheetsDocumentFacet.class, id);
        em.remove(documentFacet);
    }

    @Override
    public boolean isDupe(UpdateInfo updateInfo, GoogleSpreadsheetsUpdater.ImportSpecs importSpecs) {
        if (importSpecs.worksheetId!=null) {
            List<GoogleSpreadsheetsDocumentFacet> documentFacets = jpaDaoService.findWithQuery("SELECT doc FROM Facet_GoogleSpreadsheetDocument doc WHERE doc.spreadsheetId=? AND doc.worksheetId=? AND doc.apiKeyId=?",
                    GoogleSpreadsheetsDocumentFacet.class,
                    importSpecs.spreadsheetId, importSpecs.worksheetId, updateInfo.apiKey.getId());
            return documentFacets.size() > 0;
        } else {
            List<GoogleSpreadsheetsDocumentFacet> documentFacets = jpaDaoService.findWithQuery("SELECT doc FROM Facet_GoogleSpreadsheetDocument doc WHERE doc.spreadsheetId=? AND doc.apiKeyId=?",
                    GoogleSpreadsheetsDocumentFacet.class,
                    importSpecs.spreadsheetId, updateInfo.apiKey.getId());
            return documentFacets.size() > 0;
        }
    }

}
