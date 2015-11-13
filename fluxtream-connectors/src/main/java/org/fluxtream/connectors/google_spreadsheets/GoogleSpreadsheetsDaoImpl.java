package org.fluxtream.connectors.google_spreadsheets;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

/**
 * Created by candide on 12/11/15.
 */
@Component
@Transactional(readOnly = true)
public class GoogleSpreadsheetsDaoImpl implements GoogleSpreadsheetsDao {

    @PersistenceContext
    EntityManager em;

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

}
