package org.fluxtream.core.connectors.vos;

import org.fluxtream.core.connectors.dao.JPAFacetDao;
import org.springframework.cache.annotation.Cacheable;

/**
 * Created by candide on 04/11/15.
 */
public class VOHelper {

    public static ThreadLocal<JPAFacetDao> jpaFacetDao = new ThreadLocal<JPAFacetDao>();


}
