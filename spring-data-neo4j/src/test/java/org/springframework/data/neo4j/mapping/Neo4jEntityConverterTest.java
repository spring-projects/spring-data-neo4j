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
import org.springframework.core.convert.ConversionService;
import org.springframework.data.convert.DefaultTypeMapper;
import org.springframework.data.convert.TypeMapper;
import org.springframework.data.neo4j.core.TypeRepresentationStrategy;
import org.springframework.data.neo4j.fieldaccess.Neo4jConversionServiceFactoryBean;
import org.springframework.data.neo4j.fieldaccess.NodeDelegatingFieldAccessorFactory;
import org.springframework.data.neo4j.fieldaccess.RelationshipDelegatingFieldAccessorFactory;
import org.springframework.data.neo4j.model.Friendship;
import org.springframework.data.neo4j.model.Group;
import org.springframework.data.neo4j.model.Person;
import org.springframework.data.neo4j.model.Personality;
import org.springframework.data.neo4j.support.EntityStateHandler;
import org.springframework.data.neo4j.support.GraphDatabaseContext;
import org.springframework.data.neo4j.support.node.NodeEntityInstantiator;
import org.springframework.data.neo4j.support.node.NodeEntityStateFactory;
import org.springframework.data.neo4j.support.relationship.RelationshipEntityInstantiator;
import org.springframework.data.neo4j.support.relationship.RelationshipEntityStateFactory;
import org.springframework.data.neo4j.support.typerepresentation.NoopNodeTypeRepresentationStrategy;
import org.springframework.data.neo4j.support.typerepresentation.NoopRelationshipTypeRepresentationStrategy;

import java.util.*;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.junit.Assert.assertEquals;

/**
 * @author mh
 * @since 19.09.11
 */
public class Neo4jEntityConverterTest {

    public static final DynamicRelationshipType PERSONS = DynamicRelationshipType.withName("persons");
    private static final RelationshipType KNOWS = DynamicRelationshipType.withName("knows");
    private Neo4jEntityConverterImpl<Object,Node> converter;
    private Transaction tx;
    private GraphDatabaseContext gdc;
    private Group group;
    private Person michael;
    private Person emil;
    private Person andres;

    /* OUCH

    [MC]
    [GDC]->[GDB]
    [GDC]->[MC]
    [GDC]->[ESH]
    [ESH]->[MC]
    [ESH]->[GDB]
    [TRS]->[EI]
    [EI]->[ESH]
    [GDC]->[CS]
    [GDC]->[TRS]
    [GDC]->[ESH]
    [ESF]->[MC]
    [ESF]->[GDC]
    [ESF]->[FAF]
    [FAF]->[GDC]
    [GDC]->[EC]
    [EC]->[ESF]
    [EC]->[CS]
    [EC]->[EI]
    [EC]->[ESH]
    [EC]->[SST]
    [EC]->[TRS]
    [SST]->[ESF]

     */

    @Before
    public void setUp() throws Exception {
        final Neo4jMappingContext mappingContext = new Neo4jMappingContext();
        gdc = createContext(mappingContext);
        tx = gdc.beginTx();

        final NodeEntityStateFactory nodeEntityStateFactory = createNodeEntityStateFactory(mappingContext);
        final RelationshipEntityStateFactory relationshipEntityStateFactory = createRelationshipEntityStateFactory(mappingContext);
        final EntityStateHandler entityStateHandler = new EntityStateHandler(mappingContext, gdc.getGraphDatabaseService());
        final NodeEntityInstantiator entityInstantiator = new NodeEntityInstantiator(entityStateHandler);
        final TypeRepresentationStrategy<Node> typeRepresentationStrategy = gdc.getNodeTypeRepresentationStrategy();
        TypeMapper<Node> typeMapper = new DefaultTypeMapper<Node>(new TRSTypeAliasAccessor<Node>(typeRepresentationStrategy),asList(new ClassValueTypeInformationMapper()));
        SourceStateTransmitter<Node> nodeStateTransmitter = new SourceStateTransmitter<Node>(nodeEntityStateFactory);
        SourceStateTransmitter<Relationship> relationshipStateTransmitter = new SourceStateTransmitter<Relationship>(relationshipEntityStateFactory);
        final ConversionService conversionService = gdc.getConversionService();

        Neo4jEntityFetchHandler fetchHandler=new Neo4jEntityFetchHandler(entityStateHandler, conversionService, relationshipStateTransmitter , nodeStateTransmitter);

        converter = new Neo4jEntityConverterImpl<Object,Node>(mappingContext, conversionService, entityInstantiator, entityStateHandler, typeMapper, nodeStateTransmitter, fetchHandler);
        gdc.setNodeEntityConverter(converter);
        group = new Group();
        michael = new Person("Michael", 37);
        emil = new Person("Emil", 30);
        andres = new Person("Andrés", 36);
    }

