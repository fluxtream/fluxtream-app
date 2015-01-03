package org.fluxtream.connectors.google_spreadsheets;

import org.fluxtream.core.connectors.annotations.ObjectTypeSpec;
import org.fluxtream.core.domain.AbstractRepeatableFacet;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by candide on 30/12/14.
 */
@Entity(name="Facet_GoogleSpreadsheetRow")
@ObjectTypeSpec(name = "row", value = 2, prettyname = "Row", isMixedType = true)
public class GoogleSpreadsheetRowFacet extends AbstractRepeatableFacet {

    public GoogleSpreadsheetRowFacet(){}

    @ManyToOne(fetch=FetchType.LAZY, cascade= CascadeType.ALL)
    GoogleSpreadsheetDocumentFacet document;

    @OneToMany(mappedBy = "row", orphanRemoval = true, fetch=FetchType.EAGER, cascade= CascadeType.ALL)
    public List<GoogleSpreadsheetCellFacet> cells = new ArrayList<GoogleSpreadsheetCellFacet>();

    @Override
    public boolean allDay() {
        return allDayEvent;
    }
}
