package com.arosbio.api;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
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
import com.arosbio.api.model.ModelInfo;
import com.arosbio.api.model.RegressionResult;
import com.arosbio.impl.Predict;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@Path("/v2")
public class PredictApi  {
	private static final Logger logger = org.slf4j.LoggerFactory.getLogger(PredictApi.class);

	@Path("/modelInfo")
	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	@Operation(summary="Get information about this model", 
	responses = {
			@ApiResponse(responseCode="200", description="Model info", content = @Content(
					schema = @Schema(implementation=ModelInfo.class))),
			@ApiResponse(responseCode = "503", description = "Service not available", content = @Content(
					schema = @Schema(implementation = ErrorResponse.class)))
	})
	public Response modelInfo() {
		try {
			return Predict.getModelInfo();
		} catch (Exception e) {
			return convertToErrorResponse(e);
		}
	}

	@Path("/predict")
	@GET
	@Consumes({ MediaType.MULTIPART_FORM_DATA })
	@Produces({ MediaType.APPLICATION_JSON })
	@Operation(
			summary = "Make a prediction on a given molecule", 
			tags={"Predict"},
			description = "Predict a given molecule in SMILES or MDL v2000/v3000 format. In case a MDL is sent, it must be properly URL-encoded in UTF-8. You can use for instance https://www.urlencoder.org/ to encode your file.", 
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

			@Parameter(description = "Compound structure notation using SMILES or MDL format", required=true, example="CCCCC=O")
			@QueryParam("molecule") 
			String molecule,

			@Parameter(description = "The desired confidence of the prediction", required=false, example="0.8", 
			schema = @Schema(maximum="1.0", minimum="0.0"))
			@QueryParam("confidence") 
			Double confidence,

			@Context SecurityContext securityContext) {
		try {
			return Predict.doPredict(molecule, confidence);
		} catch (Exception e) {
			return convertToErrorResponse(e);
		}
	}
	
	@Path("/predict")
	@POST
	@Consumes({ MediaType.MULTIPART_FORM_DATA, MediaType.APPLICATION_JSON })
	@Produces({ MediaType.APPLICATION_JSON })
	@Operation(
			summary = "Make a prediction on a given molecule", 
			tags={"Predict"},
			description = "Predict a given molecule in SMILES or MDL v2000/v3000 format. In case a MDL is sent, it must be properly URL-encoded in UTF-8. You can use for instance https://www.urlencoder.org/ to encode your file.", 
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
	public Response predictPost(

			@Parameter(description = "Compound structure notation using SMILES or MDL format", required=true, example="CCCCC=O")
			@FormParam("molecule") 
			String molecule,

			@Parameter(description = "The desired confidence of the prediction", required=false, example="0.8", 
			schema = @Schema(maximum="1.0", minimum="0.0"))
			@FormParam("confidence") 
			Double confidence,

			@Context SecurityContext securityContext) {
		try {
			return Predict.doPredict(molecule, confidence);
		} catch (Exception e) {
			return convertToErrorResponse(e);
		}
	}


	@Path("/predictImage")
	@GET
	@Consumes({ MediaType.MULTIPART_FORM_DATA })
	@Produces({ "image/png", MediaType.APPLICATION_JSON }) 
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
							schema = @Schema(implementation = ErrorResponse.class))) 
			}
			)
	public Response predictImageGet( 

			@Parameter(description = "Compound structure notation using SMILES or MDL format", 
			required=false, 
			example="CCCCC=O")
			@QueryParam("molecule") String molecule,

			@Parameter(description = "The desired confidence of the prediction", required=false, example="0.8", 
				schema = @Schema(maximum="1.0", minimum="0.0"))
			@QueryParam("confidence") Double confidence,

			@Parameter(description = "Image width in pixels",
				schema = @Schema(maximum=""+Predict.MAX_IMAGE_SIZE, minimum=""+Predict.MIN_IMAGE_SIZE))
			@DefaultValue("600") @QueryParam("imageWidth") 
			int imageWidth,

			@Parameter(description = "Image height in pixels",
				schema = @Schema(maximum=""+Predict.MAX_IMAGE_SIZE, minimum=""+Predict.MIN_IMAGE_SIZE)) 
			@DefaultValue("600") @QueryParam("imageHeight") 
			int imageHeight,

			@Parameter(description = "Add title to the image (using the model name)")
			@DefaultValue("false") 
			@QueryParam("addTitle") 
			boolean addTitle,

			@Context SecurityContext securityContext ) {
		logger.debug("Initial image-size at API-level: imageHeight="+imageHeight+", imageWidth="+imageWidth);
		try {
			return Predict.doPredictImage(molecule, imageWidth, imageHeight, confidence, addTitle); 
		} catch (Exception e) {
			return convertToErrorResponse(e);
		}
	}
	
	@Path("/predictImage")
	@POST
	@Consumes({ MediaType.MULTIPART_FORM_DATA })
	@Produces({ "image/png", MediaType.APPLICATION_JSON }) 
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
							schema = @Schema(implementation = ErrorResponse.class))) 
			}
			)
	public Response predictImagePost( 

			@Parameter(description = "Compound structure notation using SMILES or MDL format", 
			required=false, 
			example="CCCCC=O")
			@FormParam("molecule") String molecule,

			@Parameter(description = "The desired confidence of the prediction", required=false, example="0.8", 
				schema = @Schema(maximum="1.0", minimum="0.0"))
			@FormParam("confidence") Double confidence,

			@Parameter(description = "Image width in pixels",
				schema = @Schema(maximum=""+Predict.MAX_IMAGE_SIZE, minimum=""+Predict.MIN_IMAGE_SIZE))
			@DefaultValue("600") 
			@FormParam("imageWidth") 
			int imageWidth,

			@Parameter(description = "Image height in pixels",
				schema = @Schema(maximum=""+Predict.MAX_IMAGE_SIZE, minimum=""+Predict.MIN_IMAGE_SIZE)) 
			@DefaultValue("600") 
			@FormParam("imageHeight") 
			int imageHeight,

			@Parameter(description = "Add title to the image (using the model name)")
			@DefaultValue("false") 
			@FormParam("addTitle") 
			boolean addTitle,

			@Context SecurityContext securityContext ) {
		logger.debug("Initial image-size at API-level: imageHeight="+imageHeight+", imageWidth="+imageWidth);
		try {
			return Predict.doPredictImage(molecule, imageWidth, imageHeight, confidence, addTitle); 
		} catch (Exception e) {
			return convertToErrorResponse(e);
		}
	}

	@Path("/health")
	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	@Operation(summary="Get the status of the prediction service", 
	responses = {
			@ApiResponse(responseCode="200", description="Service is running"),
			@ApiResponse(responseCode="503", description="Service down",content = @Content(
					schema = @Schema(implementation = ErrorResponse.class))),
	})
	public Response health() {
		try {
			return Predict.checkHealth();
		} catch (Exception e) {
			return convertToErrorResponse(e);
		}
	}

	private Response convertToErrorResponse(Exception e) {
		int code = javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
		return Response.status(code).entity(new ErrorResponse(code, "Service failure: " + e.getMessage() + ", please contact the service provider if the error was not due to user-error")).build();
	}
}
