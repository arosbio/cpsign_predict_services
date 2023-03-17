package com.arosbio.impl;

import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static jakarta.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.imageio.ImageIO;

import org.apache.commons.lang3.tuple.Triple;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.slf4j.Logger;

import com.arosbio.api.model.ClassificationResult;
import com.arosbio.api.model.ErrorResponse;
import com.arosbio.api.model.ModelInfo;
import com.arosbio.api.model.PValueMapping;
import com.arosbio.api.model.ServiceRunning;
import com.arosbio.chem.io.out.image.AtomContributionRenderer;
import com.arosbio.chem.io.out.image.RendererTemplate.RenderInfo;
import com.arosbio.chem.io.out.image.fields.ColorGradientField;
import com.arosbio.chem.io.out.image.fields.PValuesField;
import com.arosbio.chem.io.out.image.fields.TextField;
import com.arosbio.chem.io.out.image.layout.Position.Vertical;
import com.arosbio.cheminf.ChemCPClassifier;
import com.arosbio.cheminf.SignificantSignature;
import com.arosbio.cheminf.io.ModelSerializer;
import com.arosbio.color.gradient.ColorGradient;
import com.arosbio.color.gradient.GradientFactory;
import com.arosbio.io.UriUtils;
import com.arosbio.services.utils.CDKMutexLock;
import com.arosbio.services.utils.ChemUtils;
import com.arosbio.services.utils.Utils;

import jakarta.ws.rs.core.Response;


public class Predict {

	public static final String DEFAULT_MODEL_PATH = Utils.DEFAULT_MODEL_PATH;
	public static final String MODEL_FILE_ENV_VARIABLE = "MODEL_FILE";

	private static Logger logger = org.slf4j.LoggerFactory.getLogger(Predict.class);
	private static ErrorResponse serverErrorResponse = null;
	private static ChemCPClassifier model;

	static {
		init();
	}
		
	public static synchronized void init() {
		
		serverErrorResponse = null;
		model = null;

		final String model_file = System.getenv(MODEL_FILE_ENV_VARIABLE)!=null ? System.getenv(MODEL_FILE_ENV_VARIABLE) : DEFAULT_MODEL_PATH;

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
				modelURI = new File(model_file).toURI();
				if (modelURI == null)
					throw new IOException("did not locate the model file");
				if (!UriUtils.canReadFromURI(modelURI)) {
					throw new IllegalArgumentException("Cannot read from URI: " + modelURI);
				}
			} catch (Exception e) {
				logger.error("No model could be loaded: " +e.getMessage());
				serverErrorResponse = new ErrorResponse(SERVICE_UNAVAILABLE, "No model found at server init - service needs to be re-deployed with a valid model config");
			}

			if (serverErrorResponse == null) {
				// TODO - allow to set encryption key
				try {
					
					model = (ChemCPClassifier) ModelSerializer.loadChemPredictor(modelURI, null);
					logger.info("Loaded model");
				} catch (IOException | InvalidKeyException | IllegalArgumentException e) {
					logger.error("Could not load the model: {}",  e.getMessage());
					serverErrorResponse = new ErrorResponse(SERVICE_UNAVAILABLE, "Model could not be loaded at server init ("+e.getMessage()+") - service needs to be re-deployed");
				}
			}
		}
	}

	public static Response checkHealth() {
		if( serverErrorResponse != null) {
			return Utils.getResponse(new ErrorResponse(SERVICE_UNAVAILABLE, serverErrorResponse.getMessage()));
		} else {
			return Response.ok(new ServiceRunning()).build();
		}
	}


	public static Response getModelInfo() {
		if (serverErrorResponse != null) {
			return Utils.getResponse(serverErrorResponse);
		}

		return Response.ok(new ModelInfo(model.getModelInfo())).build();
	}

	public static Response doPredict(String molecule) {
		if (serverErrorResponse != null)
			return Utils.getResponse(serverErrorResponse);
		
		Triple<IAtomContainer,String,Response> molValidation = ChemUtils.validateMoleculeInput(molecule, true);
		if (molValidation.getRight() != null) {
			return molValidation.getRight();
		}
		IAtomContainer molToPredict = molValidation.getLeft();
		String smiles = molValidation.getMiddle();

		CDKMutexLock.requireLock();
		try {
			Map<String, Double> res = model.predict(molToPredict);

			logger.debug("Successfully finished predicting smiles={}, pvalues={}",smiles, res );
			List<PValueMapping> pvalues = new ArrayList<>();
			for (Entry<String, Double> entry : res.entrySet()) {
				pvalues.add(new PValueMapping(entry.getKey(), entry.getValue()));
			}

			return Response.ok( new ClassificationResult(pvalues, smiles, model.getModelInfo().getName()) ).build();
		} catch (Exception e) {
			logger.warn("Failed predicting smiles={}:\n\t{}", smiles, Utils.getStackTrace(e));
			return Utils.getResponse( new ErrorResponse(INTERNAL_SERVER_ERROR, "Server error - error during prediction") );
		} finally {
			CDKMutexLock.releaseLock();
		}
	}

	public static Response doPredictImage(String molecule, int imageWidth, int imageHeight, boolean addPredictionField, boolean addTitleField, boolean addLegendField) {
		logger.debug("got a predict-image task, imageWidth={}, imageHeight={}",imageWidth,imageHeight);

		if (serverErrorResponse != null)
			return Utils.getResponse(serverErrorResponse);
		
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
		Map<String,Double> pVals = null;
		CDKMutexLock.requireLock();
		try {
			signSign = model.predictSignificantSignature(molToPredict);
			if (addPredictionField && imageWidth>80) {
				pVals = model.predict(molToPredict);
			}
		} catch (Exception | Error e) {
			logger.warn("Failed predicting smiles={}, error:\n{}",smiles, Utils.getStackTrace(e));
			return Utils.getResponse(new ErrorResponse(INTERNAL_SERVER_ERROR, "Error during prediction: " + e.getMessage()) );
		} finally {
			CDKMutexLock.releaseLock();
		}

		// Create the depiction
		try {
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
			// Add p-values if specified
			if (addPredictionField && pVals !=null){
				builder.addFieldUnderMol(new PValuesField.Builder(model.getLabelsSet()).build());
			}

			// Add the gradient
			if (addLegendField){
				builder.addFieldUnderMol(new ColorGradientField.Builder(gradient).build());
			}
			
			BufferedImage image = builder.build().render(new RenderInfo.Builder(molToPredict, signSign)
				.pValues(pVals).build())
				.getImage();

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(image, "png", baos);
			byte[] imageData = baos.toByteArray();

			return Response.ok( new ByteArrayInputStream(imageData) ).build();
		} catch (Exception | Error e) {
			logger.warn("Failed creating depiction for SMILES={}, error:\n{}",smiles, Utils.getStackTrace(e));
			return Utils.getResponse(new ErrorResponse(INTERNAL_SERVER_ERROR, "Error during image generation: " + e.getMessage()) );
		}
	}

}
