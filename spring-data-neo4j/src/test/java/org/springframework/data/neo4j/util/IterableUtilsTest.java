/*
 * Copyright (c)  [2011-2015] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package org.springframework.data.neo4j.util;

import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.ogm.exception.NotFoundException;
import org.neo4j.ogm.testutil.MultiDriverTestClass;

import java.util.Arrays;
import java.util.Collections;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.springframework.data.neo4j.util.IterableUtils.*;

/**
 * @author Michal Bachman
 */
public class IterableUtilsTest extends MultiDriverTestClass {

    @Test
    public void checkContainsCollections() {
        assertTrue(contains(asList("a", "b"), "b"));
        assertFalse(contains(asList("a", "b"), "c"));
    }

    @Test
    public void checkContainsRealIterables() {

        GraphDatabaseService database = getGraphDatabaseService();

        Node node;

        try (Transaction tx = database.beginTx()) {
            node = database.createNode();
            tx.success();
        }

        try (Transaction tx = database.beginTx()) {
            assertTrue(contains(database.getAllNodes(), node));
        }

        try (Transaction tx = database.beginTx()) {
            database.getNodeById(0).delete();
            tx.success();
        }

        try (Transaction tx = database.beginTx()) {
            assertFalse(contains(database.getAllNodes(), node));
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
