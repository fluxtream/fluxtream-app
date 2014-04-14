package org.fluxtream.connectors.fitbit;

import java.util.ArrayList;
import java.util.List;
import org.fluxtream.core.TimeInterval;
import org.fluxtream.core.connectors.vos.AbstractLocalTimeInstantFacetVO;
import org.fluxtream.core.domain.GuestSettings;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class FitbitTrackerActivityFacetVO extends AbstractLocalTimeInstantFacetVO<FitbitTrackerActivityFacet> {

	public int steps;
    public int caloriesOut;
//	List<FitbitStepsVO> stepsPerMinute;
	List<FitbitCaloriesVO> caloriesPerMinute;
    public int activeScore;
    public float MET;
	@Override
	public void fromFacet(FitbitTrackerActivityFacet facet, TimeInterval timeInterval, GuestSettings settings) {
        this.date = facet.date;
		steps = facet.steps;
		caloriesOut = facet.caloriesOut;
		activeScore = facet.activeScore;
		MET = round(activeScore*0.01f+1, 2);
		if (facet.caloriesJson!=null) {
			this.caloriesPerMinute = new ArrayList<FitbitCaloriesVO>();
			
			JSONObject json = JSONObject.fromObject(facet.caloriesJson);
			JSONObject intraday = json.getJSONObject("activities-log-calories-intraday");
			JSONArray stepsArray = intraday.getJSONArray("dataset");
			
			for (int i=0; i<stepsArray.size(); i++) {
				JSONObject entry = stepsArray.getJSONObject(i);
				int calories = entry.getInt("value");
				int level = entry.getInt("level");
				String time = entry.getString("time");
				String[] timeParts = time.split(":");
				int hours = Integer.valueOf(timeParts[0]);
				int minutes = Integer.valueOf(timeParts[1]);
				FitbitCaloriesVO caloriesIntraday = new FitbitCaloriesVO();
				caloriesIntraday.calories = calories;
				caloriesIntraday.level = level;
				caloriesIntraday.minute = hours*60+minutes;
				this.caloriesPerMinute.add(caloriesIntraday);
			}
		}
	}
	
}
