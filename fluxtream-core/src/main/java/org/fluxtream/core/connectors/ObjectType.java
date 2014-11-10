package org.fluxtream.core.connectors;

import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;
import org.fluxtream.core.connectors.annotations.ObjectTypeSpec;
import org.fluxtream.core.domain.AbstractFacet;

import java.util.*;

@ApiModel("Each Connector maintains bags of facets of different types. An ObjectType is the description of such a facet type.")
public class ObjectType {

    @ApiModelProperty(value="The name of the object type", required=true)
    String name;
    @ApiModelProperty(value="The pretty name of the object type", required=true)
    String prettyname;
    boolean isDateBased;
    boolean isMixedType;
    boolean isClientFacet;
    String visibleClause;
    String orderBy;

	private static Map<Connector,List<ObjectType>> connectorObjectTypes = new Hashtable<Connector,List<ObjectType>>();
	
	private static Map<Connector,Map<String,ObjectType>> connectorNamedObjectTypes = new Hashtable<Connector,Map<String,ObjectType>>();
	
	private static Map<Connector,Map<Integer,ObjectType>> connectorObjectTypeValues = new Hashtable<Connector,Map<Integer,ObjectType>>();
	
	private static Map<String, ObjectType> customObjectTypes = new Hashtable<String, ObjectType>();

    /**
	 * "Custom" objectTypes are there to compute a value (hashCode())
	 * for special API calls. We use it for counting those calls so we
	 * don't confuse them with the usual API calls that retrieve data.
	 * @param name
	 */
	public static void registerCustomObjectType(String name) {
		ObjectType customObjectType = new ObjectType();
		customObjectType.name = name;
		customObjectType.value = name.hashCode();
		customObjectTypes.put(name, customObjectType);
	}
	
	public static ObjectType getCustomObjectType(String name) {
		return customObjectTypes.get(name);
	}

    public String getApiKeyAttributeName(String attName) {
        return new StringBuilder(name).append("/").append(attName).toString();
    }

	public static ObjectType getObjectType(Connector connector, int objectType) {
		Map<Integer, ObjectType> connectorObjectTypes = connectorObjectTypeValues.get(connector);
        if (connectorObjectTypes!=null) {
    		ObjectType type = connectorObjectTypes.get(objectType);
    		return type;
        }
        return null;
	}
	
	static void addObjectType(String name, Connector connector, ObjectType value) {
		if (!connectorObjectTypes.containsKey(connector))
			connectorObjectTypes.put(connector, new Vector<ObjectType>());
		connectorObjectTypes.get(connector).add(value);
		if (!connectorNamedObjectTypes.containsKey(connector))
			connectorNamedObjectTypes.put(connector, new Hashtable<String,ObjectType>());
		connectorNamedObjectTypes.get(connector).put(value.name(), value);
		if (!connectorObjectTypeValues.containsKey(connector))
			connectorObjectTypeValues.put(connector, new Hashtable<Integer,ObjectType>());
		connectorObjectTypeValues.get(connector).put(value.value(), value);
	}
	
	public static ObjectType getObjectType(Connector connector, String name) {
		Map<String, ObjectType> connectorObjectTypes = connectorNamedObjectTypes.get(connector);
		ObjectType namedObjectType = connectorObjectTypes.get(name);
		return namedObjectType;
	}

	public static List<ObjectType> getObjectTypes(Connector connector, int objectTypes) {
		List<ObjectType> connectorTypes = connectorObjectTypes.get(connector);
		if (connectorTypes==null) return null;
		List<ObjectType> result = new ArrayList<ObjectType>();
		for (ObjectType objectType : connectorTypes) {
			if ((objectTypes & objectType.value())!=0)
				result.add(objectType);
		}
		return result;
	}

    @ApiModelProperty(value="Is data referred to by this object type (local) date based?", required=true)
    public boolean isDateBased() {
        return isDateBased;
    }

    @ApiModelProperty(value="Is it a client, non-technical, facet, of the type that mean something to the end-user?", required=true)
    public boolean isClientFacet() {
        return isClientFacet;
    }

	public String toString() {
		return name;
	}
	
	public String getName() {
		return name;
	}
	
	int value;
	boolean isImageType;
	
	Class<? extends AbstractFacet> facetClass;

	ObjectType(){}
	
	public Class<? extends AbstractFacet> facetClass() {
		return facetClass;
	}
	
	public String name() {
		return name;
	}

    @ApiModelProperty(value="The bitmask value for this ObjectType", required=true)
	public int value() {
		return value;
	}

    @ApiModelProperty(value="Does the data this object type refers to contain an image?", required=true)
	public boolean isImageType() {
		return isImageType;
	}

	public String prettyname() {
		return prettyname;
	}

    @ApiModelProperty(hidden=true)
    public String visibleClause() {
        return visibleClause;
    }

    @ApiModelProperty(hidden=true)
    public String orderBy() {
        return orderBy;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ObjectType that = (ObjectType)o;

        if (isDateBased != that.isDateBased) {
            return false;
        }
        if (isImageType != that.isImageType) {
            return false;
        }
        if (value != that.value) {
            return false;
        }
        if (facetClass != null ? !facetClass.equals(that.facetClass) : that.facetClass != null) {
            return false;
        }
        if (name != null ? !name.equals(that.name) : that.name != null) {
            return false;
        }
        if (prettyname != null ? !prettyname.equals(that.prettyname) : that.prettyname != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (prettyname != null ? prettyname.hashCode() : 0);
        result = 31 * result + (isDateBased ? 1 : 0);
        result = 31 * result + (isMixedType ? 1 : 0);
        result = 31 * result + value;
        result = 31 * result + (isImageType ? 1 : 0);
        result = 31 * result + (facetClass != null ? facetClass.hashCode() : 0);
        return result;
    }

    @ApiModelProperty(value="Can this object type refer both to date-based data and epoch time-based intervals?", required=true)
    public boolean isMixedType() {
        return isMixedType;
    }

    public static int getObjectTypeValue(final Class<? extends AbstractFacet> facetClass) {
        try {
            final ObjectTypeSpec annotation = facetClass.getAnnotation(ObjectTypeSpec.class);
            final int value = annotation.value();
            return value;
        } catch (Throwable t) {
            final String message = "Could not get Facet ObjectType value for " + facetClass.getName();
            throw new RuntimeException(message);
        }
    }
}
