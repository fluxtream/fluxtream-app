package org.fluxtream.connectors.google_spreadsheets;

import org.fluxtream.core.connectors.annotations.ObjectTypeSpec;
import org.fluxtream.core.domain.AbstractFacet;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import java.util.List;

/**
 * Created by candide on 30/12/14.
 */
@Entity(name="Facet_GoogleSpreadsheetDocument")
@ObjectTypeSpec(name = "document", value = 1, prettyname = "Document", clientFacet = false)
public class GoogleSpreadsheetDocumentFacet extends AbstractFacet {

    @OneToMany(mappedBy = "rows", orphanRemoval = true, fetch=FetchType.LAZY, cascade=CascadeType.ALL)
    public List<GoogleSpreadsheetRowFacet> rows;

    public GoogleSpreadsheetDocumentFacet() {}

}
