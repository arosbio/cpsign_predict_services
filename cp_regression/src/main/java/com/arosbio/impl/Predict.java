package com.arosbio.impl;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.security.InvalidKeyException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.ws.rs.core.Response;

import org.javatuples.Triplet;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.slf4j.Logger;

import com.arosbio.api.model.BadRequestError;
import com.arosbio.api.model.ErrorResponse;
import com.arosbio.api.model.ModelInfo;
import com.arosbio.api.model.RegressionResult;
import com.arosbio.api.model.ServiceRunning;
import com.arosbio.auth.InvalidLicenseException;
import com.arosbio.chem.io.out.GradientFigureBuilder;
import com.arosbio.chem.io.out.depictors.MoleculeGradientDepictor;
import com.arosbio.chem.io.out.fields.ColorGradientField;
import com.arosbio.chem.io.out.fields.PredictionIntervalField;
import com.arosbio.chem.io.out.fields.TitleField;
import com.arosbio.commons.auth.PermissionsCheck;
import com.arosbio.depict.GradientFactory;
import com.arosbio.io.UriUtils;
import com.arosbio.modeling.CPSignFactory;
import com.arosbio.modeling.cheminf.SignaturesCPRegression;
import com.arosbio.modeling.cheminf.SignificantSignature;
import com.arosbio.modeling.io.ModelLoader;
import com.arosbio.modeling.ml.cp.CPRegressionPrediction;
import com.arosbio.services.utils.CDKMutexLock;
import com.arosbio.services.utils.ChemUtils;
import com.arosbio.services.utils.Utils;
import com.google.common.collect.Range;

public class Predict {

	public static final String DEFAULT_LICENSE_PATH = "/opt/app-root/modeldata/license.license";
	public static final String DEFAULT_MODEL_PATH = "/opt/app-root/modeldata/model.jar";
	public static final String MODEL_FILE_ENV_VARIABLE = "MODEL_FILE";
	public static final String LICENSE_FILE_ENV_VARIABLE = "LICENSE_FILE";

	private static Logger logger = org.slf4j.LoggerFactory.getLogger(Predict.class);
	private static ErrorResponse serverErrorResponse = null;
	private static SignaturesCPRegression model;
	private static CPSignFactory factory;

