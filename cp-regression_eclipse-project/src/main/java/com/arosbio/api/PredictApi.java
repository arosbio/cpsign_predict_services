package com.arosbio.api;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.slf4j.Logger;

import com.arosbio.api.model.BadRequestError;
import com.arosbio.api.model.ErrorResponse;
import com.arosbio.api.model.RegressionResult;
import com.arosbio.api.utils.NotFoundException;
import com.arosbio.impl.Predict;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@Path("/v2")
public class PredictApi  {
	private static Logger logger = org.slf4j.LoggerFactory.getLogger(PredictApi.class);
	
	@Path("/predict")
	@GET
	@Consumes({ MediaType.MULTIPART_FORM_DATA })
	@Produces({ MediaType.APPLICATION_JSON })
	@Operation(
			summary = "Make a prediction on a given molecule", 
			tags={"Predict"},
			description = "Predict a given molecule in SMILES, MDL v2000/v3000 format. In case a MDL is sent, it must be properly URL-encoded in UTF-8. You can use for instance https://www.urlencoder.org/ to encode your file.", 
			responses = { 
					@ApiResponse(responseCode = "200", description = "OK", content = @Content(
							schema = @Schema(implementation = RegressionResult.class))),

					@ApiResponse(responseCode = "400", description = "Bad Request",content = @Content(
							schema = @Schema(implementation = BadRequestError.class))),

					@ApiResponse(responseCode = "500", description = "Prediction error", content = @Content(
							schema = @Schema(implementation = ErrorResponse.class))),

					@ApiResponse(responseCode = "503", description = "Service not available", content = @Content(
							schema = @Schema(implementation = ErrorResponse.class)))
			}
			)
	public Response predictGet(

			@Parameter(description = "Compound structure notation using SMILES or MDL format", required=false, example="CCCCC=O")
			@QueryParam("molecule") String molecule,

			@Parameter(description = "The desired confidence of the prediction") //, allowableValues="range(0,1)")
			@DefaultValue("0.8") @QueryParam("confidence") double confidence,

			@Context SecurityContext securityContext)
					throws NotFoundException {
		return Predict.doPredict(molecule, confidence);
	}


	@Path("/predictImage")
	@GET
	@Consumes({ MediaType.MULTIPART_FORM_DATA })
	@Produces({ "image/png" }) //, MediaType.APPLICATION_JSON
	@Operation(
			summary = "Make a prediction image for the given molecule",
			tags={"Predict"},
			description = "Predict a given molecule to get a prediction image, accepts SMILES or MDL v2000/v3000 format. In case a MDL is sent, it must be properly URL-encoded in UTF-8. You can use for instance https://www.urlencoder.org/ to encode your file.",
			responses = { 
					@ApiResponse(responseCode = "200", description = "OK"),

					@ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(
							schema = @Schema(implementation = BadRequestError.class))),

					@ApiResponse(responseCode = "500", description = "Prediction error", content = @Content(
							schema = @Schema(implementation = ErrorResponse.class))),

					@ApiResponse(responseCode = "503", description = "Service not available", content = @Content(
							schema = @Schema(implementation = ErrorResponse.class))) }
			)
	public Response predictImageGet( 

			@Parameter(description = "Compound structure notation using SMILES or MDL format", 
			required=false, 
			example="CCCCC=O")
			@QueryParam("molecule") String molecule,

			@Parameter(description = "Image width (min 50 pixels, max 5000 pixels)") // TODO, allowableValues="range[50,5000]")
			@DefaultValue("600") @QueryParam("imageWidth") int imageWidth,

			@Parameter(description = "Image height (min 50 pixels, max 5000 pixels)") // TODO, allowableValues="range[50,5000]")
			@DefaultValue("600") @QueryParam("imageHeight") int imageHeight,

			@Parameter(description = "Confidence of prediction (writes prediction interval in figure)", required=false)
			@QueryParam("confidence") Double confidence,

			@Parameter(description = "Add title to the image (using the model name)")
			@DefaultValue("false") @QueryParam("addTitle") boolean addTitle,

			@Context SecurityContext securityContext ) {
		logger.debug("Initial image-size at API-level: imageHeight="+imageHeight+", imageWidth="+imageWidth);

		return Predict.doPredictImage(molecule, imageWidth, imageHeight, confidence, addTitle); //delegate.predictImageGet(molecule, imageWidth, imageHeight, confidence, addTitle, securityContext);
	}

	@Path("/health")
	@GET
	@HEAD
	@Produces({ MediaType.APPLICATION_JSON })
	@Operation(summary="Get the status of the prediction service", 
	responses = {
			@ApiResponse(responseCode="200", description="Service is running"),
			@ApiResponse(responseCode="500", description="Service down")
	})
	public Response health() {
		return Predict.checkHealth();
	}
}
