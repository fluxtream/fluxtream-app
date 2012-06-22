package com.fluxtream.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.sf.json.JSONObject;
import org.codehaus.plexus.util.StringUtils;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
public class DashboardWidget {


    public Map<String,String> WidgetDescription = new HashMap<String,String>();
    public List<String> SupportedLanguages;
    public String VendorIdentifier;
    public List<String> RequiredConnectors;
    private String WidgetIcon;
    private String WidgetName;
    private String WidgetURL;
    private String BundleCopyright;
    private String BundleIdentifier;
    private String BundleVersion;

    /**

    "WidgetDescription" : {"en" : ""},
    "WidgetIcon" : "",
    "WidgetName" : "",
    "WidgetURL" : "",
    "BundleCopyright" : "",
    "BundleIdentifier" : "",
    "BundleVersion" : "",
    "SupportedLanguages" : "",
    "VendorIdentifier" : "",
    "RequiredConnectors":"bodymedia, fitbit"

     */
    public DashboardWidget(final JSONObject manifestJSON) {
        try {
            JSONObject descDict = JSONObject.fromObject(manifestJSON.getString("WidgetDescription"));
            for (Object o : descDict.keySet()) {
                String key = (String) o;
                WidgetDescription.put(key, descDict.getString(key));
            }
            WidgetIcon = manifestJSON.getString("WidgetIcon");
            WidgetName = manifestJSON.getString("WidgetName");
            WidgetURL = manifestJSON.getString("WidgetURL");
            BundleCopyright = manifestJSON.getString("BundleCopyright");
            BundleIdentifier = manifestJSON.getString("BundleIdentifier");
            BundleVersion = manifestJSON.getString("BundleVersion");
            SupportedLanguages = new ArrayList<String>(
                    Arrays.asList(
                            StringUtils.split(manifestJSON.getString("SupportedLanguages"),
                                              ",")
                    )
            );
            VendorIdentifier = manifestJSON.getString("VendorIdentifier");
            RequiredConnectors = new ArrayList<String>(
                    Arrays.asList(
                            StringUtils.split(manifestJSON.getString("RequiredConnectors"),
                                              ",")
                    )
            );
        } catch (Throwable t) {
            throw new RuntimeException("Invalid manifest JSON (" + t.getMessage() + ")");
        }
    }

    public boolean matchesUserConnectors(List<String> userConnectorNames) {
        for (String requiredConnector : RequiredConnectors) {
            for (String userConnectorName : userConnectorNames) {
                if (userConnectorName.equalsIgnoreCase(requiredConnector.trim()))
                    return true;
            }
        }
        return false;
    }

}
