package io.swagger.api;

import io.swagger.model.*;
import io.swagger.api.PredictApiService;
import io.swagger.api.factories.PredictApiServiceFactory;

import io.swagger.annotations.ApiParam;
import io.swagger.jaxrs.*;

import io.swagger.model.BadRequestError;
import io.swagger.model.Classification;
import io.swagger.model.Error;

import java.util.List;
import io.swagger.api.NotFoundException;

import java.io.InputStream;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import javax.servlet.ServletConfig;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.*;
import javax.validation.constraints.*;

@Path("/predict")


@io.swagger.annotations.Api(description = "the predict API")
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2017-10-06T12:54:15.929Z")
public class PredictApi  {
   private final PredictApiService delegate;

   public PredictApi(@Context ServletConfig servletContext) {
      PredictApiService delegate = null;

      if (servletContext != null) {
         String implClass = servletContext.getInitParameter("PredictApi.implementation");
         if (implClass != null && !"".equals(implClass.trim())) {
            try {
               delegate = (PredictApiService) Class.forName(implClass).newInstance();
            } catch (Exception e) {
               throw new RuntimeException(e);
            }
         } 
      }

      if (delegate == null) {
         delegate = PredictApiServiceFactory.getPredictApi();
      }

      this.delegate = delegate;
   }

    @POST
    
    @Consumes({ "multipart/form-data" })
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Make a prediction on the given SMILES", notes = "", response = Classification.class, tags={  })
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 200, message = "OK", response = Classification.class),
        
        @io.swagger.annotations.ApiResponse(code = 400, message = "Bad Request", response = Classification.class),
        
        @io.swagger.annotations.ApiResponse(code = 500, message = "Prediction error", response = Classification.class),
        
        @io.swagger.annotations.ApiResponse(code = 503, message = "Service not available", response = Classification.class) })
    public Response predictPost(@ApiParam(value = "The desired confidence of the prediction", required=true, defaultValue="0.8")@FormDataParam("confidence")  Double confidence
,@ApiParam(value = "Compound structure notation using SMILES notation", required=true, defaultValue="CCCCC=O")@FormDataParam("smiles")  String smiles
,@Context SecurityContext securityContext)
    throws NotFoundException {
        return delegate.predictPost(confidence,smiles,securityContext);
    }
}
