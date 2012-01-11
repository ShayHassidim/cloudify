package org.cloudifysource.dsl.internal;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.MissingMethodException;
import groovy.lang.MissingPropertyException;
import groovy.util.ConfigObject;
import groovy.util.ConfigSlurper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.Set;

import org.cloudifysource.dsl.Service;
import org.cloudifysource.dsl.context.ServiceContext;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.openspaces.admin.Admin;
import org.openspaces.core.cluster.ClusterInfo;
import org.openspaces.ui.UserInterface;


/*******
 * This class is a work in progress. DO NOT USE IT!
 * 
 * @author barakme
 * 
 */

public class DSLReader {

	private static Logger logger = Logger.getLogger(DSLReader.class.getName());

	private boolean loadUsmLib = true;

	private ClusterInfo clusterInfo;
	private Admin admin;
	private ServiceContext context;

	private String propertiesFileName;

	private boolean isRunningInGSC;

	private File dslFile;
	private File workDir;

	private String dslFileNameSuffix;

	private File propertiesFile;

	private boolean createServiceContext = true;

	private Map<String, Object> bindingProperties = new HashMap<String, Object>();

	private String dslContents;

	public static final String SERVICE_DSL_FILE_NAME_SUFFIX = "-service.groovy";
	public static final String APPLICATION_DSL_FILE_NAME_SUFFIX = "-application.groovy";
	public static final String CLOUD_DSL_FILE_NAME_SUFFIX = "-cloud.groovy";

	private static final String DSL_FILE_PATH_PROPERTY_NAME = "dslFilePath";

	private void initDslFile() throws FileNotFoundException {
		if (dslFile != null) {
			return;
		}

		if (dslContents != null) {
			return;
		}

		if (workDir == null) {
			throw new IllegalArgumentException("both dslFile and workDir are null");
		}

		if (this.dslFileNameSuffix == null) {
			throw new IllegalArgumentException("dslFileName suffix has not been set");
		}

		if (!workDir.exists()) {
			throw new FileNotFoundException("Cannot find " + workDir.getAbsolutePath());
		}

		if (!workDir.isDirectory()) {
			throw new IllegalArgumentException(workDir.getAbsolutePath() + " must be a directory");
		}

		this.dslFile = findDefaultDSLFile(dslFileNameSuffix, workDir);

	}

