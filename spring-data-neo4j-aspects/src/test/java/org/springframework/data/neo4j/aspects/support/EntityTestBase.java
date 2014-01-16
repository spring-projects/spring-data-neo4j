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
package org.springframework.data.neo4j.aspects.support;

import org.junit.Before;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.neo4j.aspects.FriendshipRepository;
import org.springframework.data.neo4j.aspects.GroupRepository;
import org.springframework.data.neo4j.aspects.PersonRepository;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.mapping.StoredEntityType;
import org.springframework.data.neo4j.support.node.Neo4jHelper;
import org.springframework.test.context.transaction.BeforeTransaction;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author mh
 * @since 15.10.11
 */
public class EntityTestBase {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired protected Neo4jTemplate neo4jTemplate;
    @Autowired protected ConversionService conversionService;

    @Autowired protected GraphDatabaseService graphDatabaseService;

    @Autowired protected PersonRepository personRepository;
    @Autowired protected GroupRepository groupRepository;
    @Autowired protected FriendshipRepository friendshipRepository;
    protected TestTeam testTeam;

    @Before
    public void createTeam() throws Exception {
        testTeam = new TestTeam(neo4jTemplate);
    }

    protected Node getNodeState(Object entity) {
        return neo4jTemplate.getPersistentState(entity);
    }
    protected Long getNodeId(Object entity) {
        final Node node = neo4jTemplate.getPersistentState(entity);
        return node == null ? null : node.getId();
    }
    protected Long getRelationshipId(Object entity) {
        final Relationship rel = neo4jTemplate.getPersistentState(entity);
        return rel == null ? null : rel.getId();
    }

    protected boolean hasPersistentState(Object entity) {
        return neo4jTemplate.getPersistentState(entity)!=null;
    }

    protected Relationship getRelationshipState(Object entity) {
        return neo4jTemplate.getPersistentState(entity);
    }

    @SuppressWarnings("unchecked")
    public <T> T persist(T entity) {
        return (T) neo4jTemplate.save(entity);
    }

    protected <T> Set<T> set(T... values) {
        return new HashSet<T>(Arrays.<T>asList(values));
    }

    protected void manualCleanDb() {
        try (Transaction tx = graphDatabaseService.beginTx()) {
            cleanDb();
            tx.success();
        }
	}

    @BeforeTransaction
    public void cleanDb() {
        Neo4jHelper.cleanDb(neo4jTemplate);
    }

    protected StoredEntityType typeOf(Class<?> type) {
        return neo4jTemplate.getEntityType(type);
    }
}
