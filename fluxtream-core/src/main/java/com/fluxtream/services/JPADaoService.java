package com.fluxtream.services;

import java.util.List;

import com.fluxtream.connectors.Connector;

public interface JPADaoService {

	public <T> List<T> find(String queryName, Class<T> clazz, int firstResult,
			int maxResults, Object... params);

	public <T> List<T> find(String queryName, Class<T> clazz, Object... params);

	public <T> T findOne(String queryName, Class<T> clazz, Object... params);
	
	public long countFacets(Connector connector, long guestId);
	
	public void persist(Object o);

	public void remove(Class<?> clazz, long id);
}
