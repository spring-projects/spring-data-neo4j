/**
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.mapping;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.index.Index;
import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.test.ImpermanentGraphDatabase;
import org.springframework.data.neo4j.fieldaccess.Neo4jConversionServiceFactoryBean;
import org.springframework.data.neo4j.fieldaccess.NodeDelegatingFieldAccessorFactory;
import org.springframework.data.neo4j.model.Group;
import org.springframework.data.neo4j.model.Person;
import org.springframework.data.neo4j.model.Personality;
import org.springframework.data.neo4j.support.EntityStateHandler;
import org.springframework.data.neo4j.support.GraphDatabaseContext;
import org.springframework.data.neo4j.support.node.NodeEntityInstantiator;
import org.springframework.data.neo4j.support.node.NodeEntityStateFactory;
import org.springframework.data.neo4j.support.typerepresentation.NoopNodeTypeRepresentationStrategy;

import java.util.*;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.junit.Assert.assertEquals;

/**
 * @author mh
 * @since 19.09.11
 */
public class Neo4jNodeConverterTest {

    private Neo4jNodeConverterImpl converter;
    private Transaction tx;
    private GraphDatabaseContext gdc;

    @Before
    public void setUp() throws Exception {
        final Neo4jMappingContext mappingContext = new Neo4jMappingContext();
        gdc = createContext(mappingContext);
        tx = gdc.beginTx();
        final NodeEntityStateFactory nodeEntityStateFactory = new NodeEntityStateFactory();
        nodeEntityStateFactory.setMappingContext(mappingContext);
        nodeEntityStateFactory.setGraphDatabaseContext(gdc);
        nodeEntityStateFactory.setNodeDelegatingFieldAccessorFactory(new NodeDelegatingFieldAccessorFactory(gdc));
        converter = new Neo4jNodeConverterImpl();
        converter.setNodeEntityStateFactory(nodeEntityStateFactory);
        gdc.setConverter(converter);
    }

    private GraphDatabaseContext createContext(Neo4jMappingContext mappingContext) throws Exception {
        GraphDatabaseContext gdc = new GraphDatabaseContext();
        final ImpermanentGraphDatabase gdb = new ImpermanentGraphDatabase();
        gdc.setGraphDatabaseService(gdb);
        gdc.setMappingContext(mappingContext);
        final EntityStateHandler entityStateHandler = new EntityStateHandler(mappingContext, gdb);
        gdc.setNodeTypeRepresentationStrategy(new NoopNodeTypeRepresentationStrategy(new NodeEntityInstantiator(entityStateHandler)));
        gdc.setConversionService(new Neo4jConversionServiceFactoryBean().getObject());
        gdc.setEntityStateHandler(entityStateHandler);
        return gdc;
    }

    @After
    public void tearDown() throws Exception {
        tx.failure();
        tx.finish();
        gdc.getGraphDatabaseService().shutdown();
    }

    @Test
    public void testWriteEntityToNewNode() {
        final Person p = new Person("Michael",37);
        converter.write(p,null);
        final Node createdNode = gdc.getNodeById(p.getId());
        assertEquals("Michael", createdNode.getProperty("name"));
    }
    @Test
    public void testFindNewlyWrittenNodeInIndex() {
        final Person p = new Person("Michael",37);
        converter.write(p,null);
        final Node createdNode = gdc.getNodeById(p.getId());
        final Index<Node> index = gdc.getIndex(Person.class,Person.NAME_INDEX);
        final Node found = index.get("name", "Michael").getSingle();
        assertEquals("node found in index", createdNode, found);
    }
    @Test
    public void testWriteEntityToExistingNode() {
        final Node existingNode = gdc.createNode();
        final Person p = new Person("Michael",37);
        converter.write(p, existingNode);
        assertEquals("Entity uses provided node", existingNode.getId(), p.getId());
        assertEquals("Michael", existingNode.getProperty("name"));
    }

    @Test
    public void testUpdateExistingNode() {
        final Node existingNode = gdc.createNode();
        existingNode.setProperty("name","Test");
        assertEquals("Test", existingNode.getProperty("name"));
        final Person p = new Person("Michael",37);
        converter.write(p, existingNode);
        assertEquals("Michael", existingNode.getProperty("name"));
        p.setName("Emil");
        converter.write(p, existingNode);
        assertEquals("Emil", existingNode.getProperty("name"));
    }

    @Test
    public void testWriteConvertedPropertiesToExistingNode() {
        final Node existingNode = gdc.createNode();
        final Person p = new Person("Michael",37);
        p.setBirthdate(new Date(100));
        p.setPersonality(Personality.EXTROVERT);
        converter.write(p, existingNode);
        assertEquals("100", existingNode.getProperty("birthdate"));
        assertEquals("EXTROVERT", existingNode.getProperty("personality"));
    }
    @Test
    public void testReadConvertedPropertiesToExistingNode() {
        final Node existingNode = gdc.createNode();
        existingNode.setProperty("name","Michael");
        existingNode.setProperty("age",36);
        existingNode.setProperty("personality","EXTROVERT");
        existingNode.setProperty("birthdate","100");
        final Person p = converter.read(Person.class,existingNode);
        assertEquals("Michael", p.getName());
        assertEquals(36, p.getAge());
        assertEquals(new Date(100),p.getBirthdate());
        assertEquals(Personality.EXTROVERT,p.getPersonality());
    }

