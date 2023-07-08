package com.arosbio.impl;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static jakarta.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.security.InvalidKeyException;
import java.util.Arrays;

import javax.security.auth.Destroyable;

import org.apache.commons.lang3.tuple.Triple;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.slf4j.Logger;

import com.arosbio.api.model.BadRequestError;
import com.arosbio.api.model.ErrorResponse;
import com.arosbio.api.model.ModelInfo;
import com.arosbio.api.model.RegressionResult;
import com.arosbio.api.model.ServiceRunning;
import com.arosbio.chem.io.out.image.AtomContributionRenderer;
import com.arosbio.chem.io.out.image.RendererTemplate.RenderInfo;
import com.arosbio.chem.io.out.image.fields.ColorGradientField;
import com.arosbio.chem.io.out.image.fields.PredictionIntervalField;
import com.arosbio.chem.io.out.image.fields.TextField;
import com.arosbio.chem.io.out.image.layout.Position.Vertical;
import com.arosbio.cheminf.ChemCPRegressor;
import com.arosbio.cheminf.SignificantSignature;
import com.arosbio.cheminf.io.ModelSerializer;
import com.arosbio.color.gradient.ColorGradient;
import com.arosbio.color.gradient.GradientFactory;
import com.arosbio.encryption.EncryptionSpecification;
import com.arosbio.io.UriUtils;
import com.arosbio.ml.cp.CPRegressionPrediction;
import com.arosbio.services.utils.CDKMutexLock;
import com.arosbio.services.utils.ChemUtils;
import com.arosbio.services.utils.Utils;
import com.google.common.collect.Range;

import jakarta.ws.rs.core.Response;

public class Predict {

	public static final String DEFAULT_MODEL_PATH = Utils.DEFAULT_MODEL_PATH;
	public static final String MODEL_FILE_ENV_VARIABLE = Utils.MODEL_FILE_ENV_VARIABLE;

	private static Logger logger = org.slf4j.LoggerFactory.getLogger(Predict.class);
	private static ErrorResponse serverErrorResponse = null;
	private static ChemCPRegressor model;

	static {
		init();
	}

	public static synchronized void init(){

		// Reset the server state
		serverErrorResponse = null;
		model = null;

		final String model_file = Utils.getModelURL();
		final EncryptionSpecification specificationOrNull = Utils.getEncryptionKeyOrNull();

		// Get the root logger for cpsign 
		Logger cpsingLogger =  org.slf4j.LoggerFactory.getLogger("com.arosbio");
		if (cpsingLogger instanceof ch.qos.logback.classic.Logger) {
			ch.qos.logback.classic.Logger cpsignRoot = (ch.qos.logback.classic.Logger) cpsingLogger;
			// Disable all cpsign-output
			cpsignRoot.setLevel(ch.qos.logback.classic.Level.OFF);
		}

		// Enable debug output for this library
		Logger predictServerLogger = org.slf4j.LoggerFactory.getLogger("com.arosbio.impl");
		if (predictServerLogger instanceof ch.qos.logback.classic.Logger) {
			ch.qos.logback.classic.Logger cpLogDLogger = (ch.qos.logback.classic.Logger) predictServerLogger;
			cpLogDLogger.setLevel(ch.qos.logback.classic.Level.DEBUG);
		}

		// load the model - only if no error previously encountered
		if (serverErrorResponse == null) {
			URI modelURI = null;
			try {
				logger.debug("Trying to load in the model");
				modelURI = UriUtils.getURI(model_file);
				if (modelURI == null)
					throw new IOException("did not locate the model file");
				if (!UriUtils.canReadFromURI(modelURI)) {
					throw new IllegalArgumentException("Cannot read from URI: " + modelURI);
				}
			} catch (Exception e) {
				logger.error("No model could be loaded", e);
				serverErrorResponse = new ErrorResponse(SERVICE_UNAVAILABLE, "No model found at server init - service needs to be re-deployed with a valid model config");
			}

			if (serverErrorResponse == null) {
				try {
					model = (ChemCPRegressor) ModelSerializer.loadChemPredictor(modelURI, specificationOrNull);
					model.getDataset().setMinHAC(0); // to allow generating images for small molecules
					logger.info("Loaded model");
				} catch (IOException | InvalidKeyException | IllegalArgumentException e) {
					logger.error("Could not load the model", e);
					serverErrorResponse = new ErrorResponse(SERVICE_UNAVAILABLE, "Model could not be loaded at server init ("+e.getMessage()+") - service needs to be re-deployed");
				}
			}
		}
		
		// Clean up potential encryption key
		if (specificationOrNull != null && (specificationOrNull instanceof Destroyable)){
			try{
				((Destroyable)specificationOrNull).destroy();
			} catch (Exception e){
				logger.debug("Failed destroying encryption spec");
			}
		}
	}

