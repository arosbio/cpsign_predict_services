package com.arosbio.services.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;

import jakarta.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openscience.cdk.interfaces.IAtomContainer;

import com.arosbio.api.model.BadRequestError;

import suites.classes.UnitTest;

@Category(UnitTest.class)
public class TestParseMolecule {
	
	@Test
	public void testParseFailingSmiles() throws Exception {
		String smiles = "C#[C+]1CCN(N)C1=N";
		String out = Utils.decodeURL(smiles);
		IAtomContainer mol = ChemUtils.parseMolOrFail(out);
		String resSmiles = ChemUtils.getAsSmiles(mol, out);
		Assert.assertEquals(smiles, resSmiles);
	}
	
	@Test
	public void testParseSMILES() throws MalformedURLException, IllegalArgumentException {
		IAtomContainer res = ChemUtils.parseMolOrFail("CCCCC=O");
		Assert.assertNotNull(res);
		Assert.assertTrue(res.getAtomCount() > 3);
	}
	
	@Test
	public void testMDLv3000() throws IOException {
		InputStream mdl = this.getClass().getResourceAsStream("/data/mdl_v3000.txt");
		String mdlStr = IOUtils.toString(mdl, StandardCharsets.UTF_8);
		
		IAtomContainer res = ChemUtils.parseMolOrFail(mdlStr);
		Assert.assertNotNull(res);
		
		Assert.assertTrue(res.getAtomCount() > 3);
	}
	
	@Test
	public void testMDLv2000() throws Exception {
		InputStream mdl = this.getClass().getResourceAsStream("/data/mdl_v2000.txt");
		String mdlStr = IOUtils.toString(mdl, StandardCharsets.UTF_8);
		
		IAtomContainer res = ChemUtils.parseMolOrFail(mdlStr); //parseMolecule(mdlStr);
		Assert.assertNotNull(res);
//		Assert.assertNull(res.getValue1());
		
		Assert.assertTrue(res.getAtomCount() > 3);
		
		Assert.assertEquals(1, ChemUtils.getAsSmiles(res, mdlStr).split("\n").length);
	}
	
	
	
	@Test
	public void testEmptyMDLFile() {
		String v2000 = "\n" + 
				"JME 2021-07-13 Wed Sep 22 10:12:45 GMT+200 2021\n" + 
				"\n" + 
				"  0  0  0  0  0  0  0  0  0  0999 V2000\n" + 
				"M  END\n" + 
				"";
		
		String v3000 = "\n" + 
				"JME 2021-07-13 Wed Sep 22 10:08:16 GMT+200 2021\n" + 
				"\n" + 
				"  0  0  0  0  0  0  0  0  0  0999 V3000\n" + 
				"M  V30 BEGIN CTAB\n" + 
				"M  V30 COUNTS 0 0 0 0 0\n" + 
				"M  V30 BEGIN ATOM\n" + 
				"M  V30 END ATOM\n" + 
				"M  V30 BEGIN BOND\n" + 
				"M  V30 END BOND\n" + 
				"M  V30 END CTAB\n" + 
				"M  END\n" + 
				"";
		validateMol(v2000);
		validateMol(v3000);
	}
	public static void validateMol(String empty) {
		
		// If failing for missing molecule
		Triple<IAtomContainer, String, Response> res = ChemUtils.validateMoleculeInput(empty, true);
		Assert.assertNull(res.getLeft());
		Assert.assertNull(res.getMiddle());
		Assert.assertNotNull(res.getRight());
		Assert.assertTrue(res.getRight().getEntity() instanceof BadRequestError);
		
		// If not failing
		res = ChemUtils.validateMoleculeInput(empty, false);
		Assert.assertNull(res.getLeft());
		Assert.assertNull(res.getMiddle());
		Assert.assertNull(res.getRight());
	}
}
