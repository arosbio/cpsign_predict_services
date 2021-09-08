package com.arosbio.services.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openscience.cdk.interfaces.IAtomContainer;

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
	
}
