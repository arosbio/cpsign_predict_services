package io.swagger.api;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

public abstract class PredictApiService {
    public abstract Response predictPost(String smiles,SecurityContext securityContext) throws NotFoundException;
	public abstract Response predictImagePost(String smiles, int imageWidth, int imageHeight, boolean addPvalsLabel, boolean addTitle, SecurityContext securityContext);
}
