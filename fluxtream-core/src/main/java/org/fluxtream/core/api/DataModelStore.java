package org.fluxtream.core.api;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.converter.ModelConverters;
import com.wordnik.swagger.model.Model;
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
import java.util.List;

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
            for (ClassModel model : list){
                if (model.qualifiedType.equals(className))
                    return Response.ok(model).build();
            }
            throw new ClassNotFoundException();
        }
        catch (ClassNotFoundException e) {
            return Response.status(400).entity("Could not find " + className).build();
        }
        catch (Exception e){
            return Response.status(500).entity("An error occurred: " + e.getMessage()).build();
        }
    }
}
