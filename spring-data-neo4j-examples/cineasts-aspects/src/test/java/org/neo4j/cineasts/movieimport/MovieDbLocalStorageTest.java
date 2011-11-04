package org.neo4j.cineasts.movieimport;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Collections;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author mh
 * @since 13.03.11
 */
public class MovieDbLocalStorageTest {


    private static final String ID = "111";
    private static final Map<String,String> DATA = Collections.singletonMap("id", ID);
    protected MovieDbLocalStorage storage;

    @Before
    public void setUp() throws Exception {
        storage = new MovieDbLocalStorage("target/data");
    }

    @Test
    public void testHasMovie() throws Exception {
        storage.storeMovie(ID,DATA);
        assertEquals(true, storage.hasMovie(ID));
    }

    @Test
    public void testLoadMovie() throws Exception {
        storage.storeMovie(ID,DATA);
        assertEquals(DATA,storage.loadMovie(ID));
    }

    @Test
    public void testStoreMovie() throws Exception {
        storage.storeMovie(ID,DATA);
        assertEquals(true, new File("target/data/movie_"+ID+".json").exists());
    }

    @Test
    public void testHasPerson() throws Exception {
        storage.storePerson(ID,DATA);
        assertEquals(true, storage.hasPerson(ID));
    }

    @Test
    public void testLoadPerson() throws Exception {
        storage.storePerson(ID,DATA);
        assertEquals(DATA,storage.loadPerson(ID));
    }
    @Test
    public void testLoadPersonFromList() throws Exception {
        storage.storePerson(ID,asList(DATA));
        assertEquals(DATA,storage.loadPerson(ID));
    }
    @Test
    public void testLoadPersonFromInvalidList() throws Exception {
        storage.storePerson(ID, asList("Nothing found."));
        final Map personData = storage.loadPerson(ID);
        assertTrue("person unexpectedly found", personData.containsKey("not_found"));
    }

    @Test
    public void testStorePerson() throws Exception {
        storage.storePerson(ID,DATA);
        assertEquals(true, new File("target/data/person_"+ID+".json").exists());
    }
}
