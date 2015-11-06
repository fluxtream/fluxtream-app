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
            documents.add(new Spreadsheet(doc.collectionLabel, doc.itemLabel, doc.getId()));
        }
    }
}
