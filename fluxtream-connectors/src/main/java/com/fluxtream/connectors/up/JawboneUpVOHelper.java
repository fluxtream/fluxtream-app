package com.fluxtream.connectors.up;

import com.fluxtream.Configuration;
import com.fluxtream.domain.AbstractFacet;

/**
 * User: candide
 * Date: 11/02/14
 * Time: 16:27
 */
public abstract class JawboneUpVOHelper {

    static String getImageURL(String uri, AbstractFacet facet, Configuration env) {
        return String.format("%sup/img/%s/%s%s", env.get("homeBaseUrl"),
                                            facet.guestId, facet.apiKeyId, uri);
    }

    static String getImageURL(String uri, AbstractFacet facet, Configuration env, int width) {
        return String.format("%sup/img/%s/%s%s@w="+width, env.get("homeBaseUrl"),
                             facet.guestId, facet.apiKeyId, uri);
    }

}
