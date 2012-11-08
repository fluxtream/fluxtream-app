package com.fluxtream.connectors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import com.fluxtream.connectors.annotations.JsonFacetCollection;
import com.fluxtream.connectors.annotations.ObjectTypeSpec;
import com.fluxtream.connectors.annotations.Updater;
import com.fluxtream.connectors.dao.FacetDao;
import com.fluxtream.connectors.updaters.AbstractUpdater;
import com.fluxtream.connectors.vos.AbstractFacetVOCollection;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.domain.AbstractUserProfile;
import com.fluxtream.facets.extractors.AbstractFacetExtractor;
import org.apache.velocity.util.StringUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;

@Component
public class Connector {

    private static Map<String, Connector> connectors = new ConcurrentHashMap<String, Connector>();
    private static Map<Integer, Connector> connectorsByValue = new ConcurrentHashMap<Integer, Connector>();

    @Autowired
    BeanFactory beanFactory;

    static {
        Connector flxConnector = new Connector();
        flxConnector.name = "fluxtream";
        connectors.put(flxConnector.name, flxConnector);
        connectorsByValue.put(0xCAFEBABE, flxConnector);
        ObjectType objectType = new ObjectType();
        objectType.value = 0xBABEFACE;
        objectType.name = "comment";
        ObjectType.addObjectType(objectType.name, flxConnector, objectType);
    }

    @SuppressWarnings("rawtypes")
    private Map<String, Class<? extends AbstractFacetVOCollection>> jsonFacetCollectionClasses = new ConcurrentHashMap<String, Class<? extends AbstractFacetVOCollection>>();

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

    public FacetDao getCustomDao() {
        return null;
    }

    public static Collection<Connector> getAllConnectors() {
        return connectors.values();
    }

    public static Connector fromString(String s) {
        return connectors.get(s);
    }

    @SuppressWarnings("rawtypes")
    public AbstractFacetVOCollection getJsonFacetCollection() {
        return getJsonFacetCollection(null);
    }

    @SuppressWarnings("rawtypes")
    private AbstractFacetVOCollection getJsonFacetCollection(String name) {
        if (name == null)
            name = "default";
        Class<? extends AbstractFacetVOCollection> clazz = jsonFacetCollectionClasses
                .get(name);
        if (clazz == null)
            return null;
        try {
            return clazz.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(
                    "Could not instantiate json facet collection: "
                    + this.getClass().getName() + "/" + name);
        }
    }

    private static boolean initialized = false;

    static {
        if (!initialized) {
            ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(
                    false);
            scanner.addIncludeFilter(new AnnotationTypeFilter(Updater.class));
            for (BeanDefinition bd : scanner
                    .findCandidateComponents("com.fluxtream.connectors"))
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
        connector.manageable = updaterAnnotation.isManageable();
        connector.prettyName = updaterAnnotation.prettyName();
        connector.value = updaterAnnotation.value();
        connector.updateStrategyType = updaterAnnotation
                .updateStrategyType();
        connector.hasFacets = updaterAnnotation.hasFacets();
        // set connectors' object types
        Class<? extends AbstractFacet>[] facetTypes = updaterAnnotation
                .objectTypes();
        if (updaterAnnotation.extractor() != AbstractFacetExtractor.class)
            connector.extractorClass = updaterAnnotation.extractor();
        if (facetTypes.length == 1) {
            connector.facetClass = facetTypes[0];
            if (updaterAnnotation.userProfile() != AbstractUserProfile.class)
                connector.userProfileClass = updaterAnnotation
                        .userProfile();
        }
        connector.defaultChannels = updaterAnnotation.defaultChannels();
        List<ObjectType> connectorObjectTypes = new ArrayList<ObjectType>();
        connector.objectTypeExtractorClasses = new ConcurrentHashMap<Integer, Class<? extends AbstractFacetExtractor>>();
        for (Class<? extends AbstractFacet> facetType : facetTypes) {
            ObjectTypeSpec ots = facetType
                    .getAnnotation(ObjectTypeSpec.class);
            // objectTypes are mandatory only if there are more than 1
            if (ots == null) {
                if (facetTypes.length>1)
                    throw new RuntimeException(
                            "No ObjectTypeSpec Annotation for Facet ["
                            + facetType.getName() + "]");
                else
                    continue;
            }
            ObjectType objectType = new ObjectType();
            objectType.facetClass = facetType;
            objectType.value = ots.value();
            objectType.name = ots.name();
            objectType.prettyname = ots.prettyname();
            objectType.isImageType = ots.isImageType();
            objectType.isDateBased = ots.isDateBased();
            if (ots.extractor() != null) {
                connector.addObjectTypeExtractorClass(
                        objectType.value, ots.extractor(),
                        ots.parallel());
            }
            connectorObjectTypes.add(objectType);
            ObjectType.addObjectType(ots.name(), connector,
                                     objectType);
        }
        if (connectorObjectTypes.size()>0)
            connector.objectTypes = connectorObjectTypes.toArray(new ObjectType[0]);

        JsonFacetCollection jsonFacetAnnotation = connector.updaterClass
                .getAnnotation(JsonFacetCollection.class);
        if (jsonFacetAnnotation != null)
            connector.jsonFacetCollectionClasses.put(
                    jsonFacetAnnotation.name(),
                    jsonFacetAnnotation.value());

        connector.name = connectorName;
        connectors.put(connectorName, connector);
        connectorsByValue.put(connector.value(), connector);

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

    public enum UpdateStrategyType {
        ALWAYS_UPDATE, INCREMENTAL
    }

    UpdateStrategyType updateStrategyType = UpdateStrategyType.INCREMENTAL;

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
    private String[] additionalParameters;
    private boolean manageable;
    private Class<? extends AbstractUpdater> updaterClass;
    private AbstractUpdater updater;

    public AbstractUpdater getUpdater() {
        if (this.updater==null)
            this.updater = beanFactory.getBean(updaterClass);
        return this.updater;
    }

    public boolean isManageable(){
        return manageable;
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
                return beanFactory.getBean(extractorClass);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public UpdateStrategyType updateStrategyType() {
        return this.updateStrategyType;
    }

    public String prettyName() {
        return prettyName;
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

    public boolean hasAdditionalParameters() {
        return additionalParameters != null && additionalParameters.length > 0;
    }

    public String[] getAdditionalParameters() {
        return additionalParameters;
    }

}
