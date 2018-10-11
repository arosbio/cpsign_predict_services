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
import java.net.URLDecoder;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.imageio.ImageIO;
import javax.ws.rs.core.Response;

import org.javatuples.Pair;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.slf4j.Logger;

import com.genettasoft.chem.io.out.MoleculeFigure.GradientFigureBuilder;
import com.genettasoft.chem.io.out.MoleculeGradientDepictor;
import com.genettasoft.chem.io.out.fields.ColorGradientField;
import com.genettasoft.chem.io.out.fields.PValuesField;
import com.genettasoft.chem.io.out.fields.TitleField;
import com.genettasoft.depict.GradientFactory;
import com.genettasoft.modeling.CPSignFactory;
import com.genettasoft.modeling.cheminf.SignaturesCPClassification;
import com.genettasoft.modeling.cheminf.SignificantSignature;
import com.genettasoft.modeling.io.bndTools.BNDLoader;

import io.swagger.model.PValueMapping;

public class Predict {

	private static Logger logger = org.slf4j.LoggerFactory.getLogger(Predict.class);
	private static Response serverErrorResponse = null;
	private static SignaturesCPClassification model;
	private static CPSignFactory factory;

	private static final int MIN_IMAGE_SIZE = 50;
	private static final int MAX_IMAGE_SIZE = 5000;
	private static final String URL_ENCODING = "UTF-8";

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
					model = (SignaturesCPClassification) BNDLoader.loadModel(modelURI, factory.getEncryptionSpec());
				}
				else {
					model = (SignaturesCPClassification) BNDLoader.loadModel(modelURI, null);
				}
				logger.info("Loaded model");
			} catch (IllegalAccessException | IOException | InvalidKeyException | IllegalArgumentException e) {
				logger.error("Could not load the model", e);
				serverErrorResponse = Response.status(500).entity( new io.swagger.model.Error(500, "Server error - could not load the built model").toString() ).build();
			}
		}
	}

	public static Response doPredict(String molecule) {
		logger.debug("got a prediction task");

		if (serverErrorResponse != null)
			return serverErrorResponse;

		if (molecule==null || molecule.isEmpty()){
			logger.debug("Missing arguments 'molecule'");
			return Response.status(400).entity( new io.swagger.model.BadRequestError(400, "missing argument", Arrays.asList("molecule")).toString() ).build();
		}

		// Clean the molecule-string from URL encoding
		try {
			if (molecule != null && !molecule.isEmpty())
				molecule = URLDecoder.decode(molecule, URL_ENCODING);
		} catch (Exception e) {
			return Response.status(400).entity( new io.swagger.model.BadRequestError(400, "Could not decode molecule text", Arrays.asList("molecule")).toString()).build();
		}

		// try to parse an IAtomContainer - or fail
		Pair<IAtomContainer, Response> molOrFail = ChemUtils.parseMolecule(molecule);
		if (molOrFail.getValue1() != null)
			return molOrFail.getValue1();

		IAtomContainer molToPredict=molOrFail.getValue0();

		// Generate SMILES to have in the response
		String smiles = null;
		try {
			smiles = ChemUtils.getAsSmiles(molToPredict, molecule);
			logger.debug("prediction-task for smiles=" + smiles);
		} catch (Exception e) {
			logger.debug("Failed creating smiles from IAtomContainer",e);
			return Response.status(500).entity( new io.swagger.model.Error(500, "Could not generate SMILES for molecule").toString() ).build();
		}

		try {
			Map<String, Double> res = model.predictMondrian(molToPredict);
			CDKMutexLock.releaseLock();
			logger.debug("Successfully finished predicting smiles="+smiles+", pvalues=" + res );
			List<PValueMapping> pvalues = new ArrayList<>();
			for (Entry<String, Double> entry : res.entrySet()) {
				pvalues.add(new PValueMapping(entry.getKey(), entry.getValue()));
			}

			return Response.status(200).entity( new io.swagger.model.ClassificationResult(pvalues, smiles, model.getModelName()).toString() ).build();
		} catch (IllegalAccessException | CDKException e) {
			CDKMutexLock.releaseLock();
			logger.debug("Failed predicting smiles=" + molecule, e);
			return Response.status(500).entity( new io.swagger.model.Error(500, "Server error - error during prediction").toString() ).build();
		}
	}

	public static Response doPredictImage(String molecule, int imageWidth, int imageHeight, boolean addPvaluesField, boolean addTitle) {
		if (serverErrorResponse != null)
			return serverErrorResponse;

		if(imageWidth < MIN_IMAGE_SIZE || imageHeight < MIN_IMAGE_SIZE){
			logger.warn("Failing execution due to too small image requested");
			return Response.status(400).entity(new io.swagger.model.BadRequestError(400,"image height and with must be at least "+MIN_IMAGE_SIZE+" pixels", Arrays.asList("imageWidth", "imageHeight")).toString()).build();
		}

		if (imageWidth > MAX_IMAGE_SIZE || imageHeight> MAX_IMAGE_SIZE){
			logger.warn("Failing execution due to too large image requested");
			return Response.status(400).entity(new io.swagger.model.BadRequestError(400,"image height and width can maximum be "+MAX_IMAGE_SIZE+" pixels", Arrays.asList("imageWidth", "imageHeight")).toString()).build();
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
		try {
			molecule = URLDecoder.decode(molecule, URL_ENCODING);
		} catch (Exception e) {
			return Response.status(400).entity( new io.swagger.model.BadRequestError(400, "Could not decode molecule text", Arrays.asList("molecule")).toString()).build();
		}

		// try to parse an IAtomContainer - or fail
		Pair<IAtomContainer, Response> molOrFail = ChemUtils.parseMolecule(molecule);
		if (molOrFail.getValue1() != null)
			return molOrFail.getValue1();

		IAtomContainer molToPredict=molOrFail.getValue0();

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
				builder.addFieldOverImg(new TitleField(model.getModelName()));
			}
			// add confidence interval only if given confidence and image size is big enough
			if (addPvaluesField && imageWidth>80){
				Map<String,Double> pVals = model.predictMondrian(molToPredict);
				builder.addFieldUnderImg(new PValuesField(pVals));
			}
			builder.addFieldUnderImg(new ColorGradientField(depictor.getColorGradient()));

			BufferedImage image = builder.build(molToPredict, signSign.getAtomValues()).getImage();

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(image, "png", baos);
			byte[] imageData = baos.toByteArray();

			return Response.ok( new ByteArrayInputStream(imageData) ).build();
		} catch (IllegalAccessException | CDKException | IOException e) {
			logger.debug("Failed predicting molecule=" + molecule, e);
			return Response.status(500).entity( new io.swagger.model.Error(500, "Server error - error during prediction").toString() ).build();
		} finally {
			CDKMutexLock.releaseLock();
		}
	}

}
