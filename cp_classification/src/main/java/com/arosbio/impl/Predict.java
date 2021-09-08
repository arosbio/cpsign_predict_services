package com.arosbio.impl;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.imageio.ImageIO;
import javax.ws.rs.core.Response;

import org.openscience.cdk.interfaces.IAtomContainer;
import org.slf4j.Logger;

import com.arosbio.api.model.BadRequestError;
import com.arosbio.api.model.ClassificationResult;
import com.arosbio.api.model.ErrorResponse;
import com.arosbio.api.model.ModelInfo;
import com.arosbio.api.model.PValueMapping;
import com.arosbio.api.model.ServiceRunning;
import com.arosbio.chem.io.out.GradientFigureBuilder;
import com.arosbio.chem.io.out.depictors.MoleculeGradientDepictor;
import com.arosbio.chem.io.out.fields.ColorGradientField;
import com.arosbio.chem.io.out.fields.PValuesField;
import com.arosbio.chem.io.out.fields.TitleField;
import com.arosbio.commons.auth.PermissionsCheck;
import com.arosbio.depict.GradientFactory;
import com.arosbio.io.UriUtils;
import com.arosbio.modeling.CPSignFactory;
import com.arosbio.modeling.cheminf.SignaturesCPClassification;
import com.arosbio.modeling.cheminf.SignificantSignature;
import com.arosbio.modeling.io.ModelLoader;
import com.arosbio.services.utils.CDKMutexLock;
import com.arosbio.services.utils.ChemUtils;
import com.arosbio.services.utils.Utils;


public class Predict {

	public static final String DEFAULT_LICENSE_PATH = "/opt/app-root/modeldata/license.license";
	public static final String DEFAULT_MODEL_PATH = "/opt/app-root/modeldata/model.jar";
	public static final String MODEL_FILE_ENV_VARIABLE = "MODEL_FILE";
	public static final String LICENSE_FILE_ENV_VARIABLE = "LICENSE_FILE";

	private static Logger logger = org.slf4j.LoggerFactory.getLogger(Predict.class);
	private static ErrorResponse serverErrorResponse = null;
	private static SignaturesCPClassification model;
	private static CPSignFactory factory;

	public static final int MIN_IMAGE_SIZE = 100;
	public static final int MAX_IMAGE_SIZE = 5000;

