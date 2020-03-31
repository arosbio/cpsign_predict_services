package com.arosbio.api.rest;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.arosbio.api.rest.predict.Predict;

public class StartupPredictListener implements ServletContextListener {

	public void contextInitialized(ServletContextEvent event) {
		// causes the static initialization 
		new Predict();
	}

	public void contextDestroyed(ServletContextEvent event) {
		// Do stuff during webapp's shutdown.
	}

}
