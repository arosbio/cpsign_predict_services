package suites;

import org.junit.experimental.categories.Categories;
import org.junit.experimental.categories.Categories.IncludeCategory;
import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;

import com.arosbio.AllTests;

import suites.classes.UnitTest;

@RunWith(Categories.class)
@IncludeCategory(UnitTest.class)
@SuiteClasses({
	AllTests.class
	})
public class UnitTestSuite {

}
