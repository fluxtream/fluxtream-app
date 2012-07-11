package com.fluxtream.connectors.bodymedia;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.Lob;
import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.fitbit.FitbitActivityFacet;
import com.fluxtream.connectors.fitbit.FitbitCaloriesVO;
import com.fluxtream.connectors.vos.AbstractFacetVO;
import com.fluxtream.domain.GuestSettings;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class BodymediaBurnFacetVO extends AbstractFacetVO<BodymediaBurnFacet>{

    public int totalCalories = 0;
    public int estimatedCalories = 0;
    public int predictedCalories = 0;

    public String burnJson;

    @Override
    protected void fromFacet(final BodymediaBurnFacet facet, final TimeInterval timeInterval, final GuestSettings settings) {
        this.totalCalories = facet.totalCalories;
        this.estimatedCalories = facet.estimatedCalories;
        this.predictedCalories = facet.predictedCalories;
    }
}
