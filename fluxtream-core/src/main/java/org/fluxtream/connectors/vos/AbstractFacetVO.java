package org.fluxtream.connectors.vos;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.ResourceBundle;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeSet;
import org.fluxtream.OutsideTimeBoundariesException;
import org.fluxtream.TimeInterval;
import org.fluxtream.connectors.Connector;
import org.fluxtream.connectors.ObjectType;
import org.fluxtream.connectors.annotations.ObjectTypeSpec;
import org.fluxtream.domain.AbstractFacet;
import org.fluxtream.domain.GuestSettings;
import org.fluxtream.utils.SecurityUtils;
import org.fluxtream.utils.TimeUtils;
import org.joda.time.DateTimeZone;

public abstract class AbstractFacetVO<T extends AbstractFacet> {

	public String type;
	public String description;
	public long id;
	public String comment;
    public final SortedSet<String> tags = new TreeSet<String>();
	public String subType;
    public String ogLink;

    public transient int api;
    public transient int objectType;

	/**
	 * Thread-safe cache for vo classes
	 */
	private static Hashtable<String, Class<? extends AbstractFacetVO<? extends AbstractFacet>>> voClasses;
    private static Hashtable<Class<? extends AbstractFacet>,String> objectTypeNames;

	static {
		voClasses = new Hashtable<String, Class<? extends AbstractFacetVO<? extends AbstractFacet>>>();
        objectTypeNames = new Hashtable<Class<? extends AbstractFacet>, String>();
	}

    public String date;

    public void extractValues(T facet, TimeInterval timeInterval, GuestSettings settings) throws OutsideTimeBoundariesException {
		getType(facet);
		this.id = facet.getId();
        this.api = facet.api;
        this.objectType = facet.objectType;
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

        // Set default date which subclasses can overwrite if they want to
        DateTimeZone zone = DateTimeZone.forTimeZone(timeInterval.getTimeZone(facet.start));
        this.date = TimeUtils.dateFormatter.withZone(zone).print(facet.start);


        fromFacet(facet, timeInterval, settings);
        ResourceBundle res = ResourceBundle.getBundle("facetSharing");
        final ArrayList<String> openGraphSharableFacets = new ArrayList(Arrays.asList(res.getString("opengraph").split(",")));
        final Connector connector = Connector.fromValue(facet.api);
        String facetName = String.format("%s.%s", connector.getName(), ObjectType.getObjectType(connector, facet.objectType));
        if (openGraphSharableFacets.contains(facetName)&&isShareable(facet)) {
            String encryptedUrl = settings.config.encrypt(String.format("%s/%s/%s", facet.api, facet.objectType, String.valueOf(id)));
            try {
                encryptedUrl = URLEncoder.encode(encryptedUrl, "UTF-8");
            } catch (UnsupportedEncodingException e) {}
            ogLink = String.format("%sopenGraph/%s.html", settings.config.get("homeBaseUrl"),
                                   encryptedUrl);
        }
	}

    protected boolean isShareable(T facet) {
        return true;
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

    protected static final float round(float v, int decimals) {
        float mul = (float)Math.pow(10, decimals);
        return (float) Math.round(v * mul) / mul;
    }

    protected static final double round(double v, int decimals) {
        double mul = Math.pow(10, decimals);
        return Math.round(v * mul) / mul;
    }

}
