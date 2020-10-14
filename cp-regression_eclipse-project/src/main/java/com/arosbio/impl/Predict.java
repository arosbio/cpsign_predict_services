package com.arosbio.impl;

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
import java.util.Arrays;

import javax.imageio.ImageIO;
import javax.ws.rs.core.Response;

import org.javatuples.Pair;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.slf4j.Logger;

import com.arosbio.api.model.BadRequestError;
import com.arosbio.api.model.ErrorResponse;
import com.arosbio.api.model.ServiceRunning;
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
import com.google.common.collect.Range;

public class Predict {

	public static final String DEFAULT_LICENSE_PATH = "/opt/app-root/modeldata/license.license";
	public static final String DEFAULT_MODEL_PATH = "/opt/app-root/modeldata/model.jar";
	public static final String MODEL_FILE_ENV_VARIABLE = "MODEL_FILE";
	public static final String LICENSE_FILE_ENV_VARIABLE = "LICENSE_FILE";

	private static Logger logger = org.slf4j.LoggerFactory.getLogger(Predict.class);
	private static Response serverErrorResponse = null;
	private static SignaturesCPRegression model;
	private static CPSignFactory factory;

	private static final int MIN_IMAGE_SIZE = 50;
	private static final int MAX_IMAGE_SIZE = 5000;

//	private static String errorMessage = null;

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
			URI license_uri = new File(license_file).toURI();
			factory = new CPSignFactory( license_uri );
			logger.info("Initiated the CPSignFactory");
		} catch (RuntimeException | IOException re){
			logger.error("Got exception when trying to instantiate CPSignFactory: " + re.getMessage());
			serverErrorResponse = Response.status(500).entity(
					new ErrorResponse(500, "No license found at server init - service needs to be re-deployed with a valid license")
					).build();
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
				serverErrorResponse = Response.status(500).entity(
						new ErrorResponse(500, "No model found at server init - service needs to be re-depolyed with a valid model config")
						).build();
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
					serverErrorResponse = Response.status(500).entity( new ErrorResponse(500, "Model could not be loaded at server init ("+e.getMessage()+") - service needs to be re-deployed") ).build();
				}
			}
		}
	}

	public static Response doPredict(String molecule, double confidence) {
		logger.debug("got a prediction task, conf=" + confidence);

		if (serverErrorResponse != null)
			return Utils.clone(serverErrorResponse);

		if (molecule==null || molecule.isEmpty()){
			logger.debug("Missing arguments 'molecule'");
			return Response.status(400).
					entity( new BadRequestError(400, "missing argument", Arrays.asList("molecule")) ).
					build();
		}

		String decodedMolData = null;
		try {
			decodedMolData = Utils.decodeURL(molecule);
		} catch (MalformedURLException e) {
			return Response.status(400).
					entity( new BadRequestError(400, "Could not decode molecule text", Arrays.asList("molecule"))).
					build();
		} 

		IAtomContainer molToPredict = null;
		try {
			molToPredict = ChemUtils.parseMolOrFail(decodedMolData);
		} catch (IllegalArgumentException e) {
			return Response.status(400).
					entity(new BadRequestError(400, e.getMessage(), Arrays.asList("molecule")) ).
					build();
		}

		// Generate SMILES to have in the response
		String smiles = null;
		try {
			smiles = ChemUtils.getAsSmiles(molToPredict, decodedMolData);
			logger.info("prediction-task for smiles=" + smiles);
		} catch (Exception e) {
			logger.debug("Failed getting smiles:\n\t"+Utils.getStackTrace(e));
			return Response.status(500).entity( 
					new com.arosbio.api.model.ErrorResponse(500, "Could not generate SMILES for molecule") )
					.build();
		}

		// Make prediction
		CDKMutexLock.requireLock();
		try {
			CPRegressionPrediction res = model.predict(molToPredict, confidence);
			logger.debug("Successfully finished predicting smiles="+smiles+", interval=" + res );
			return Response.status(200).entity( new com.arosbio.api.model.RegressionResult(smiles,res,confidence, model.getModelInfo().getModelName()) ).build();
		} catch (Exception | Error e) {
			logger.warn("Failed predicting smiles=" + smiles +":\n\t" + Utils.getStackTrace(e));
			return Response.status(500).entity( new com.arosbio.api.model.ErrorResponse(500, "Server error - error during prediction") ).build();
		} finally{
			CDKMutexLock.releaseLock();
		}
	}

	public static Response doPredictImage(String molecule, int imageWidth, int imageHeight, Double confidence, boolean addTitle) {
		logger.debug("Got predictImage request: imageWidth="+imageWidth + ", imageHeight="+imageHeight+", conf="+confidence);
		if (serverErrorResponse != null)
			return Utils.clone(serverErrorResponse);

		if (confidence != null && (confidence < 0 || confidence > 1)){
			logger.warn("invalid argument confidence=" + confidence);
			return Response.status(400).entity(
					new BadRequestError(400, "invalid argument", Arrays.asList("confidence"))
					).build();
		}

		if (imageWidth < MIN_IMAGE_SIZE || imageHeight < MIN_IMAGE_SIZE){
			logger.warn("Failing execution due to too small image required");
			return Response.status(400).entity(
					new BadRequestError(400,"image height and width must be at least "+MIN_IMAGE_SIZE+" pixels", Arrays.asList("imageWidth", "imageHeight"))
					).build();
		}

		if (imageWidth > MAX_IMAGE_SIZE || imageHeight> MAX_IMAGE_SIZE){
			logger.warn("Failing execution due to too large image requested");
			return Response.status(400).entity(
					new BadRequestError(400,"image height and width can maximum be "+MAX_IMAGE_SIZE+" pixels", Arrays.asList("imageWidth", "imageHeight"))
					).build();
		}

		// Return empty img if no smiles sent
		if (molecule==null || molecule.isEmpty()){
			// return an empty img
			try {
				BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
				Graphics2D g2d = image.createGraphics();
				g2d.setColor(Color.WHITE);
				g2d.fillRect(0, 0, image.getWidth(), image.getHeight());
				g2d.dispose();
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ImageIO.write(image, "png", baos);
				byte[] imageData = baos.toByteArray();

				return Response.ok( new ByteArrayInputStream(imageData) ).build();
			} catch (Exception e) {
				logger.info("Failed returning empty image for empty smiles");
				return Response.status(500).entity(new ErrorResponse(500, "Server error")).build();
			}
		}

		if (molecule==null || molecule.isEmpty()){
			logger.debug("Missing arguments 'molecule'");
			return Response.status(400).
					entity( new BadRequestError(400, "missing argument", Arrays.asList("molecule")) ).
					build();
		}

		String decodedMolData = null;
		try {
			decodedMolData = Utils.decodeURL(molecule);
		} catch (MalformedURLException e) {
			return Response.status(400).
					entity( new BadRequestError(400, "Could not decode molecule text", Arrays.asList("molecule"))).
					build();
		} 

		IAtomContainer molToPredict = null;
		try {
			molToPredict = ChemUtils.parseMolOrFail(decodedMolData);
		} catch (IllegalArgumentException e) {
			return Response.status(400).
					entity(new BadRequestError(400, e.getMessage(), Arrays.asList("molecule")) ).
					build();
		}

		// Generate SMILES to have in the response
		String smiles = null;
		try {
			smiles = ChemUtils.getAsSmiles(molToPredict, molecule);
			logger.info("prediction-task for smiles=" + smiles);
		} catch (Exception e) {
			logger.debug("Failed getting smiles:\n\t"+Utils.getStackTrace(e));
			return Response.status(500).entity( 
					new ErrorResponse(500, "Could not generate SMILES for molecule: " + e.getMessage()) )
					.build();
		}

		// Make prediction + generate image
		SignificantSignature signSign = null;
		CDKMutexLock.requireLock();
		try {
			signSign = model.predictSignificantSignature(molToPredict);

			// Create the depiction
			MoleculeGradientDepictor depictor = new MoleculeGradientDepictor(GradientFactory.getDefaultBloomGradient());
			depictor.setImageHeight(imageHeight);
			depictor.setImageWidth(imageWidth);
			GradientFigureBuilder builder = new GradientFigureBuilder(depictor);

			// Add title if specified
			if (addTitle) {
				builder.addFieldOverImg(new TitleField(model.getModelInfo().getModelName()));
			}
			// add confidence interval only if given confidence and image size is big enough
			if (confidence != null && imageWidth>80){
				CPRegressionPrediction pred = model.predict(molToPredict, confidence);
				Range<Double> interval = pred.getInterval(confidence).getInterval();
				builder.addFieldUnderImg(new PredictionIntervalField(Pair.with(interval.lowerEndpoint(), interval.upperEndpoint()), confidence));
			}
			builder.addFieldUnderImg(new ColorGradientField(depictor.getColorGradient()));

			BufferedImage image = builder.build(molToPredict, signSign.getMoleculeGradient()).getImage();

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(image, "png", baos);
			byte[] imageData = baos.toByteArray();

			return Response.ok( new ByteArrayInputStream(imageData) ).build();
		} catch (Exception | Error e) {
			logger.warn("Failed predicting smiles=" + smiles + ", error:\n"+ Utils.getStackTrace(e));
			return Response.status(500).entity( 
					new ErrorResponse(500, "Error during prediction: " + e.getMessage()) 
					).build();
		} finally {
			CDKMutexLock.releaseLock();
		}
	}
	public static Response checkHealth() {
		if( serverErrorResponse != null) {
			return Utils.clone(serverErrorResponse);
		} else if (! PermissionsCheck.check()) {
			return Response.status(500).entity( new ErrorResponse(500, "License has expired" )).build();
		} else {
			return Response.status(200).entity(new ServiceRunning()).build();
		}
	}
}
