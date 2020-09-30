package io.swagger.api;

import java.util.ArrayList;
import java.util.Arrays;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import io.swagger.jaxrs.config.SwaggerContextService;
import io.swagger.models.Contact;
import io.swagger.models.Info;
import io.swagger.models.License;
import io.swagger.models.Scheme;
import io.swagger.models.Swagger;

public class Bootstrap extends HttpServlet {
	
	private static final long serialVersionUID = -3027164743593330062L;

	@Override
	public void init(ServletConfig config) throws ServletException {
		Info info = new Info()
				.title("Conformal Prediction Regression Server")
				.version("0.1.1")
				.description("Service that deploys a CPSign regression model and allows for predictions to be made by the deployed model.")
				.termsOfService("")
				.contact(new Contact()
						.email("ola@arosbio.com")
						.url("arosbio.com/"))
				.license(new License()
						.name("© Aros Bio - All rights reserved")
						.url(""));

		String path = config.getServletContext().getContextPath();
		path = path + "/v1";


		Swagger swagger = new Swagger().info(info).basePath(path);
		swagger.setSchemes( new ArrayList<Scheme>(Arrays.asList(Scheme.HTTP,Scheme.HTTPS)) );

		new SwaggerContextService().withServletConfig(config).updateSwagger(swagger);
	}
}
