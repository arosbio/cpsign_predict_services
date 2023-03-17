package com.arosbio.impl;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

public class StartupPredictListener implements ServletContextListener {

	public void contextInitialized(ServletContextEvent event) {
		// causes the static initialization 
		new Predict();
	}

	public void contextDestroyed(ServletContextEvent event) {
		// Do stuff during webapp's shutdown.
	}

}
