package com.genettasoft.api.predict;

import java.awt.font.ImageGraphicAttribute;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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

import com.genettasoft.depict.GradientFactory;
import com.genettasoft.depict.MoleculeDepictor;
import com.genettasoft.modeling.CPSignFactory;
import com.genettasoft.modeling.cheminf.SignaturesCPClassification;
import com.genettasoft.modeling.cheminf.SignificantSignature;
import com.genettasoft.modeling.io.bndTools.BNDLoader;
import com.genettasoft.modeling.io.chemwriter.MolImageDepictor;

import io.swagger.model.PValueMapping;

public class Predict {

	private static Logger logger = org.slf4j.LoggerFactory.getLogger(Predict.class);
	private static Response serverErrorResponse = null;
	private static SignaturesCPClassification model;
	private static CPSignFactory factory;
	
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
	
	public static Response doPredict(String smiles) {
		logger.debug("got a prediction task, smiles="+smiles);
		
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
			Map<String, Double> res = model.predictMondrian(molToPredict);
			CDKMutexLock.releaseLock();
			logger.debug("Successfully finished predicting smiles="+smiles+", interval=" + res );
			List<PValueMapping> pvalues = new ArrayList<>();
			for (Entry<String, Double> entry : res.entrySet()) {
				pvalues.add(new PValueMapping(entry.getKey(), entry.getValue()));
			}
				
			return Response.status(200).entity( new io.swagger.model.Classification(pvalues, smiles).toString() ).build();
		} catch (IllegalAccessException | CDKException e) {
			CDKMutexLock.releaseLock();
			logger.debug("Failed predicting smiles=" + smiles, e);
			return Response.status(500).entity( new io.swagger.model.Error(500, "Server error - error during prediction").toString() ).build();
		}
	}
	
	public static Response doPredictImage(String smiles) {
		if(serverErrorResponse != null)
			return serverErrorResponse;
		
		if (smiles==null || smiles.isEmpty()){
			logger.debug("Missing arguments 'smiles'");
			return Response.status(400).entity( new io.swagger.model.BadRequestError(400, "missing argument", Arrays.asList("smiles")).toString() ).build();
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
			 MolImageDepictor depictor = MolImageDepictor.getGradientDepictor(GradientFactory.getDefaultBloomGradient());
//			 MoleculeDepictor depictor = MoleculeDepictor.getBloomDepictor();
			 depictor.setDepictLegend(true);
			 
			 BufferedImage image = depictor.depictMolecule(molToPredict, signSign.getAtomValues());
//			 BufferedImage image = depictor.depict(molToPredict, signSign.getAtomValues())
			 
			 ByteArrayOutputStream baos = new ByteArrayOutputStream();
			 ImageIO.write(image, "png", baos);
			 byte[] imageData = baos.toByteArray();
			 
			 return Response.ok( new ByteArrayInputStream(imageData) ).build();
		}
		catch (IllegalAccessException | CDKException | IOException e) {
			logger.debug("Failed predicting smiles=" + smiles, e);
			return Response.status(500).entity( new io.swagger.model.Error(500, "Server error - error during prediction").toString() ).build();
		}
		finally {
			CDKMutexLock.releaseLock();
		}
	}
}
