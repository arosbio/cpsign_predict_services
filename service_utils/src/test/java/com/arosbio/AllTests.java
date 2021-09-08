package com.arosbio;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ 
	com.arosbio.api.model.TestSerialization.class,
	com.arosbio.services.utils.TestParseMolecule.class,
	})
public class AllTests {}