	public static File findDefaultDSLFile(final String fileNameSuffix, final File dir) {
		
		final File[] files = dir.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(final File dir, final String name) {
				return name.endsWith(fileNameSuffix);
			}
		});

		if (files.length > 1) {
			throw new IllegalArgumentException("Found multiple configuration files: " + Arrays.toString(files) + ". "
					+ "Only one may be supplied in the folder.");
		}

		if (files.length == 0) {
			throw new IllegalArgumentException("Cannot find configuration file in " + dir.getAbsolutePath());
		}

		return files[0];
	}

	private void init() throws IOException {
		initDslFile();
		initPropertiesFile();

	}

	public <T> T readDslEntity(Class<T> clazz) throws DSLException {

		Object result = readDslObject();
		if (result == null) {
			throw new IllegalStateException("The file " + dslFile + " evaluates to null, not to a DSL object");
		}
		if (!(clazz.isAssignableFrom(result.getClass()))) {
			throw new IllegalStateException("The file: " + dslFile + " did not evaluate to the required object type");
		}

		@SuppressWarnings("unchecked")
		T resultObject = (T) result;
		return resultObject;
	}

	public Object readDslObject() throws DSLException {
		try {
			init();
		} catch (IOException e) {
			throw new DSLException("Failed to initialize DSL Reader: " + e.getMessage(), e);
		}

		LinkedHashMap<Object, Object> properties = null;
		try {
			properties = createDSLProperties();
		} catch (Exception e) {
			// catching exception here, as groovy config slurper may throw just
			// about anything
			throw new IllegalArgumentException("Failed to load properties file " + this.propertiesFile, e);
		}

		// create an uninitialized service context
		if (this.createServiceContext) {
			this.context = new ServiceContext();
		}
		// create the groovy shell, loaded with our settings
		final GroovyShell gs = createGroovyShell(properties);
		Object result = evaluateGroovyScript(gs);

		if (this.createServiceContext) {
			if (isRunningInGSC) {
				this.context.init((Service) result, admin, workDir.getAbsolutePath(), clusterInfo);
			} else {
				this.context.initInIntegratedContainer((Service) result, workDir.getAbsolutePath());
			}
		}

		return result;
		// return new DSLServiceCompilationResult(service, ctx, dslFile);

	}

	private Object evaluateGroovyScript(final GroovyShell gs) {
		// Evaluate class using a FileReader, as the *-service files create a
		// class with an illegal name
		Object result = null;

		if (this.dslContents == null) {

			FileReader reader = null;
			try {
				reader = new FileReader(dslFile);
				result = gs.evaluate(reader, "dslEntity");
			} catch (final IOException e) {
				throw new IllegalStateException("The file " + dslFile + " could not be read", e);
			} catch (MissingMethodException e) {
				throw new IllegalArgumentException("Could not resolve DSL entry with name: " + e.getMethod(), e);
			} catch (MissingPropertyException e) {
				throw new IllegalArgumentException("Could not resolve DSL entry with name: " + e.getProperty(), e);
			} finally {
				if (reader != null) {
					try {
						reader.close();
					} catch (IOException e) {
						// ignore
					}
				}
			}
		} else {
			try {
				result = gs.evaluate(this.dslContents, "dslEntity");
			} catch (final CompilationFailedException e) {
				throw new IllegalArgumentException("The file " + dslFile + " could not be compiled", e);
			}

		}
		return result;
	}

	private void initPropertiesFile() throws IOException {
		if (this.propertiesFileName != null) {
			this.propertiesFile = new File(workDir, this.propertiesFileName);

			if (!propertiesFile.exists()) {
				throw new FileNotFoundException("Could not find properties file: " + propertiesFileName);
			}
			if (!propertiesFile.isFile()) {
				throw new FileNotFoundException(propertiesFileName + " is not a file!");
			}

			return;

		}
		
		if(this.dslFile == null) {
			return;
		}
		// look for default properties file
		// using format <dsl file name>.properties
		final String baseFileName = dslFile.getName();

		final int indexOfLastComma = baseFileName.lastIndexOf(".");
		String fileNamePrefix = null;
		if (indexOfLastComma < 0) {
			fileNamePrefix = baseFileName;
		} else {
			fileNamePrefix = baseFileName.substring(0, indexOfLastComma);
		}

		final String defaultPropertiesFileName = fileNamePrefix + ".properties";

		File defaultPropertiesFile = new File(workDir, defaultPropertiesFileName);

		if (defaultPropertiesFile.exists()) {
			this.propertiesFileName = defaultPropertiesFileName;
			this.propertiesFile = defaultPropertiesFile;
		}

	}

	@SuppressWarnings("unchecked")
	private LinkedHashMap<Object, Object> createDSLProperties() throws IOException {

		if (this.propertiesFile == null) {
			return new LinkedHashMap<Object, Object>();
		}

		try {
			ConfigObject config = new ConfigSlurper().parse(propertiesFile.toURI().toURL());

			return config;
		} catch (Exception e) {
			throw new IOException("Failed to read properties file: " + propertiesFile, e);
		}

	}

	private GroovyShell createGroovyShell(final LinkedHashMap<Object, Object> properties) {

		final String baseClassName = BaseDslScript.class.getName();

		final List<String> serviceJarFiles = createJarFileListForService();
		final CompilerConfiguration cc = createCompilerConfiguration(baseClassName, serviceJarFiles);

		final Binding binding = createGroovyBinding(properties, context, dslFile);

		final GroovyShell gs = new GroovyShell(ServiceReader.class.getClassLoader(), // this.getClass().getClassLoader(),
				binding, cc);
		return gs;
	}

	private CompilerConfiguration createCompilerConfiguration(final String baseClassName, List<String> extraJarFileNames) {
		final CompilerConfiguration cc = new CompilerConfiguration();
		final ImportCustomizer ic = new ImportCustomizer();

		ic.addStarImports(org.cloudifysource.dsl.Service.class.getPackage().getName(), UserInterface.class
				.getPackage().getName(), org.cloudifysource.dsl.context.Service.class.getPackage().getName());

		ic.addImports(org.cloudifysource.dsl.utils.ServiceUtils.class.getName());

		cc.addCompilationCustomizers(ic);

		cc.setScriptBaseClass(baseClassName);

		cc.setClasspathList(extraJarFileNames);
		return cc;
	}

	private Binding createGroovyBinding(final LinkedHashMap<Object, Object> properties, ServiceContext context,
			File dslFile) {
		final Binding binding = new Binding();

		Set<Entry<String, Object>> bindingPropertiesEntries = this.bindingProperties.entrySet();
		for (Entry<String, Object> entry : bindingPropertiesEntries) {
			binding.setVariable(entry.getKey(), entry.getValue());
		}

		if (properties != null) {
			Set<Entry<Object, Object>> entries = properties.entrySet();
			for (Entry<Object, Object> entry : entries) {
				binding.setVariable((String) entry.getKey(), entry.getValue());
			}
			if (context != null) {
				binding.setVariable("context", context);
			}
		}

		binding.setVariable(DSL_FILE_PATH_PROPERTY_NAME, (dslFile == null ? null : dslFile.getPath()));
		return binding;
	}

	private List<String> createJarFileListForService() {

		logger.fine("Adding jar files to service compile path");
		if (!this.isLoadUsmLib()) {
			logger.fine("Ignoring usmlib - external jar files will not be added to classpath!");
			// when running in GSC, the usmlib jars are placed in the PU lib dir
			// automatically
			return new ArrayList<String>(0);
		}
		if(dslFile == null) {
			logger.fine("DSL file location not specified. Skipping usmlib jar loading!");
			return new ArrayList<String>(0);
		}

		final File serviceDir = dslFile.getParentFile();
		final File usmLibDir = new File(serviceDir, CloudifyConstants.USM_LIB_DIR);
		if (!usmLibDir.exists()) {
			logger.fine("No usmlib dir was found at: " + usmLibDir + " - no jars will be added to the classpath!");
			return new ArrayList<String>(0);
		}

		if (usmLibDir.isFile()) {
			throw new IllegalArgumentException("The service includes a file called: " + CloudifyConstants.USM_LIB_DIR
					+ ". This name may only be used for a directory containing service jar files");
		}

		File[] libFiles = usmLibDir.listFiles();
		List<String> result = new ArrayList<String>(libFiles.length);
		for (File file : libFiles) {
			if (file.isFile() && file.getName().endsWith(".jar")) {
				result.add(file.getAbsolutePath());
			}
		}

		logger.fine("Extra jar files list: " + result);
		return result;
	}

	// //////////////
	// Accessors ///
	// //////////////

	public ClusterInfo getClusterInfo() {
		return clusterInfo;
	}

	public void setClusterInfo(ClusterInfo clusterInfo) {
		this.clusterInfo = clusterInfo;
	}

	public Admin getAdmin() {
		return admin;
	}

	public void setAdmin(Admin admin) {
		this.admin = admin;
	}

	public ServiceContext getContext() {
		return context;
	}

	public void setContext(ServiceContext context) {
		this.context = context;
	}

	public String getPropertiesFileName() {
		return propertiesFileName;
	}

	public void setPropertiesFileName(String propertiesFileName) {
		this.propertiesFileName = propertiesFileName;
	}

	public boolean isRunningInGSC() {
		return isRunningInGSC;
	}

	public void setRunningInGSC(boolean isRunningInGSC) {
		this.isRunningInGSC = isRunningInGSC;
	}

	public File getDslFile() {
		return dslFile;
	}

	public void setDslFile(File dslFile) {
		this.dslFile = dslFile;
	}

	public File getWorkDir() {
		return workDir;
	}

	public void setWorkDir(File workDir) {
		this.workDir = workDir;
	}

	public boolean isCreateServiceContext() {
		return createServiceContext;
	}

	public void setCreateServiceContext(boolean createServiceContext) {
		this.createServiceContext = createServiceContext;
	}

	public void addProperty(String key, Object value) {
		bindingProperties.put(key, value);

	}

	public void setDslContents(String dslContents) {
		this.dslContents = dslContents;

	}

	public String getDslFileNameSuffix() {
		return dslFileNameSuffix;
	}

	public void setDslFileNameSuffix(String dslFileNameSuffix) {
		this.dslFileNameSuffix = dslFileNameSuffix;
	}

	public boolean isLoadUsmLib() {
		return loadUsmLib;
	}

	public void setLoadUsmLib(boolean loadUsmLib) {
		this.loadUsmLib = loadUsmLib;
	}

}