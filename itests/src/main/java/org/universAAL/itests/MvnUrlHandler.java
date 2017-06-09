/*
Copyright 2011-2014 AGH-UST, http://www.agh.edu.pl
Faculty of Computer Science, Electronics and Telecommunications
Department of Computer Science 

See the NOTICE file distributed with this work for additional
information regarding copyright ownership

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package org.universAAL.itests;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;

import org.ops4j.pax.url.maven.commons.MavenConfigurationImpl;
import org.ops4j.pax.url.maven.commons.MavenSettings;
import org.ops4j.pax.url.maven.commons.MavenSettingsImpl;
import org.ops4j.pax.url.mvn.Handler;
import org.ops4j.pax.url.mvn.ServiceConstants;
import org.ops4j.pax.url.mvn.internal.Connection;
import org.ops4j.util.property.PropertiesPropertyResolver;

/**
 * Class used for intercepting opening connection to maven url (such url is
 * starting with "mvn:).
 * 
 * @author rotgier
 * 
 */
public class MvnUrlHandler extends Handler {

	/**
	 * Indication that only local repository should be used in resolving
	 * artifacts.
	 */
	private boolean useOnlyLocalRepo = false;

	/**
	 * Maven settings file provided on given host.
	 */
	private MavenSettings mavenSettings;

	/**
	 * Simply the constructor.
	 * 
	 * @param useOnlyLocalRepo
	 *            Parameter for setting useOnlyLocalRepo field.
	 */
	public MvnUrlHandler(final boolean useOnlyLocalRepo) {
		this.useOnlyLocalRepo = useOnlyLocalRepo;
	}

	@Override
	protected URLConnection openConnection(final URL url) throws IOException {
		final MavenConfigurationImpl config = new MavenConfigurationImpl(
				new PropertiesPropertyResolver(System.getProperties()), ServiceConstants.PID);
		mavenSettings = new MavenSettingsImpl(config.getSettingsFileUrl(), config.useFallbackRepositories());
		config.setSettings(new ControlledMavenSettings());
		return new Connection(url, config);
	}

	/**
	 * Helper classes for intercepting referencing to MavenSettings and
	 * restricting repositories list when necessary.
	 * 
	 * @author rotgier
	 * 
	 */
	private class ControlledMavenSettings implements MavenSettings {

		public String getLocalRepository() {
			return mavenSettings.getLocalRepository();
		}

		public Map<String, Map<String, String>> getMirrorSettings() {
			return mavenSettings.getMirrorSettings();
		}

		public Map<String, Map<String, String>> getProxySettings() {
			return mavenSettings.getProxySettings();
		}

		public String getRepositories() {
			if (useOnlyLocalRepo) {
				String localRepo = getLocalRepository();
				return null;
			} else {
				String reposStr = mavenSettings.getRepositories();
				/* quick fix related to pax runner implementation mistake */
				if (!reposStr.contains("uaal")) {
					return "http://depot.universaal.org/maven-repo/releases/@id=uaal,http://depot.universaal.org/maven-repo/snapshots/@noreleases@id=uaal-snapshots@snapshots,http://depot.universaal.org/maven-repo/thirdparty/@id=uaal-thirdparty,http://depot.universaal.org/maven-repo/thirdparty/@id=iks-repository,http://osgi.sonatype.org/content/groups/pax-runner@id=paxrunner,http://repo1.maven.org/maven2@id=central,http://repository.ops4j.org/maven2@id=ops4j-releases,http://repository.springsource.com/maven/bundles/release@id=springsource-bundles-release,http://repository.springsource.com/maven/bundles/external@id=springsource-bundles-external";
				}
				// if (1==1){
				// return
				// "http://depot.universaal.org/maven-repo/releases/@id=uaal,http://depot.universaal.org/maven-repo/snapshots/@noreleases@id=uaal-snapshots@snapshots,http://depot.universaal.org/maven-repo/thirdparty/@id=uaal-thirdparty,http://depot.universaal.org/maven-repo/thirdparty/@id=iks-repository,http://osgi.sonatype.org/content/groups/pax-runner@id=paxrunner,http://repo1.maven.org/maven2@id=central,http://repository.ops4j.org/maven2@id=ops4j-releases,http://repository.springsource.com/maven/bundles/release@id=springsource-bundles-release,http://repository.springsource.com/maven/bundles/external@id=springsource-bundles-external,http://repo.aduna-software.org/maven2/releases@id=aduna";
				// }
				String[] repos = reposStr.split(",");
				StringBuilder modifiedRepos = null;
				for (String repo : repos) {
					if (modifiedRepos == null) {
						modifiedRepos = new StringBuilder();
					} else {
						modifiedRepos.append(',');
					}
					String[] repoSpecs = repo.split("@");
					StringBuilder modifiedRepo = null;
					for (String repoSpec : repoSpecs) {
						if (repoSpec.startsWith("id=")) {
							String id = repoSpec.substring(3);
							if ("uaal-snapshots".equals(id)) {
								repo = repo + "@snapshots";
							}
						}
					}
					modifiedRepos.append(repo);
				}
				return modifiedRepos.toString();
				// return reposStr;
			}
		}

	}

}