	static {

		final String license_file = System.getenv(LICENSE_FILE_ENV_VARIABLE)!=null ? System.getenv(LICENSE_FILE_ENV_VARIABLE) : DEFAULT_LICENSE_PATH;
		final String model_file = System.getenv(MODEL_FILE_ENV_VARIABLE)!=null?System.getenv(MODEL_FILE_ENV_VARIABLE) : DEFAULT_MODEL_PATH;

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
			URI license_uri = new File(license_file).toURI();
			factory = new CPSignFactory( license_uri );
			logger.info("Initiated the CPSignFactory");
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
						model = (SignaturesCPClassification) ModelLoader.loadModel(modelURI, factory.getEncryptionSpec());
					}
					else {
						model = (SignaturesCPClassification) ModelLoader.loadModel(modelURI, null);
					}
					logger.info("Loaded model");
				} catch (IllegalAccessException | IOException | InvalidKeyException | IllegalArgumentException e) {
					logger.error("Could not load the model", e);
					serverErrorResponse = new ErrorResponse(SERVICE_UNAVAILABLE, "Model could not be loaded at server init ("+e.getMessage()+") - service needs to be re-deployed");
				}
			}
		}
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


	public static Response getModelInfo() {
		if (serverErrorResponse != null) {
			return Utils.getResponse(serverErrorResponse);
		}

		return Response.ok(new ModelInfo(model.getModelInfo())).build();
	}

	public static Response doPredict(String molecule) {
		if (serverErrorResponse != null)
			return Utils.getResponse(serverErrorResponse);

		if (molecule==null || molecule.isEmpty()){
			logger.debug("Missing arguments 'molecule'");
			return Utils.getResponse(new BadRequestError(BAD_REQUEST, "missing argument", Arrays.asList("molecule")) );
		}

		String decodedMolData = null;
		try {
			decodedMolData = Utils.decodeURL(molecule);
		} catch (MalformedURLException e) {
			return Utils.getResponse( new BadRequestError(BAD_REQUEST, "Could not decode molecule text", Arrays.asList("molecule")));
		} 

		IAtomContainer molToPredict = null;
		try {
			molToPredict = ChemUtils.parseMolOrFail(decodedMolData);
		} catch (IllegalArgumentException e) {
			return Utils.getResponse(new BadRequestError(BAD_REQUEST, e.getMessage(), Arrays.asList("molecule")) );
		}

		// Generate SMILES to have in the response
		String smiles = null;
		try {
			smiles = ChemUtils.getAsSmiles(molToPredict, decodedMolData);
			logger.info("prediction-task for smiles=" + smiles);
		} catch (Exception e) {
			logger.debug("Failed getting smiles:\n\t"+Utils.getStackTrace(e));
			return Utils.getResponse(new ErrorResponse(INTERNAL_SERVER_ERROR, "Could not generate SMILES for molecule") );
		}

		CDKMutexLock.requireLock();
		try {
			Map<String, Double> res = model.predictMondrian(molToPredict);

			logger.debug("Successfully finished predicting smiles="+smiles+", pvalues=" + res );
			List<PValueMapping> pvalues = new ArrayList<>();
			for (Entry<String, Double> entry : res.entrySet()) {
				pvalues.add(new PValueMapping(entry.getKey(), entry.getValue()));
			}

			return Response.ok( new ClassificationResult(pvalues, smiles, model.getModelInfo().getModelName()) ).build();
		} catch (Exception e) {
			logger.warn("Failed predicting smiles=" + smiles +":\n\t" + Utils.getStackTrace(e));
			return Utils.getResponse( new ErrorResponse(INTERNAL_SERVER_ERROR, "Server error - error during prediction") );
		} finally {
			CDKMutexLock.releaseLock();
		}
	}

	private static boolean isValidSize(int size) {
		return ! (size < MIN_IMAGE_SIZE || size > MAX_IMAGE_SIZE);
	}

	public static Response doPredictImage(String molecule, int imageWidth, int imageHeight, boolean addPvaluesField, boolean addTitle) {
		logger.info("got a predict-image task, imageWidth="+imageWidth+", imageHeight="+imageHeight);

		if (serverErrorResponse != null)
			return Utils.getResponse(serverErrorResponse);

		if (! isValidSize(imageWidth) && ! isValidSize(imageHeight)) {
			logger.warn("Failing execution due to invalid image size");
			return Utils.getResponse(
					new BadRequestError(400,"image width and height must be in the range ["+MIN_IMAGE_SIZE + ","+MAX_IMAGE_SIZE+"]", Arrays.asList("imageWidth", "imageHeight")));
		}

		if (! isValidSize(imageWidth)){
			logger.warn("Failing execution due to invalid image size");
			return Utils.getResponse(
					new BadRequestError(400,"image width must be in the range ["+MIN_IMAGE_SIZE + ","+MAX_IMAGE_SIZE+"]", Arrays.asList("imageWidth")));
		}

		if (! isValidSize(imageHeight)){
			logger.warn("Failing execution due to invalid image size");
			return Utils.getResponse(
					new BadRequestError(BAD_REQUEST,"image height must be in the range ["+MIN_IMAGE_SIZE + ","+MAX_IMAGE_SIZE+"]", Arrays.asList("imageHeight")));
		}

		// Return empty img if no smiles sent
		if (molecule==null || molecule.isEmpty()){
			// return an empty img
			try{
				BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
				Graphics2D g2d = image.createGraphics();
				g2d.setColor(Color.WHITE);
				g2d.fillRect(0, 0, image.getWidth(), image.getHeight());
				g2d.dispose();
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ImageIO.write(image, "png", baos);
				byte[] imageData = baos.toByteArray();

				return Response.ok( new ByteArrayInputStream(imageData) ).build();
			} catch (IOException e) {
				logger.info("Failed returning empty image for empty smiles");
				return Utils.getResponse(new ErrorResponse(INTERNAL_SERVER_ERROR, "Server error"));
			}
		}

		String decodedMolData = null;
		try {
			decodedMolData = Utils.decodeURL(molecule);
		} catch (MalformedURLException e) {
			return Utils.getResponse(new BadRequestError(BAD_REQUEST, "Could not decode molecule text", Arrays.asList("molecule")));
		} 

		IAtomContainer molToPredict = null;
		try {
			molToPredict = ChemUtils.parseMolOrFail(decodedMolData);
		} catch (IllegalArgumentException e) {
			return Utils.getResponse(new BadRequestError(BAD_REQUEST, e.getMessage(), Arrays.asList("molecule")));
		}

		// Get smiles representation of molecule (mostly for debugging)
		String smiles = null;
		try {
			smiles = ChemUtils.getAsSmiles(molToPredict, molecule);
		} catch (Exception e) {
			smiles = "<no SMILES available>";
			logger.debug("Failed getting smiles:\n\t"+Utils.getStackTrace(e));
		}

		// Make prediction 
		SignificantSignature signSign = null;
		Map<String,Double> pVals = null;
		CDKMutexLock.requireLock();
		try {
			signSign = model.predictSignificantSignature(molToPredict);
			if (addPvaluesField && imageWidth>80) {
				pVals = model.predictMondrian(molToPredict);
			}
		} catch (Exception | Error e) {
			logger.warn("Failed predicting smiles=" + smiles + ", error:\n"+ Utils.getStackTrace(e));
			return Utils.getResponse(new ErrorResponse(INTERNAL_SERVER_ERROR, "Error during prediction: " + e.getMessage()) );
		} finally {
			CDKMutexLock.releaseLock();
		}

		// Create the depiction
		try {

			// Create the depiction
			MoleculeGradientDepictor depictor = new MoleculeGradientDepictor(GradientFactory.getDefaultBloomGradient());
			depictor.setImageHeight(imageHeight);
			depictor.setImageWidth(imageWidth);
			GradientFigureBuilder builder = new GradientFigureBuilder(depictor);

			// Add title if specified
			if (addTitle) {
				builder.addFieldOverImg(new TitleField(model.getModelInfo().getModelName()));
			}

			// Add p-values if specified
			if (pVals !=null){
				builder.addFieldUnderImg(new PValuesField(pVals));
			}
			builder.addFieldUnderImg(new ColorGradientField(depictor.getColorGradient()));

			BufferedImage image = builder.build(molToPredict, signSign.getMoleculeGradient()).getImage();

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(image, "png", baos);
			byte[] imageData = baos.toByteArray();

			return Response.ok( new ByteArrayInputStream(imageData) ).build();
		} catch (Exception | Error e) {
			logger.warn("Failed creating depiction for SMILES=" + smiles + ", error:\n"+ Utils.getStackTrace(e));
			return Utils.getResponse(new ErrorResponse(INTERNAL_SERVER_ERROR, "Error during image generation: " + e.getMessage()) );
		}
	}

}
