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

public class MvnUrlHandler extends Handler {

    private boolean useOnlyLocalRepo;

    private MavenSettings mavenSettings;

    public MvnUrlHandler(boolean useOnlyLocalRepo) {
	this.useOnlyLocalRepo = useOnlyLocalRepo;
    }

    @Override
    protected URLConnection openConnection(final URL url) throws IOException {
	final MavenConfigurationImpl config = new MavenConfigurationImpl(
		new PropertiesPropertyResolver(System.getProperties()),
		ServiceConstants.PID);
	mavenSettings = new MavenSettingsImpl(config.getSettingsFileUrl(),
		config.useFallbackRepositories());
	config.setSettings(new ControlledMavenSettings());
	return new Connection(url, config);
    }

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
		String [] repos = reposStr.split(",");
		StringBuilder modifiedRepos = null;
		for (String repo : repos) {
		    if (modifiedRepos == null) {
			modifiedRepos = new StringBuilder();
		    } else {
			modifiedRepos.append(',');
		    }
		    String [] repoSpecs = repo.split("@");
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
		//return reposStr;
	    }
	}

    }

}
