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


    public Map<String,String> description = new HashMap<String,String>();
    public String icon;
    public String name;
    public String url;
    public String copyright;
    public String identifier;
    public String version;
    public List<String> supportedLanguages;
    public String vendorIdentifier;
    public List<String> requiredConnectors;

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
                description.put(key, descDict.getString(key));
            }
            icon = manifestJSON.getString("WidgetIcon");
            name = manifestJSON.getString("WidgetName");
            url = manifestJSON.getString("WidgetURL");
            copyright = manifestJSON.getString("BundleCopyright");
            identifier = manifestJSON.getString("BundleIdentifier");
            version = manifestJSON.getString("BundleVersion");
            supportedLanguages = new ArrayList<String>(
                    Arrays.asList(
                            StringUtils.split(manifestJSON.getString("SupportedLanguages"),
                                              ",")
                    )
            );
            vendorIdentifier = manifestJSON.getString("VendorIdentifier");
            requiredConnectors = new ArrayList<String>(
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
        for (String requiredConnector : requiredConnectors) {
            for (String userConnectorName : userConnectorNames) {
                if (userConnectorName.equalsIgnoreCase(requiredConnector.trim()))
                    return true;
            }
        }
        return false;
    }

}
