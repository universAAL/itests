/*******************************************************************************
 * Copyright 2013 Universidad Politécnica de Madrid
 * Copyright 2013 Fraunhofer-Gesellschaft - Institute for Computer Graphics Research
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package org.universAAL.itests.platform;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.felix.main.AutoProcessor;
import org.apache.felix.main.Main;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.springframework.osgi.test.platform.OsgiPlatform;
import org.universAAL.itests.conf.IntegrationTestConsts;

/**
 * @author amedrano
 *
 */
public class FelixPlatform4_2 implements OsgiPlatform {

    private static final String bundleDir = "./bundle";
    private static final String cacheDir = "./cache";
    private Framework m_fwk;
    private Properties configurationProperties;

    /** {@ inheritDoc}	 */
    public void start() throws Exception {
	try
        {
            // (8) Create an instance and initialize the framework.
//            FrameworkFactory factory = getFrameworkFactory();
	    FrameworkFactory factory = new org.apache.felix.framework.FrameworkFactory();
            HashMap<String, String> cfg = new HashMap<String, String>();
            for (Object k : configurationProperties.keySet()) {
		cfg.put(k.toString(), configurationProperties.get(k).toString());
		//System.out.println(k + " -> " + configurationProperties.get(k));
	    }
            m_fwk = factory.newFramework(cfg);
            m_fwk.init();
            // (9) Use the system bundle context to process the auto-deploy
            // and auto-install/auto-start properties.
            AutoProcessor.process(configurationProperties, m_fwk.getBundleContext());
            // (10) Start the framework.
            m_fwk.start();
            // (11) Wait for framework to stop to exit the VM.
            //m_fwk.waitForStop(0);
        }
        catch (Exception ex)
        {
            System.err.println("Could not create framework: " + ex);
            ex.printStackTrace();
            System.exit(0);
        }

    }

    /** {@ inheritDoc}	 */
    public void stop() throws Exception {
	if (m_fwk != null)
	    m_fwk.stop();

    }
	/**
	 * {@inheritDoc}
	 * 
	 * This implementation considers existing system properties as well as
	 * platform specific ones, defined in this class. The system properties are
	 * convenient for changing the configuration directly from the command line
	 * (useful for CI builds) leaving the programmer to ultimately decide the
	 * actual configuration used.
	 */
	public Properties getConfigurationProperties() {
		// check if defaults should apply
		if (configurationProperties == null) {
			configurationProperties = new Properties();
			// system properties
			configurationProperties.putAll(System.getProperties());
			// local properties
			configurationProperties.putAll(getPlatformProperties());
			// uAAL properties
			configurationProperties.putAll(IntegrationTestConsts.getuAALMWProperties());
			return configurationProperties;
		}
		return configurationProperties;
	}
	

    /** {@ inheritDoc}	 */
    public BundleContext getBundleContext() {
	if (m_fwk != null)
	    return m_fwk.getBundleContext();
	return null;
    }
    
    private Properties getPlatformProperties(){
	// (2) Load system properties.
        Main.loadSystemProperties();

        System.setProperty("felix.config.properties", Main.class.getClassLoader().getResource("default.properties").toString());
        
        //System.out.println("Felix config Properties is : " + System.getProperty("felix.config.properties"));
        
        Properties configProps = new Properties();
        Map<String, String> config = Main.loadConfigProperties();
        if (config != null) {
	    for (String k : config.keySet()) {
		if (k != null && config.get(k) != null){
		    configProps.put(k, config.get(k));
		    //System.out.println(k + " -> " + config.get(k));
		}
	    }
	}
	// (4) Copy framework properties from the system properties.
        Main.copySystemProperties(configProps);
            
        // (5) Use the specified auto-deploy directory over default.
        configProps.setProperty(AutoProcessor.AUTO_DEPLOY_DIR_PROPERY, bundleDir);

        // (6) Use the specified bundle cache directory over default.
        configProps.setProperty(Constants.FRAMEWORK_STORAGE, cacheDir);
        return configProps;
    }
}
