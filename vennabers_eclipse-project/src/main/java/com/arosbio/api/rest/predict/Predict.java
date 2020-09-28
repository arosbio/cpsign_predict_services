package com.arosbio.api.rest.predict;

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

import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.slf4j.Logger;

import com.arosbio.chem.io.out.GradientFigureBuilder;
import com.arosbio.chem.io.out.depictors.MoleculeGradientDepictor;
import com.arosbio.chem.io.out.fields.ColorGradientField;
import com.arosbio.chem.io.out.fields.ProbabilityField;
import com.arosbio.chem.io.out.fields.TitleField;
import com.arosbio.commons.auth.PermissionsCheck;
import com.arosbio.depict.GradientFactory;
import com.arosbio.modeling.CPSignFactory;
import com.arosbio.modeling.cheminf.SignaturesVAPClassification;
import com.arosbio.modeling.cheminf.SignaturesVAPClassification.SignaturesCVAPResult;
import com.arosbio.modeling.cheminf.SignificantSignature;
import com.arosbio.modeling.io.ModelLoader;

import io.swagger.model.BadRequestError;
import io.swagger.model.PValueMapping;
import io.swagger.model.VenaberResult;

public class Predict {

	private static Logger logger = org.slf4j.LoggerFactory.getLogger(Predict.class);
	private static Response serverErrorResponse = null;
	private static SignaturesVAPClassification model;
	private static CPSignFactory factory;

	private static final int MIN_IMAGE_SIZE = 50;
	private static final int MAX_IMAGE_SIZE = 5000;
	
	private static String errorMessage = null;

	static {

		final String license_file =
				System.getenv("LICENSE_FILE")!=null?System.getenv("LICENSE_FILE"):"/opt/app-root/modeldata/license.license";
		final String model_file =
				System.getenv("MODEL_FILE")!=null?System.getenv("MODEL_FILE"):"/opt/app-root/modeldata/model.jar";

		// Get the root logger for cpsign
		Logger cpsingLogger =  org.slf4j.LoggerFactory.getLogger("com.genettasoft.modeling");
		if(cpsingLogger instanceof ch.qos.logback.classic.Logger) {
			ch.qos.logback.classic.Logger cpsignRoot = (ch.qos.logback.classic.Logger) cpsingLogger;
			// Disable all cpsign-output
			cpsignRoot.setLevel(ch.qos.logback.classic.Level.OFF);
		}

		// Enable debug output for this library
		Logger predictionServiceLogger = org.slf4j.LoggerFactory.getLogger("com.arosbio.api.rest");
		if(predictionServiceLogger instanceof ch.qos.logback.classic.Logger) {
			ch.qos.logback.classic.Logger cpLogDLogger = (ch.qos.logback.classic.Logger) predictionServiceLogger;
			cpLogDLogger.setLevel(ch.qos.logback.classic.Level.DEBUG);
		}

		// Instantiate the factory 
		try{
			URI license_uri = new File(license_file).toURI();
			factory = new CPSignFactory( license_uri );
			logger.info("Initiated the CPSignFactory");
		} catch (RuntimeException | IOException re){
			errorMessage = re.getMessage();
			logger.error("Got exception when trying to instantiate CPSignFactory: " + re.getMessage());
			serverErrorResponse = Response.status(500).entity( new io.swagger.model.Error(500, re.getMessage() ).toString() ).build();
		}
		// load the model - only if no error previously encountered
		if (serverErrorResponse == null) {
			try {
				logger.debug("Trying to load in the model");
				URI modelURI = new File(model_file).toURI();
				if (modelURI == null)
					throw new IOException("did not locate the model file");
				if ( factory.supportEncryption() ) {
					model = (SignaturesVAPClassification) ModelLoader.loadModel(modelURI, factory.getEncryptionSpec());
				}
				else {
					model = (SignaturesVAPClassification) ModelLoader.loadModel(modelURI, null);
				}
				logger.info("Loaded model");
			} catch (IllegalAccessException | IOException | InvalidKeyException | IllegalArgumentException e) {
				errorMessage = e.getMessage();
				logger.error("Could not load the model", e);
				serverErrorResponse = Response.status(500).entity( new io.swagger.model.Error(500, "Server error - could not load the built model").toString() ).build();
			}
		}
	}

