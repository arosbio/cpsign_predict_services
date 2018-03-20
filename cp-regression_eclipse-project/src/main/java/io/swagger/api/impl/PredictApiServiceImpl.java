package io.swagger.api.impl;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import com.genettasoft.api.predict.Predict;

import io.swagger.api.NotFoundException;
import io.swagger.api.PredictApiService;
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2017-10-06T12:54:15.929Z")
public class PredictApiServiceImpl extends PredictApiService {
    @Override
    public Response predictGet(String smiles, double confidence, SecurityContext securityContext) throws NotFoundException {
    		return Predict.doPredict(smiles, confidence);
    }
    
	@Override
	public Response predictImageGet(String smiles, int imageWidth, int imageHeight, Double confidence,boolean addTitle,
			SecurityContext securityContext) {
		return Predict.doPredictImage(smiles, imageWidth, imageHeight, confidence,addTitle);
	}
}
