package com.fluxtream.services;

import java.util.List;
import com.fluxtream.connectors.Connector;

public interface JPADaoService {

    @SuppressWarnings("unused")
	public <T> List<T> findWithLimit(String queryName, Class<T> clazz, int firstResult, int maxResults, Object... params);

	public <T> List<T> find(String queryName, Class<T> clazz, Object... params);

	public <T> T findOne(String queryName, Class<T> clazz, Object... params);

    @SuppressWarnings("unused")
	public long countFacets(Connector connector, long guestId);

    public int execute(String jpql);

    public void persist(Object o);

	public void remove(Class<?> clazz, long id);

    <T> List<T>  executeQueryWithLimit(String queryString, int i, Class<T> clazz, Object... params);

    Long executeNativeQuery(String queryString);

    List executeNativeQuery(String s, Object... params);
}
