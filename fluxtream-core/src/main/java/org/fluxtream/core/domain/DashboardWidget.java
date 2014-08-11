package org.fluxtream.core.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.bind.annotation.XmlRootElement;
import net.sf.json.JSONObject;
import org.codehaus.plexus.util.StringUtils;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@XmlRootElement
public class DashboardWidget {

    public Map<String,String> WidgetDescription = new HashMap<String,String>();
    public Map<String,String> WidgetTitle = new HashMap<String,String>();
    public List<String> SupportedLanguages;
    public String VendorIdentifier;
    public List<String> RequiredConnectors;
    public String WidgetIcon;
    public String WidgetName;
    public String WidgetRepositoryURL;
    public boolean HasSettings;

    public DashboardWidget() {
    }

    public DashboardWidget(final JSONObject manifestJSON, final String baseURL) {
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
            WidgetRepositoryURL = baseURL(baseURL);
            HasSettings = manifestJSON.getBoolean("HasSettings");
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

    private static String baseURL(final String baseURL) {
        if (baseURL.endsWith("/")) return baseURL(baseURL.substring(0, baseURL.length()-1));
        return baseURL;
    }

    public boolean matchesUserConnectors(List<String> userConnectorNames, boolean isDev) {
        for (String requiredConnector : RequiredConnectors) {
            if (isDev && requiredConnector.equals("dev"))
                return true;
            for (String userConnectorName : userConnectorNames) {
                if (userConnectorName.equalsIgnoreCase(requiredConnector.trim()))
                    return true;
            }
        }
        return false;
    }

}
