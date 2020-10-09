package io.swagger.api;

import java.util.Arrays;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder;
import io.swagger.v3.oas.integration.OpenApiConfigurationException;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
//import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

//import io.swagger.jaxrs.config.SwaggerContextService;
//import io.swagger.models.Contact;
//import io.swagger.models.Info;
//import io.swagger.models.License;
//import io.swagger.models.Scheme;
//import io.swagger.models.Swagger;

public class Bootstrap extends HttpServlet {
	
	private static final long serialVersionUID = -3027164743593330062L;

	@Override
	public void init(ServletConfig config) throws ServletException {
		OpenAPI oas = new OpenAPI();
		Info info = new Info()
				.title("Conformal Prediction Regression Server")
				.version("1.0.2")
				.description("Service that deploys a CPSign regression model and allows for predictions to be made by the deployed model.")
				.termsOfService("")
				.contact(new Contact()
						.email("ola@arosbio.com")
						.url("arosbio.com/"))
				.license(new License()
						.name("© Aros Bio - All rights reserved")
						.url(""));
		oas.setInfo(info);
		

		String path = config.getServletContext().getContextPath() + "/v2";
//		path = path + "/v2";
		SwaggerConfiguration oasConfig = new SwaggerConfiguration()
	            .openAPI(oas);
		
		try {
		      new JaxrsOpenApiContextBuilder()
		              .servletConfig(config)
		              .openApiConfiguration(oasConfig)
		              .buildContext(true);
		    } catch (OpenApiConfigurationException e) {
		      throw new ServletException(e.getMessage(), e);
		    }

//		Swagger swagger = new Swagger().info(info).basePath(path);
//		swagger.setSchemes(Arrays.asList(Scheme.HTTP,Scheme.HTTPS) );  //new ArrayList<Scheme>(
//
//		new SwaggerContextService().withServletConfig(config).updateSwagger(swagger);
	}
}