	public static Response checkHealth() {
		if( errorMessage != null) {
			return Response.status(500).entity( new io.swagger.model.Error(500, errorMessage ).toString()).build();
		} else if (! PermissionsCheck.check()) {
			return Response.status(500).entity( new io.swagger.model.Error(500, "License has expired" ).toString()).build();
		} else {
		    return Response.status(200).entity("OK").build();
		}

	}
	public static Response doPredict(String molecule) {
		logger.debug("got a prediction task");

		if (serverErrorResponse != null)
			return serverErrorResponse;

		if (molecule==null || molecule.isEmpty()){
			logger.debug("Missing arguments 'molecule'");
			return Response.status(400).
					entity( new BadRequestError(400, "missing argument", Arrays.asList("molecule")).toString() ).
					build();
		}

		String decodedMolData = null;
		try {
			decodedMolData = Utils.decodeURL(molecule);
		} catch (MalformedURLException e) {
			return Response.status(400).
					entity( new BadRequestError(400, "Could not decode molecule text", Arrays.asList("molecule")).toString()).
					build();
		} 

		IAtomContainer molToPredict = null;
		try {
			molToPredict = ChemUtils.parseMolOrFail(decodedMolData);
		} catch (IllegalArgumentException e) {
			return Response.status(400).
					entity(new BadRequestError(400, e.getMessage(), Arrays.asList("molecule")).toString() ).
					build();
		}
//		
//		if (molecule==null || molecule.isEmpty()){
//			logger.debug("Missing arguments 'molecule'");
//			return Response.status(400).entity( new BadRequestError(400, "missing argument", Arrays.asList("molecule")).toString() ).build();
//		}
//
//		// Clean the molecule-string from URL encoding
//		try {
//			molecule = URLDecoder.decode(molecule, URL_ENCODING);
//		} catch (Exception e) {
//			return Response.status(400).entity( 
//					new BadRequestError(400, "Could not decode molecule text", Arrays.asList("molecule")).toString()).build();
//		}
//
//		// try to parse an IAtomContainer - or fail
//		Pair<IAtomContainer, Response> molOrFail = null;
//		try {
//			molOrFail = ChemUtils.parseMolecule(molecule);
//			if (molOrFail.getValue1() != null)
//				return molOrFail.getValue1();
//		} catch (Exception | Error e) {
//			logger.debug("Unhandled exception in Parsing of molecule input:\n\t"+Utils.getStackTrace(e));
//			return Response.status(400).entity(
//					new BadRequestError(400, "Faulty molecule input", Arrays.asList("molecule"))).build();
//		}
//		IAtomContainer molToPredict=molOrFail.getValue0();

		// Generate SMILES to have in the response
		String smiles = null;
		try {
			smiles = ChemUtils.getAsSmiles(molToPredict, decodedMolData);
			logger.debug("prediction-task for smiles=" + smiles);
		} catch (Exception e) {
			logger.debug("Failed getting smiles:\n\t"+Utils.getStackTrace(e));
			return Response.status(500).entity( 
					new io.swagger.model.Error(500, "Could not generate SMILES for molecule").toString() )
					.build();
		}
		logger.info("prediction-task for smiles=" + smiles);

		CDKMutexLock.requireLock();
		try {
			
			SignaturesCVAPResult res = model.predict(molToPredict);
			
			logger.debug("Successfully finished predicting smiles="+smiles+", probabilites=" + res );
			
			List<PValueMapping> pvalues = new ArrayList<>();
			for (Entry<String, Double> entry : res.getProbabilties().entrySet()) {
				pvalues.add(new PValueMapping(entry.getKey(), entry.getValue()));
			}

			return Response.status(200).entity( new VenaberResult(pvalues, smiles, model.getModelInfo().getModelName()).toString() ).build();
		} catch (Exception e) {
			logger.warn("Failed predicting smiles=" + smiles +":\n\t" + Utils.getStackTrace(e));
			return Response.status(500).entity( new io.swagger.model.Error(500, "Server error - error during prediction").toString() ).build();
		} finally {
			CDKMutexLock.releaseLock();
		}
	}



