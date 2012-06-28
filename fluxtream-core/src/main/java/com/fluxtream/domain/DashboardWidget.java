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
    public Map<String,String> WidgetTitle = new HashMap<String,String>();
    public List<String> SupportedLanguages;
    public String VendorIdentifier;
    public List<String> RequiredConnectors;
    public String WidgetIcon;
    public String WidgetName;
    public String WidgetRepositoryURL;
    public String BundleCopyright;
    public String BundleIdentifier;
    public String BundleVersion;

    public DashboardWidget(final JSONObject manifestJSON) {
        try {
            JSONObject descDict = JSONObject.fromObject(manifestJSON.getString("WidgetDescription"));
            for (Object o : descDict.keySet()) {
                String key = (String) o;
                WidgetDescription.put(key, descDict.getString(key));
            }
            WidgetIcon = manifestJSON.getString("WidgetIcon");
            WidgetName = manifestJSON.getString("WidgetName");
            JSONObject titleDict = JSONObject.fromObject(manifestJSON.getString("WidgetTitle"));
            for (Object o : descDict.keySet()) {
                String key = (String) o;
                WidgetTitle.put(key, titleDict.getString(key));
            }
            WidgetRepositoryURL = manifestJSON.getString("WidgetRepositoryURL");
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
