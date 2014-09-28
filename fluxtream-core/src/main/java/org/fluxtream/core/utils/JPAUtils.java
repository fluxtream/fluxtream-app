package org.fluxtream.core.utils;

import java.lang.reflect.Field;
import java.util.List;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Query;
import org.fluxtream.core.domain.AbstractFacet;

public class JPAUtils {

    public static String getEntityName(Class<? extends AbstractFacet> facetClass) {
        try {
            final Entity annotation = facetClass.getAnnotation(Entity.class);
            final String name = annotation.name();
            return name;
        } catch (Throwable t) {
            final String message = "Could not get Facet class for connector for " + facetClass.getName();
            throw new RuntimeException(message);
        }
    }

    public static boolean hasRelation(final Class<? extends AbstractFacet> facetClass) {
        final Field[] fields = facetClass.getFields();
        for (Field field : fields) {
            if (field.getAnnotation(OneToMany.class)!=null||
                field.getAnnotation(ManyToMany.class)!=null||
                field.getAnnotation(OneToOne.class)!=null||
                field.getAnnotation(ElementCollection.class)!=null)
                return true;
        }
        return false;
    }

    public static long count(EntityManager em,
			String queryName, Object... params) {
		Query query = em.createNamedQuery(queryName);
		int i = 1;
		if (params != null)
			for (Object param : params) {
				query.setParameter(i++, param);
			}
		Number n = (Number) query.getSingleResult();
        if (n!=null)
    		return n.longValue();
        else
            return -1;
	}

	public static <T> T findUnique(EntityManager em, Class<T> clazz,
			String queryName, Object... params) {
		List<T> results = doQuery(em, queryName, true, 0, 1, params);
		if (results != null && results.size() > 0)
			return results.get(0);
		else
			return null;
	}

	private static <T> List<T> doQuery(EntityManager em, String queryName,
			boolean page, int firstResult, int maxResults, Object... params) {
		Query query = em.createNamedQuery(queryName);

		if (page) {
			query.setFirstResult(firstResult);
			query.setMaxResults(maxResults);
		}

		int i = 1;
		if (params != null)
			for (Object param : params) {
				query.setParameter(i++, param);
			}
		try {
			@SuppressWarnings("unchecked")
			List<T> results = query.getResultList();
			return results;
		} catch (Throwable t) {
			t.printStackTrace();
		}
		return null;
	}

	public static int execute(EntityManager em, String queryName,
			Object... params) {
		Query query = em.createNamedQuery(queryName);
		int i = 1;
		if (params != null) {
			for (Object param : params) {
				query.setParameter(i++, param);
			}
		}
		int rowsAffected = query.executeUpdate();
		return rowsAffected;
	}

	public static <T> List<T> find(EntityManager em, Class<T> clazz,
			String queryName, Object... params) {
		List<T> results = doQuery(em, queryName, false, -1, -1, params);
		return results;
	}

	public static <T> List<T> findWithLimit(EntityManager em, Class<T> clazz, String queryName, int firstResult, int maxResults, Object... params) {
		List<T> results = doQuery(em, queryName, true, firstResult,
				maxResults, params);
		return results;
	}

    public static <T> List<T> findPaged(final EntityManager em, final Class<T> clazz,
                                            final String queryName, final int pageSize, final int page,
                                            Object... params) {
        List<T> results = doQuery(em, queryName, true, pageSize*page,
                                  pageSize, params);
        return results;
    }

    public static String asListOfString(final String...strings) {
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<strings.length; i++) {
            if (i>0) sb.append(",");
            sb.append("'").append(strings[i]).append("'");
        }
        return sb.toString();
    }
}
