package com.fluxtream.utils;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

public class JPAUtils {

	public static <T> long count(EntityManager em, Class<T> clazz,
			String queryName, Object... params) {
		Query query = em.createNamedQuery(queryName);
		int i = 1;
		if (params != null)
			for (Object param : params) {
				query.setParameter(i++, param);
			}
		Number n = (Number) query.getSingleResult();
		return n.longValue();
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

	public static <T> List<T> find(EntityManager em, Class<T> clazz,
			String queryName, int firstResult, int maxResults, Object... params) {
		List<T> results = doQuery(em, queryName, true, firstResult,
				maxResults, params);
		return results;
	}
}
