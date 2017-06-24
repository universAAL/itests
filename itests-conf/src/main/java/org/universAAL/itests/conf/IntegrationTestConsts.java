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
package org.universAAL.itests.conf;

import java.net.URL;
import java.util.Properties;

/**
 * Class containing constants related to integration testing and the process of
 * generating composite files.
 *
 * @author rotgier
 *
 */
public class IntegrationTestConsts {
	public static String RUN_DIR_GROUP_ID = "org.universAAL.support";

	public static String RUN_DIR_ARTIFACT_ID = "itests-rundir";

	public static String SEPARATED_ARTIFACT_DEPS = "target/separated_artifact_deps";

	public static String TEST_COMPOSITE = "target/artifact-test.composite";

	public static String getRunDirMvnUrl() {
		String rundirVersion = MavenUtilsRunDir.getArtifactVersion(IntegrationTestConsts.RUN_DIR_GROUP_ID,
				IntegrationTestConsts.RUN_DIR_ARTIFACT_ID);
		return String.format("mvn:%s/%s/%s", IntegrationTestConsts.RUN_DIR_GROUP_ID,
				IntegrationTestConsts.RUN_DIR_ARTIFACT_ID, rundirVersion);
	}

	public static Properties getuAALMWProperties() {
		try {
			URL prop = IntegrationTestConsts.class.getResource("/uAALmw.properties");
			Properties p = new Properties();
			p.load(prop.openStream());
			return p;
		} catch (Exception e) {
			return new Properties();
		}
	}
}
