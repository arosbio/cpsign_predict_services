package com.arosbio.impl;

import java.io.File;

import org.openscience.cdk.interfaces.IAtomContainer;

import com.arosbio.cheminf.ChemCPRegressor;
import com.arosbio.cheminf.SignificantSignature;
import com.arosbio.cheminf.data.ChemDataset;
import com.arosbio.cheminf.io.ModelSerializer;
import com.arosbio.data.FeatureVector;
import com.arosbio.services.utils.ChemUtils;

public class TestWhyFailing {
	
	// Only for debugging purposes while coding. now I think error handling works OK
//	@Test
	public void testPredictGradient() throws Exception {
		
		ChemCPRegressor model = (ChemCPRegressor) ModelSerializer.loadChemPredictor(new File("/Users/staffan/git/gs.modeling/cpsign/src/test/resources/resources/models/acp_reg_libsvm-1.5.0.cpsign").toURI(), null);
		
		IAtomContainer mol = ChemUtils.parseMolOrFail("CCCCCC");
//		SmilesParser sp = new SmilesParser(SilentChemObjectBuilder.getInstance());
		ChemDataset cp = model.getDataset();
		System.err.println("Num attr: " + cp.getNumAttributes());
		FeatureVector vec = model.getDataset().convertToFeatureVector(mol);
		System.err.println("Vector: " + vec);
		
		SignificantSignature res =  model.predictSignificantSignature(mol);
		
		System.err.println(res);
		System.err.println(res.getFullGradient());
		
	}

}
