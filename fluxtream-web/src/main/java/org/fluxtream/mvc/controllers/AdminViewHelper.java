package org.fluxtream.mvc.controllers;

import java.util.List;
import org.fluxtream.core.domain.ApiKey;

/**
 * User: candide
 * Date: 26/08/14
 * Time: 12:07
 */
public class AdminViewHelper {

    public static String setChecked(final List<ApiKey.Status> statusFilters, ApiKey.Status status) {
        return statusFilters.contains(status) ? "CHECKED=CHECKED" : "";
    }

}