    private NodeEntityStateFactory createNodeEntityStateFactory(Neo4jMappingContext mappingContext) {
        final NodeEntityStateFactory nodeEntityStateFactory = new NodeEntityStateFactory();
        nodeEntityStateFactory.setMappingContext(mappingContext);
        nodeEntityStateFactory.setGraphDatabaseContext(gdc);
        nodeEntityStateFactory.setNodeDelegatingFieldAccessorFactory(new NodeDelegatingFieldAccessorFactory(gdc));
        return nodeEntityStateFactory;
    }
    private RelationshipEntityStateFactory createRelationshipEntityStateFactory(Neo4jMappingContext mappingContext) {
        final RelationshipEntityStateFactory relationshipEntityStateFactory = new RelationshipEntityStateFactory();
        relationshipEntityStateFactory.setMappingContext(mappingContext);
        relationshipEntityStateFactory.setGraphDatabaseContext(gdc);
        relationshipEntityStateFactory.setRelationshipDelegatingFieldAccessorFactory(new RelationshipDelegatingFieldAccessorFactory(gdc));
        return relationshipEntityStateFactory;
    }

    private GraphDatabaseContext createContext(Neo4jMappingContext mappingContext) throws Exception {
        GraphDatabaseContext gdc = new GraphDatabaseContext();
        final ImpermanentGraphDatabase gdb = new ImpermanentGraphDatabase();
        gdc.setGraphDatabaseService(gdb);
        gdc.setMappingContext(mappingContext);
        final EntityStateHandler entityStateHandler = new EntityStateHandler(mappingContext, gdb);
        gdc.setNodeTypeRepresentationStrategy(new NoopNodeTypeRepresentationStrategy(new NodeEntityInstantiator(entityStateHandler)));
        gdc.setRelationshipTypeRepresentationStrategy(new NoopRelationshipTypeRepresentationStrategy(new RelationshipEntityInstantiator(entityStateHandler)));
        gdc.setConversionService(new Neo4jConversionServiceFactoryBean().getObject());
        gdc.setEntityStateHandler(entityStateHandler);
        gdc.createCypherExecutor();
        return gdc;
    }


    private List<Node> groupMemberNodes() {
        return groupMemberNodes(groupNode());
    }

    private List<Node> groupMemberNodes(Node node) {
        return getRelatedNodes(node, "persons", Direction.OUTGOING);
    }

    @After
    public void tearDown() throws Exception {
        tx.failure();
        tx.finish();
        gdc.getGraphDatabaseService().shutdown();
    }

    @Test
    public void testWriteEntityToNewNode() {
        storeInGraph(michael);
        assertEquals("Michael", michaelNode().getProperty("name"));
    }

    @Test
    public void testFindNewlyWrittenNodeInIndex() {
        storeInGraph(michael);
        final Node createdNode = michaelNode();
        final Index<Node> index = gdc.getIndex(Person.class, Person.NAME_INDEX);
        final Node found = index.get("name", "Michael").getSingle();
        assertEquals("node found in index", createdNode, found);
    }

    private Node michaelNode() {
        return gdc.getNodeById(michael.getId());
    }

    @Test
    public void testWriteEntityToExistingNode() {
        final Node existingNode = createNewNode();
        converter.write(michael, existingNode);
        assertEquals("Entity uses provided node", (Long) existingNode.getId(), michael.getId());
        assertEquals("Michael", existingNode.getProperty("name"));
    }

    private Node createNewNode() {
        return gdc.createNode();
    }

    @Test
    public void testUpdateExistingNode() {
        final Node existingNode = createNewNode();
        existingNode.setProperty("name", "Test");
        assertEquals("Test", existingNode.getProperty("name"));
        converter.write(michael, existingNode);
        assertEquals("Michael", existingNode.getProperty("name"));
        michael.setName("Emil");
        converter.write(michael, existingNode);
        assertEquals("Emil", existingNode.getProperty("name"));
    }

    @Test
    public void testWriteConvertedPropertiesToExistingNode() {
        final Node existingNode = createNewNode();
        michael.setBirthdate(new Date(100));
        michael.setPersonality(Personality.EXTROVERT);
        converter.write(michael, existingNode);
        assertEquals("100", existingNode.getProperty("birthdate"));
        assertEquals("EXTROVERT", existingNode.getProperty("personality"));
    }

