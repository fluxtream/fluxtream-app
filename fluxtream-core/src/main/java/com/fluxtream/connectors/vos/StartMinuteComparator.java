package com.fluxtream.connectors.vos;
import java.util.Comparator;

import com.fluxtream.domain.AbstractFacet;

public class StartMinuteComparator implements Comparator<AbstractInstantFacetVO<? extends AbstractFacet>> {
	
	public int compare(AbstractInstantFacetVO<? extends AbstractFacet> o1, AbstractInstantFacetVO<? extends AbstractFacet> o2) {
		return (o1.startMinute > o2.startMinute)?1:-1;
	}

}
