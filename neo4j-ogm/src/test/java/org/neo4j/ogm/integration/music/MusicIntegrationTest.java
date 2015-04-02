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

import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.ogm.domain.music.Album;
import org.neo4j.ogm.domain.music.Artist;
import org.neo4j.ogm.domain.music.Recording;
import org.neo4j.ogm.domain.music.Studio;
import org.neo4j.ogm.integration.InMemoryServerTest;
import org.neo4j.ogm.model.Property;
import org.neo4j.ogm.session.SessionFactory;

/**
 * @author Luanne Misquitta
 */
public class MusicIntegrationTest extends InMemoryServerTest {

	@BeforeClass
	public static void init() throws IOException {
		setUp();
		session = new SessionFactory("org.neo4j.ogm.domain.music").openSession("http://localhost:" + neoPort);
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

		please = session.loadByProperty(Album.class, new Property<String, Object>("name", "Please Please Me")).iterator().next();
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
}
