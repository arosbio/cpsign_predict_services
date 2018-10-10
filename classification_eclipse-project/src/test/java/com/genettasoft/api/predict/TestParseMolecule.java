package com.genettasoft.api.predict;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.javatuples.Pair;
import org.junit.Assert;
import org.junit.Test;
import org.openscience.cdk.interfaces.IAtomContainer;

public class TestParseMolecule {
	
	@Test
	public void testParseSMILES() {
		Pair<IAtomContainer, Response> res = ChemUtils.parseMolecule("CCCCC=O");
		Assert.assertNotNull(res.getValue0());
		Assert.assertNull(res.getValue1());
	}
	
	@Test
	public void testMDLv3000() throws IOException {
		InputStream mdl = this.getClass().getResourceAsStream("/resources/mdl_v3000.txt");
		String mdlStr = IOUtils.toString(mdl, StandardCharsets.UTF_8);
		
		Pair<IAtomContainer, Response> res = ChemUtils.parseMolecule(mdlStr);
		Assert.assertNotNull(res.getValue0());
		Assert.assertNull(res.getValue1());
		
		Assert.assertTrue(res.getValue0().getAtomCount() > 3);
	}
	
	@Test
	public void testMDLv2000() throws Exception {
		InputStream mdl = this.getClass().getResourceAsStream("/resources/mdl_v2000.txt");
		String mdlStr = IOUtils.toString(mdl, StandardCharsets.UTF_8);
		
		Pair<IAtomContainer, Response> res = ChemUtils.parseMolecule(mdlStr);
		Assert.assertNotNull(res.getValue0());
		Assert.assertNull(res.getValue1());
		
		Assert.assertTrue(res.getValue0().getAtomCount() > 3);
	}
}
