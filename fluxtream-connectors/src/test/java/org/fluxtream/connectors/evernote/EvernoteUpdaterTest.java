package org.fluxtream.connectors.evernote;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.fluxtream.core.services.impl.BodyTrackHelper;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;

/**
 * User: candide
 * Date: 25/02/14
 * Time: 12:29
 */
public class EvernoteUpdaterTest {

    @Test
    public void testAddStyleParts() throws Exception {
        EvernoteUpdater eut = new EvernoteUpdater();
        List<NotebookConfig> notebookConfigs = new ArrayList<NotebookConfig>();
        addNotebookConfig(notebookConfigs, "red", "1");
        addNotebookConfig(notebookConfigs, "green", "2");
        addNotebookConfig(notebookConfigs, "blue", "3");
        addNotebookConfig(notebookConfigs, "orange", "4");
        addNotebookConfig(notebookConfigs, "yellow", "5");
        addNotebookConfig(notebookConfigs, "magenta", "6");
        addNotebookConfig(notebookConfigs, "cyan", "7");
        BodyTrackHelper.ChannelStyle channelStyle = new BodyTrackHelper.ChannelStyle();
        channelStyle.timespanStyles = new BodyTrackHelper.MainTimespanStyle();
        channelStyle.timespanStyles.defaultStyle = new BodyTrackHelper.TimespanStyle();
        channelStyle.timespanStyles.defaultStyle.fillColor = "green";
        channelStyle.timespanStyles.defaultStyle.borderColor = "green";
        channelStyle.timespanStyles.defaultStyle.borderWidth = 2;
        channelStyle.timespanStyles.defaultStyle.top = 0.0;
        channelStyle.timespanStyles.defaultStyle.bottom = 1.0;
        channelStyle.timespanStyles.values = new HashMap();
        eut.addStyleParts(notebookConfigs, channelStyle);
        ObjectMapper mapper = new ObjectMapper();
        System.out.println(mapper.writeValueAsString(channelStyle));
    }

    private void addNotebookConfig(final List<NotebookConfig> notebookConfigs, final String guid, String color) {
        NotebookConfig notebookConfig = new NotebookConfig();
        notebookConfig.backgroundColor = color;
        notebookConfig.guid = guid;
        notebookConfigs.add(notebookConfig);
    }
}
