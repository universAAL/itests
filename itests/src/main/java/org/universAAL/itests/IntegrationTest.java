package org.universAAL.itests;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.FileUtils;
import org.ops4j.pax.scanner.ScannedBundle;
import org.ops4j.pax.scanner.bundle.internal.BundleScanner;
import org.ops4j.pax.scanner.composite.internal.CompositeScanner;
import org.ops4j.pax.scanner.internal.ProvisionServiceImpl;
import org.ops4j.util.property.PropertyResolver;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.osgi.test.AbstractConfigurableBundleCreatorTests;
import org.springframework.osgi.test.platform.Platforms;
import org.springframework.util.Assert;
import org.universAAL.itests.conf.IntegrationTestConsts;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This class has to be extended for the purpose of OSGi integration test
 * implementation. IntegrationTest extends class from Spring DM framework and
 * adds feature of setting up the TestCase with the use of Eclipse launch
 * configuration or Pax composite files.
 * 
 * Comment about logging: org.universAAL.middleware.container.utils.LogUtils
 * cannot be used because BundleContext (and therefore ModuleContext) does not
 * yet exist for the bundle in which integration tests are to be launched. Thus
 * exceptions are simple printed out to screen.
 * 
 * @author rotgier
 * 
 */
public class IntegrationTest extends AbstractConfigurableBundleCreatorTests {

    private String eclipseLaunchFile;

    private String[] paxArtifactsUrls;

    private String bundlesConfLocation;

    private boolean runArgsAreSet = false;

    private Boolean useOnlyLocalRepo;

    private int logLevel = 4;

    private Object junitTestActivator;

    private String DEFAULT_RUNDIR_TMP = "target";

    private boolean ignoreVersionMismatch = false;

    /**
     * Symbolic name of bundle in which integration tests will be performed. The
     * name is extracted from manifest.
     * 
     */
    private String bundleSymbolicName;

    /**
     * The version of bundle in which integration tests will be performed. The
     * version is extracted from manifest.
     * 
     */
    private String bundleVersion;

    /**
     * Invoking noargument constructor indicates as follows:
     * <ul>
     * <li>Default uAAL rundir, deployed to the nexus, is used
     * <li>The pax composite from project's target/artifact.composite file is
     * launched. If target/artifact.composite is not present then
     * artifact.composite in current directory (project's base directory) is
     * launched.
     * <li>The following default uAAL run arguments are used:
     * <ul>
     * <li>-Dosgi.noShutdown=true
     * <li>-Dfelix.log.level=4
     * <li>-Dorg.universAAL.middleware.peer.is_coordinator=true
     * <li>
     * -Dorg.universAAL.middleware.peer.member_of=urn:org.universAAL.aal_space
     * :test_env
     * </ul>
     * </ul>
     * If there is a need to change some of above assumptions then appropriate
     * setter method has to be invoked.
     */
    protected IntegrationTest() {
    }

    /**
     * Invoking this constructor indicates that provided eclipse launch
     * configuration is used for:
     * <ul>
     * <li>providing set of bundles which should be started,
     * <li>providing run arguments,
     * <li>providing bundles.configuration.location (uAAL runtime configuration
     * directory)
     * </ul>
     * Run arguments and bundles.configuration.location can be overridden by
     * invocation of appropriate setter methods.
     * 
     * @param eclipseLaunchConfiguration
     *            path to the launch configuration which will be used for
     *            setting up the OSGi platform in which TestCase will be
     *            executed.
     */
    protected IntegrationTest(final String eclipseLaunchConfiguration) {
	this.eclipseLaunchFile = eclipseLaunchFile;
    }

    /**
     * Method for logging.
     * 
     * @param logMsg
     *            Log message to be printed.
     */
    protected void log(final String logMsg) {
	System.out.println(logMsg);
    }

