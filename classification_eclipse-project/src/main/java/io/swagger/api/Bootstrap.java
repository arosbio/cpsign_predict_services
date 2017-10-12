package io.swagger.api;

import io.swagger.jaxrs.config.SwaggerContextService;
import io.swagger.models.*;

import io.swagger.models.auth.*;

import javax.servlet.http.HttpServlet;
import javax.servlet.ServletContext;

import java.util.ArrayList;
import java.util.Arrays;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

public class Bootstrap extends HttpServlet {
  @Override
  public void init(ServletConfig config) throws ServletException {
    Info info = new Info()
      .title("Swagger Server")
      .description("Service that deploys a CPSign classification model and allows for predictions to be made by the deployed model.")
      .termsOfService("")
      .contact(new Contact()
        .email("info@genettasoft.com"))
      .license(new License()
        .name("")
        .url(""));
    
    String path = config.getServletContext().getContextPath();
    path = path + "cpsign.predict.rest.classification-1.0.0/v1";
    

    Swagger swagger = new Swagger().info(info).basePath(path);
    swagger.setSchemes( new ArrayList<Scheme>(Arrays.asList(Scheme.HTTP)) );

    new SwaggerContextService().withServletConfig(config).updateSwagger(swagger);
  }
}
