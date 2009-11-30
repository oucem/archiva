package org.apache.archiva.web.xmlrpc.services;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.archiva.indexer.search.RepositorySearch;
import org.apache.archiva.indexer.search.SearchResultHit;
import org.apache.archiva.indexer.search.SearchResultLimits;
import org.apache.archiva.indexer.search.SearchResults;
import org.apache.archiva.indexer.util.SearchUtil;
import org.apache.archiva.metadata.model.ProjectVersionMetadata;
import org.apache.archiva.metadata.model.ProjectVersionReference;
import org.apache.archiva.metadata.repository.MetadataResolver;
import org.apache.archiva.metadata.repository.storage.maven2.MavenProjectFacet;
import org.apache.archiva.web.xmlrpc.api.SearchService;
import org.apache.archiva.web.xmlrpc.api.beans.Artifact;
import org.apache.archiva.web.xmlrpc.api.beans.Dependency;
import org.apache.archiva.web.xmlrpc.security.XmlRpcUserRepositories;
import org.apache.maven.archiva.database.ArchivaDAO;
import org.apache.maven.archiva.database.ArtifactDAO;
import org.apache.maven.archiva.database.constraints.ArtifactsByChecksumConstraint;
import org.apache.maven.archiva.database.constraints.UniqueVersionConstraint;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.model.ArchivaProjectModel;
import org.codehaus.plexus.spring.PlexusInSpringTestCase;
import org.easymock.MockControl;
import org.easymock.classextension.MockClassControl;

/**
 * SearchServiceImplTest
 *
 * @version $Id: SearchServiceImplTest.java
 */