    /**
     * Formatting log messages with a use of String.format.
     * 
     * @param format
     *            Format in accordance with String.format API.
     * @param args
     *            Arguments to be printed in accordance with passed format.
     * @return Final formatted string.
     */
    protected String formatMsg(final String format, final Object... args) {
	if (args != null) {
	    return String.format(format, args);
	} else {
	    return format;
	}
    }

    /**
     * Logs all bundles.
     */
    protected void logAllBundles() {
	log("\n\n\nThe following bundles are installed in the integration testing framework:");
	int i = 1;
	for (Bundle b : bundleContext.getBundles()) {
	    log(formatMsg("     %2s. %s-%s", i++, b.getSymbolicName(),
		    ((String) b.getHeaders().get("Bundle-Version"))
			    .replaceFirst("\\.SNAPSHOT", "-SNAPSHOT")));
	}
	log("\n\n\n");
    }

    /**
     * Helper method for extracting zipped archive provided as input stream into
     * given directory.
     * 
     * @param is
     * @param destDirStr
     */
    private void unzipInpuStream(final InputStream is, final String destDirStr) {
	try {
	    File destDir = new File(destDirStr);
	    final int BUFFER = 1024;
	    BufferedOutputStream dest = null;
	    ZipInputStream zis = new ZipInputStream(new BufferedInputStream(is));
	    ZipEntry entry;
	    while ((entry = zis.getNextEntry()) != null) {
		System.out.println("Extracting: " + entry);
		if (entry.getName().startsWith("META-INF")) {
		    // META-INF (which includes MANIFEST) should not be
		    // unpacked. It should be just ignored
		    continue;
		}
		if (entry.isDirectory()) {
		    File newDir = new File(destDir, entry.getName());
		    newDir.mkdirs();
		} else {
		    int count;
		    byte[] data = new byte[BUFFER];
		    // write the files to the disk
		    FileOutputStream fos = new FileOutputStream(new File(
			    destDir, entry.getName()));
		    dest = new BufferedOutputStream(fos, BUFFER);
		    while ((count = zis.read(data, 0, BUFFER)) != -1) {
			dest.write(data, 0, count);
		    }
		    dest.flush();
		    dest.close();
		}
	    }
	    zis.close();
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    /**
     * Checks if some configuration parameters were provided and if not sets the
     * defaults.
     */
    private void setDefaults() {
	try {
	    if (useOnlyLocalRepo == null) {
		useOnlyLocalRepo = false;
	    }
	    if (eclipseLaunchFile == null) {
		if (paxArtifactsUrls == null) {
		    File generatedComposite = new File(
			    IntegrationTestConsts.TEST_COMPOSITE);
		    if (generatedComposite.exists()) {
			useOnlyLocalRepo = true;
			setPaxArtifactUrls("file:"
				+ IntegrationTestConsts.TEST_COMPOSITE);
		    } else {
			setPaxArtifactUrls("file:artifact.composite");
		    }
		}
		if (!runArgsAreSet) {
		    setRunArguments("osgi.noShutdown", "true",
			    "felix.log.level", Integer.toString(logLevel),
			    "org.universAAL.middleware.peer.is_coordinator",
			    "true");
		}
		addProtocolHandlers();
		if (bundlesConfLocation == null) {
		    URL runDirURL = new URL(IntegrationTestConsts.getRunDirMvnUrl());
		    unzipInpuStream(runDirURL.openStream(), DEFAULT_RUNDIR_TMP);
		    bundlesConfLocation = DEFAULT_RUNDIR_TMP
			    + "/rundir/confadmin";
		}
		setRunArguments("bundles.configuration.location",
			bundlesConfLocation);
	    }
	} catch (Exception ex) {
	    ex.printStackTrace();
	}
    }

    /**
     * Sets felix.log.level property. Acceptable values are as follows:
     * <ul>
     * <li>1 - LOG_ERROR: Used for error messages
     * <li>2 - LOG_WARNING: Used for warning messages. This is the default value
     * of the felix.cm.loglevel property if it is not set or if the value cannot
     * be converted to an integer.
     * <li>3 - LOG_INFO: Used for informational messages
     * <li>4 - LOG_DEBUG: Used for debug messages
     * <ul>
     * Default value is LOG_DEBUG.
     */
    protected void setLogLevel(final int level) {
	this.logLevel = level;
    }

    /**
     * Sets run arguments of uAAL platform.
     * 
     * @param args
     *            Arguments have to provided as list of strings. There has to be
     *            even number of arguments. All odd strings are interpreted as
     *            keys and all even string are interpreted as values.
     */
    protected void setRunArguments(final String... args) {
	Assert.isTrue(args.length % 2 == 0, "");
	for (int i = 0; i < args.length / 2; i++) {
	    System.setProperty(args[i], args[i + 1]);
	}
	runArgsAreSet = true;
    }

    /**
     * Sets run arguments of uAAL platform.
     * 
     * @param p
     *            Properties which represent run arguments.
     */
    protected void setRunArguments(final Properties p) {
	System.setProperties(p);
	runArgsAreSet = true;
    }

    /**
     * Sets urls of pax artifacts which should be launched for integration
     * testing.
     * 
     * @param urls
     *            List of pax artifacts urls.
     */
    protected void setPaxArtifactUrls(final String... urls) {
	List<String> paxUrls = new ArrayList<String>();
	for (String url : urls) {
	    if (url.endsWith("composite")) {
		paxUrls.add("scan-composite:" + url);
	    } else {
		paxUrls.add("scan-bundle:" + url);
	    }
	}
	this.paxArtifactsUrls = paxUrls.toArray(new String[paxUrls.size()]);
    }

    /**
     * Sets bundles.configuration.location system property.
     * 
     * @param path
     *            Path to which bundles.configuration.location is to be set.
     */
    protected void setBundleConfLocation(final String path) {
	this.bundlesConfLocation = path;
    }

    /**
     * Sets flag which can force mvn url handler to look up only local maven
     * repository. Default value is false;
     * 
     * @param useOnlyLocalRepo
     */
    protected void setUseOnlyLocalRepo(final boolean useOnlyLocalRepo) {
	this.useOnlyLocalRepo = useOnlyLocalRepo;
    }

    /**
     * If set to true than version mismatch between bundle version specified in
     * pom and bundle version specified in the manifest is ignored.
     * 
     */
    public void setIgnoreVersionMismatch(final boolean ignoreVersionMismatch) {
	this.ignoreVersionMismatch = ignoreVersionMismatch;
    }

    /**
     * Helper class used for sorting bundles from the launch configuration by
     * the runlevel.
     * 
     * @author rotgier
     * 
     */
    class BundleToLaunch {
	BundleToLaunch(final String bundleUrl, final int runLevel) {
	    this.bundleUrl = bundleUrl;
	    this.runLevel = runLevel;
	}

	String bundleUrl;
	int runLevel;
    }

    /**
     * This method registers URL Handlers for the "mvn" and "wrap" protocols.
     * Because regular API (URL.setURLStreamHandlerFactory(
     * URLStreamHandlerFactory )) provides only setting new handlers and does
     * not provide adding. Registering Handlers is hacked with the use of Java
     * Reflection.
     */
    private void addProtocolHandlers() {
	try {
	    Field handlersField = URL.class.getDeclaredField("handlers");
	    handlersField.setAccessible(true);
	    Hashtable handlers = (Hashtable) handlersField.get(null);
	    handlers.put("mvn", new MvnUrlHandler(useOnlyLocalRepo));
	    handlers.put("wrap", new org.ops4j.pax.url.wrap.Handler());
	} catch (Exception ex) {
	    ex.printStackTrace();
	    throw new RuntimeException(ex);
	}
    }

    /**
     * This method informs Spring DM framework that Felix should be used as the
     * OSGi platform for the tests.
     */
    @Override
    protected String getPlatformName() {
	return Platforms.FELIX;
    }

    /**
     * Method postProcessBundleContext has to be overridden to wrap system
     * bundleContext into a fake one. Thanks to that installing bundle can be
     * intercepted and JunitTestActivator can be created and started.
     */
    @Override
    protected void postProcessBundleContext(final BundleContext context)
	    throws Exception {
	BundleContext fakeBC = (BundleContext) Proxy.newProxyInstance(
		BundleContext.class.getClassLoader(),
		new Class[] { BundleContext.class }, new FakeBundleContext(
			context));
	super.postProcessBundleContext(fakeBC);
    }

    /**
     * This method copies contents of target/classes to target/test-classes.
     * Thanks to that regular classes of given bundle can be used for testing
     * without a need to load the bundle from maven repository. It is very
     * important because in the maven build cycle "test" precedes "install". If
     * this method will not be invoked, when bundle does not exist in the maven
     * repository there is a deadlock - bundle cannot be tested because it is
     * not in the repo and bundle cannot be installed in the repo because tests
     * fail.
     * 
     * Additionally method rewrites bundle manifest for purpose of adding
     * imports to packages related to itests bundle.
     * 
     * @throws IOException
     * 
     */
    private void prepareClassesToTests() throws Exception {
	FileUtils.copyDirectory(new File("./target/classes"), new File(
		"./target/test-classes"));
	File separatedArtifactDepsFile = new File(
		IntegrationTestConsts.SEPARATED_ARTIFACT_DEPS);
	if (separatedArtifactDepsFile.exists()) {
	    BufferedReader reader = new BufferedReader(new InputStreamReader(
		    new FileInputStream(separatedArtifactDepsFile)));
	    String line = null;
	    while ((line = reader.readLine()) != null) {
		if (!line.isEmpty()) {
		    unzipInpuStream(new URL(line).openStream(),
			    "target/test-classes");
		}
	    }
	}

	Manifest bundleMf = new Manifest(new FileInputStream(
		"./target/classes/META-INF/MANIFEST.MF"));
	Attributes mainAttribs = bundleMf.getMainAttributes();

	bundleSymbolicName = mainAttribs.getValue("Bundle-SymbolicName");
	bundleVersion = mainAttribs.getValue("Bundle-Version");
	bundleVersion = bundleVersion.replaceFirst("\\.SNAPSHOT", "-SNAPSHOT");

	mainAttribs.put(new Attributes.Name("Import-Package"),
		mainAttribs.getValue("Import-Package")
			+ ",org.universAAL.itests,org.springframework.util");
	String dynamicImports = mainAttribs.getValue("DynamicImport-Package");
	if (dynamicImports == null) {
	    dynamicImports = "*";
	    mainAttribs.put(new Attributes.Name("DynamicImport-Package"),
		    dynamicImports);
	}

	bundleMf.write(new FileOutputStream(
		"./target/test-classes/META-INF/MANIFEST.MF"));
    }

    /**
     * This method checks if artifact of given mvn url should be launched for
     * integration test. For now method unfilters only the bundle in which the
     * integration test is enclosed.
     * 
     * @param url
     * @return
     */
    private BundleToLaunch filterArtifactUrl(final String url) {
	if (url.startsWith("wrap") || url.startsWith("mvn")) {
	    String[] paxArgStrs = url.split("@");
	    String bundleUrlStr = paxArgStrs[0];
	    String[] bundleUrlArr = bundleUrlStr.split("/");
	    if (bundleUrlArr != null && bundleUrlArr.length == 3) {
		if (bundleUrlArr[1].equals(bundleSymbolicName)) {
		    if (!bundleUrlArr[2].equals(bundleVersion)) {
			if (!ignoreVersionMismatch) {
			    String msg = String
				    .format("Version mismatch! The integration test is enclosed in bundle %s-%s but bundle %s is supposed to be launched.\n"
					    + "If there is a need for integration testing of bundle %s then the test should be placed in bundle's source.\n"
					    + "You can accept version mismatch by invoking setter method before the test but be aware that in such a case\n"
					    + "source code that You see in your IDE is not the one that You are actually testing !!!",
					    bundleSymbolicName, bundleVersion,
					    url, url);
			    throw new IllegalStateException(msg);
			}
		    }
		    return null;
		}
	    }
	    int runLevel = -1;
	    if (paxArgStrs.length > 1) {
		runLevel = Integer.parseInt(paxArgStrs[1]);
	    }
	    return new BundleToLaunch(bundleUrlStr, runLevel);
	}
	return null;
    }

    /**
     * This method parses pax run arguments from eclipse launch configuration,
     * extracts list of bundles and sorts it by the runlevel. Bundles are
     * wrapped in the org.springframework.core.io.Resource class as URLs with
     * "wrap" and "mvn" protocol.
     * 
     * @param paxArgs
     *            pax run arguments from the launch configurations provided as
     *            DOM NodeList
     * @return Returns list of resources.
     * @throws IOException
     */
    private List<Resource> parsePaxArgs(final NodeList paxArgs)
	    throws IOException {
	List<BundleToLaunch> bundleList = new ArrayList<BundleToLaunch>();
	for (int i = 0; i < paxArgs.getLength(); i++) {
	    Node paxArgNode = paxArgs.item(i);
	    if (paxArgNode.getAttributes() != null) {
		Node valueAttrib = paxArgNode.getAttributes().getNamedItem(
			"value");
		if (valueAttrib != null) {
		    String paxArg = valueAttrib.getTextContent();
		    BundleToLaunch bToLaunch = filterArtifactUrl(paxArg);
		    if (bToLaunch != null) {
			bundleList.add(bToLaunch);
		    }
		}
	    }
	}
	Collections.sort(bundleList, new Comparator<BundleToLaunch>() {
	    public int compare(final BundleToLaunch o1, final BundleToLaunch o2) {
		if (o1.runLevel < o2.runLevel)
		    return -1;
		if (o1.runLevel > o2.runLevel)
		    return 1;
		return 0;
	    }

	});
	List<Resource> bundleResources = new ArrayList<Resource>();
	for (BundleToLaunch b : bundleList) {
	    bundleResources.add(new UrlResource(b.bundleUrl));
	}
	return bundleResources;
    }

    /**
     * This method parses run arguments which are passed in a regular command
     * line format (-Dkey=value ... -Dkey=value). Arguments are set as system
     * properties but only if they were not explicitly set before by means of
     * setter methods.
     * 
     * @param vmArgs
     *            Arguments to JVM.
     */
    private void parseRunArgs(final String vmArgs) {
	for (String vmArg : vmArgs.split(" ")) {
	    vmArg = vmArg.trim();
	    if (!vmArg.startsWith("-D")) {
		throw new RuntimeException(String.format(
			"vmArg %s does not start with -D", vmArg));
	    }
	    vmArg = vmArg.substring(2);
	    String[] vmArgKeyValue = vmArg.split("=");
	    if (vmArgKeyValue[0].equals("bundles.configuration.location")) {
		if (this.bundlesConfLocation != null) {
		    System.setProperty(vmArgKeyValue[0],
			    this.bundlesConfLocation);
		} else {
		    /**
		     * If bundles configuration location was not provided then
		     * the one from launch file is used.
		     */
		    System.setProperty(vmArgKeyValue[0], vmArgKeyValue[1]);
		}
	    } else {
		/*
		 * Run arguments should taken from launch configuration only if
		 * they were not provided by setter method.
		 */
		if (!runArgsAreSet) {
		    System.setProperty(vmArgKeyValue[0], vmArgKeyValue[1]);
		}
	    }
	}
    }

    /**
     * This method processes configured eclipse launch configuration. Extracts
     * list of bundles and sets run arguments as system properties.
     * 
     * @return Returns list of resources.
     */
    private List<Resource> processEclipseLaunhFile() {
	List<Resource> bundleResources = null;
	try {
	    Document doc = DocumentBuilderFactory.newInstance()
		    .newDocumentBuilder().parse(new File(eclipseLaunchFile));
	    Node root = doc.getFirstChild();
	    NodeList nodes = root.getChildNodes();
	    for (int i = 0; i < nodes.getLength(); i++) {
		Node n = nodes.item(i);
		String name = n.getNodeName();
		if (name != null && name.equals("listAttribute")) {
		    NamedNodeMap attribs = n.getAttributes();
		    Node keyAttrib = attribs.getNamedItem("key");
		    String keyAttribStr = keyAttrib.getTextContent();
		    if (keyAttribStr
			    .equals("org.ops4j.pax.cursor.runArguments")) {
			NodeList paxArgs = n.getChildNodes();
			bundleResources = parsePaxArgs(paxArgs);
		    }
		}
		if (name != null && name.equals("stringAttribute")) {
		    NamedNodeMap attribs = n.getAttributes();
		    Node keyAttrib = attribs.getNamedItem("key");
		    String keyAttribStr = keyAttrib.getTextContent();
		    if (keyAttribStr
			    .equals("org.eclipse.jdt.launching.VM_ARGUMENTS")) {
			String vmArgs = attribs.getNamedItem("value")
				.getTextContent();
			parseRunArgs(vmArgs);

		    }
		}
	    }
	    return bundleResources;
	} catch (Exception ex) {
	    ex.printStackTrace();
	    throw new RuntimeException(ex);
	}
    }

    /**
     * This method processes configured urls of pax artifacts (composites and
     * bundles) and returns single list of bundles.
     * 
     * @return Returns list of resources.
     * @throws Exception
     *             This method can throw multiple exceptions so it was
     *             aggregated to the most general one - the Exception.
     */
    private List<Resource> processPaxArtifactUrls() throws Exception {
	InvocationHandler dummyProxyHandler = new InvocationHandler() {
	    public Object invoke(final Object proxy, final Method method,
		    final Object[] args) throws Throwable {
		return null;
	    }
	};
	PropertyResolver dummyPropertyResolver = new PropertyResolver() {
	    public String get(final String arg0) {
		return null;
	    }
	};
	BundleContext dummyBundleContext = (BundleContext) Proxy
		.newProxyInstance(BundleContext.class.getClassLoader(),
			new Class[] { BundleContext.class }, dummyProxyHandler);

	ProvisionServiceImpl provisionService = new ProvisionServiceImpl(
		dummyBundleContext);
	CompositeScanner compositeScanner = new CompositeScanner(
		dummyPropertyResolver, provisionService);
	BundleScanner bundleScanner = new BundleScanner(dummyPropertyResolver);

	provisionService.addScanner(bundleScanner, "scan-bundle");
	provisionService.addScanner(compositeScanner, "scan-composite");
	List<Resource> bundleResource = new ArrayList<Resource>();
	for (String compositeUrl : paxArtifactsUrls) {
	    List<ScannedBundle> scannedBundles = provisionService
		    .scan(compositeUrl);
	    Set<String> bundlesAlreadyAdded = new HashSet<String>();
	    for (ScannedBundle scannedBundle : scannedBundles) {
		BundleToLaunch bToLaunch = filterArtifactUrl(scannedBundle
			.getLocation());
		if (bToLaunch != null) {
		    if (!bundlesAlreadyAdded.contains(bToLaunch.bundleUrl)) {
			bundlesAlreadyAdded.add(bToLaunch.bundleUrl);
			bundleResource
				.add(new UrlResource(bToLaunch.bundleUrl));
		    }
		}
	    }
	}
	return bundleResource;
    }

    /**
     * Adds additionall dependencies which are needed for launching uAAL
     * Integration Test.
     * 
     * @return Returns array of resources.
     */
    private Resource[] insertNeededDeps(final List<Resource> bundles)
	    throws Exception {
	String itestsVersion = MavenUtils.getArtifactVersion(
		"org.universAAL.support", "itests");
	bundles.add(
		0,
		new UrlResource(
			"mvn:org.apache.commons/com.springsource.org.apache.commons.io/1.4.0"));
	bundles.add(0, new UrlResource("mvn:org.universAAL.support/itests/"
		+ itestsVersion));
	bundles.add(0, new UrlResource(
		"mvn:org.ops4j.pax.url/pax-url-wrap/1.3.5"));
	bundles.add(0, new UrlResource(
		"mvn:org.ops4j.pax.url/pax-url-mvn/1.3.5"));
	bundles.add(0, new UrlResource(
		"mvn:org.ops4j.pax.url/pax-url-mvn/1.3.5"));
	return bundles.toArray(new Resource[bundles.size()]);
    }

    /**
     * This method returns sorted list of bundles which will be started for the
     * purpose of the TestCase. Method parses launch configuration provided in
     * the constructor and extracts pax run arguments as well as JVM arguments.
     * JVM arguments are then set by means of java.lang.System class. The
     * "bundles.configuration.location" JVM argument provided in the launch
     * configuration is ignored and the "bundlesConfLocation" property is used
     * instead.
     * 
     * @return Returns array of resources.
     */
    @Override
    protected Resource[] getTestBundles() {
	try {
	    setDefaults();
	    prepareClassesToTests();
	    Resource[] testBundles = null;
	    if (eclipseLaunchFile != null) {
		testBundles = insertNeededDeps(processEclipseLaunhFile());
	    } else {
		testBundles = insertNeededDeps(processPaxArtifactUrls());
	    }
	    log("Following bundles are going to be installed in itests framework:");
	    int i = 1;
	    for (Resource bundle : testBundles) {
		log(i++ + ". " + bundle.getDescription());
	    }
	    return testBundles;
	} catch (RuntimeException ex) {
	    ex.printStackTrace();
	    throw ex;
	} catch (Exception ex) {
	    ex.printStackTrace();
	    throw new RuntimeException(ex);
	}
    }

    /**
     * The class is used for intercepting installation of test bundle. When
     * interception occurs JunitTestActivator is created (using ClassLoader of
     * itests bundle) and it's start method is invoked.
     * 
     * @author rotgier
     * 
     */
    public class FakeBundleContext implements InvocationHandler {

	BundleContext systemBC;

	/**
	 * This flag is used to ensure that JunitTestActivator is created and
	 * started only once.
	 */
	private boolean initializedJunitTestActivator = false;

	public FakeBundleContext(final BundleContext bc) {
	    this.systemBC = bc;
	}

	public Object invoke(final Object proxy, final Method method,
		final Object[] args) throws Throwable {
	    Object ret = method.invoke(systemBC, args);
	    String mName = method.getName();
	    if ("installBundle".equals(mName)) {
		if (!initializedJunitTestActivator) {
		    Class junitTestActivatorClass = null;
		    for (Bundle b : systemBC.getBundles()) {
			if ("itests".equals(b.getSymbolicName())) {
			    junitTestActivatorClass = b
				    .loadClass("org.springframework.osgi.test.JUnitTestActivator");
			    junitTestActivator = junitTestActivatorClass
				    .newInstance();
			    Method activatorStartMethod = junitTestActivatorClass
				    .getMethod("start", BundleContext.class);
			    if (ret instanceof Bundle) {
				Bundle newBundle = (Bundle) ret;
				newBundle.start();
				BundleContext newBc = newBundle
					.getBundleContext();
				activatorStartMethod.invoke(junitTestActivator,
					newBc);
			    }
			    break;
			}
		    }
		    initializedJunitTestActivator = true;
		}
	    }
	    return ret;
	}

    }

}