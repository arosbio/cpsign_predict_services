package io.swagger.api.impl;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import com.genettasoft.api.predict.Predict;

import io.swagger.api.NotFoundException;
import io.swagger.api.PredictApiService;
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2017-10-06T12:54:15.929Z")
public class PredictApiServiceImpl extends PredictApiService {
    @Override
    public Response predictPost(String smiles, SecurityContext securityContext) throws NotFoundException {
    		return Predict.doPredict(smiles);
    }
    
	@Override
	public Response predictImagePost(String smiles, int imageWidth, int imageHeight, 
			boolean addPvalsLabel, boolean addTitle,
			SecurityContext securityContext) {
		return Predict.doPredictImage(smiles, imageWidth, imageHeight, addPvalsLabel, addTitle);
	}
}
