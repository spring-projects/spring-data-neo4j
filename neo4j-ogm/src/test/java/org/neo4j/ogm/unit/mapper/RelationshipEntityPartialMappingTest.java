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

package org.neo4j.ogm.unit.mapper;

import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.ogm.domain.cineasts.partial.Actor;
import org.neo4j.ogm.domain.cineasts.partial.Movie;

/*
 * The purpose of these tests is to describe the behaviour of the
 * mapper when a RelationshipEntity object is not referenced by
 * both of its Related entities.
 */
public class RelationshipEntityPartialMappingTest extends MappingTest {

    @BeforeClass
    public static void setUp() {
        MappingTest.setUp("org.neo4j.ogm.domain.cineasts.partial");
    }

    @Test
    public void testCreateActorRoleAndMovie() {

        Actor keanu = new Actor("Keanu Reeves");
        Movie matrix = new Movie("The Matrix");

        // note: this does not establish a role relationsip on the matrix
        keanu.addRole("Neo", matrix);

        saveAndVerify(keanu,
                        "create (a:Actor {name:'Keanu Reeves'}) " +
                        "create (m:Movie {name:'The Matrix'}) " +
                        "create (a)-[:ACTS_IN {played:'Neo'}]->(m)");
    }

}
