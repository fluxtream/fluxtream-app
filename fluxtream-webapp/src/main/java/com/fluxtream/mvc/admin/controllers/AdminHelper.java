package com.fluxtream.mvc.admin.controllers;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fluxtream.Configuration;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.ObjectType;
import com.fluxtream.services.ApiDataService;
import com.fluxtream.services.ConnectorUpdateService;
import com.fluxtream.services.GuestService;
import com.fluxtream.services.JPADaoService;
import com.fluxtream.services.MetadataService;

@Component
public class AdminHelper implements BeanFactoryAware {

	@Autowired
	public MetadataService metadataService;

	@Autowired
	public ApiDataService apiDataService;
	
	@Autowired
	public Configuration env;

	@Autowired
	public JPADaoService jpaDaoService;

	@Autowired
	public GuestService guestService;

	@Autowired
	public ConnectorUpdateService connectorUpdateService;

	private BeanFactory beanFactory;
	
	public String getLabelForObjectTypes (Connector connector, int objectTypes) {
		ObjectType[] otypes = connector.getObjectTypesForValue(objectTypes);
		if (otypes!=null) {
			String l = "";
			for(int i=0; i<otypes.length; i++) {
				if (i>0) l+="/";
				l += otypes[i].name();
			}
			return l;
		}
		else
			return "(default)";
	}
	
	public Object getHelper(String connectorName) {
		Object bean = beanFactory.getBean(connectorName + "Helper");
		return bean;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}
	
	public long countFacets(Connector connector, long guestId) {
		return jpaDaoService.countFacets(connector, guestId);
	}
}
