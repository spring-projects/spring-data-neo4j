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
import org.neo4j.ogm.domain.cineasts.annotated.Actor;
import org.neo4j.ogm.domain.cineasts.annotated.Movie;
import org.neo4j.ogm.domain.cineasts.annotated.Role;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Vince Bickers
 */
public class RelationshipEntityMappingTest extends MappingTest {

    @BeforeClass
    public static void setUp() {
        MappingTest.setUp("org.neo4j.ogm.domain.cineasts.annotated");
    }

    @Test
    public void testThatAnnotatedRelationshipOnRelationshipEntityCreatesTheCorrectRelationshipTypeInTheGraph() {
        Movie hp = new Movie();
        hp.setTitle("Goblet of Fire");
        hp.setYear(2005);

        Actor daniel = new Actor("Daniel Radcliffe");
        daniel.playedIn(hp, "Harry Potter");
        saveAndVerify(daniel, "CREATE (m:Movie {title : 'Goblet of Fire',year:2005 } ) create (a:Actor {name:'Daniel Radcliffe'}) create (a)-[:ACTS_IN {role:'Harry Potter'}]->(m)");

    }

    @Test
    public void testThatRelationshipEntityNameIsUsedAsRelationshipTypeWhenTypeIsNotDefined() {
        Movie hp = new Movie();
        hp.setTitle("Goblet of Fire");
        hp.setYear(2005);

        Actor daniel = new Actor("Daniel Radcliffe");
        daniel.nominatedFor(hp, "Saturn Award", 2005);
        saveAndVerify(daniel, "CREATE (m:Movie {title : 'Goblet of Fire',year:2005 } ) create (a:Actor {name:'Daniel Radcliffe'}) create (a)-[:NOMINATIONS {name:'Saturn Award', year:2005}]->(m)");
        //Not quite sure if the relationship type should be the field name or the RelationshipEntity class name?
    }

}
