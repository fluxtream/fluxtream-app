package org.fluxtream.mvc.views;

import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.mvc.models.ConnectorModel;

import java.util.ArrayList;
import java.util.List;

public class ViewsHelper<T> {
	
	public static List<ConnectorModel> toConnectorModels(List<ApiKey> keys) {
		List<ConnectorModel> vos = new ArrayList<ConnectorModel>();
		for (ApiKey key : keys) {
			Connector connector = key.getConnector();
			if (connector!=null) {
				vos.add(ViewsHelper.connectorModel(connector));			
			}
		}
		return vos;
	}
	
	public static ConnectorModel connectorModel(Connector api) {
		ConnectorModel vo = new ConnectorModel();
		vo.connectorName = api.getName();
		vo.prettyName = api.prettyName();
		return vo;
	}
	
	public List<ArrayList<T>> rows(List<T> l, int cols) {
		List<ArrayList<T>> rows = new ArrayList<ArrayList<T>>();
		int index = 0;
		if (l.size()>0) {
			int nrows = l.size()/cols;
			for (int i=0; i<nrows; i++) {
				ArrayList<T> row = new ArrayList<T>();
				rows.add(row);
				for (int j=0; j<cols; j++) {
					row.add(l.get(index++));
				}
			}
			int mod = l.size()%cols;
			if (mod>0) {
				ArrayList<T> row = new ArrayList<T>();
				rows.add(row);
				for (int j=0; j<mod; j++) {
					row.add(l.get(index++));
				}
			}
		}
		return rows;
	}
}
