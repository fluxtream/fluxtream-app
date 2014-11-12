package org.fluxtream.core.api;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.converter.ModelConverters;
import com.wordnik.swagger.model.Model;
import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.connectors.ObjectType;
import org.fluxtream.core.domain.AbstractFacet;
import org.fluxtream.core.mvc.models.ClassModel;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import scala.collection.Iterator;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

@Path("/v1/datamodel")
@Component("RESTDataModelStore")
@Api(value = "/datamodel", description = "Get Data Model")
@Scope("request")
public class DataModelStore {

    @GET
    @Path("/get")
    @Produces({MediaType.APPLICATION_JSON})
    public Response test(@QueryParam("class") String className){
        try{
            if (className == null)
                throw new ClassNotFoundException();
            List<ClassModel> list = new ArrayList<ClassModel>();
            Iterator<Model> i = ModelConverters.readAll(Class.forName(className)).iterator();
            while (i.hasNext())
                list.add(new ClassModel(i.next()));
            return Response.ok(list).build();
        }
        catch (ClassNotFoundException e) {
            return Response.status(400).entity("Could not find " + className).build();
        }
        catch (Exception e){
            return Response.status(500).entity("An error occurred: " + e.getMessage()).build();
        }
    }

    @GET
    @Path("/facetVos")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getAllFacetClasses(){
        TreeSet<Connector> sortedConnectors = new TreeSet<Connector>(new Comparator<Connector>() {
            @Override
            public int compare(Connector connector, Connector connector2) {
                return connector.getName().compareTo(connector2.getName());
            }
        });
        sortedConnectors.addAll(Connector.getAllConnectors());
        List<ConnectorFacetClasses> facetVos = new ArrayList<ConnectorFacetClasses>();
        for (Connector sortedConnector : sortedConnectors) {
            final ObjectType[] objectTypes = sortedConnector.objectTypes();
            if (objectTypes!=null) {
                ConnectorFacetClasses cfc = new ConnectorFacetClasses();
                cfc.connectorName = sortedConnector.getPrettyName();
                for (ObjectType objectType : objectTypes) {
                    final Class<? extends AbstractFacet> aClass = objectType.facetClass();
                    try {
                        final String voClassName = aClass.getName() + "VO";
                        // if the VO class exists, add it
                        Class.forName(voClassName);
                        cfc.facetVos.add(voClassName);
                    } catch (Exception e) {
                    }
                }
                facetVos.add(cfc);
            }
        }
        return Response.ok().entity(facetVos).build();
    }

    public static class ConnectorFacetClasses {
        public String connectorName;
        public List<String> facetVos = new ArrayList<String>();
    }

}
