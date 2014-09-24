package org.fluxtream.core.mvc.models;

import com.wordnik.swagger.model.Model;
import com.wordnik.swagger.model.ModelProperty;
import com.wordnik.swagger.model.ModelRef;
import scala.Tuple2;
import scala.collection.Iterator;
import scala.collection.mutable.LinkedHashMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This model is used to convert the scala objects produced by swagger into Java objects that we can automatically serialize.
 *
 * //Example usage:
 * List<ClassModel> list = new ArrayList<ClassModel>();
 * Iterator<Model> i = ModelConverters.readAll(ClassModel.class).iterator();
 * while (i.hasNext())
 *   list.add(new ClassModel(i.next()));
 *
 * Created by justin on 9/24/14.
 */
public class ClassModel {
    public String id;
    public String name;
    public String qualifiedType;
    public Map<String,ClassModelProperty> properties;
    public String description;
    public String baseModel;
    public String discriminator;
    public List<String> subTypes;

    public ClassModel(Model m){
        id = m.id();
        name = m.name();
        qualifiedType = m.qualifiedType();

        description =  m.description().isEmpty() ? null : m.description().get();
        baseModel = m.baseModel().isEmpty() ? null : m.baseModel().get();
        discriminator = m.discriminator().isEmpty() ? null : m.discriminator().get();

        properties = new HashMap<String,ClassModelProperty>();

        LinkedHashMap<String,ModelProperty> propertiesMap = m.properties();
        Iterator<Tuple2<String,ModelProperty>> i = propertiesMap.iterator();
        while (i.hasNext()){
            Tuple2<String,ModelProperty> t = i.next();
            properties.put(t._1(),new ClassModelProperty(t._2()));
        }

        subTypes = new ArrayList<String>();

        Iterator<String> stypes = m.subTypes().iterator();
        while (stypes.hasNext())
            subTypes.add(stypes.next());
    }

    public static class ClassModelProperty {

        public String type;
        public String qualifiedType;
        public int position;
        public boolean required;
        public String description;
        public com.wordnik.swagger.model.AllowableValues allowableValues;
        public ClassModelPropertyRef items;

        public ClassModelProperty(ModelProperty m) {
            type = m.type();
            qualifiedType = m.qualifiedType();
            position = m.position();
            required = m.required();
            description = m.description().isEmpty() ? null : m.description().get();

            //TODO: parse allowable values
            //note if we do this assignment we'll crash the serializer since the object can't be serialized
            //allowableValues = m.allowableValues();


            items = m.items().isEmpty() ? null : new ClassModelPropertyRef(m.items().get());

        }
    }

    public static class ClassModelPropertyRef{

        public String type;
        public String ref;
        public String qualifiedType;

        public ClassModelPropertyRef(ModelRef m) {
            type = m.type();
            ref = m.ref().isEmpty() ? null : m.ref().get();
            qualifiedType = m.qualifiedType().isEmpty() ? null : m.qualifiedType().get();
        }
    }



}
