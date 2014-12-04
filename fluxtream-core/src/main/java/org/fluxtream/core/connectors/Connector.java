package org.fluxtream.core.connectors;

import org.apache.commons.lang.ArrayUtils;
import org.apache.velocity.util.StringUtils;
import org.fluxtream.core.aspects.FlxLogger;
import org.fluxtream.core.connectors.annotations.ObjectTypeSpec;
import org.fluxtream.core.connectors.annotations.Updater;
import org.fluxtream.core.connectors.bodytrackResponders.AbstractBodytrackResponder;
import org.fluxtream.core.connectors.updaters.AbstractUpdater;
import org.fluxtream.core.domain.AbstractFacet;
import org.fluxtream.core.domain.AbstractUserProfile;
import org.fluxtream.core.facets.extractors.AbstractFacetExtractor;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Connector {

    static FlxLogger logger = FlxLogger.getLogger(Connector.class);

    UpdateStrategyType updateStrategyType = UpdateStrategyType.INCREMENTAL;

    private static Map<String, Connector> connectors = new ConcurrentHashMap<String, Connector>();
    private static Map<Integer, Connector> connectorsByValue = new ConcurrentHashMap<Integer, Connector>();
    private static Map<String, Connector> connectorsByPrettyName = new ConcurrentHashMap<String, Connector>();
    private static Map<String, Connector> connectorsByDeviceNickname = new ConcurrentHashMap<String, Connector>();

    private Class<? extends AbstractFacetExtractor> extractorClass;
    private Map<Integer, Class<? extends AbstractFacetExtractor>> objectTypeExtractorClasses;
    private Class<? extends AbstractUserProfile> userProfileClass;
    private ObjectType[] objectTypes;
    private Class<? extends AbstractFacet> facetClass;
    private int value;
    private String name;
    private String prettyName;
    private int[] objectTypeValues;
    private boolean hasFacets;
    private String[] defaultChannels;
    private Class<? extends AbstractUpdater> updaterClass;
    private Class<? extends AbstractBodytrackResponder> bodytrackResponder;
    private String deviceNickname;

    static {
        Connector flxConnector = new Connector();
        flxConnector.name = "fluxtream";
        flxConnector.deviceNickname = "FluxtreamCapture";
        connectors.put(flxConnector.name, flxConnector);
        connectorsByValue.put(0xCAFEBABE, flxConnector);
        // NOTE! This connector has no pretty name, and ConcurrentHashMaps don't allow keys or values to be null, so
        // we won't add it to the connectorsByPrettyName map.
        ObjectType objectType = new ObjectType();
        objectType.value = 0xBABEFACE;
        objectType.name = "comment";
        ObjectType.addObjectType(objectType.name, flxConnector, objectType);
    }

    private int[] deleteOrder;
    private Class<? extends SharedConnectorFilter> sharedConnectorFilterClass;

    public Class<? extends SharedConnectorFilter> sharedConnectorFilterClass() {
        return sharedConnectorFilterClass;
    }

    public boolean supportsFiltering() {
        return this.sharedConnectorFilterClass != DefaultSharedConnectorFilter.class;
    }

    public String toString() {
        String string = "{name:" + name;
        if (this.objectTypes != null) {
            string += ", objectTypes:[";
            for (int i = 0; i < objectTypes.length; i++) {
                if (i > 0)
                    string += ", ";
                string += objectTypes[i].getName() + "/"
                          + objectTypes[i].value();
            }
            string += "], objectTypeValues: [" + toString(objectTypeValues())
                      + "], objectTypeExtractorClasses: "
                      + this.objectTypeExtractorClasses + "}";
        } else {
            string += ", extractorClass: " + this.extractorClass + "}";
        }
        return string;
    }

    private String toString(int[] values) {
        String s = "";
        for (int i = 0; i < values.length; i++) {
            if (i > 0)
                s += ", ";
            s += values[i];
        }
        return s;
    }

    public static Collection<Connector> getAllConnectors() {
        return connectors.values();
    }

    public static Connector fromString(String s) {
        return connectors.get(s);
    }

    private static boolean initialized = false;

    static {
        if (!initialized) {
            ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(
                    false);
            scanner.addIncludeFilter(new AnnotationTypeFilter(Updater.class));
            for (BeanDefinition bd : scanner
                    .findCandidateComponents("org.fluxtream.connectors"))
                extractConnectorMetadata(bd);
            initialized = true;
            for (Connector connector : connectors.values()) {
                System.out.println(connector);
            }
        }
    }

    private static void extractConnectorMetadata(final BeanDefinition bd) {
        String beanClassName = bd.getBeanClassName();
        String connectorName = getConnectorName(beanClassName);
        Connector connector = new Connector();
        connector.updaterClass = getUpdaterClass(beanClassName);
        Updater updaterAnnotation = connector.updaterClass
                .getAnnotation(Updater.class);
        // set connectors' pretty name
        connector.prettyName = updaterAnnotation.prettyName();
        connector.deviceNickname = updaterAnnotation.deviceNickname().equals(Updater.DEVICE_NICKNAME_NONE)
                                 ? updaterAnnotation.prettyName()==null ? connectorName : updaterAnnotation.prettyName()
                                 : updaterAnnotation.deviceNickname();
        connector.value = updaterAnnotation.value();
        connector.updateStrategyType = updaterAnnotation
                .updateStrategyType();
        connector.hasFacets = updaterAnnotation.hasFacets();
        connector.name = connectorName;
        connector.sharedConnectorFilterClass = updaterAnnotation.sharedConnectorFilter();
        connector.deleteOrder = updaterAnnotation.deleteOrder();
        // set connectors' object types
        Class<? extends AbstractFacet>[] facetTypes = updaterAnnotation
                .objectTypes();
        if (updaterAnnotation.extractor() != AbstractFacetExtractor.class)
            connector.extractorClass = updaterAnnotation.extractor();
        if (facetTypes.length == 1) {
            connector.facetClass = facetTypes[0];
        }
        if (updaterAnnotation.userProfile() != AbstractUserProfile.class)
            connector.userProfileClass = updaterAnnotation
                    .userProfile();
        connector.defaultChannels = updaterAnnotation.defaultChannels();
        List<ObjectType> connectorObjectTypes = new ArrayList<ObjectType>();
        for (Class<? extends AbstractFacet> facetType : facetTypes) {
            final ObjectType objectType = getFacetTypeMetadata(connector, facetTypes, facetType);
            connectorObjectTypes.add(objectType);
            ObjectType.addObjectType(objectType.name(), connector, objectType);
        }

        if (connectorObjectTypes.size()>0)
            connector.objectTypes = connectorObjectTypes.toArray(new ObjectType[0]);

        connectors.put(connectorName, connector);
        connectorsByValue.put(connector.value(), connector);
        connectorsByDeviceNickname.put(connector.deviceNickname, connector);
        if (connector.prettyName != null) {
            connectorsByPrettyName.put(connector.prettyName.toLowerCase(), connector);
        }
        final Class<? extends AbstractBodytrackResponder> bodytrackResponderClass = updaterAnnotation.bodytrackResponder();
        if (!(bodytrackResponderClass == AbstractBodytrackResponder.class))
            connector.bodytrackResponder = bodytrackResponderClass;
    }

    private static ObjectType getFacetTypeMetadata(final Connector connector,
                                             final Class<? extends AbstractFacet>[] facetTypes,
                                             final Class<? extends AbstractFacet> facetType) {
        ObjectTypeSpec ots = facetType
                .getAnnotation(ObjectTypeSpec.class);
        // objectTypes are mandatory only if there are more than 1
        if (ots == null) {
            if (facetTypes.length>1)
                throw new RuntimeException(
                        "No ObjectTypeSpec Annotation for Facet ["
                        + facetType.getName() + "]");
            else
                return null;
        }
        ObjectType objectType = new ObjectType();
        objectType.facetClass = facetType;
        objectType.value = ots.value();
        objectType.name = ots.name();
        objectType.prettyname = ots.prettyname();
        objectType.isImageType = ots.isImageType();
        objectType.isDateBased = ots.isDateBased();
        objectType.isMixedType = ots.isMixedType();
        objectType.isClientFacet = ots.clientFacet();
        objectType.visibleClause = ots.visibleClause().equals("")?null:ots.visibleClause();
        objectType.orderBy = ots.orderBy().equals("")?null:ots.orderBy();
        if (ots.extractor() != null && ots.extractor()!=AbstractFacetExtractor.class) {
            connector.addObjectTypeExtractorClass(
                    objectType.value, ots.extractor(),
                    ots.parallel());
        }
        return objectType;
    }

    public boolean hasImageObjectType() {
        if (objectTypes==null) return false;
        for (ObjectType objectType: objectTypes) {
            if (objectType.isImageType())
                return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends AbstractUpdater> getUpdaterClass(
            String beanClassName) {
        Class<? extends AbstractUpdater> updaterClass = null;
        try {
            updaterClass = (Class<? extends AbstractUpdater>) Class
                    .forName(beanClassName);
        } catch (Throwable t) {
            throw new RuntimeException("Could not get Updater Class for ["
                                       + beanClassName + "]");
        }
        return updaterClass;
    }

    private void addObjectTypeExtractorClass(int objectTypeValue,
                                             Class<? extends AbstractFacetExtractor> extractorClass,
                                             boolean parallel) {
        if (this.objectTypeExtractorClasses == null)
            this.objectTypeExtractorClasses = new ConcurrentHashMap<Integer, Class<? extends AbstractFacetExtractor>>();
        if (!parallel
            && this.objectTypeExtractorClasses
                .containsValue(extractorClass)) {
            Set<Integer> keySet = this.objectTypeExtractorClasses.keySet();
            int previousObjectType = -1;
            for (Integer objectType : keySet) {
                if (this.objectTypeExtractorClasses.get(objectType) == extractorClass) {
                    previousObjectType = objectType;
                    this.objectTypeExtractorClasses.remove(objectType);
                    break;
                }
            }
            this.objectTypeExtractorClasses.put(previousObjectType
                                                + objectTypeValue, extractorClass);
        } else
            this.objectTypeExtractorClasses
                    .put(objectTypeValue, extractorClass);
    }

    public static String getConnectorName(String beanClassName) {
        final String[] splits = StringUtils.split(beanClassName, ".");
        return splits[splits.length-2];
    }

    public Class<? extends AbstractUpdater> getUpdaterClass() {
        return updaterClass;
    }

    public String statusNotificationName() {
        return new StringBuilder(getName()).append(".status").toString();
    }

    public enum UpdateStrategyType {
        ALWAYS_UPDATE, INCREMENTAL
    }

    public boolean isAutonomous() {
        final Class<?>[] interfaces = this.updaterClass.getInterfaces();
        for (Class<?> anInterface : interfaces) {
            if (anInterface==Autonomous.class)
                return true;
        }
        return false;
    }

    private Connector() {
    }

    public String[] getDefaultChannels(){
        return defaultChannels;
    }

    public ObjectType[] objectTypes() {
        return this.objectTypes;
    }

    public int[] objectTypeValues() {
        if (this.objectTypeValues == null) {
            if (this.objectTypeExtractorClasses != null
                && this.objectTypeExtractorClasses.size() > 0) {
                Set<Integer> keySet = this.objectTypeExtractorClasses.keySet();
                Iterator<Integer> eachKey = keySet.iterator();
                this.objectTypeValues = new int[keySet.size()];
                for (int i = 0; eachKey.hasNext(); i++) {
                    this.objectTypeValues[i] = eachKey.next().intValue();
                }
            } else {
                this.objectTypeValues = new int[] { -1 };
            }
        }
        return this.objectTypeValues;
    }

    public Class<? extends AbstractUserProfile> userProfileClass() {
        return userProfileClass;
    }

    public Class<? extends AbstractFacet> facetClass() {
        return facetClass;
    }

    public String getName() {
        return name;
    }

    public int getValue() {
        return value;
    }

    public int value() {
        return value;
    }

    public AbstractFacetExtractor extractor(int objectTypes, BeanFactory beanFactory) {
        if (extractorClass != null)
            try {
                return beanFactory.getBean(extractorClass);
            } catch (Exception e) {
                e.printStackTrace();
            }
        else if (objectTypes != -1) {
            Iterator<Integer> eachObjectTypeValue = objectTypeExtractorClasses.keySet().iterator();
            Class<? extends AbstractFacetExtractor> extractorClass = null;
            while (eachObjectTypeValue.hasNext()) {
                int objectTypeValue = eachObjectTypeValue.next();
                if ((objectTypes&objectTypeValue)!=0) {
                    extractorClass = objectTypeExtractorClasses.get(objectTypeValue);
                    break;
                }
            }
            try {
                if (extractorClass!=null)
                    return beanFactory.getBean(extractorClass);
                else {
                    logger.error("COULD NOT FIND EXTRACTOR CLASS FOR " + objectTypes);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public UpdateStrategyType updateStrategyType() {
        return this.updateStrategyType;
    }

    public String getPrettyName() {
        return prettyName();
    }

    public String getDeviceNickname() {
        return deviceNickname;
    }

    public String prettyName() {
        return prettyName;
    }

    public int[] getDeleteOrder() {
        return deleteOrder;
    }

    public boolean hasDeleteOrder() {
        return !ArrayUtils.isEquals(deleteOrder, new int[]{-1});
    }

    public ObjectType[] getObjectTypesForValue(int value) {
        if (this.objectTypes==null) return null;
        List<ObjectType> result = new ArrayList<ObjectType>();
        for(ObjectType objectType : objectTypes) {
            if ((value&objectType.value)!=0)
                result.add(objectType);
        }
        return result.toArray(new ObjectType[0]);
    }

    public boolean hasFacets() {
        return hasFacets;
    }

    public static Connector getConnector(String apiName) {
        return fromString(apiName.toLowerCase());
    }

    public static Connector fromValue(int api) {
        return connectorsByValue.get(api);
    }

    public static Connector fromDeviceNickname(String deviceNickname) {
        return connectorsByDeviceNickname.get(deviceNickname);
    }

    /**
     * Returns the Connector having the given pretty name.  Returns <code>null</code> if no such connector exists or
     * if the given pretty name is <code>null</code>.
     */
    public static Connector fromPrettyName(@Nullable final String prettyName) {
        if (prettyName != null) {
            return connectorsByPrettyName.get(prettyName);
        }
        return null;
    }

    public AbstractBodytrackResponder getBodytrackResponder(BeanFactory beanFactory){
        try{
            if (bodytrackResponder!=null) {
                final AbstractBodytrackResponder bean = beanFactory.getBean(bodytrackResponder);
                return bean;
            }
        }
        catch (Exception e){
            System.out.println("COULD NOT INSTANTIATE RESPONDER: " + bodytrackResponder);
            System.out.println("PLEASE CHECK THAT IT HAS THE @Component ANNOTATION!");
            return null;
        }
        return null;
    }

}
