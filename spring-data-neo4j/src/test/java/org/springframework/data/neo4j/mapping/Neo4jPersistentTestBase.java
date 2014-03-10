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
import org.neo4j.test.TestGraphDatabaseFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.convert.DefaultTypeMapper;
import org.springframework.data.convert.TypeMapper;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.config.BasePackageScanner;
import org.springframework.data.neo4j.fieldaccess.DelegatingFieldAccessorFactory;
import org.springframework.data.neo4j.fieldaccess.FieldAccessorFactoryFactory;
import org.springframework.data.neo4j.fieldaccess.Neo4jConversionServiceFactoryBean;
import org.springframework.data.neo4j.fieldaccess.NodeDelegatingFieldAccessorFactory;
import org.springframework.data.neo4j.fieldaccess.RelationshipDelegatingFieldAccessorFactory;
import org.springframework.data.neo4j.model.Group;
import org.springframework.data.neo4j.model.Person;
import org.springframework.data.neo4j.support.DelegatingGraphDatabase;
import org.springframework.data.neo4j.support.Infrastructure;
import org.springframework.data.neo4j.support.MappingInfrastructureFactoryBean;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.index.IndexProviderImpl;
import org.springframework.data.neo4j.support.mapping.*;
import org.springframework.data.neo4j.support.node.EntityStateFactory;
import org.springframework.data.neo4j.support.node.NodeEntityInstantiator;
import org.springframework.data.neo4j.support.node.NodeEntityStateFactory;
import org.springframework.data.neo4j.support.relationship.RelationshipEntityInstantiator;
import org.springframework.data.neo4j.support.relationship.RelationshipEntityStateFactory;
import org.springframework.data.neo4j.support.schema.SchemaIndexProvider;
import org.springframework.data.neo4j.support.typerepresentation.ClassValueTypeInformationMapper;
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
    protected Neo4jTemplate template;
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
        public Long id;
        public String name;
    }

    @Before
    public void setUp() throws Exception {
        // todo cleanup !!
        mappingContext = new Neo4jMappingContext();
        Infrastructure infrastructure = createInfrastructure(mappingContext);
        template = new Neo4jTemplate(infrastructure);
        conversionService = template.getConversionService();


        tx = template.getGraphDatabase().beginTx();
        group = new Group();
        michael = new Person("Michael", 37);
        emil = new Person("Emil", 30);
        andres = new Person("Andrés", 36);
    }

    private Infrastructure createInfrastructure(Neo4jMappingContext mappingContext) throws Exception {
        MappingInfrastructureFactoryBean factoryBean = new MappingInfrastructureFactoryBean();
        final GraphDatabaseService gdb = new TestGraphDatabaseFactory().newImpermanentDatabase();
        factoryBean.setGraphDatabaseService(gdb);
        final DelegatingGraphDatabase graphDatabase = new DelegatingGraphDatabase(gdb);
        factoryBean.setGraphDatabase(graphDatabase);
        factoryBean.setMappingContext(mappingContext);
        final EntityStateHandler entityStateHandler = new EntityStateHandler(mappingContext, graphDatabase);
        final NoopNodeTypeRepresentationStrategy nodeTypeRepresentationStrategy = new NoopNodeTypeRepresentationStrategy();
        factoryBean.setNodeTypeRepresentationStrategy(nodeTypeRepresentationStrategy);
        final NoopRelationshipTypeRepresentationStrategy relationshipTypeRepresentationStrategy = new NoopRelationshipTypeRepresentationStrategy();
        factoryBean.setRelationshipTypeRepresentationStrategy(relationshipTypeRepresentationStrategy);
        factoryBean.setConversionService(new Neo4jConversionServiceFactoryBean().getObject());
        factoryBean.setEntityStateHandler(entityStateHandler);

        EntityStateFactory<Node> nodeEntityStateFactory = new NodeEntityStateFactory(mappingContext, new FieldAccessorFactoryFactory() {
                    public DelegatingFieldAccessorFactory create(Neo4jTemplate template) {
                        return new NodeDelegatingFieldAccessorFactory(template);
                    }
                });

        EntityStateFactory<Relationship> relationshipEntityStateFactory = new RelationshipEntityStateFactory(mappingContext, new FieldAccessorFactoryFactory() {
                    public DelegatingFieldAccessorFactory create(Neo4jTemplate template) {
                        return new RelationshipDelegatingFieldAccessorFactory(template);
                    }
                });
        factoryBean.setNodeEntityStateFactory(nodeEntityStateFactory);
        factoryBean.setRelationshipEntityStateFactory(relationshipEntityStateFactory);

        mappingContext.setEntityIndexCreator(new EntityIndexCreator(new IndexProviderImpl(graphDatabase), new SchemaIndexProvider(graphDatabase),true));
        mappingContext.setSimpleTypeHolder(null);
        setBasePackage(mappingContext);

        nodeEntityInstantiator = new NodeEntityInstantiator(entityStateHandler);
        relationshipEntityInstantiator = new RelationshipEntityInstantiator(entityStateHandler);
        nodeTypeMapper = new DefaultTypeMapper<Node>(new TRSTypeAliasAccessor<Node>(nodeTypeRepresentationStrategy), asList(new ClassValueTypeInformationMapper()));
        nodeStateTransmitter = new SourceStateTransmitter<Node>(nodeEntityStateFactory);
        relationshipStateTransmitter = new SourceStateTransmitter<Relationship>(relationshipEntityStateFactory);

        fetchHandler = new Neo4jEntityFetchHandler(entityStateHandler, conversionService, nodeStateTransmitter, relationshipStateTransmitter);
        final EntityTools<Node> nodeEntityTools = new EntityTools<Node>(nodeTypeRepresentationStrategy, nodeEntityStateFactory, nodeEntityInstantiator, mappingContext);
        final EntityTools<Relationship> relationshipEntityTools = new EntityTools<Relationship>(relationshipTypeRepresentationStrategy, relationshipEntityStateFactory, relationshipEntityInstantiator, mappingContext);

        entityPersister = new Neo4jEntityPersister(conversionService, nodeEntityTools, relationshipEntityTools, mappingContext, entityStateHandler);
        mappingContext.afterPropertiesSet();
        factoryBean.afterPropertiesSet();
        return factoryBean.getObject();
    }

    protected void setBasePackage(Neo4jMappingContext mappingContext) throws ClassNotFoundException {
        setBasePackage(mappingContext, getClass().getPackage().getName());
    }

    protected void setBasePackage(Neo4jMappingContext mappingContext, String...basePackages) throws ClassNotFoundException {
        mappingContext.setInitialEntitySet(BasePackageScanner.scanBasePackageForClasses(basePackages));
    }

    protected List<Node> groupMemberNodes() {
        return groupMemberNodes(groupNode());
    }

    private List<Node> groupMemberNodes(Node node) {
        return getRelatedNodes(node, "persons", Direction.OUTGOING);
    }

    @After
    public void tearDown() throws Exception {
        if (tx!=null) {
            tx.failure();
            tx.close();
        }
        template.getGraphDatabaseService().shutdown();
    }

    protected Node michaelNode() {
        return template.getNode(michael.getId());
    }

    protected Node createNewNode() {
        return template.createNode();
    }

    protected Group storeInGraph(Group g) {
        final Long id = g.getId();
        if (id != null) {
            write(g, template.getNode(id));
        } else {
            write(g, null);
        }
        return g;
    }

    protected Person storeInGraph(Person p) {
        final Long id = p.getId();
        if (id != null) {
            write(p, template.getNode(id));
        } else {
            write(p, null);
        }
        return p;
    }

    protected Object write(Object entity, Node node) {
        entityPersister.write(entity, node, template.getMappingPolicy(entity), template, null );
        return entity;
    }

    @SuppressWarnings("unchecked")
    private <T> T storeInGraph(T obj) {
        return (T) write(obj, null);
    }

    protected Node groupNode() {
        return template.getNode(group.getId());
    }

    protected <T> Set<T> set(T... objs) {
        return new HashSet<T>(asList(objs));
    }

    protected <T> Set<T> set(Iterable<T> objs) {
        return IteratorUtil.addToCollection(objs, new HashSet<T>());
    }

    protected Node andresNode() {
        return template.getNode(andres.getId());
    }

    protected Node emilNode() {
        return template.getNode(emil.getId());
    }

    public Person readPerson(Node node) {
        return entityPersister.read(Person.class, node, template.getMappingPolicy(Person.class), template);
    }

    protected Relationship makeFriends(Node from, Node to, int years) {
        Relationship friendship = from.createRelationshipTo(to, KNOWS);
        friendship.setProperty("Friendship.years", years);
        return friendship;
    }

    public Group readGroup(Node node) {
        return entityPersister.read(Group.class, node,template.getMappingPolicy(Group.class), template);
    }

    protected List<Node> getRelatedNodes(Node startNode, String type, Direction direction) {
        List<Node> result = new ArrayList<Node>();
        for (Relationship relationship : startNode.getRelationships(DynamicRelationshipType.withName(type), direction)) {
            result.add(relationship.getOtherNode(startNode));
        }
        return result;
    }
}
