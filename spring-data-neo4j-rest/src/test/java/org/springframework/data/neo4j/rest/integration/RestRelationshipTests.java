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

package org.springframework.data.neo4j.rest.integration;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.GraphDatabaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.annotation.*;
import org.springframework.data.neo4j.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.config.Neo4jConfiguration;
import org.springframework.data.neo4j.rest.SpringCypherRestGraphDatabase;
import org.springframework.data.neo4j.rest.SpringRestGraphDatabase;
import org.springframework.data.neo4j.rest.support.RestTestBase;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import static org.junit.Assert.assertEquals;

/**
* @author mh
* @since 28.03.11
*/
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = RestRelationshipTests.MyConfig.class)
public class RestRelationshipTests {

    @Configuration
    public static class MyConfig extends Neo4jConfiguration {
        public MyConfig() {
            setBasePackage("org.springframework.data.neo4j.rest.integration");
        }

        @Bean
        public GraphDatabaseService graphDatabaseService() {
            return new SpringCypherRestGraphDatabase(RestTestBase.SERVER_ROOT_URI);
//            return new SpringRestGraphDatabase(RestTestBase.SERVER_ROOT_URI);
        }
    }

    @NodeEntity
    public static class A {
        @GraphId
        Long id;
        String name;

        public A(String name) {
            this.name = name;
        }

        public A() { }
    }

    @RelationshipEntity(type = "AREL")
    public static class ARel {
        @GraphId
        Long id;
        @StartNode A a1;
        @EndNode
        A a2;

        String prop;

        public ARel(A a1, A a2, String prop) {
            this.a1 = a1;
            this.a2 = a2;
            this.prop = prop;
        }

        public ARel() { }
    }

    @Autowired
    Neo4jTemplate neo4jTemplate;

    @BeforeClass
    public static void startDb() throws Exception {
        RestTestBase.startDb();
    }

    @Before
    public void cleanDb() {
        RestTestBase.cleanDb();
    }

    @AfterClass
    public static void shutdownDb() {
        RestTestBase.shutdownDb();

    }

    @Test
    public void testRelationshipSaveProperty() {
        A a1 = neo4jTemplate.save(new A("a1"));
        A a2 = neo4jTemplate.save(new A("a2"));
        ARel rel = neo4jTemplate.save(new ARel(a1, a2, "foo"));
        ARel rel2 = neo4jTemplate.findOne(rel.id, ARel.class);
        assertEquals("foo",rel2.prop);
    }

}
