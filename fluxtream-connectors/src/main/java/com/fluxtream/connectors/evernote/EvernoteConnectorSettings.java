package com.fluxtream.connectors.evernote;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * User: candide
 * Date: 07/01/14
 * Time: 12:05
 */
public class EvernoteConnectorSettings implements Serializable {

    public List<NotebookConfig> notebooks = new ArrayList<NotebookConfig>();

    void addNotebookConfig(NotebookConfig config){
        notebooks.add(config);
    }

    NotebookConfig getNotebook(String guid) {
        for (NotebookConfig notebook : notebooks) {
            if (notebook.guid.equals(guid))
                return notebook;
        }
        return null;
    }
}
