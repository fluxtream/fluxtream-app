package org.fluxtream.core.mvc.models;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * User: candide
 * Date: 06/07/14
 * Time: 21:45
 */
public class CoachModel {

    public List<SharedConnectorModel> sharedConnectors;
    public Map<String,List<SharedChannelModel>> sharedChannels = new TreeMap<String,List<SharedChannelModel>>();
    public String username;
    public String fullname;

    public CoachModel() {}

}
