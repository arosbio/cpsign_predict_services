package io.swagger.api;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

public abstract class PredictApiService {
    public abstract Response predictGet(String smiles, double confidence, SecurityContext securityContext) throws NotFoundException;
	public abstract Response predictImageGet(String smiles, int imageWidth, int imageHeight, Double confidence, boolean addTitle, SecurityContext securityContext);
}
