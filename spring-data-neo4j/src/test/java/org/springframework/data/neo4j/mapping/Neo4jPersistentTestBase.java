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
import org.neo4j.graphdb.*;
import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.test.ImpermanentGraphDatabase;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.convert.DefaultTypeMapper;
import org.springframework.data.convert.TypeMapper;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.fieldaccess.Neo4jConversionServiceFactoryBean;
import org.springframework.data.neo4j.fieldaccess.NodeDelegatingFieldAccessorFactory;
import org.springframework.data.neo4j.fieldaccess.RelationshipDelegatingFieldAccessorFactory;
import org.springframework.data.neo4j.model.Group;
import org.springframework.data.neo4j.model.Person;
import org.springframework.data.neo4j.support.EntityStateHandler;
import org.springframework.data.neo4j.support.EntityTools;
import org.springframework.data.neo4j.support.GraphDatabaseContext;
import org.springframework.data.neo4j.support.node.NodeEntityInstantiator;
import org.springframework.data.neo4j.support.node.NodeEntityStateFactory;
import org.springframework.data.neo4j.support.relationship.RelationshipEntityInstantiator;
import org.springframework.data.neo4j.support.relationship.RelationshipEntityStateFactory;
import org.springframework.data.neo4j.support.typerepresentation.NoopNodeTypeRepresentationStrategy;
import org.springframework.data.neo4j.support.typerepresentation.NoopRelationshipTypeRepresentationStrategy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;

/**
 * @author mh
 * @since 12.10.11
 */
public class Neo4jPersistentTestBase {
    private Transaction tx;
    protected GraphDatabaseContext gdc;
    protected NodeEntityStateFactory nodeEntityStateFactory;
    protected RelationshipEntityStateFactory relationshipEntityStateFactory;
    protected EntityStateHandler entityStateHandler;
    protected NodeEntityInstantiator nodeEntityInstantiator;
    protected RelationshipEntityInstantiator relationshipEntityInstantiator;
    protected TypeMapper<Node> nodeTypeMapper;
    protected SourceStateTransmitter<Node> nodeStateTransmitter;
    protected SourceStateTransmitter<Relationship> relationshipStateTransmitter;
    protected ConversionService conversionService;
    protected Neo4jEntityFetchHandler fetchHandler;
    protected Neo4jMappingContext mappingContext;
    protected Neo4jEntityPersister entityPersister;


    protected Group group;
    protected Person michael;
    protected Person emil;
    protected Person andres;
    public static final RelationshipType PERSONS = DynamicRelationshipType.withName("persons");
    protected static final RelationshipType KNOWS = DynamicRelationshipType.withName("knows");

    @NodeEntity
    public static class Developer {
        @GraphId
        Long id;
        String name;
    }

    @Before
    public void setUp() throws Exception {
        mappingContext = new Neo4jMappingContext();
        gdc = createContext(mappingContext);
        tx = gdc.beginTx();

        nodeEntityStateFactory = createNodeEntityStateFactory(mappingContext);
        relationshipEntityStateFactory = createRelationshipEntityStateFactory(mappingContext);
        gdc.setNodeEntityStateFactory(nodeEntityStateFactory);
        gdc.setRelationshipEntityStateFactory(relationshipEntityStateFactory);

        gdc.postConstruct();

        entityStateHandler = gdc.getEntityStateHandler();

        nodeEntityInstantiator = new NodeEntityInstantiator(entityStateHandler);
        relationshipEntityInstantiator = new RelationshipEntityInstantiator(entityStateHandler);
        nodeTypeMapper = new DefaultTypeMapper<Node>(new TRSTypeAliasAccessor<Node>(gdc.getNodeTypeRepresentationStrategy()),asList(new ClassValueTypeInformationMapper()));
        nodeStateTransmitter = new SourceStateTransmitter<Node>(nodeEntityStateFactory);
        relationshipStateTransmitter = new SourceStateTransmitter<Relationship>(relationshipEntityStateFactory);
        conversionService = gdc.getConversionService();

        fetchHandler = new Neo4jEntityFetchHandler(entityStateHandler, conversionService, nodeStateTransmitter, relationshipStateTransmitter);
        final EntityTools<Node> nodeEntityTools = new EntityTools<Node>(gdc.getNodeTypeRepresentationStrategy(), nodeEntityStateFactory, nodeEntityInstantiator);
        final EntityTools<Relationship> relationshipEntityTools = new EntityTools<Relationship>(gdc.getRelationshipTypeRepresentationStrategy(), relationshipEntityStateFactory, relationshipEntityInstantiator);

        entityPersister = new Neo4jEntityPersister(conversionService, nodeEntityTools, relationshipEntityTools, mappingContext, entityStateHandler);

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
        return gdc;
    }

    protected List<Node> groupMemberNodes() {
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

    protected Node michaelNode() {
        return gdc.getNodeById(michael.getId());
    }

    protected Node createNewNode() {
        return gdc.createNode();
    }

    protected Group storeInGraph(Group g) {
        final Long id = g.getId();
        if (id != null) {
            write(g, gdc.getNodeById(id));
        } else {
            write(g, null);
        }
        return g;
    }

    protected Person storeInGraph(Person p) {
        final Long id = p.getId();
        if (id != null) {
            write(p, gdc.getNodeById(id));
        } else {
            write(p,null);
        }
        return p;
    }

    protected Object write(Object entity, Node node) {
        entityPersister.write(entity, node);
        return entity;
    }

    @SuppressWarnings("unchecked")
    private <T> T storeInGraph(T obj) {
        return (T) write(obj,null);
    }

    protected Node groupNode() {
        return gdc.getNodeById(group.getId());
    }

    protected <T> Set<T> set(T... objs) {
        return new HashSet<T>(asList(objs));
    }

    protected <T> Set<T> set(Iterable<T> objs) {
        return IteratorUtil.addToCollection(objs, new HashSet<T>());
    }

    protected Node andresNode() {
        return gdc.getNodeById(andres.getId());
    }

    protected Node emilNode() {
        return gdc.getNodeById(emil.getId());
    }

    public Person readPerson(Node node) {
        return entityPersister.read(Person.class, node);
    }
    public Group readGroup(Node node) {
        return entityPersister.read(Group.class, node);
    }

    protected List<Node> getRelatedNodes(Node startNode, String type, Direction direction) {
        List<Node> result = new ArrayList<Node>();
        for (Relationship relationship : startNode.getRelationships(DynamicRelationshipType.withName(type), direction)) {
            result.add(relationship.getOtherNode(startNode));
        }
        return result;
    }
}
