package com.genettasoft.api.predict;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.security.InvalidKeyException;
import java.util.Arrays;

import javax.imageio.ImageIO;
import javax.ws.rs.core.Response;

import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.slf4j.Logger;

import com.genettasoft.chem.io.out.MoleculeFigure.GradientFigureBuilder;
import com.genettasoft.chem.io.out.MoleculeGradientDepictor;
import com.genettasoft.chem.io.out.fields.ColorGradientField;
import com.genettasoft.chem.io.out.fields.PredictionIntervalField;
import com.genettasoft.depict.GradientFactory;
import com.genettasoft.modeling.CPSignFactory;
import com.genettasoft.modeling.cheminf.SignaturesCPRegression;
import com.genettasoft.modeling.cheminf.SignificantSignature;
import com.genettasoft.modeling.io.bndTools.BNDLoader;
import com.genettasoft.modeling.ml.cp.CPRegressionResult;

public class Predict {

	private static Logger logger = org.slf4j.LoggerFactory.getLogger(Predict.class);
	private static Response serverErrorResponse = null;
	private static SignaturesCPRegression model;
	private static CPSignFactory factory;

	private static final int MIN_IMAGE_SIZE = 50;
	private static final int MAX_IMAGE_SIZE = 5000;

	static {

		// Get the root logger for cpsign
		Logger cpsingLogger =  org.slf4j.LoggerFactory.getLogger("com.genettasoft.modeling");
		if(cpsingLogger instanceof ch.qos.logback.classic.Logger) {
			ch.qos.logback.classic.Logger cpsignRoot = (ch.qos.logback.classic.Logger) cpsingLogger;
			// Disable all cpsign-output
			cpsignRoot.setLevel(ch.qos.logback.classic.Level.OFF);
		}

		// Enable debug output for this library
		Logger cpLogDLogging = org.slf4j.LoggerFactory.getLogger("se.uu.farmbio");
		if(cpLogDLogging instanceof ch.qos.logback.classic.Logger) {
			ch.qos.logback.classic.Logger cpLogDLogger = (ch.qos.logback.classic.Logger) cpLogDLogging;
			cpLogDLogger.setLevel(ch.qos.logback.classic.Level.DEBUG);
		}

		// Instantiate the factory 
		try{
			factory = new CPSignFactory( new FileInputStream( new File("/opt/app-root/modeldata/license.license") ) );
			logger.info("Initiated the CPSignFactory");
		} catch (RuntimeException | IOException re){
			logger.error("Got exception when trying to instantiate CPSignFactory: " + re.getMessage());
			serverErrorResponse = Response.status(500).entity( new io.swagger.model.Error(500, re.getMessage() ).toString() ).build();
		}
		// load the model - only if no error previously encountered
		if (serverErrorResponse == null) {
			try {
				logger.debug("Trying to load in the model");
				URI modelURI = new File("/opt/app-root/modeldata/model.jar").toURI();
				if (modelURI == null)
					throw new IOException("did not locate the model file");
				if ( factory.supportEncryption() ) {
					model = (SignaturesCPRegression) BNDLoader.loadModel(modelURI, factory.getEncryptionSpec());
				}
				else {
					model = (SignaturesCPRegression) BNDLoader.loadModel(modelURI, null);
				}
				logger.info("Loaded model");
			} catch (IllegalAccessException | IOException | InvalidKeyException | IllegalArgumentException e) {
				logger.error("Could not load the model", e);
				serverErrorResponse = Response.status(500).entity( new io.swagger.model.Error(500, "Server error - could not load the built model").toString() ).build();
			}
		}
	}