	public static Response doPredict(String molecule, Double confidence) {
		logger.debug("got a prediction task, conf={}", confidence);

		if (serverErrorResponse != null)
			return Utils.getResponse(serverErrorResponse);
		
		if (confidence == null || confidence <0 || confidence>1) {
			logger.debug("Invalid confidence={}", confidence);
			return Utils.getResponse(new BadRequestError(BAD_REQUEST, "Invalid confidence '"+confidence+"'", Arrays.asList("confidence")) );
		}
		
		Triple<IAtomContainer,String,Response> molValidation = ChemUtils.validateMoleculeInput(molecule, true);
		if (molValidation.getRight() != null) {
			return molValidation.getRight();
		}
		IAtomContainer molToPredict = molValidation.getLeft();
		String smiles = molValidation.getMiddle();

		// Make prediction
		CDKMutexLock.requireLock();
		try {
			CPRegressionPrediction res = null;
			if (confidence != null) {
				res = model.predict(molToPredict, confidence);
			}
			logger.debug("Successfully finished predicting smiles={}, interval={}", smiles, res);
			return Response.ok( new RegressionResult(smiles,res,confidence, model.getModelInfo().getName()) ).build();
		} catch (Exception | Error e) {
			logger.warn("Failed predicting smiles={}:\n\t{}",smiles, Utils.getStackTrace(e));
			return Utils.getResponse( new ErrorResponse(INTERNAL_SERVER_ERROR, "Server error - error during prediction") );
		} finally{
			CDKMutexLock.releaseLock();
		}
	}
	
	public static Response doPredictImage(String molecule, int imageWidth, int imageHeight, Double confidence, boolean addTitleField, boolean addPredictionField, boolean addLegendField) {
		logger.debug("Got predictImage request: imageWidth={}, imageHeight={}, conf={}",imageWidth,imageHeight,confidence);
		if (serverErrorResponse != null)
			return Utils.getResponse(serverErrorResponse);

		if (confidence != null && (confidence < 0 || confidence > 1)){
			logger.warn("invalid argument confidence={}", confidence);
			return Utils.getResponse(new BadRequestError(BAD_REQUEST, "invalid argument", Arrays.asList("confidence")));
		}
		
		Response r = Utils.validateImageSize(imageWidth, imageHeight);
		if (r != null) {
			return r;
		}
		
		Triple<IAtomContainer,String,Response> molValidation = ChemUtils.validateMoleculeInput(molecule, false);
		if (molValidation.getRight() != null) {
			return molValidation.getRight();
		}
		IAtomContainer molToPredict = molValidation.getLeft();
		String smiles = molValidation.getMiddle();

		// Return empty img if no mol sent
		if (molToPredict == null){
			// return an empty img
			return Utils.getEmptyImageResponse(imageWidth, imageHeight);
		}
		
		// Make prediction 
		SignificantSignature signSign = null;
		CPRegressionPrediction pred = null; 
		CDKMutexLock.requireLock();
		try {
			signSign = model.predictSignificantSignature(molToPredict);
			if (addPredictionField && confidence!=null && imageWidth>80) {
				pred = model.predict(molToPredict, confidence);
			}
		} catch (Exception | Error e) {
			logger.warn("Failed predicting smiles={}, error:\n{}",smiles, Utils.getStackTrace(e));
			return Utils.getResponse(new ErrorResponse(INTERNAL_SERVER_ERROR, "Error during prediction: " + e.getMessage()) );
		} finally {
			CDKMutexLock.releaseLock();
		}
			
		// Create the depiction
		try {
			RenderInfo.Builder info = new RenderInfo.Builder(molToPredict, signSign);

			ColorGradient gradient = GradientFactory.getDefaultBloomGradient();
			AtomContributionRenderer.Builder builder = new AtomContributionRenderer.Builder()
				.colorScheme(gradient) // Decide which gradient or color scheme to use
				.height(imageHeight)
				.width(imageWidth);

			// Add title if specified
			if (addTitleField) {
				builder.addFieldOverMol(
					new TextField.Immutable.Builder(model.getModelInfo().getName()).alignment(Vertical.CENTERED).build()
					);
			}
			// add confidence interval only if given confidence and image size is big enough
			if (addPredictionField && pred != null){
				try {
					Range<Double> predInterval = pred.getInterval(confidence).getInterval();
					if (predInterval != null){
						builder.addFieldUnderMol(new PredictionIntervalField.Builder(confidence).build());
						info.predictionInterval(predInterval, confidence);
					}
						
				} catch (Exception e) {
					logger.error("failed adding prediction-interval to image. the prediction = {}\n{}", pred, Utils.getStackTrace(e));
				}
			}
			
			// Add the gradient
			if (addLegendField){
				builder.addFieldUnderMol(new ColorGradientField.Builder(gradient).build());
			}
			
			BufferedImage image = builder.build()
				.render(info.build())
				.getImage();

			return Response.ok( new ByteArrayInputStream(Utils.convertToByteArray(image)) ).build();
		} catch (Exception | Error e) {
			logger.warn("Failed creating depiction for SMILES={}, error:\n{}",smiles, Utils.getStackTrace(e));
			return Utils.getResponse(new ErrorResponse(INTERNAL_SERVER_ERROR, "Error during image generation: " + e.getMessage()) );
		} 
	}
	
	public static Response getModelInfo() {
		if (serverErrorResponse != null) {
			return Utils.getResponse(serverErrorResponse);
		}
		
		return Response.ok(new ModelInfo(model.getModelInfo())).build();
	}
	
	public static Response checkHealth() {
		if( serverErrorResponse != null) {
			return Utils.getResponse(new ErrorResponse(SERVICE_UNAVAILABLE, serverErrorResponse.getMessage()));
		} else {
			return Response.ok(new ServiceRunning()).build();
		}
	}
}
