package com.arosbio.impl;

import java.io.File;

import org.openscience.cdk.interfaces.IAtomContainer;

import com.arosbio.modeling.CPSignFactory;
import com.arosbio.modeling.cheminf.ChemicalProblem;
import com.arosbio.modeling.cheminf.SignaturesCPRegression;
import com.arosbio.modeling.cheminf.SignaturesCPRegression.SignificantSignatureCPRegression;
import com.arosbio.modeling.data.FeatureVector;
import com.arosbio.modeling.io.ModelLoader;
import com.arosbio.services.utils.ChemUtils;

public class TestWhyFailing {
	
	// Only for debugging purposes while coding. now I think error handling works OK
//	@Test
	public void testPredictGradient() throws Exception {
		new CPSignFactory( new File("/Users/staffan/git/gs.modeling/cpsign/src/test/resources/resources/auth/cpsign-1.0-develop-standard.license").toURI() );
		
		SignaturesCPRegression model = (SignaturesCPRegression) ModelLoader.loadModel(new File("/Users/staffan/git/gs.modeling/cpsign/src/test/resources/resources/models/acp_reg_libsvm-1.5.0.cpsign").toURI(), null);
		
		IAtomContainer mol = ChemUtils.parseMolOrFail("CCCCCC");
//		SmilesParser sp = new SmilesParser(SilentChemObjectBuilder.getInstance());
		ChemicalProblem cp = model.getProblem();
		System.err.println("Num attr: " + cp.getNumAttributes());
		FeatureVector vec = model.getProblem().convertToFeatureVector(mol);
		System.err.println("Vector: " + vec);
		
		SignificantSignatureCPRegression res =  model.predictSignificantSignature(mol);
		
		System.err.println(res);
		System.err.println(res.getMoleculeGradient());
		
	}

}
