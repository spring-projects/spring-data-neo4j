/*
 * Copyright (c) 2014 GraphAware
 *
 * This file is part of GraphAware.
 *
 * GraphAware is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details. You should have received a copy of
 * the GNU General Public License along with this program.  If not, see
 * <http://www.gnu.org/licenses/>.
 */

package org.springframework.data.neo4j.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.ogm.exception.NotFoundException;
import org.neo4j.test.TestGraphDatabaseFactory;

import java.util.Arrays;
import java.util.Collections;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.neo4j.tooling.GlobalGraphOperations.at;
import static org.springframework.data.neo4j.util.IterableUtils.*;

/**
 * Unit test for {@link com.graphaware.common.util.IterableUtils}.
 */
public class IterableUtilsTest {

    @Test
    public void checkContainsCollections() {
        assertTrue(contains(asList("a", "b"), "b"));
        assertFalse(contains(asList("a", "b"), "c"));
    }

    @Test
    public void checkContainsRealIterables() {
        GraphDatabaseService database = new TestGraphDatabaseFactory().newImpermanentDatabase();

        Node node;

        try (Transaction tx = database.beginTx()) {
            node = database.createNode();
            tx.success();
        }

        try (Transaction tx = database.beginTx()) {
            assertTrue(contains(at(database).getAllNodes(), node));
        }

        try (Transaction tx = database.beginTx()) {
            database.getNodeById(0).delete();
            tx.success();
        }

        try (Transaction tx = database.beginTx()) {
            assertFalse(contains(at(database).getAllNodes(), node));
        }

        database.shutdown();
    }

    @Test
    public void singleElementShouldBeReturnedWhenIterableHasOneElement() {
        assertEquals("test", getSingleOrNull(Collections.singletonList("test")));
    }

    @Test
    public void singleElementShouldBeReturnedWhenIterableHasOneElement2() {
        assertEquals("test", getSingle(Collections.singletonList("test")));
    }

    @Test
    public void nullShouldBeReturnedWhenIterableHasNoElements() {
        assertNull(getSingleOrNull(Collections.emptyList()));
    }

    @Test(expected = NotFoundException.class)
    public void exceptionShouldBeThrownWhenIterableHasNoElements() {
        getSingle(Collections.emptyList());
    }

    @Test
    public void exceptionShouldBeThrownWhenIterableHasNoElements2() {
        try {
            getSingle(Collections.emptyList(), "test");
        } catch (NotFoundException e) {
            assertEquals("test", e.getMessage());
        }
    }

    @Test(expected = IllegalStateException.class)
    public void exceptionShouldBeThrownWhenIterableHasMoreThanOneElement() {
        getSingleOrNull(Arrays.asList("test1", "test2"));
    }

    @Test(expected = IllegalStateException.class)
    public void exceptionShouldBeThrownWhenIterableHasMoreThanOneElement2() {
        getSingle(Arrays.asList("test1", "test2"));
    }

    @Test
    public void firstElementShouldBeReturnedWhenIterableHasOneElement() {
        assertEquals("test", getFirstOrNull(Collections.singletonList("test")));
    }

    @Test
    public void firstElementShouldBeReturnedWhenIterableHasOneElement2() {
        assertEquals("test", getFirst(Collections.singletonList("test"), "test"));
    }

    @Test
    public void nullShouldBeReturnedWhenIterableHasNoElementsWhenRequestingFirst() {
        assertNull(getFirstOrNull(Collections.emptyList()));
    }

    @Test(expected = NotFoundException.class)
    public void exceptionShouldBeThrownWhenIterableHasNoElementsWhenRequestingFirst() {
        getFirst(Collections.emptyList(), "test");
    }

    @Test
    public void exceptionShouldBeThrownWhenIterableHasNoElements2WhenRequestingFirst() {
        try {
            getFirst(Collections.emptyList(), "test");
        } catch (NotFoundException e) {
            assertEquals("test", e.getMessage());
        }
    }

    @Test
    public void shouldReturnFirstWhenThereIsMoreThanOne() {
        assertEquals("test1", getFirstOrNull(Arrays.asList("test1", "test2")));
    }

    @Test
    public void exceptionShouldBeThrownWhenIterableHasMoreThanOneElement2WhenRequestingFirst() {
        assertEquals("test1", getFirst(Arrays.asList("test1", "test2"), "test"));
    }
}
