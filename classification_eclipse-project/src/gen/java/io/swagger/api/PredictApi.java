package io.swagger.api;

import javax.servlet.ServletConfig;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.api.factories.PredictApiServiceFactory;
import io.swagger.model.BadRequestError;
import io.swagger.model.Classification;
import io.swagger.model.Error;

@Path("/")

@Api()
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

	@Path("/predict")
	@GET
	@Consumes({ "multipart/form-data" })
	@Produces({ "application/json" })
	@ApiOperation(value = "Make a prediction on the given SMILES", 
	notes = "Predict a given SMILES to get the p-values for the given class, in JSON format", 
	response = Void.class, 
	tags={"Predict"})
	@ApiResponses(value = { 
			@ApiResponse(code = 200, message = "OK", response = Classification.class),

			@ApiResponse(code = 400, message = "Bad Request", response = BadRequestError.class),

			@ApiResponse(code = 500, message = "Prediction error", response = Error.class),

			@ApiResponse(code = 503, message = "Service not available", response = Error.class) })
	public Response predictPost(
			@ApiParam(value = "Compound structure notation using SMILES notation", required=true)
			@DefaultValue("CCCCC=O") @QueryParam("smiles") String smiles,
			@Context SecurityContext securityContext)
					throws NotFoundException {
		return delegate.predictPost(smiles,securityContext);
	}


	@Path("/predictImage")
	@GET
	@Consumes({ "multipart/form-data" })
	@Produces("image/png")
	@ApiOperation(value = "Make a prediction image from the given SMILES", 
	notes = "Predict a given SMILES to get a prediction image",
	response = Void.class, 
	tags={"Predict"})
	@ApiResponses(value = { 
			@ApiResponse(code = 200, message = "OK"),

			@ApiResponse(code = 400, message = "Bad Request", response = BadRequestError.class),

			@ApiResponse(code = 500, message = "Prediction error", response = Error.class),

			@ApiResponse(code = 503, message = "Service not available", response = Error.class) })
	public Response predictImagePost( 
			@ApiParam(value = "Compound structure notation using SMILES notation", required=true)
			@DefaultValue("CCCCC=O") @QueryParam("smiles") String smiles, 
			@Context SecurityContext securityContext ) {

		return delegate.predictImagePost(smiles, securityContext);
	}
}
