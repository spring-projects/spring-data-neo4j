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

import java.util.*;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.springframework.data.neo4j.repository.query.cypher.Suit.*;

enum Suit {
    CLUB, DIAMOND, HEART, SPADE
}

@NodeEntity
class EnumEntity {
    @GraphId
    Long id;

    Suit suit;

    Suit[] otherSuits;

    EnumEntity() {
    }

    EnumEntity(Suit suit) {
        this.suit = suit;
        List<Suit> suits = new ArrayList<Suit>(asList(Suit.values()));
        suits.remove(suit);
        this.otherSuits = suits.toArray(new Suit[0]);
    }
}

interface EnumEntityRepository extends CRUDRepository<EnumEntity> {
    @Query("start n=node(*) where n.suit in {0} return n")
    EnumEntity findUsingEnum(Suit value);

    @Query("start n=node(*) where n.suit in {0} return n")
    EnumEntity findUsingArrayOfEnum(Suit... value);

    @Query("start n=node(*) where n.otherSuits in {0} return n")
    EnumEntity findUsingArrayOfArrayOfString(Iterable<String[]> value);

    @Query("start n=node(*) where n.otherSuits in {0} return n")
    EnumEntity findUsingArrayOfArrayOfEnum(Iterable<Suit[]> value);
}

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class InParameterisedByEnumTests {
    @Configuration
    @EnableNeo4jRepositories
    static class TestConfig extends Neo4jConfiguration {

        TestConfig() throws ClassNotFoundException {
            setBasePackage(EnumEntity.class.getPackage().getName());
        }

        @Bean
        GraphDatabaseService graphDatabaseService() {
            return new TestGraphDatabaseFactory().newImpermanentDatabase();
        }
    }

    @Autowired
    GraphDatabaseService graphDatabaseService;

    @Autowired
    EnumEntityRepository enumEntityRepository;

    @Before
    public void before() {
        Neo4jHelper.cleanDb(graphDatabaseService, true);

        Transaction transaction = graphDatabaseService.beginTx();

        try {
            enumEntityRepository.save(new EnumEntity(SPADE));

            transaction.success();
        } finally {
            transaction.finish();
        }
    }

    @Test
    public void shouldSupportSingleEnumsAsParameterForIn() throws Exception {
        assertThat(enumEntityRepository.findUsingEnum(SPADE).suit, is(SPADE));
    }

    @Test
    public void shouldSupportArrayOfEnumAsParameterForIn() throws Exception {
        assertThat(enumEntityRepository.findUsingArrayOfEnum(SPADE).suit, is(SPADE));
        assertThat(enumEntityRepository.findUsingArrayOfEnum(SPADE, HEART).suit, is(SPADE));
    }

    @Test
    public void shouldSupportArrayOfArrayOfEnumAsParameterForIn() throws Exception {
        assertThat(enumEntityRepository.findUsingArrayOfArrayOfString(asList(new String[]{"CLUB", "DIAMOND", "HEART"}, new String[]{"SPADE", "DIAMOND"})).suit, is(SPADE));
        assertThat(enumEntityRepository.findUsingArrayOfArrayOfEnum(asList(new Suit[]{CLUB, DIAMOND, HEART}, new Suit[]{SPADE, DIAMOND})).suit, is(SPADE));
    }
}
