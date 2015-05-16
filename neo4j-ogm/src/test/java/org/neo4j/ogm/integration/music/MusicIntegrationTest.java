/*
 * Copyright (c)  [2011-2015] "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package org.neo4j.ogm.integration.music;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.ogm.cypher.Parameter;
import org.neo4j.ogm.domain.music.Album;
import org.neo4j.ogm.domain.music.Artist;
import org.neo4j.ogm.domain.music.Recording;
import org.neo4j.ogm.domain.music.Studio;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.testutil.Neo4jIntegrationTestRule;

/**
 * @author Luanne Misquitta
 */
public class MusicIntegrationTest {

    @ClassRule
    public static Neo4jIntegrationTestRule neo4jRule = new Neo4jIntegrationTestRule();

    private static Session session;

    @Before
	public void init() throws IOException {
		session = new SessionFactory("org.neo4j.ogm.domain.music").openSession(neo4jRule.baseNeoUrl());
	}

	@After
	public void clear() {
		session.purgeDatabase();
	}
	/**
	 * @see DATAGRAPH-589
	 */
	@Test
	public void shouldSaveAndRetrieveEntitiesWithInvalidCharsInLabelsAndRels() {
		Studio emi = new Studio("EMI Studios, London");

		Artist theBeatles = new Artist("The Beatles");
		Album please = new Album("Please Please Me");
		Recording pleaseRecording = new Recording(please, emi, 1963);
		please.setRecording(pleaseRecording);
		theBeatles.getAlbums().add(please);
		please.setArtist(theBeatles);
		session.save(theBeatles);

		theBeatles = session.loadAll(Artist.class).iterator().next();
		assertEquals("The Beatles", theBeatles.getName());
		assertEquals(1, theBeatles.getAlbums().size());
		assertEquals("Please Please Me", theBeatles.getAlbums().iterator().next().getName());
		assertEquals("EMI Studios, London", theBeatles.getAlbums().iterator().next().getRecording().getStudio().getName());

		please = session.loadByProperty(Album.class, new Parameter("name", "Please Please Me")).iterator().next();
		assertEquals("The Beatles", please.getArtist().getName());

		Album hard = new Album("A Hard Day's Night");
		hard.setArtist(theBeatles);
		Recording hardRecording = new Recording(hard, emi, 1964);
		hard.setRecording(hardRecording);
		theBeatles.getAlbums().add(hard);
		session.save(hard);

		Collection<Album> albums = session.loadAll(Album.class);
		assertEquals(2, albums.size());
		for (Album album : albums) {
			if (album.getName().equals("Please Please Me")) {
				assertEquals(1963, album.getRecording().getYear());
			} else {
				assertEquals(1964, album.getRecording().getYear());
			}
		}
	}

	/**
	 * @see DATAGRAPH-631
	 */
	@Test
	public void shouldLoadStudioWithLocationMissingInDomainModel() {
		new ExecutionEngine(neo4jRule.getGraphDatabaseService()).execute("CREATE (s:Studio {`studio-name`:'Abbey Road Studios'})");
		Studio studio = session.loadByProperty(Studio.class, new Parameter("studio-name","Abbey Road Studios")).iterator().next();
		assertNotNull(studio);

	}

	/**
	 * @see DATAGRAPH-629
	 */
	@Test
	public void shouldRetrieveEntityByPropertyWithZeroDepth() {
		Studio emi = new Studio("EMI Studios, London");

		Artist theBeatles = new Artist("The Beatles");
		Album please = new Album("Please Please Me");
		Recording pleaseRecording = new Recording(please, emi, 1963);
		please.setRecording(pleaseRecording);
		theBeatles.getAlbums().add(please);
		please.setArtist(theBeatles);
		session.save(theBeatles);

		theBeatles = session.loadAll(Artist.class).iterator().next();
		assertEquals("The Beatles", theBeatles.getName());
		assertEquals(1, theBeatles.getAlbums().size());
		assertEquals("Please Please Me", theBeatles.getAlbums().iterator().next().getName());
		assertEquals("EMI Studios, London", theBeatles.getAlbums().iterator().next().getRecording().getStudio().getName());

		session.clear();

		please = session.loadByProperty(Album.class, new Parameter("name", "Please Please Me"),0).iterator().next();
		assertEquals("Please Please Me",please.getName());
		assertNull(please.getArtist());
		assertNull(please.getRecording());
	}
}
