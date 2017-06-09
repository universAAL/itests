/*
 * Copyright 2009 Alin Dreghiciu.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.universAAL.itests.conf;

import java.io.IOException;
import java.util.Properties;

/**
 * Utility methods related to Apache Maven.
 * 
 * @author Alin Dreghiciu (adreghiciu@gmail.com)
 * @since 0.3.1, March 09, 2009
 */
public class MavenUtilsRunDir {

	/**
	 * Utility class. Ment to be used via the static factory methods.
	 */
	private MavenUtilsRunDir() {
		// utility class
	}

	/**
	 * Gets the artifact version out of dependencies file. The dependencies file
	 * had to be generated by using the maven plugin.
	 * 
	 * @param groupId
	 *            artifact group id
	 * @param artifactId
	 *            artifact id
	 * 
	 * @return found version
	 * 
	 * @throws RuntimeException
	 *             - If artifact version cannot be found
	 */
	public static String getArtifactVersion(final String groupId, final String artifactId) {
		final Properties dependencies = new Properties();
		try {
			dependencies.load(MavenUtilsRunDir.class.getClassLoader().getResourceAsStream("dependencies.properties"));
			final String version = dependencies.getProperty(groupId + "/" + artifactId + "/version");
			if (version == null) {
				throw new RuntimeException("Could not resolve version. Do you have a dependency for " + groupId + "/"
						+ artifactId + " in your maven project?");
			}
			return version;
		} catch (IOException e) {
			// TODO throw a better exception
			throw new RuntimeException("Could not resolve version. Did you configured the plugin in your maven project?"
					+ "Or maybe you did not run the maven build and you are using an IDE?");
		}
	}

}