    @Test
    public void testReadConvertedPropertiesFromExistingNode() {
        final Node existingNode = createNewNode();
        existingNode.setProperty("name", "Michael");
        existingNode.setProperty("age", 36);
        existingNode.setProperty("personality", "EXTROVERT");
        existingNode.setProperty("birthdate", "100");
        final Person p = converter.read(Person.class, existingNode);
        assertEquals("Michael", p.getName());
        assertEquals(36, p.getAge());
        assertEquals(new Date(100), p.getBirthdate());
        assertEquals(Personality.EXTROVERT, p.getPersonality());
    }

    @Test
    public void testDeleteProperty() {
        final Node existingNode = createNewNode();
        converter.write(michael, existingNode);
        michael.setName(null);
        converter.write(michael, existingNode);
        assertEquals(false, existingNode.hasProperty("name"));
    }

    @Test
    public void testReadEntityFromExistingNode() {
        final Node node = createNewNode();
        node.setProperty("name", "Emil");
        final Person p = converter.read(Person.class, node);
        assertEquals("Emil", p.getName());
    }

    @Test
    public void testSetRelationshipWithPreExistingNode() {
        Person emil1 = emil;
        storeInGraph(emil1);
        michael.setBoss(emil);
        storeInGraph(michael);
        final Node boss = getRelatedNodes(michaelNode(), "boss", Direction.INCOMING).get(0);
        assertEquals("added additional relationship end node", emil.getId(), (Long) boss.getId());
    }

    private Group storeInGraph(Group g) {
        final Long id = g.getId();
        if (id != null) {
            converter.write(g, gdc.getNodeById(id));
        } else {
            converter.write(g, null);
        }
        return g;
    }

    private Person storeInGraph(Person p) {
        final Long id = p.getId();
        if (id != null) {
            converter.write(p, gdc.getNodeById(id));
        } else {
            converter.write(p, null);
        }
        return p;
    }

    private <T> T storeInGraph(T obj) {
        converter.write(obj, null);
        return obj;
    }

    @Test
    public void testAddRelationshipWithPreExistingNode() {
        storeInGraph(michael);
        group.setPersons(singleton(michael));
        storeInGraph(group);
        final Collection<Relationship> persons = IteratorUtil.asCollection(groupNode().getRelationships(PERSONS, Direction.OUTGOING));
        final Node michaelNode = persons.iterator().next().getOtherNode(groupNode());
        assertEquals("added michaelNode to group", michael.getId(), (Long) michaelNode.getId());
    }

    private Node groupNode() {
        return gdc.getNodeById(group.getId());
    }

    @Test
    public void testAddRelationshipWithTwoExistingNodes() {
        storeInGraph(michael);
        storeInGraph(andres);
        group.setPersons(set(andres, michael));
        storeInGraph(group);
        final Collection<Node> persons = groupMemberNodes();
        assertEquals(2, persons.size());
        assertEquals(asList(michaelNode(), andresNode()), persons);
    }

    @Test
    public void testAddRelationshipWithTwoPeopleButJustOneExistingNodes() {
        storeInGraph(michael);
        group.setPersons(set(andres, michael));
        storeInGraph(group);
        final Collection<Node> persons = groupMemberNodes();
        assertEquals(2, persons.size());
        assertEquals(asList(michaelNode(), andresNode()), persons);
    }


    private <T> Set<T> set(T... objs) {
        return new HashSet<T>(asList(objs));
    }

    private <T> Set<T> set(Iterable<T> objs) {
        return IteratorUtil.addToCollection(objs, new HashSet<T>());
    }

    @Test
    public void testAddRelationshipCascadeOverTwoSteps() {
        andres.setBoss(emil);
        group.setPersons(singleton(andres));
        storeInGraph(group);
        final Collection<Node> persons = groupMemberNodes();
        assertEquals(1, persons.size());
        assertEquals(asList(andresNode()), persons);
        assertEquals(emil.getId(), (Long) getRelatedNodes(andresNode(), "boss", Direction.INCOMING).get(0).getId());
    }

    private Node andresNode() {
        return gdc.getNodeById(andres.getId());
    }

    private Node emilNode() {
        return gdc.getNodeById(emil.getId());
    }