    @Test
    public void testDeleteProperty() {
        final Node existingNode = gdc.createNode();
        final Person p = new Person("Michael",37);
        converter.write(p, existingNode);
        p.setName(null);
        converter.write(p, existingNode);
        assertEquals(false, existingNode.hasProperty("name"));
    }

    @Test
    public void testReadEntityFromExistingNode() {
        final Node node = gdc.createNode();
        node.setProperty("name","Emil");
        final Person p = converter.read(Person.class, node);
        assertEquals("Emil", p.getName());
    }

    @Test
    public void testSetRelationshipWithPreExistingNode() {
        final Person p = new Person("Michael",37);
        final Person emil = new Person("Emil", 30);
        converter.write(emil, null);
        p.setBoss(emil);
        converter.write(p, null);
        final Node node = gdc.getNodeById(p.getId());
        final Node boss = node.getRelationships(DynamicRelationshipType.withName("boss"), Direction.INCOMING).iterator().next().getStartNode();
        assertEquals("added additional relationship end node",emil.getId(), boss.getId());
    }
    @Test
    public void testAddRelationshipWithPreExistingNode() {
        Group g=new Group();
        final Person michael = new Person("Michael",37);
        converter.write(michael, null);
        g.setPersons(singleton(michael));
        converter.write(g,null);
        final Node groupNode = gdc.getNodeById(g.getId());
        final Collection<Relationship> persons = IteratorUtil.asCollection(groupNode.getRelationships(DynamicRelationshipType.withName("persons"), Direction.OUTGOING));
        final Node michaelNode = persons.iterator().next().getOtherNode(groupNode);
        assertEquals("added michaelNode to group",michael.getId(), michaelNode.getId());
    }

    @Test
    public void testAddRelationshipWithTwoExistingNodes() {
        Group group=new Group();
        final Person michael = new Person("Michael",37);
        final Person andres = new Person("Andrés",36);
        converter.write(michael, null);
        converter.write(andres, null);
        group.setPersons(new HashSet<Person>(asList(michael, andres)));
        converter.write(group,null);
        final Node groupNode = gdc.getNodeById(group.getId());
        final Collection<Node> persons = getRelatedNodes(groupNode, "persons", Direction.OUTGOING);
        assertEquals(2,persons.size());
        assertEquals(asList(gdc.getNodeById(michael.getId()),gdc.getNodeById(andres.getId())), persons);
    }

    @Test
    public void testAddRelationshipWithTwoPeopleButJustOneExistingNodes() {
        Group group=new Group();
        final Person michael = new Person("Michael",37);
        final Person andres = new Person("Andrés",36);
        converter.write(michael, null);
        group.setPersons(new HashSet<Person>(asList(michael, andres)));
        converter.write(group,null);
        final Node groupNode = gdc.getNodeById(group.getId());
        final Collection<Node> persons = getRelatedNodes(groupNode, "persons", Direction.OUTGOING);
        assertEquals(2,persons.size());
        assertEquals(asList(gdc.getNodeById(michael.getId()),gdc.getNodeById(andres.getId())), persons);
    }

    @Test
    public void testAddRelationshipCascadeOverTwoSteps() {
        Group group=new Group();
        final Person andres = new Person("Andrés",36);
        final Person emil = new Person("Emil",30);
        andres.setBoss(emil);
        group.setPersons(singleton(andres));
        converter.write(group,null);
        final Node groupNode = gdc.getNodeById(group.getId());
        final Collection<Node> persons = getRelatedNodes(groupNode, "persons", Direction.OUTGOING);
        assertEquals(1,persons.size());
        final Node andresNode = gdc.getNodeById(andres.getId());
        assertEquals(asList(andresNode), persons);
        assertEquals(emil.getId(), getRelatedNodes(andresNode,"boss",Direction.INCOMING).get(0).getId());
    }

    private List<Node> getRelatedNodes(Node startNode, String type, Direction direction) {
        List<Node> result=new ArrayList<Node>();
        for (Relationship relationship : startNode.getRelationships(DynamicRelationshipType.withName(type), direction)) {
            result.add(relationship.getOtherNode(startNode));
        }
        return result;
    }

    @Test
    public void testSetRelationshipWithNonExistingNode() {
        final Person michael = new Person("Michael",37);
        final Person emil = new Person("Emil", 30);
        michael.setBoss(emil);
        converter.write(michael, null);
        final Node node = gdc.getNodeById(michael.getId());
        final Node boss = getRelatedNodes(node,"boss",Direction.INCOMING).get(0);
        assertEquals("added additional relationship end node",emil.getId(), boss.getId());
    }
    @Test
    public void testAddRelationshipWithNonExistingNode() {
        Group g=new Group();
        final Person michael = new Person("Michael",37);
        g.setPersons(singleton(michael));
        converter.write(g,null);
        final Node groupNode = gdc.getNodeById(g.getId());
        final Node michaelNode = getRelatedNodes(groupNode,"persons",Direction.OUTGOING).get(0);
        assertEquals("added member to group",michael.getId(), michaelNode.getId());
    }
}
