package org.fluxtream.connectors.google_spreadsheets;

import org.fluxtream.core.connectors.annotations.ObjectTypeSpec;
import org.fluxtream.core.domain.AbstractFacet;
import org.hibernate.annotations.Type;

import javax.persistence.*;

/**
 * Created by candide on 30/12/14.
 */
@Entity(name="Facet_GoogleSpreadsheetCell")
@ObjectTypeSpec(name = "row", value = 4, prettyname = "Row", clientFacet = false)
public class GoogleSpreadsheetCellFacet extends AbstractFacet {

    @Lob
    public String value;

    @Type(type="yes_no")
    public boolean isNumeric;

    public GoogleSpreadsheetCellFacet() {}

    @ManyToOne(fetch= FetchType.LAZY, cascade= CascadeType.ALL)
    public GoogleSpreadsheetRowFacet row;

}