	public static Response doPredictImage(String molecule, int imageWidth, int imageHeight, boolean addPvaluesField, boolean addTitle) {
		logger.info("got a predict-image task, imageWidth="+imageWidth+", imageHeight="+imageHeight);

		if (serverErrorResponse != null)
			return serverErrorResponse;

		if(imageWidth < MIN_IMAGE_SIZE || imageHeight < MIN_IMAGE_SIZE){
			logger.warn("Failing execution due to too small image requested");
			return Response.status(400).entity(new BadRequestError(400,"image height and width must be at least "+MIN_IMAGE_SIZE+" pixels", Arrays.asList("imageWidth", "imageHeight")).toString()).build();
		}

		if (imageWidth > MAX_IMAGE_SIZE || imageHeight> MAX_IMAGE_SIZE){
			logger.warn("Failing execution due to too large image requested");
			return Response.status(400).entity(new BadRequestError(400,"image height and width can maximum be "+MAX_IMAGE_SIZE+" pixels", Arrays.asList("imageWidth", "imageHeight")).toString()).build();
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
				return Response.status(500).entity(new io.swagger.model.Error(500, "Server error").toJSON()).build();
			}
		}

		// Clean the molecule-string from URL encoding
		if (molecule==null || molecule.isEmpty()){
			logger.debug("Missing arguments 'molecule'");
			return Response.status(400).
					entity( new BadRequestError(400, "missing argument", Arrays.asList("molecule")).toString() ).
					build();
		}

		String decodedMolData = null;
		try {
			decodedMolData = Utils.decodeURL(molecule);
		} catch (MalformedURLException e) {
			return Response.status(400).
					entity( new BadRequestError(400, "Could not decode molecule text", Arrays.asList("molecule")).toString()).
					build();
		} 

		IAtomContainer molToPredict = null;
		try {
			molToPredict = ChemUtils.parseMolOrFail(decodedMolData);
		} catch (IllegalArgumentException e) {
			return Response.status(400).
					entity(new BadRequestError(400, e.getMessage(), Arrays.asList("molecule")).toString() ).
					build();
		}

		// Get smiles representation of molecule
		String smiles = null;
		try {
			smiles = ChemUtils.getAsSmiles(molToPredict, decodedMolData);
		} catch (Exception e) {
			logger.debug("Failed getting smiles:\n\t"+Utils.getStackTrace(e));
			return Response.status(400).entity(new BadRequestError(400, "Could not generate SMILES for molecule",Arrays.asList("molecule"))).build();
		}

		// Make prediction + image
		SignificantSignature signSign = null;
		try {
			CDKMutexLock.requireLock(); // require the lock again!
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
			if (addPvaluesField && imageWidth>80){
				Map<String,Double> pVals = model.predictProbabilities(molToPredict);
				builder.addFieldUnderImg(new ProbabilityField(pVals));
			}
			builder.addFieldUnderImg(new ColorGradientField(depictor.getColorGradient()));

			BufferedImage image = builder.build(molToPredict, signSign.getMoleculeGradient()).getImage();

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(image, "png", baos);
			byte[] imageData = baos.toByteArray();

			return Response.ok( new ByteArrayInputStream(imageData) ).build();
		} catch (IllegalAccessException | CDKException | IOException e) {
			logger.warn("Failed predicting smiles=" + smiles + ", error:\n"+ Utils.getStackTrace(e));
			return Response.status(500).entity( new io.swagger.model.Error(500, "Server error - error during prediction").toString() ).build();
		} finally {
			CDKMutexLock.releaseLock();
		}
	}

}
