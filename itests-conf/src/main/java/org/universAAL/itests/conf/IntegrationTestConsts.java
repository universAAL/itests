package org.universAAL.itests.conf;

import java.net.MalformedURLException;
import java.net.URL;

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
	String rundirVersion = MavenUtilsRunDir.getArtifactVersion(
		IntegrationTestConsts.RUN_DIR_GROUP_ID,
		IntegrationTestConsts.RUN_DIR_ARTIFACT_ID);
	return String.format("mvn:%s/%s/%s",
		IntegrationTestConsts.RUN_DIR_GROUP_ID,
		IntegrationTestConsts.RUN_DIR_ARTIFACT_ID, rundirVersion);
    }
}