	static {
		final String license_file = System.getenv(LICENSE_FILE_ENV_VARIABLE)!=null?System.getenv(LICENSE_FILE_ENV_VARIABLE):DEFAULT_LICENSE_PATH;
		final String model_file = System.getenv(MODEL_FILE_ENV_VARIABLE)!=null?System.getenv(MODEL_FILE_ENV_VARIABLE):DEFAULT_MODEL_PATH;

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

		// Instantiate the factory 
		try {
			logger.info("Attempting to load license from: " + license_file);
			URI license_uri = new File(license_file).toURI();
			factory = new CPSignFactory( license_uri );
			logger.info("Initiated the CPSignFactory");
		} catch (InvalidLicenseException e) {
			logger.error("Got exception when trying to instantiate CPSignFactory: " + e.getMessage());
			serverErrorResponse = new ErrorResponse(SERVICE_UNAVAILABLE, "Invalid license at server init - service needs to be re-deployed with a valid license");
		} catch (RuntimeException | IOException re){
			logger.error("Got exception when trying to instantiate CPSignFactory: " + re.getMessage());
			serverErrorResponse = new ErrorResponse(SERVICE_UNAVAILABLE, "No license found at server init - service needs to be re-deployed with a valid license");
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
				logger.error("No model could be loaded", e);
				serverErrorResponse = new ErrorResponse(SERVICE_UNAVAILABLE, "No model found at server init - service needs to be re-depolyed with a valid model config");
			}

			if (serverErrorResponse == null) {

				try {
					if ( factory.supportEncryption() ) {
						model = (SignaturesCPRegression) ModelLoader.loadModel(modelURI, factory.getEncryptionSpec());
					}
					else {
						model = (SignaturesCPRegression) ModelLoader.loadModel(modelURI, null);
					}
					logger.info("Loaded model");
				} catch (IllegalAccessException | IOException | InvalidKeyException | IllegalArgumentException e) {
					logger.error("Could not load the model", e);
					serverErrorResponse = new ErrorResponse(SERVICE_UNAVAILABLE, "Model could not be loaded at server init ("+e.getMessage()+") - service needs to be re-deployed");
				}
			}
		}
	}

	public static Response doPredict(String molecule, Double confidence) {
		logger.debug("got a prediction task, conf=" + confidence);

		if (serverErrorResponse != null)
			return Utils.getResponse(serverErrorResponse);
		
		if (confidence == null || confidence <0 || confidence>1) {
			logger.debug("Invalid confidence=" + confidence);
			return Utils.getResponse(new BadRequestError(BAD_REQUEST, "Invalid confidence '"+confidence+"'", Arrays.asList("confidence")) );
		}
		
		Triplet<IAtomContainer,String,Response> molValidation = ChemUtils.validateMoleculeInput(molecule, true);
		if (molValidation.getValue2() != null) {
			return molValidation.getValue2();
		}
		IAtomContainer molToPredict = molValidation.getValue0();
		String smiles = molValidation.getValue1();

		// Make prediction
		CDKMutexLock.requireLock();
		try {
			CPRegressionPrediction res = null;
			if (confidence != null) {
				res = model.predict(molToPredict, confidence);
			}
			logger.debug("Successfully finished predicting smiles="+smiles+", interval=" + res );
			return Response.ok( new RegressionResult(smiles,res,confidence, model.getModelInfo().getModelName()) ).build();
		} catch (Exception | Error e) {
			logger.warn("Failed predicting smiles=" + smiles +":\n\t" + Utils.getStackTrace(e));
			return Utils.getResponse( new ErrorResponse(INTERNAL_SERVER_ERROR, "Server error - error during prediction") );
		} finally{
			CDKMutexLock.releaseLock();
		}
	}
	
	public static Response doPredictImage(String molecule, int imageWidth, int imageHeight, Double confidence, boolean addTitle) {
		logger.debug("Got predictImage request: imageWidth="+imageWidth + ", imageHeight="+imageHeight+", conf="+confidence);
		if (serverErrorResponse != null)
			return Utils.getResponse(serverErrorResponse);

		if (confidence != null && (confidence < 0 || confidence > 1)){
			logger.warn("invalid argument confidence=" + confidence);
			return Utils.getResponse(new BadRequestError(BAD_REQUEST, "invalid argument", Arrays.asList("confidence")));
		}
		
		Response r = Utils.validateImageSize(imageWidth, imageHeight);
		if (r != null) {
			return r;
		}
		
		Triplet<IAtomContainer,String,Response> molValidation = ChemUtils.validateMoleculeInput(molecule, false);
		if (molValidation.getValue2() != null) {
			return molValidation.getValue2();
		}
		IAtomContainer molToPredict = molValidation.getValue0();
		String smiles = molValidation.getValue1();

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
			if (confidence!=null && imageWidth>80) {
				pred = model.predict(molToPredict, confidence);
			}
		} catch (Exception | Error e) {
			logger.warn("Failed predicting smiles=" + smiles + ", error:\n"+ Utils.getStackTrace(e));
			return Utils.getResponse(new ErrorResponse(INTERNAL_SERVER_ERROR, "Error during prediction: " + e.getMessage()) );
		} finally {
			CDKMutexLock.releaseLock();
		}
			
		// Create the depiction
		try {
			
			MoleculeGradientDepictor depictor = new MoleculeGradientDepictor(GradientFactory.getDefaultBloomGradient());
			depictor.setForceRecalculate2DCoords(false);
			depictor.setImageHeight(imageHeight);
			depictor.setImageWidth(imageWidth);
			GradientFigureBuilder builder = new GradientFigureBuilder(depictor);

			// Add title if specified
			if (addTitle) {
				builder.addFieldOverImg(new TitleField(model.getModelInfo().getModelName()));
			}
			// add confidence interval only if given confidence and image size is big enough
			if (pred != null){
				try {
					Range<Double> predInterval = pred.getInterval(confidence).getInterval();
					if (predInterval != null)
						builder.addFieldUnderImg(new PredictionIntervalField(predInterval, confidence));
				} catch (Exception e) {
					logger.error("failed adding prediction-interval to image. the prediction = " + pred, e);
				}
			}
			builder.addFieldUnderImg(new ColorGradientField(depictor.getColorGradient()));
			
			Map<Integer,Double> grad = signSign.getMoleculeGradient();
			BufferedImage image = builder.build(molToPredict, (grad!=null? grad : new HashMap<>())).getImage();

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(image, "png", baos);
			byte[] imageData = baos.toByteArray();

			return Response.ok( new ByteArrayInputStream(imageData) ).build();
		} catch (Exception | Error e) {
			logger.warn("Failed creating depiction for SMILES=" + smiles + ", error:\n"+ Utils.getStackTrace(e));
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
		} else if (! PermissionsCheck.check()) {
			return Utils.getResponse(new ErrorResponse(SERVICE_UNAVAILABLE, "License has expired" ));
		} else {
			return Response.ok(new ServiceRunning()).build();
		}
	}
}
