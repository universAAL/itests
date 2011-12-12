package org.universAAL.itests.aether.demo;

/*******************************************************************************
 * Copyright (c) 2010-2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

import org.apache.maven.repository.internal.MavenRepositorySystemSession;
import org.ops4j.pax.url.maven.commons.MavenConfigurationImpl;
import org.ops4j.pax.url.maven.commons.MavenSettings;
import org.ops4j.pax.url.maven.commons.MavenSettingsImpl;
import org.ops4j.pax.url.mvn.ServiceConstants;
import org.ops4j.util.property.PropertiesPropertyResolver;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.repository.LocalRepository;
import org.sonatype.aether.repository.RemoteRepository;

/**
 * A helper to boot the repository system and a repository system session.
 */
public class Booter
{
    public static MavenSettings mavenSettings;
    
    static {
	final MavenConfigurationImpl config = new MavenConfigurationImpl(
		new PropertiesPropertyResolver(System.getProperties()),
		ServiceConstants.PID);
	mavenSettings = new MavenSettingsImpl(config.getSettingsFileUrl(),
		config.useFallbackRepositories());
	config.setSettings(mavenSettings);
	String repositories = mavenSettings.getRepositories();
	
    }

    public static RepositorySystem newRepositorySystem()
    {
        return ManualRepositorySystemFactory.newRepositorySystem();
    }

    public static RepositorySystemSession newRepositorySystemSession( RepositorySystem system )
    {
        MavenRepositorySystemSession session = new MavenRepositorySystemSession();

        LocalRepository localRepo = new LocalRepository( mavenSettings.getLocalRepository() );
        session.setLocalRepositoryManager( system.newLocalRepositoryManager( localRepo ) );

        session.setTransferListener( new ConsoleTransferListener() );
        session.setRepositoryListener( new ConsoleRepositoryListener() );

        // uncomment to generate dirty trees
        // session.setDependencyGraphTransformer( null );

        return session;
    }

    public static RemoteRepository newCentralRepository()
    {
        return new RemoteRepository( "central", "default", "http://repo1.maven.org/maven2/" );
    }

}
