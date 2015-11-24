package org.fluxtream.connectors.google_spreadsheets;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by candide on 06/11/15.
 */
public class GoogleSpreadsheetsSettings implements Serializable {

    class Spreadsheet {
        String collectionLabel, itemLabel;
        boolean status_up, status_down;
        String message, stackTrace;
        int numberOfRows;
        long id;

        public Spreadsheet(String collectionLabel, String itemLabel, long id) {
            this.collectionLabel = collectionLabel;
            this.itemLabel = itemLabel;
            this.id = id;
        }
    }

    List<Spreadsheet> documents = new ArrayList<Spreadsheet>();

    public GoogleSpreadsheetsSettings(List<GoogleSpreadsheetsDocumentFacet> docs) {
        for (GoogleSpreadsheetsDocumentFacet doc : docs) {
            Spreadsheet ss = new Spreadsheet(doc.collectionLabel, doc.itemLabel, doc.getId());
            ss.status_up = (doc.status== GoogleSpreadsheetsDocumentFacet.Status.UP);
            ss.status_down = (doc.status== GoogleSpreadsheetsDocumentFacet.Status.DOWN);
            ss.message = doc.message;
            ss.stackTrace = doc.stackTrace;
            ss.numberOfRows = doc.numberOfRows;
            documents.add(ss);
        }
    }
}
