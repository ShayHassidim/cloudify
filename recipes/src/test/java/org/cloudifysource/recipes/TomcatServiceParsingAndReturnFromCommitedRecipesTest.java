package org.cloudifysource.recipes;

import java.io.File;

import org.cloudifysource.dsl.Service;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.junit.Test;


public class TomcatServiceParsingAndReturnFromCommitedRecipesTest {
	
	private final static String LEGAL_RESOURCES_PATH = "target/classes/tomcat/";
	private String nameInGroovy = "tomcat";
	private File tomcatDslFile;
	private File tomcatWorkDir;
	private Service service;
	
	@Test
	public void fullyConfigedGroovy() throws Exception
    {
		tomcatDslFile = new File(LEGAL_RESOURCES_PATH + "tomcat-service.groovy");
		tomcatWorkDir = new File(LEGAL_RESOURCES_PATH);
		service = ServiceReader.getServiceFromFile(tomcatDslFile, tomcatWorkDir).getService();
		ServiceTestUtil.validateName(service , nameInGroovy);
		ServiceTestUtil.validateIcon(service);
    }
	
	@Test
	public void getServiceFromDirInvocation() throws Exception
    {
		tomcatWorkDir = new File(LEGAL_RESOURCES_PATH);
		service = ServiceReader.getServiceFromDirectory(tomcatWorkDir, CloudifyConstants.DEFAULT_APPLICATION_NAME).getService();
		ServiceTestUtil.validateName(service , nameInGroovy);
		ServiceTestUtil.validateIcon(service);
    }
	
}