public class SearchServiceImplTest
    extends PlexusInSpringTestCase
{
    private SearchService searchService;

    private MockControl userReposControl;

    private XmlRpcUserRepositories userRepos;

    private MockControl searchControl;

    private RepositorySearch search;

    private MockControl archivaDAOControl;

    private ArchivaDAO archivaDAO;

    private MockControl artifactDAOControl;

    private ArtifactDAO artifactDAO;

    private static final String ARCHIVA_TEST_ARTIFACT_ID = "archiva-xmlrpc-test";

    private static final String ARCHIVA_TEST_GROUP_ID = "org.apache.archiva";

    private MockControl metadataResolverControl;

    private MetadataResolver metadataResolver;

    @Override
    public void setUp()
        throws Exception
    {
        userReposControl = MockClassControl.createControl( XmlRpcUserRepositories.class );
        userRepos = (XmlRpcUserRepositories) userReposControl.getMock();

        archivaDAOControl = MockControl.createControl( ArchivaDAO.class );
        archivaDAOControl.setDefaultMatcher( MockControl.ALWAYS_MATCHER );
        archivaDAO = (ArchivaDAO) archivaDAOControl.getMock();

        searchControl = MockControl.createControl( RepositorySearch.class );
        searchControl.setDefaultMatcher( MockControl.ALWAYS_MATCHER );
        search = (RepositorySearch) searchControl.getMock();

        metadataResolverControl = MockControl.createControl( MetadataResolver.class );
        metadataResolver = (MetadataResolver) metadataResolverControl.getMock();

        artifactDAOControl = MockControl.createControl( ArtifactDAO.class );
        artifactDAO = (ArtifactDAO) artifactDAOControl.getMock();

        searchService = new SearchServiceImpl( userRepos, archivaDAO, metadataResolver, search );
    }

    // MRM-1230
    public void testQuickSearchModelPackagingIsUsed()
        throws Exception
    {
        List<String> observableRepoIds = new ArrayList<String>();
        observableRepoIds.add( "repo1.mirror" );
        observableRepoIds.add( "public.releases" );

        userReposControl.expectAndReturn( userRepos.getObservableRepositories(), observableRepoIds );

        SearchResults results = new SearchResults();
        List<String> versions = new ArrayList<String>();
        versions.add( "1.0" );

        SearchResultHit resultHit = new SearchResultHit();
        resultHit.setGroupId( ARCHIVA_TEST_GROUP_ID );
        resultHit.setArtifactId( "archiva-webapp" );
        resultHit.setVersions( versions );
        resultHit.setRepositoryId( null );

        results.addHit( SearchUtil.getHitId( ARCHIVA_TEST_GROUP_ID, "archiva-webapp" ), resultHit );

        SearchResultLimits limits = new SearchResultLimits( SearchResultLimits.ALL_PAGES );

        searchControl.expectAndDefaultReturn( search.search( "", observableRepoIds, "archiva", limits, null ),
                                              results );

        archivaDAOControl.expectAndReturn( archivaDAO.query(
            new UniqueVersionConstraint( observableRepoIds, resultHit.getGroupId(), resultHit.getArtifactId() ) ),
                                           null );

        ProjectVersionMetadata model = new ProjectVersionMetadata();
        model.setId( "1.0" );
        MavenProjectFacet facet = new MavenProjectFacet();
        facet.setPackaging( "war" );
        model.addFacet( facet );

        metadataResolverControl.expectAndReturn(
            metadataResolver.getProjectVersion( "repo1.mirror", ARCHIVA_TEST_GROUP_ID, "archiva-webapp", "1.0" ),
            model );

        userReposControl.replay();
        searchControl.replay();
        metadataResolverControl.replay();
        archivaDAOControl.replay();

        List<Artifact> artifacts = searchService.quickSearch( "archiva" );

        userReposControl.verify();
        searchControl.verify();
        metadataResolverControl.verify();
        archivaDAOControl.verify();

        assertNotNull( artifacts );
        assertEquals( 1, artifacts.size() );

        Artifact artifact = artifacts.get( 0 );
        assertEquals( ARCHIVA_TEST_GROUP_ID, artifact.getGroupId() );
        assertEquals( "archiva-webapp", artifact.getArtifactId() );
        assertEquals( "1.0", artifact.getVersion() );
        assertEquals( "war", artifact.getType() );
        assertNotNull( "Repository should not be null!", artifact.getRepositoryId() );
        assertEquals( "repo1.mirror", artifact.getRepositoryId() );
    }

    public void testQuickSearchDefaultPackagingIsUsed()
        throws Exception
    {
        List<String> observableRepoIds = new ArrayList<String>();
        observableRepoIds.add( "repo1.mirror" );
        observableRepoIds.add( "public.releases" );

        userReposControl.expectAndReturn( userRepos.getObservableRepositories(), observableRepoIds );

        SearchResults results = new SearchResults();
        List<String> versions = new ArrayList<String>();
        versions.add( "1.0" );

        SearchResultHit resultHit = new SearchResultHit();
        resultHit.setRepositoryId( null );
        resultHit.setGroupId( ARCHIVA_TEST_GROUP_ID );
        resultHit.setArtifactId( ARCHIVA_TEST_ARTIFACT_ID );
        resultHit.setVersions( versions );

        results.addHit( SearchUtil.getHitId( ARCHIVA_TEST_GROUP_ID, ARCHIVA_TEST_ARTIFACT_ID ), resultHit );

        SearchResultLimits limits = new SearchResultLimits( SearchResultLimits.ALL_PAGES );

        searchControl.expectAndDefaultReturn( search.search( "", observableRepoIds, "archiva", limits, null ),
                                              results );

        archivaDAOControl.expectAndReturn( archivaDAO.query(
            new UniqueVersionConstraint( observableRepoIds, resultHit.getGroupId(), resultHit.getArtifactId() ) ),
                                           null );

        metadataResolverControl.expectAndReturn(
            metadataResolver.getProjectVersion( "repo1.mirror", ARCHIVA_TEST_GROUP_ID, ARCHIVA_TEST_ARTIFACT_ID,
                                                "1.0" ), null );

        ProjectVersionMetadata model = new ProjectVersionMetadata();
        model.setId( "1.0" );
        metadataResolverControl.expectAndReturn(
            metadataResolver.getProjectVersion( "public.releases", ARCHIVA_TEST_GROUP_ID, ARCHIVA_TEST_ARTIFACT_ID,
                                                "1.0" ), model );

        userReposControl.replay();
        searchControl.replay();
        metadataResolverControl.replay();
        archivaDAOControl.replay();

        List<Artifact> artifacts = searchService.quickSearch( "archiva" );

        userReposControl.verify();
        searchControl.verify();
        metadataResolverControl.verify();
        archivaDAOControl.verify();

        assertNotNull( artifacts );
        assertEquals( 1, artifacts.size() );

        Artifact artifact = artifacts.get( 0 );
        assertEquals( ARCHIVA_TEST_GROUP_ID, artifact.getGroupId() );
        assertEquals( ARCHIVA_TEST_ARTIFACT_ID, artifact.getArtifactId() );
        assertEquals( "1.0", artifact.getVersion() );
        assertEquals( "jar", artifact.getType() );
        assertEquals( "public.releases", artifact.getRepositoryId() );
    }

    public void testQuickSearchArtifactRegularSearch()
        throws Exception
    {
        List<String> observableRepoIds = new ArrayList<String>();
        observableRepoIds.add( "repo1.mirror" );
        observableRepoIds.add( "public.releases" );

        userReposControl.expectAndReturn( userRepos.getObservableRepositories(), observableRepoIds );

        SearchResults results = new SearchResults();
        List<String> versions = new ArrayList<String>();
        versions.add( "1.0" );

        SearchResultHit resultHit = new SearchResultHit();
        resultHit.setGroupId( ARCHIVA_TEST_GROUP_ID );
        resultHit.setArtifactId( ARCHIVA_TEST_ARTIFACT_ID );
        resultHit.setVersions( versions );
        resultHit.setRepositoryId( null );

        results.addHit( SearchUtil.getHitId( resultHit.getGroupId(), resultHit.getArtifactId() ), resultHit );

        archivaDAOControl.expectAndReturn( archivaDAO.query(
            new UniqueVersionConstraint( observableRepoIds, resultHit.getGroupId(), resultHit.getArtifactId() ) ),
                                           null );

        SearchResultLimits limits = new SearchResultLimits( SearchResultLimits.ALL_PAGES );

        searchControl.expectAndDefaultReturn( search.search( "", observableRepoIds, "archiva", limits, null ),
                                              results );

        ProjectVersionMetadata model = new ProjectVersionMetadata();
        model.setId( "1.0" );
        MavenProjectFacet facet = new MavenProjectFacet();
        facet.setPackaging( "jar" );
        model.addFacet( facet );

        metadataResolverControl.expectAndReturn(
            metadataResolver.getProjectVersion( "repo1.mirror", ARCHIVA_TEST_GROUP_ID, ARCHIVA_TEST_ARTIFACT_ID,
                                                "1.0" ), model );

        userReposControl.replay();
        searchControl.replay();
        archivaDAOControl.replay();
        metadataResolverControl.replay();

        List<Artifact> artifacts = searchService.quickSearch( "archiva" );

        userReposControl.verify();
        searchControl.verify();
        archivaDAOControl.verify();
        metadataResolverControl.verify();

        assertNotNull( artifacts );
        assertEquals( 1, artifacts.size() );

        Artifact artifact = artifacts.get( 0 );
        assertEquals( ARCHIVA_TEST_GROUP_ID, artifact.getGroupId() );
        assertEquals( ARCHIVA_TEST_ARTIFACT_ID, artifact.getArtifactId() );
        assertEquals( "1.0", artifact.getVersion() );
        assertEquals( "jar", artifact.getType() );
        assertNotNull( "Repository should not be null!", artifact.getRepositoryId() );
        assertEquals( "repo1.mirror", artifact.getRepositoryId() );
    }

    public void testQuickSearchNoResults()
        throws Exception
    {
        List<String> observableRepoIds = new ArrayList<String>();
        observableRepoIds.add( "repo1.mirror" );
        observableRepoIds.add( "public.releases" );

        userReposControl.expectAndReturn( userRepos.getObservableRepositories(), observableRepoIds );

        SearchResults results = new SearchResults();
        SearchResultLimits limits = new SearchResultLimits( SearchResultLimits.ALL_PAGES );

        searchControl.expectAndDefaultReturn( search.search( "", observableRepoIds, "non-existent", limits, null ),
                                              results );
        userReposControl.replay();
        searchControl.replay();

        List<Artifact> artifacts = searchService.quickSearch( "test" );

        userReposControl.verify();
        searchControl.verify();

        assertNotNull( artifacts );
        assertEquals( 0, artifacts.size() );
    }

    public void testGetArtifactByChecksum()
        throws Exception
    {
        Date whenGathered = new Date();

        ArtifactsByChecksumConstraint constraint = new ArtifactsByChecksumConstraint( "a1b2c3aksjhdasfkdasasd" );
        List<ArchivaArtifact> artifacts = new ArrayList<ArchivaArtifact>();
        ArchivaArtifact artifact =
            new ArchivaArtifact( ARCHIVA_TEST_GROUP_ID, ARCHIVA_TEST_ARTIFACT_ID, "1.0", "", "jar", "test-repo" );
        artifact.getModel().setWhenGathered( whenGathered );
        artifacts.add( artifact );

        archivaDAOControl.expectAndReturn( archivaDAO.getArtifactDAO(), artifactDAO );
        artifactDAO.queryArtifacts( constraint );
        artifactDAOControl.setMatcher( MockControl.ALWAYS_MATCHER );
        artifactDAOControl.setReturnValue( artifacts );

        archivaDAOControl.replay();
        artifactDAOControl.replay();

        List<Artifact> results = searchService.getArtifactByChecksum( "a1b2c3aksjhdasfkdasasd" );

        archivaDAOControl.verify();
        artifactDAOControl.verify();

        assertNotNull( results );
        assertEquals( 1, results.size() );
    }

    public void testGetArtifactVersionsArtifactExists()
        throws Exception
    {
        List<String> observableRepoIds = new ArrayList<String>();
        observableRepoIds.add( "repo1.mirror" );
        observableRepoIds.add( "public.releases" );

        userReposControl.expectAndReturn( userRepos.getObservableRepositories(), observableRepoIds );
        metadataResolverControl.expectAndReturn(
            metadataResolver.getProjectVersions( "repo1.mirror", ARCHIVA_TEST_GROUP_ID, ARCHIVA_TEST_ARTIFACT_ID ),
            Arrays.asList( "1.0", "1.1-beta-2", "1.2" ) );
        metadataResolverControl.expectAndReturn(
            metadataResolver.getProjectVersions( "public.releases", ARCHIVA_TEST_GROUP_ID, ARCHIVA_TEST_ARTIFACT_ID ),
            Arrays.asList( "1.1-beta-1", "1.1", "1.2.1-SNAPSHOT" ) );

        userReposControl.replay();
        metadataResolverControl.replay();

        List<Artifact> artifacts = searchService.getArtifactVersions( ARCHIVA_TEST_GROUP_ID, ARCHIVA_TEST_ARTIFACT_ID );

        userReposControl.verify();
        metadataResolverControl.verify();

        assertNotNull( artifacts );
        assertEquals( 6, artifacts.size() );
        assertEquals( new Artifact( "repo1.mirror", ARCHIVA_TEST_GROUP_ID, ARCHIVA_TEST_ARTIFACT_ID, "1.0", "pom" ),
                      artifacts.get( 0 ) );
        assertEquals(
            new Artifact( "public.releases", ARCHIVA_TEST_GROUP_ID, ARCHIVA_TEST_ARTIFACT_ID, "1.1-beta-1", "pom" ),
            artifacts.get( 3 ) );
        assertEquals(
            new Artifact( "repo1.mirror", ARCHIVA_TEST_GROUP_ID, ARCHIVA_TEST_ARTIFACT_ID, "1.1-beta-2", "pom" ),
            artifacts.get( 1 ) );
        assertEquals( new Artifact( "public.releases", ARCHIVA_TEST_GROUP_ID, ARCHIVA_TEST_ARTIFACT_ID, "1.1", "pom" ),
                      artifacts.get( 4 ) );
        assertEquals( new Artifact( "repo1.mirror", ARCHIVA_TEST_GROUP_ID, ARCHIVA_TEST_ARTIFACT_ID, "1.2", "pom" ),
                      artifacts.get( 2 ) );
        assertEquals(
            new Artifact( "public.releases", ARCHIVA_TEST_GROUP_ID, ARCHIVA_TEST_ARTIFACT_ID, "1.2.1-SNAPSHOT", "pom" ),
            artifacts.get( 5 ) );
    }

    public void testGetArtifactVersionsByDateArtifactExists()
        throws Exception
    {
        // TODO
    }

    public void testGetArtifactVersionsByDateArtifactDoesNotExist()
        throws Exception
    {
        // TODO
    }

    public void testGetDependenciesArtifactExists()
        throws Exception
    {
        String repoId = "repo1.mirror";

        ProjectVersionMetadata model = new ProjectVersionMetadata();
        model.setId( "1.0" );

        org.apache.archiva.metadata.model.Dependency dependency = new org.apache.archiva.metadata.model.Dependency();
        dependency.setGroupId( "org.apache.commons" );
        dependency.setArtifactId( "commons-logging" );
        dependency.setVersion( "2.0" );

        model.addDependency( dependency );

        dependency = new org.apache.archiva.metadata.model.Dependency();
        dependency.setGroupId( "junit" );
        dependency.setArtifactId( "junit" );
        dependency.setVersion( "2.4" );
        dependency.setScope( "test" );

        model.addDependency( dependency );

        userReposControl.expectAndReturn( userRepos.getObservableRepositories(), Collections.singletonList( repoId ) );
        metadataResolverControl.expectAndReturn(
            metadataResolver.getProjectVersion( repoId, ARCHIVA_TEST_GROUP_ID, ARCHIVA_TEST_ARTIFACT_ID, "1.0" ),
            model );

        metadataResolverControl.replay();
        userReposControl.replay();

        List<Dependency> dependencies =
            searchService.getDependencies( ARCHIVA_TEST_GROUP_ID, ARCHIVA_TEST_ARTIFACT_ID, "1.0" );

        metadataResolverControl.verify();
        userReposControl.verify();

        assertNotNull( dependencies );
        assertEquals( 2, dependencies.size() );
        assertEquals( new Dependency( "org.apache.commons", "commons-logging", "2.0", null, null, null ),
                      dependencies.get( 0 ) );
        assertEquals( new Dependency( "junit", "junit", "2.4", null, null, "test" ), dependencies.get( 1 ) );
    }

    public void testGetDependenciesArtifactDoesNotExist()
        throws Exception
    {
        String repoId = "repo1.mirror";

        userReposControl.expectAndReturn( userRepos.getObservableRepositories(), Collections.singletonList( repoId ) );
        metadataResolverControl.expectAndReturn(
            metadataResolver.getProjectVersion( repoId, ARCHIVA_TEST_GROUP_ID, ARCHIVA_TEST_ARTIFACT_ID, "1.0" ),
            null );

        userReposControl.replay();
        metadataResolverControl.replay();

        try
        {
            searchService.getDependencies( ARCHIVA_TEST_GROUP_ID, ARCHIVA_TEST_ARTIFACT_ID, "1.0" );
            fail( "An exception should have been thrown." );
        }
        catch ( Exception e )
        {
            assertEquals( "Artifact does not exist.", e.getMessage() );
        }

        userReposControl.verify();
        metadataResolverControl.verify();
    }

    public void testGetDependencyTreeArtifactExists()
        throws Exception
    {
        // TODO
    }

    public void testGetDependencyTreeArtifactDoesNotExist()
        throws Exception
    {
        // TODO
    }

    public void testGetDependees()
        throws Exception
    {
        List<String> observableRepoIds = new ArrayList<String>();
        String repoId = "repo1.mirror";
        observableRepoIds.add( repoId );

        List<ProjectVersionReference> dependeeModels = new ArrayList<ProjectVersionReference>();
        ProjectVersionReference dependeeModel = new ProjectVersionReference();
        dependeeModel.setNamespace( ARCHIVA_TEST_GROUP_ID );
        dependeeModel.setProjectId( "archiva-dependee-one" );
        dependeeModel.setProjectVersion( "1.0" );
        dependeeModels.add( dependeeModel );

        dependeeModel = new ProjectVersionReference();
        dependeeModel.setNamespace( ARCHIVA_TEST_GROUP_ID );
        dependeeModel.setProjectId( "archiva-dependee-two" );
        dependeeModel.setProjectVersion( "1.0" );
        dependeeModels.add( dependeeModel );

        userReposControl.expectAndReturn( userRepos.getObservableRepositories(), observableRepoIds );
        metadataResolverControl.expectAndReturn(
            metadataResolver.getProjectReferences( repoId, ARCHIVA_TEST_GROUP_ID, ARCHIVA_TEST_ARTIFACT_ID, "1.0" ),
            dependeeModels );

        metadataResolverControl.replay();
        userReposControl.replay();

        List<Artifact> dependees = searchService.getDependees( ARCHIVA_TEST_GROUP_ID, ARCHIVA_TEST_ARTIFACT_ID, "1.0" );

        metadataResolverControl.verify();
        userReposControl.verify();

        assertNotNull( dependees );
        assertEquals( 2, dependees.size() );
        assertEquals( new Artifact( repoId, ARCHIVA_TEST_GROUP_ID, "archiva-dependee-one", "1.0", "" ),
                      dependees.get( 0 ) );
        assertEquals( new Artifact( repoId, ARCHIVA_TEST_GROUP_ID, "archiva-dependee-two", "1.0", "" ),
                      dependees.get( 1 ) );
    }

    public void testGetDependeesArtifactDoesNotExist()
        throws Exception
    {
        List<String> observableRepoIds = new ArrayList<String>();
        observableRepoIds.add( "repo1.mirror" );
        observableRepoIds.add( "public.releases" );

        // no longer differentiating between a project not being present and a project that is present but with
        // no references. If it is later determined to be needed, we will need to modify the metadata content repository
        userReposControl.expectAndReturn( userRepos.getObservableRepositories(), observableRepoIds );
        metadataResolverControl.expectAndReturn(
            metadataResolver.getProjectReferences( "repo1.mirror", ARCHIVA_TEST_GROUP_ID, ARCHIVA_TEST_ARTIFACT_ID,
                                                   "1.0" ), Collections.<ProjectVersionReference>emptyList() );
        metadataResolverControl.expectAndReturn(
            metadataResolver.getProjectReferences( "public.releases", ARCHIVA_TEST_GROUP_ID, ARCHIVA_TEST_ARTIFACT_ID,
                                                   "1.0" ), Collections.<ProjectVersionReference>emptyList() );

        userReposControl.replay();
        metadataResolverControl.replay();

        assertTrue( searchService.getDependees( ARCHIVA_TEST_GROUP_ID, ARCHIVA_TEST_ARTIFACT_ID, "1.0" ).isEmpty() );
    }
}