package com.genettasoft.api.predict;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.openscience.cdk.interfaces.IAtomContainer;

public class TestParseMolecule {
	
	@Test
	public void testParseSMILES() throws MalformedURLException, IllegalArgumentException {
		IAtomContainer res = ChemUtils.parseMolOrFail("CCCCC=O");
		Assert.assertNotNull(res);
		Assert.assertTrue(res.getAtomCount() > 3);
	}
	
	@Test
	public void testMDLv3000() throws IOException {
		InputStream mdl = this.getClass().getResourceAsStream("/resources/mdl_v3000.txt");
		String mdlStr = IOUtils.toString(mdl, StandardCharsets.UTF_8);
		
		IAtomContainer res = ChemUtils.parseMolOrFail(mdlStr);
		Assert.assertNotNull(res);
		
		Assert.assertTrue(res.getAtomCount() > 3);
	}
	
	@Test
	public void testMDLv2000() throws Exception {
		InputStream mdl = this.getClass().getResourceAsStream("/resources/mdl_v2000.txt");
		String mdlStr = IOUtils.toString(mdl, StandardCharsets.UTF_8);
		
		IAtomContainer res = ChemUtils.parseMolOrFail(mdlStr); //parseMolecule(mdlStr);
		Assert.assertNotNull(res);
//		Assert.assertNull(res.getValue1());
		
		Assert.assertTrue(res.getAtomCount() > 3);
		
		Assert.assertEquals(1, ChemUtils.getAsSmiles(res, mdlStr).split("\n").length);
	}
	
}
