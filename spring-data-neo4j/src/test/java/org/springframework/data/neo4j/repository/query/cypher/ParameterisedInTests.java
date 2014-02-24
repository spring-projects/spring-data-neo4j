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
package org.springframework.data.neo4j.repository.query.cypher;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.test.TestGraphDatabaseFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.config.Neo4jConfiguration;
import org.springframework.data.neo4j.repository.CRUDRepository;
import org.springframework.data.neo4j.support.node.Neo4jHelper;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@NodeEntity
class RandomEntity {
    @GraphId
    Long id;

    String name;

    RandomEntity() {
    }

    RandomEntity(String name) {
        this.name = name;
    }
}

interface RandomEntityRepository extends CRUDRepository<RandomEntity> {
    @Query("start n=node(*) where n.name in {0} return n")
    RandomEntity findUsingIterable(Iterable<String> names);

    @Query("start n=node(*) where n.name in {0} return n")
    RandomEntity findUsingArray(String[] names);

    @Query("start n=node(*) where n.name in {0} return n")
    RandomEntity findUsingVarargs(String... names);
}

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class ParameterisedInTests {
    @Configuration
    @EnableNeo4jRepositories
    static class TestConfig extends Neo4jConfiguration {
        TestConfig() throws ClassNotFoundException {
            setBasePackage(RandomEntity.class.getPackage().getName());
        }

        @Bean
        GraphDatabaseService graphDatabaseService() {
            return new TestGraphDatabaseFactory().newImpermanentDatabase();
        }
    }

    @Autowired
    GraphDatabaseService graphDatabaseService;

    @Autowired
    RandomEntityRepository randomEntityRepository;

    @Before
    public void before() {
        Neo4jHelper.cleanDb(graphDatabaseService, true);

        Transaction transaction = graphDatabaseService.beginTx();

        try {
            randomEntityRepository.save(new RandomEntity("Huey"));
            randomEntityRepository.save(new RandomEntity("Dewey"));
            randomEntityRepository.save(new RandomEntity("Louie"));

            transaction.success();
        } finally {
            transaction.finish();
        }
    }

    @Test
    public void shouldSupportParametersAsIterable() throws Exception {
        assertThat(randomEntityRepository.findUsingIterable(asList("Huey")).name, is("Huey"));
    }

    @Test
    public void shouldSupportParametersAsArray() throws Exception {
        assertThat(randomEntityRepository.findUsingArray(new String[]{"Dewey"}).name, is("Dewey"));
    }

    @Test
    public void shouldSupportParametersAsVarargs() throws Exception {
        assertThat(randomEntityRepository.findUsingVarargs("Louie").name, is("Louie"));
    }
}
