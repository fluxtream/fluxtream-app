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
@Entity(name="Facet_GoogleSpreadsheetRow")
@ObjectTypeSpec(name = "row", value = 1, prettyname = "Row", isMixedType = true)
public class GoogleSpreadsheetRowFacet extends AbstractFacet {

    public GoogleSpreadsheetRowFacet(){}

    @OneToMany(mappedBy = "row", orphanRemoval = true, fetch= FetchType.EAGER, cascade= CascadeType.ALL)
    public List<GoogleSpreadsheetCellFacet> cells;

    @Override
    protected void makeFullTextIndexable() {

    }
}
