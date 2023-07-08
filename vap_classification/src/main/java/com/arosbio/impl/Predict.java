package com.arosbio.impl;

import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static jakarta.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.security.auth.Destroyable;

import org.apache.commons.lang3.tuple.Triple;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.slf4j.Logger;

import com.arosbio.api.model.ErrorResponse;
import com.arosbio.api.model.ModelInfo;
import com.arosbio.api.model.PredictionResult;
import com.arosbio.api.model.ProbabilityMapping;
import com.arosbio.api.model.ServiceRunning;
import com.arosbio.chem.io.out.image.AtomContributionRenderer;
import com.arosbio.chem.io.out.image.RendererTemplate.RenderInfo;
import com.arosbio.chem.io.out.image.fields.ColorGradientField;
import com.arosbio.chem.io.out.image.fields.ProbabilityField;
import com.arosbio.chem.io.out.image.fields.TextField;
import com.arosbio.chem.io.out.image.layout.Position.Vertical;
import com.arosbio.cheminf.ChemVAPClassifier;
import com.arosbio.cheminf.SignificantSignature;
import com.arosbio.cheminf.io.ModelSerializer;
import com.arosbio.color.gradient.ColorGradient;
import com.arosbio.color.gradient.GradientFactory;
import com.arosbio.encryption.EncryptionSpecification;
import com.arosbio.io.UriUtils;
import com.arosbio.ml.vap.avap.CVAPPrediction;
import com.arosbio.services.utils.CDKMutexLock;
import com.arosbio.services.utils.ChemUtils;
import com.arosbio.services.utils.Utils;

import jakarta.ws.rs.core.Response;

public class Predict {

	public static final String DEFAULT_MODEL_PATH = Utils.DEFAULT_MODEL_PATH;
	public static final String MODEL_FILE_ENV_VARIABLE = Utils.MODEL_FILE_ENV_VARIABLE;

	private static Logger logger = org.slf4j.LoggerFactory.getLogger(Predict.class);
	private static ErrorResponse serverErrorResponse = null;
	private static ChemVAPClassifier model;

	static {
		init();
	}

	public static void init() {
		
		// Reset server state
		serverErrorResponse = null;
		model = null;

		final String model_file = Utils.getModelURL();
		EncryptionSpecification specificationOrNull = null; 
		try{
			specificationOrNull = Utils.getEncryptionKeyOrNull();
		} catch (IllegalArgumentException e){
			logger.debug("tried to deploy encryted model but could not load encryption key correctly");
			serverErrorResponse = new ErrorResponse(SERVICE_UNAVAILABLE, "No model could be loaded at server init - failed loading encryption key");
		}

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
					model = (ChemVAPClassifier) ModelSerializer.loadChemPredictor(modelURI, specificationOrNull);
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

			CVAPPrediction<String> res = model.predict(molToPredict);

			logger.debug("Successfully finished predicting smiles={}, probabilities={}",smiles, res );

			List<ProbabilityMapping> pvalues = new ArrayList<>();
			for (Entry<String, Double> entry : res.getProbabilities().entrySet()) {
				pvalues.add(new ProbabilityMapping(entry.getKey(), entry.getValue()));
			}

			return Response.ok( new PredictionResult(pvalues, smiles, model.getModelInfo().getName())).build();
		} catch (Exception e) {
			logger.warn("Failed predicting smiles={}\n{}", smiles, Utils.getStackTrace(e));
			return Utils.getResponse( new ErrorResponse(INTERNAL_SERVER_ERROR, "Server error - error during prediction") );
		} finally {
			CDKMutexLock.releaseLock();
		}
	}

	public static Response doPredictImage(String molecule, int imageWidth, int imageHeight, boolean addPredictionField, boolean addTitleField, boolean addLegendField) {
		logger.debug("got a predict-image task, imageWidth={}, imageHeight={}",+imageWidth,imageHeight);

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
		
		// Make prediction + image
		SignificantSignature signSign = null;
		Map<String,Double> probs = null;
		try {
			CDKMutexLock.requireLock(); // require the lock again!
			signSign = model.predictSignificantSignature(molToPredict);
			if (addPredictionField && imageWidth>80) {
				probs = model.predictProbabilities(molToPredict);
			}
		} catch (Exception | Error e) {
			logger.warn("Failed predicting smiles={}, error:\n{}", smiles, Utils.getStackTrace(e));
			return Utils.getResponse(new ErrorResponse(INTERNAL_SERVER_ERROR, "Error during prediction: " + e.getMessage()) );
		} finally {
			CDKMutexLock.releaseLock();
		}

		// Create the depiction
		try {
			// Create the depiction
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
			// Add probabilities if specified
			if (addPredictionField && probs !=null){
				builder.addFieldUnderMol(new ProbabilityField.Builder(model.getLabelsSet()).build());
			}
			if (addLegendField){
				builder.addFieldUnderMol(new ColorGradientField.Builder(gradient).build());
			}

			BufferedImage image = builder.build()
				.render(new RenderInfo.Builder(molToPredict, signSign).probabilities(probs).build())
				.getImage();

			return Response.ok( new ByteArrayInputStream(Utils.convertToByteArray(image)) ).build();

		} catch (Exception | Error e) {
			logger.warn("Failed creating depiction for SMILES={}, error:\n{}", smiles, Utils.getStackTrace(e));
			return Utils.getResponse(new ErrorResponse(INTERNAL_SERVER_ERROR, "Error during image generation: " + e.getMessage()) );
		} 
	}

}
