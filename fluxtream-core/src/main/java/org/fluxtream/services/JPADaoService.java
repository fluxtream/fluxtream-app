package org.fluxtream.services;

import java.util.List;
import org.fluxtream.connectors.Connector;

public interface JPADaoService {

    @SuppressWarnings("unused")
	public <T> List<T> findWithLimit(String queryName, Class<T> clazz, int firstResult, int maxResults, Object... params);

	public <T> List<T> find(String queryName, Class<T> clazz, Object... params);

	public <T> T findOne(String queryName, Class<T> clazz, Object... params);

    @SuppressWarnings("unused")
	public long countFacets(Connector connector, long guestId);

    public int execute(String jpql, Object... params);

    <T> List<T>  executeQueryWithLimit(String queryString, int i, Class<T> clazz, Object... params);

    <T> List<T>  executeQueryWithLimitAndOffset(String queryString, int i, int offset, Class<T> clazz, Object... params);

    Long executeNativeQuery(String queryString);

    List executeNativeQuery(String s, Object... params);
}
