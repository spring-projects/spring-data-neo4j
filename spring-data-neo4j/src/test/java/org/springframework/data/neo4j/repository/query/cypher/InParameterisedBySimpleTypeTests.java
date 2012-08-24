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
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.test.ImpermanentGraphDatabase;
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

import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@NodeEntity
class SimpleTypeEntity {
    @GraphId
    Long id;

    boolean bo;
    byte by;
    short sh;
    int i;
    long l;
    float f;
    double d;
    char c;
    String st = UUID.randomUUID().toString();

    SimpleTypeEntity() {
    }

    SimpleTypeEntity(boolean bo, byte by, short sh, int i, long l, float f, double d, char c, String st) {
        this.bo = bo;
        this.by = by;
        this.sh = sh;
        this.i = i;
        this.l = l;
        this.f = f;
        this.d = d;
        this.c = c;
        this.st = st;
    }
}

interface SimpleTypeEntityRepository extends CRUDRepository<SimpleTypeEntity> {
    @Query("start n=node(*) where n.bo in {0} return n")
    SimpleTypeEntity findUsingBoolean(boolean... value);

    @Query("start n=node(*) where n.by in {0} return n")
    SimpleTypeEntity findUsingByte(byte... value);

    @Query("start n=node(*) where n.sh in {0} return n")
    SimpleTypeEntity findUsingShort(short... value);

    @Query("start n=node(*) where n.i in {0} return n")
    SimpleTypeEntity findUsingInt(int... value);

    @Query("start n=node(*) where n.l in {0} return n")
    SimpleTypeEntity findUsingLong(long... value);

    @Query("start n=node(*) where n.f in {0} return n")
    SimpleTypeEntity findUsingFloat(float... value);

    @Query("start n=node(*) where n.d in {0} return n")
    SimpleTypeEntity findUsingDouble(double... value);

    @Query("start n=node(*) where n.c in {0} return n")
    SimpleTypeEntity findUsingChar(char... value);

    @Query("start n=node(*) where n.st in {0} return n")
    SimpleTypeEntity findUsingString(String... value);
}

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class InParameterisedBySimpleTypeTests {
    @Configuration
    @EnableNeo4jRepositories
    static class TestConfig extends Neo4jConfiguration {
        @Bean
        GraphDatabaseService graphDatabaseService() {
            return new ImpermanentGraphDatabase();
        }
    }

    public static final byte BY = 0x42;
    public static final boolean BO = true;
    public static final short S = 23;
    public static final int I = 63;
    public static final long L = 87l;
    public static final float F = 3.14f;
    public static final double D = 2.97;
    public static final char C = '!';

    public static final String ST = "dub";

    @Autowired
    GraphDatabaseService graphDatabaseService;

    @Autowired
    SimpleTypeEntityRepository simpleTypeEntityRepository;

    @Before
    public void before() {
        Neo4jHelper.cleanDb(graphDatabaseService, true);

        Transaction transaction = graphDatabaseService.beginTx();

        try {
            simpleTypeEntityRepository.save(new SimpleTypeEntity());
            simpleTypeEntityRepository.save(new SimpleTypeEntity(BO, BY, S, I, L, F, D, C, ST));

            transaction.success();
        } finally {
            transaction.finish();
        }
    }

    @Test
    public void shouldSupportSimpleTypesAsParametersForIn() throws Exception {
        assertThat(simpleTypeEntityRepository.findUsingBoolean(BO).bo, is(BO));
        assertThat(simpleTypeEntityRepository.findUsingShort(S).sh, is(S));
        assertThat(simpleTypeEntityRepository.findUsingInt(I).i, is(I));
        assertThat(simpleTypeEntityRepository.findUsingLong(L).l, is(L));
        assertThat(simpleTypeEntityRepository.findUsingFloat(F).f, is(F));
        assertThat(simpleTypeEntityRepository.findUsingDouble(D).d, is(D));
        assertThat(simpleTypeEntityRepository.findUsingChar(C).c, is(C));
        assertThat(simpleTypeEntityRepository.findUsingString(ST).st, is(ST));
    }

    @Test
    @Ignore("see https://github.com/neo4j/community/issues/788")
    public void shouldSupportByteAsParameterForIn() throws Exception {
        assertThat(simpleTypeEntityRepository.findUsingByte(BY).by, is(BY));
    }
}