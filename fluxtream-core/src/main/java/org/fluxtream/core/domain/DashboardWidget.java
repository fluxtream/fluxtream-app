package org.fluxtream.core.domain;

import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;
import net.sf.json.JSONObject;
import org.codehaus.plexus.util.StringUtils;
import org.fluxtream.core.utils.RequestUtils;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.*;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@XmlRootElement
@ApiModel("A widget's full definition (before it is added to a dashboard)")
public class DashboardWidget {

    @ApiModelProperty(value="Localized description", required=true)
    public Map<String,String> WidgetDescription = new HashMap<String,String>();
    @ApiModelProperty(value="Localized title", required=true)
    public Map<String,String> WidgetTitle = new HashMap<String,String>();
    @ApiModelProperty(value="Supported locales", required=true)
    public List<String> SupportedLanguages;
    @ApiModelProperty(value="Who created this?", required=true)
    public String VendorIdentifier;
    @ApiModelProperty(value="List of connectors that make this widget relevant", required=true)
    public List<String> RequiredConnectors;
    @ApiModelProperty(value="Whether or not the widget needs access to everything in the client", required=true)
    public boolean fullAccess;
    @ApiModelProperty(value="Icon URL", required=true)
    public String WidgetIcon;
    @ApiModelProperty(value="Name of the widget", required=true)
    public String WidgetName;
    @ApiModelProperty(value="Where does this widget come from?", required=true)
    public String WidgetRepositoryURL;
    @ApiModelProperty(value="Does it support settings?", required=true)
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
            final String manifestRequiredConnectors = manifestJSON.getString("RequiredConnectors");
            if (!manifestRequiredConnectors.equals("null")) {
                RequiredConnectors = new ArrayList<String>(
                        Arrays.asList(
                                StringUtils.split(manifestRequiredConnectors,
                                                  ",")
                        )
                );
            } else
                RequiredConnectors = null;
            fullAccess = manifestJSON.has("fullAccess") ? manifestJSON.getBoolean("fullAccess") : false;
        } catch (Throwable t) {
            throw new RuntimeException("Invalid manifest JSON (" + t.getMessage() + ")");
        }
    }

    private static String baseURL(final String baseURL) {
        if (baseURL.endsWith("/")) return baseURL(baseURL.substring(0, baseURL.length()-1));
        return baseURL;
    }

    public boolean matchesUserConnectors(List<String> userConnectorNames, boolean isDev) {
        if (RequiredConnectors==null) return true;
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
