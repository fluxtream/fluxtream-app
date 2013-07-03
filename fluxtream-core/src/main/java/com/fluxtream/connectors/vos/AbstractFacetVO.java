package com.fluxtream.connectors.vos;

import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeSet;
import com.fluxtream.OutsideTimeBoundariesException;
import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.annotations.ObjectTypeSpec;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.domain.GuestSettings;
import com.fluxtream.utils.SecurityUtils;

public abstract class AbstractFacetVO<T extends AbstractFacet> {

	public String type;
	public String description;
	public long id;
	public String comment;
    public final SortedSet<String> tags = new TreeSet<String>();
	public String subType;

	/**
	 * Thread-safe cache for vo classes
	 */
	private static Hashtable<String, Class<? extends AbstractFacetVO<? extends AbstractFacet>>> voClasses;
    private static Hashtable<Class<? extends AbstractFacet>,String> objectTypeNames;

	static {
		voClasses = new Hashtable<String, Class<? extends AbstractFacetVO<? extends AbstractFacet>>>();
        objectTypeNames = new Hashtable<Class<? extends AbstractFacet>, String>();
	}

	public void extractValues(T facet, TimeInterval timeInterval, GuestSettings settings) throws OutsideTimeBoundariesException {
		getType(facet);
		this.id = facet.getId();
		if (facet.comment!=null&&!facet.comment.equals("")) {
			if (SecurityUtils.isDemoUser())
				this.comment = "***demo - comment hidden***";
			else {
				try {
					this.comment = new String(facet.comment.getBytes(), "utf-8");
				} catch (UnsupportedEncodingException e) {}
			}
		}
        if (facet.hasTags()) {
            if (!SecurityUtils.isDemoUser()) {
                tags.addAll(facet.getTagsAsStrings());
            }
        }
        fromFacet(facet, timeInterval, settings);
	}

    /**
     * Returns a copy of this VO's set of tags. Assumes {@link #extractValues} has already been called. Guaranteed to
     * not return <code>null</code>, but may return an empty {@link SortedSet}.
     */
    public SortedSet<String> getTags() {
        return new TreeSet<String>(tags);
    }

    protected void getType(T facet) {
		Connector connector = Connector.fromValue(facet.api);
		this.type = connector.getName();
		if (facet.objectType != -1) {
            final String objectTypeName = getObjectTypeName(facet);
            this.type += "-" + objectTypeName;
		}
		this.subType = getSubtype(facet);
		if (subType!=null)
			this.type += "-" + subType;
	}

    protected String getObjectTypeName(T facet) {
        if (objectTypeNames.containsKey(facet.getClass()))
            return objectTypeNames.get(facet.getClass());
        String objectTypeName = facet.getClass().getAnnotation(ObjectTypeSpec.class).name();
        objectTypeNames.put(facet.getClass(), objectTypeName);
        return objectTypeName;
    }
	
	protected String getSubtype(T facet) {
		return null;
	}

	protected abstract void fromFacet(T facet, TimeInterval timeInterval, GuestSettings settings) throws OutsideTimeBoundariesException;

	public static int toMinuteOfDay(java.util.Date date, TimeZone tz) {
		if (date == null)
			return 0;
		Calendar c = new GregorianCalendar(tz);
		c.setTime(date);
		int startMinute = c.get(Calendar.HOUR_OF_DAY) * 60
				+ c.get(Calendar.MINUTE);
		return startMinute;
	}

	/**
	 * We return a corresponding class to extract a value object from
	 * a facet, using a naming convention: MyFacet becomes MyFacetVO
	 * @param facet
	 * @return
	 * @throws ClassNotFoundException
	 */
	@SuppressWarnings("unchecked")
	public static <T extends AbstractFacet> Class<? extends AbstractFacetVO<T>> getFacetVOClass(
			T facet) throws ClassNotFoundException {
		try {
			String name = facet.getClass().getName();
			if (voClasses.get(name) == null) {
				String namePrefix = name.substring(0, name.length() - 5);
				String jsonFacetClassName = namePrefix + "FacetVO";
				voClasses.put(name, (Class<? extends AbstractFacetVO<T>>) Class
						.forName(jsonFacetClassName));
			}
			Class<? extends AbstractFacetVO<? extends AbstractFacet>> voClass = voClasses
					.get(name);
			return (Class<? extends AbstractFacetVO<T>>) voClass;
		} catch (ClassNotFoundException e) {
//			logger.generalError("Could not find corresponding \"FacetVO\" class for "
//					+ facet.getClass().getName());
			throw e;
		}
	}
}
