/*
 * Copyright (c) 2014-2015 "GraphAware"
 *
 * GraphAware Ltd
 *
 * This file is part of Neo4j-OGM.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.neo4j.ogm.unit.entityaccess;

import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.ogm.domain.canonical.ArbitraryRelationshipEntity;
import org.neo4j.ogm.domain.social.Individual;
import org.neo4j.ogm.entityaccess.EntityFactory;
import org.neo4j.ogm.metadata.MappingException;
import org.neo4j.ogm.metadata.MetaData;
import org.neo4j.ogm.model.NodeModel;
import org.neo4j.ogm.model.RelationshipModel;

public class ObjectFactoryTest {

    private EntityFactory objectCreator;

    @Before
    public void setUp() {
        this.objectCreator = new EntityFactory(new MetaData("org.neo4j.ogm.domain.social", "org.neo4j.ogm.domain.canonical"));
    }

    @Test
    public void shouldConstructObjectOfParticularTypeUsingItsDefaultZeroArgConstructor() {
        RelationshipModel personRelationshipModel = new RelationshipModel();
        personRelationshipModel.setType("MEMBER_OF");
        ArbitraryRelationshipEntity gary = this.objectCreator.newObject(personRelationshipModel);
        assertNotNull(gary);

        NodeModel personNodeModel = new NodeModel();
        personNodeModel.setLabels(new String[] {"Individual"});
        Individual sheila = this.objectCreator.newObject(personNodeModel);
        assertNotNull(sheila);
    }

    @Test
    public void shouldHandleMultipleLabelsSafely() {
        NodeModel personNodeModel = new NodeModel();
        personNodeModel.setLabels(new String[] {"Female", "Individual", "Lass"});
        Individual ourLass = this.objectCreator.newObject(personNodeModel);
        assertNotNull(ourLass);
    }

    @Test(expected = MappingException.class)
    public void shouldFailIfZeroArgConstructorIsNotPresent() {
        RelationshipModel edge = new RelationshipModel();
        edge.setId(49L);
        edge.setType("ClassWithoutZeroArgumentConstructor");
        this.objectCreator.newObject(edge);
    }

    @Test
    public void shouldBeAbleToConstructObjectWithNonPublicZeroArgConstructor() {
        NodeModel vertex = new NodeModel();
        vertex.setId(163L);
        vertex.setLabels(new String[] {"ClassWithPrivateConstructor"});
        this.objectCreator.newObject(vertex);
    }

    @Test(expected = MappingException.class)
    public void shouldFailForGraphModelComponentWithNoTaxa() {
        NodeModel vertex = new NodeModel();
        vertex.setId(302L);
        vertex.setLabels(new String[0]);
        this.objectCreator.newObject(vertex);
    }

}
