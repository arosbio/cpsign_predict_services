package com.arosbio.services.utils;


import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;

import java.io.ByteArrayInputStream;
import java.net.MalformedURLException;
import java.util.Arrays;

import jakarta.ws.rs.core.Response;

import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.io.MDLV2000Reader;
import org.openscience.cdk.io.MDLV3000Reader;
import org.openscience.cdk.silent.AtomContainer;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmiFlavor;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.smiles.SmilesParser;
import org.slf4j.Logger;

import com.arosbio.api.model.BadRequestError;

public class ChemUtils {

	private static Logger logger = org.slf4j.LoggerFactory.getLogger(ChemUtils.class);
	private static SmilesGenerator sg = new SmilesGenerator(SmiFlavor.Canonical);
	private static final SmilesParser parser = new SmilesParser(SilentChemObjectBuilder.getInstance());

	public static IAtomContainer parseMolOrFail(String moleculeData) 
			throws IllegalArgumentException {

		try {
			CDKMutexLock.requireLock();

			if (moleculeData.split("\n",2).length > 1) {
				// MDL file format
				if (moleculeData.contains("V2000")) {
					logger.debug("molecule data given in MDL v2000 format");
					try (MDLV2000Reader reader = new MDLV2000Reader(new ByteArrayInputStream(moleculeData.getBytes()));){
						return reader.read(new AtomContainer());
					} catch (Exception | Error e) {
						logger.debug("Failed to read molecule as MDL v2000");
						throw new IllegalArgumentException("Invalid query MDL");
					} 
				} else if (moleculeData.contains("V3000")) {
					logger.debug("molecule data given in MDL v3000 format");
					try (MDLV3000Reader reader = new MDLV3000Reader(new ByteArrayInputStream(moleculeData.getBytes()));){
						return reader.read(new AtomContainer());
					} catch (Exception | Error e) {
						logger.debug("Failed to read molecule as MDL 3000");
						throw new IllegalArgumentException("Invalid query MDL");
					} 
				} else {
					logger.debug("Molecule in non-recognizable format: {}", moleculeData.substring(0, Math.min(100, moleculeData.length())));
					throw new IllegalArgumentException("molecule given in unrecognized format");
				}

			} else {
				// Simply a single SMILES
				try {
					return parser.parseSmiles(moleculeData);
				} catch (InvalidSmilesException | IllegalArgumentException e){
					logger.debug("Got exception when parsing smiles:\n{}", Utils.getStackTrace(e));
					throw new IllegalArgumentException("Invalid query SMILES '" + moleculeData + '\'');
				}  

			}
		} finally {
			CDKMutexLock.releaseLock();
		}

	}
	
	public static synchronized String getAsSmiles(IAtomContainer mol, String originalMol) throws CDKException {
		if (originalMol.split("\n",2).length == 1)
			return originalMol;
		return sg.create(mol);
	}
	
	public static Triple<IAtomContainer,String,Response> validateMoleculeInput(
			String molecule, boolean failIfMissing){
		
		if (molecule==null || molecule.isEmpty()){
			logger.debug("Missing argument 'molecule'");
			if (failIfMissing)
				return ImmutableTriple.of(null, null, 
						Utils.getResponse(
								new BadRequestError(BAD_REQUEST, "missing argument", 
										Arrays.asList("molecule")) ));
			else
				return ImmutableTriple.of(null, null, null);
		}

		String decodedMolData = null;
		try {
			decodedMolData = Utils.decodeURL(molecule);
		} catch (MalformedURLException e) {
			return ImmutableTriple.of(null,null,
					Utils.getResponse( 
							new BadRequestError(BAD_REQUEST, "Could not decode molecule text", 
									Arrays.asList("molecule"))));
		} 

		IAtomContainer molToPredict = null;
		try {
			molToPredict = ChemUtils.parseMolOrFail(decodedMolData);
		} catch (IllegalArgumentException e) {
			return ImmutableTriple.of(null,null,Utils.getResponse(
					new BadRequestError(BAD_REQUEST, e.getMessage(), Arrays.asList("molecule")) ));
		}
		
		// Check again if empty mol - e.g. a valid MOL file but with no atoms
		if (molToPredict.getAtomCount() == 0) {
			logger.debug("Empty molecule sent, possibly as MDL");
			if (failIfMissing)
				return ImmutableTriple.of(null, null, 
						Utils.getResponse(
								new BadRequestError(BAD_REQUEST, "missing argument", 
										Arrays.asList("molecule")) ));
			else
				return ImmutableTriple.of(null, null, null);
		}

		// Generate SMILES to have in the response
		String smiles = null;
		try {
			smiles = ChemUtils.getAsSmiles(molToPredict, decodedMolData);
			logger.debug("prediction-task for smiles={}", smiles);
		} catch (Exception e) {
			smiles = "<no SMILES available>";
			logger.debug("Failed getting smiles:\n\t"+Utils.getStackTrace(e));
		}
		
		// Here everything should be OK
		return ImmutableTriple.of(molToPredict, smiles, null);
	}
}