    @Test
    public void testDeleteSingleRelationship() {
        emil.setBoss(andres);
        storeInGraph(emil);

        emil.setBoss(null);
        storeInGraph(emil);

        assertEquals(0, getRelatedNodes(emilNode(), "boss", Direction.INCOMING).size());
    }

    @Test
    public void testDeleteMultipleRelationships() {
        group.setPersons(set(storeInGraph(emil), storeInGraph(michael), storeInGraph(andres)));
        storeInGraph(group);

        group.getPersons().remove(emil);
        storeInGraph(group);

        assertEquals(set(andresNode(), michaelNode()), set(groupMemberNodes()));
    }

    @Test
    public void testReadRelationshipCollectionFromGraph() {
        Node groupNode = createNewNode();
        Node p1 = createNewNode();
        Node p2 = createNewNode();
        groupNode.createRelationshipTo(p1, PERSONS);
        groupNode.createRelationshipTo(p2, PERSONS);

        Group g = converter.read(Group.class, groupNode);
        assertEquals(set(readPerson(p1), readPerson(p2)), set(g.getPersons()));
    }

    @Test
    public void testReadRelationshipIterableFromGraph() {
        Node groupNode = createNewNode();
        Node p1 = createNewNode();
        Node p2 = createNewNode();
        groupNode.createRelationshipTo(p1, PERSONS);
        groupNode.createRelationshipTo(p2, PERSONS);

        Group g = converter.read(Group.class, groupNode);
        assertEquals(set(readPerson(p1), readPerson(p2)), set(g.getReadOnlyPersons()));
    }

    @Test
    public void testRelationshipCollectionModificationIsReflectedInGraph() {
        group.setPersons(set(storeInGraph(emil), storeInGraph(andres)));
        storeInGraph(group);

        group.getPersons().remove(emil);
        group.getPersons().add(storeInGraph(michael));
        storeInGraph(group);

        assertEquals(set(andresNode(), michaelNode()), set(groupMemberNodes()));
    }

    @Test
    public void testNullValuesForRelationshipCollectionsAreIgnored() {
        group.setPersons(set(storeInGraph(emil)));
        storeInGraph(group);
        assertEquals(set(emilNode()), set(groupMemberNodes()));
        group.setPersons(null);
        storeInGraph(group);
        assertEquals(set(emilNode()), set(groupMemberNodes()));
    }

    public Person readPerson(Node node) {
        return converter.read(Person.class, node);
    }

    private List<Node> getRelatedNodes(Node startNode, String type, Direction direction) {
        List<Node> result = new ArrayList<Node>();
        for (Relationship relationship : startNode.getRelationships(DynamicRelationshipType.withName(type), direction)) {
            result.add(relationship.getOtherNode(startNode));
        }
        return result;
    }

    @Test
    public void testSetRelationshipWithNonExistingNode() {
        michael.setBoss(emil);
        storeInGraph(michael);
        final Node node = michaelNode();
        final Node boss = getRelatedNodes(node, "boss", Direction.INCOMING).get(0);
        assertEquals("added additional relationship end node", emil.getId(), (Long) boss.getId());
    }

    @Test
    public void testAddRelationshipWithNonExistingNode() {
        group.setPersons(singleton(michael));
        storeInGraph(group);
        final Node groupNode = groupNode();
        final Node michaelNode = getRelatedNodes(groupNode, "persons", Direction.OUTGOING).get(0);
        assertEquals("added member to group", michael.getId(), (Long) michaelNode.getId());
    }


    @Test
    public void testCascadingReadWithProperties() {
        Node groupNode = createNewNode();
        Node julianNode = createNewNode();
        julianNode.setProperty("name", "Julian");
        groupNode.createRelationshipTo(julianNode, PERSONS);

        Group g = converter.read(Group.class, groupNode);
        Person julian = IteratorUtil.first(g.getPersons());
        assertEquals("Julian", julian.getName());
    }

    @Test
    public void testLoadFriendShipsFromPersons() throws Exception {
        storeInGraph(michael);
        storeInGraph(andres);

        Relationship friendshipRelationship = michaelNode().createRelationshipTo(andresNode(), KNOWS);
        friendshipRelationship.setProperty("Friendship.years", 19);

        Person m = converter.read(Person.class, michaelNode());
        Friendship friendship = IteratorUtil.first(m.getFriendships());

        assertEquals((Long) friendshipRelationship.getId(), friendship.getId());
        assertEquals(19, friendship.getYears());
        assertEquals(friendship.getPerson1(), michael);
        assertEquals(friendship.getPerson2(), andres);
    }

}