	public static Response doPredict(String smiles, double confidence) {
		logger.debug("got a prediction task: smiles="+smiles+", conf=" + confidence);

		if(serverErrorResponse != null)
			return serverErrorResponse;

		if (smiles==null || smiles.isEmpty()){
			logger.debug("Missing arguments 'smiles'");
			return Response.status(400).entity( new io.swagger.model.BadRequestError(400, "missing argument", Arrays.asList("smiles")).toString() ).build();
		}

		IAtomContainer molToPredict=null;
		CDKMutexLock.requireLock();
		try{
			molToPredict = CPSignFactory.parseSMILES(smiles);
		} catch(IllegalArgumentException e){
			logger.debug("Got exception when parsing smiles: " + e.getMessage() + "\nreturning error-msg and stopping", e);
			CDKMutexLock.releaseLock();
			return Response.status(400).entity( new io.swagger.model.BadRequestError(400, "Invalid query SMILES '" + smiles + "'", Arrays.asList("smiles")).toString() ).build();
		}

		try {
			CPRegressionResult res = model.predict(molToPredict, confidence);
			logger.debug("Successfully finished predicting smiles="+smiles+", interval=" + res );
			return Response.status(200).entity( new io.swagger.model.RegressionResult(smiles,res,confidence).toString() ).build();
		} catch (IllegalAccessException | CDKException e) {
			logger.debug("Failed predicting smiles=" + smiles, e);
			return Response.status(500).entity( new io.swagger.model.Error(500, "Server error - error during prediction").toString() ).build();
		} finally{
			CDKMutexLock.releaseLock();
		}
	}

	public static Response doPredictImage(String smiles, int imageWidth, int imageHeight, Double confidence) {
		logger.debug("Got predictImage request: smiles="+smiles + ", imageWidth="+imageWidth + ", imageHeight="+imageHeight+", conf="+confidence);
		if(serverErrorResponse != null)
			return serverErrorResponse;
		
		if (confidence != null && (confidence < 0 || confidence > 1)){
			logger.warn("invalid argument confidence=" + confidence);
			return Response.status(400).entity(new io.swagger.model.BadRequestError(400, "invalid argument", Arrays.asList("confidence")).toString()).build();
		}
		
		if (imageWidth < MIN_IMAGE_SIZE || imageHeight < MIN_IMAGE_SIZE){
			logger.warn("Failing execution due to too small image required");
			return Response.status(400).entity(new io.swagger.model.BadRequestError(400,"image height and with must be at least "+MIN_IMAGE_SIZE+" pixels", Arrays.asList("imageWidth", "imageHeight")).toString()).build();
		}

		if (imageWidth > MAX_IMAGE_SIZE || imageHeight> MAX_IMAGE_SIZE){
			logger.warn("Failing execution due to too large image requested");
			return Response.status(400).entity(new io.swagger.model.BadRequestError(400,"image height and width can maximum be "+MAX_IMAGE_SIZE+" pixels", Arrays.asList("imageWidth", "imageHeight")).toString()).build();
		}

		// Return empty img if no smiles sent
		if (smiles==null || smiles.isEmpty()){
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
		
		IAtomContainer molToPredict=null;
		CDKMutexLock.requireLock();
		try {
			molToPredict = CPSignFactory.parseSMILES(smiles);
		} catch(IllegalArgumentException e){
			logger.debug("Got exception when parsing smiles: " + e.getMessage() + "\nreturning error-msg and stopping", e);
			CDKMutexLock.releaseLock();
			return Response.status(400).entity( new io.swagger.model.BadRequestError(400, "Invalid query SMILES '" + smiles + "'", Arrays.asList("smiles")).toString() ).build();
		}

		SignificantSignature signSign = null;

		try {
			signSign = model.predictSignificantSignature(molToPredict);

			// Create the depiction
			MoleculeGradientDepictor depictor = new MoleculeGradientDepictor(GradientFactory.getDefaultBloomGradient());
			depictor.setImageHeight(imageHeight);
			depictor.setImageWidth(imageWidth);
			GradientFigureBuilder builder = new GradientFigureBuilder(depictor);

			// add confidence interval only if given confidence and image size is big enough
			if (confidence != null && imageWidth>80){
				CPRegressionResult pred = model.predict(molToPredict, confidence);
				builder.addFieldUnderImg(new PredictionIntervalField(pred.getInterval(), confidence));
			}
			builder.addFieldUnderImg(new ColorGradientField(depictor.getColorGradient()));

			BufferedImage image = builder.build(molToPredict, signSign.getAtomValues()).getImage();

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(image, "png", baos);
			byte[] imageData = baos.toByteArray();

			return Response.ok( new ByteArrayInputStream(imageData) ).build();
		} catch (IllegalAccessException | CDKException | IOException e) {
			logger.debug("Failed predicting smiles=" + smiles, e);
			return Response.status(500).entity( new io.swagger.model.Error(500, "Server error - error during prediction").toString() ).build();
		} finally {
			CDKMutexLock.releaseLock();
		}
	}
}
