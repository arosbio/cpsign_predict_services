package com.arosbio.api.rest.predict;

import java.io.ByteArrayInputStream;

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
						logger.debug("Failed to read molecule as MDL v3000");
						throw new IllegalArgumentException("Invalid query MDL");
					} 
				} else {
					logger.debug("Molecule in non-recognizeable format: " + moleculeData.substring(0, Math.min(100, moleculeData.length())));
					throw new IllegalArgumentException("molecule given in unrecognized format");
				}

			} else {
				// Simply a single SMILES
				try {
					return parser.parseSmiles(moleculeData);
				} catch (InvalidSmilesException | IllegalArgumentException e){
					logger.debug("Got exception when parsing smiles:\n" + Utils.getStackTrace(e));
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
}
