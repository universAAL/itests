package org.universAAL.itests.aether.demo;

/*******************************************************************************
 * Copyright (c) 2010-2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.apache.maven.repository.internal.DefaultArtifactDescriptorReader;
import org.apache.maven.repository.internal.MavenRepositorySystemSession;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.collection.CollectRequest;
import org.sonatype.aether.collection.DependencyGraphTransformer;
import org.sonatype.aether.deployment.DeployRequest;
import org.sonatype.aether.deployment.DeploymentException;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.graph.DependencyVisitor;
import org.sonatype.aether.impl.ArtifactDescriptorReader;
import org.sonatype.aether.installation.InstallRequest;
import org.sonatype.aether.installation.InstallationException;
import org.sonatype.aether.repository.Authentication;
import org.sonatype.aether.repository.LocalRepository;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.ArtifactDescriptorRequest;
import org.sonatype.aether.resolution.ArtifactDescriptorResult;
import org.sonatype.aether.resolution.DependencyRequest;
import org.sonatype.aether.resolution.DependencyResolutionException;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.util.graph.PreorderNodeListGenerator;
import org.sonatype.aether.util.graph.transformer.ChainedDependencyGraphTransformer;

public class Aether
{
    //private String remoteRepository;

    private RepositorySystem repositorySystem;

    private LocalRepository localRepository;

    public Aether(String localRepository )
    {
        //this.remoteRepository = remoteRepository;
        this.repositorySystem = Booter.newRepositorySystem();
        this.localRepository = new LocalRepository( localRepository );
    }

    private RepositorySystemSession newSession()
    {
        MavenRepositorySystemSession session = new MavenRepositorySystemSession();
        session.setLocalRepositoryManager( repositorySystem.newLocalRepositoryManager( localRepository ) );
        session.setTransferListener( new ConsoleTransferListener() );
        session.setRepositoryListener( new ConsoleRepositoryListener() );
        return session;
    }

    public AetherResult resolve( String groupId, String artifactId, String version )
        throws DependencyResolutionException
    {
        RepositorySystemSession session = newSession();
        Dependency dependency =
            new Dependency( new DefaultArtifact( groupId, artifactId, "", "jar", version ), "runtime" );
        RemoteRepository central = new RemoteRepository( "central", "default", "http://repo1.maven.org/maven2/" );
        RemoteRepository uAALSnapshotRepo = new RemoteRepository( "uaal-snapshots", "default", "http://depot.universaal.org/maven-repo/snapshots/" );
        
        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot( dependency );
        collectRequest.addRepository( uAALSnapshotRepo );
        collectRequest.addRepository( central );
        
        

        DependencyRequest dependencyRequest = new DependencyRequest();
        dependencyRequest.setCollectRequest( collectRequest );
        DependencyGraphTransformer depTransformer = session.getDependencyGraphTransformer();
        ChainedDependencyGraphTransformer cdepTransformer = (ChainedDependencyGraphTransformer) depTransformer;
        MavenRepositorySystemSession msession = (MavenRepositorySystemSession) session;
	msession
		.setDependencyGraphTransformer(new ChainedDependencyGraphTransformer(
			//new ConflictMarker(),
			//new JavaEffectiveScopeCalculator(),
			//new NearestVersionConflictResolver(),
			//new JavaDependencyContextRefiner()
			));

        DependencyNode rootNode = repositorySystem.resolveDependencies( session, dependencyRequest ).getRoot();
        

        StringBuilder dump = new StringBuilder();
        displayTree( rootNode, dump );

        PreorderNodeListGenerator nlg = new PreorderNodeListGenerator();
        rootNode.accept( nlg );

        return new AetherResult( rootNode, nlg.getFiles(), nlg.getClassPath() );
    }

    public void install( Artifact artifact, Artifact pom )
        throws InstallationException
    {
        RepositorySystemSession session = newSession();

        InstallRequest installRequest = new InstallRequest();
        installRequest.addArtifact( artifact ).addArtifact( pom );

        repositorySystem.install( session, installRequest );
    }

    public void deploy( Artifact artifact, Artifact pom, String remoteRepository )
        throws DeploymentException
    {
        RepositorySystemSession session = newSession();

        RemoteRepository nexus = new RemoteRepository( "nexus", "default", remoteRepository );
        Authentication authentication = new Authentication( "admin", "admin123" );
        nexus.setAuthentication( authentication );

        DeployRequest deployRequest = new DeployRequest();
        deployRequest.addArtifact( artifact ).addArtifact( pom );
        deployRequest.setRepository( nexus );

        repositorySystem.deploy( session, deployRequest );
    }

    private void displayTree( DependencyNode node, StringBuilder sb )
    {
        ByteArrayOutputStream os = new ByteArrayOutputStream( 1024 );
        node.accept( new ConsoleDependencyGraphDumper( new PrintStream( os ) ) );
        sb.append( os.toString() );
        System.out.println(os.toString());
    }
    
    
    public void test() throws DependencyResolutionException  {
	//AetherResult r = resolve("org.universAAL.ontology", "ont.risk", "0.1.0-SNAPSHOT");
	AetherResult r = resolve("org.universAAL.samples", "smp.lighting.client", "0.3.2-SNAPSHOT");
	
	final Set<String> alreadyVisited = new HashSet<String>();
	List<String> artifactsToRun = new ArrayList<String>();
	r.getRoot().accept(new DependencyVisitor(){

	    public boolean visitEnter(DependencyNode node) {
		// TODO Auto-generated method stub
		return true;
	    }

	    public boolean visitLeave(DependencyNode node) {
		Artifact dep = node.getDependency().getArtifact();
		boolean snapshot = dep.isSnapshot();
		String depUrl = String.format("mvn:%s/%s/%s",dep.getGroupId(),dep.getArtifactId(),dep.getBaseVersion());
		if (!alreadyVisited.contains(depUrl)) {
		    System.out.println("    !!!!!!     " + depUrl);
		    //System.out.println(dep.getProperties());
		    File f = dep.getFile();
		    alreadyVisited.add(depUrl);
		    
	            ArtifactDescriptorResult descriptorResult;
	            try
	            {
	                ArtifactDescriptorRequest descriptorRequest = new ArtifactDescriptorRequest();
	                descriptorResult = new ArtifactDescriptorResult( descriptorRequest );
	                descriptorRequest.setArtifact( dep );
			RemoteRepository[] reposArrays = new RemoteRepository[] {
				new RemoteRepository("central", "default",
					"http://repo1.maven.org/maven2/"),
				new RemoteRepository("uaal-snapshots",
					"default",
					"http://depot.universaal.org/maven-repo/snapshots/"),
				new RemoteRepository("uaal-thirdparty",
					"default",
					"http://depot.universaal.org/maven-repo/thirdparty/"), };
	                descriptorRequest.setRepositories( Arrays.asList(reposArrays) );
	                //descriptorRequest.setRequestContext( request.getRequestContext() );
	                //descriptorRequest.setTrace( trace );
	                //if ( isLackingDescriptor( root.getArtifact() ) )
	                RepositorySystemSession session = newSession();
			DefaultArtifactDescriptorReader descriptorReader = (DefaultArtifactDescriptorReader) ManualRepositorySystemFactory.locator
				.getService(ArtifactDescriptorReader.class);
	                //descriptorResult = descriptorReader.readArtifactDescriptor( session, descriptorRequest );
			Method loadPomMethod = descriptorReader.getClass().getDeclaredMethod(
				"loadPom", RepositorySystemSession.class,
				ArtifactDescriptorRequest.class,
				ArtifactDescriptorResult.class);
			loadPomMethod.setAccessible(true);
			Model pomModel = (Model) loadPomMethod.invoke(
				descriptorReader, session, descriptorRequest,
				descriptorResult);
			List<Profile> profiles = pomModel.getProfiles();
			for (Profile p : profiles) {
			    String id = p.getId();
			    if ("uAAL-Runtime".equals(id)) {
				for (org.apache.maven.model.Dependency runtimeDep : p.getDependencies()) {
				    System.out.println(String.format(
					    "    !!!!!!     !!!!!!  Runtime dep: %s/%s/%s", runtimeDep
						    .getGroupId(), runtimeDep
						    .getArtifactId(),
					    runtimeDep.getVersion()));
				    
				}
			    }
			}
			int i = 2;
			i++;
	            } catch (Exception ex) {
	        	ex.printStackTrace();
	            }
		}
		return true;
	    }
	    
	});
	//AetherResult r = resolve("org.universAAL.middleware", "mw.acl.upnp", "0.2.1-SNAPSHOT");
    }
    
    public static void main(String [] args) throws DependencyResolutionException {
	Aether a = new Aether(Booter.mavenSettings.getLocalRepository());
	a.test();
    }

}
