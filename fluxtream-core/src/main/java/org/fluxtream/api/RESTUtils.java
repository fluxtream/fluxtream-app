package org.fluxtream.api;

import org.fluxtream.mvc.models.StatusModel;
import com.google.gson.Gson;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
public class RESTUtils {

    private static final Gson gson = new Gson();

    public static String handleRuntimeException (RuntimeException rte) {
        return gson.toJson(new StatusModel(false, rte.getMessage()));
    }

}
