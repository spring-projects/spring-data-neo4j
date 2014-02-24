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

import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@NodeEntity
class SimpleTypeEntity {
    @GraphId
    Long id;

    boolean aBoolean;
    byte aByte;
    short aShort;
    int anInt;
    long aLong;
    float aFloat;
    double aDouble;
    char aChar;
    String aString = UUID.randomUUID().toString();

    SimpleTypeEntity() {
    }

    SimpleTypeEntity(boolean aBoolean, byte aByte, short aShort, int anInt, long aLong, float aFloat,
                     double aDouble, char aChar, String aString) {
        this.aBoolean = aBoolean;
        this.aByte = aByte;
        this.aShort = aShort;
        this.anInt = anInt;
        this.aLong = aLong;
        this.aFloat = aFloat;
        this.aDouble = aDouble;
        this.aChar = aChar;
        this.aString = aString;
    }
}

interface SimpleTypeEntityRepository extends CRUDRepository<SimpleTypeEntity> {
    @Query("start n=node(*) where n.aBoolean in {0} return n")
    SimpleTypeEntity findUsingBoolean(boolean... value);

    @Query("start n=node(*) where n.aByte in {0} return n")
    SimpleTypeEntity findUsingByte(byte... value);

    @Query("start n=node(*) where n.aShort in {0} return n")
    SimpleTypeEntity findUsingShort(short... value);

    @Query("start n=node(*) where n.anInt in {0} return n")
    SimpleTypeEntity findUsingInt(int... value);

    @Query("start n=node(*) where n.aLong in {0} return n")
    SimpleTypeEntity findUsingLong(long... value);

    @Query("start n=node(*) where n.aFloat in {0} return n")
    SimpleTypeEntity findUsingFloat(float... value);

    @Query("start n=node(*) where n.aDouble in {0} return n")
    SimpleTypeEntity findUsingDouble(double... value);

    @Query("start n=node(*) where n.aChar in {0} return n")
    SimpleTypeEntity findUsingChar(char... value);

    @Query("start n=node(*) where n.aString in {0} return n")
    SimpleTypeEntity findUsingString(String... value);
}

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class InParameterisedBySimpleTypeTests {
    @Configuration
    @EnableNeo4jRepositories
    static class TestConfig extends Neo4jConfiguration {
        TestConfig() throws ClassNotFoundException {
            setBasePackage(SimpleTypeEntity.class.getPackage().getName());
        }

        @Bean
        GraphDatabaseService graphDatabaseService() {
            return new TestGraphDatabaseFactory().newImpermanentDatabase();
        }
    }

    public static final byte BYTE = 0x42;
    public static final boolean BOOLEAN = true;
    public static final short SHORT = 23;
    public static final int INT = 63;
    public static final long LONG = 87l;
    public static final float FLOAT = 3.14f;
    public static final double DOUBLE = 2.97;
    public static final char CHAR = '!';
    public static final String STRING = "dub";

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
            simpleTypeEntityRepository.save(new SimpleTypeEntity(BOOLEAN, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE,
                    CHAR, STRING));

            transaction.success();
        } finally {
            transaction.finish();
        }
    }

    @Test
    public void shouldSupportSimpleTypesAsParametersForIn() throws Exception {
        assertThat(simpleTypeEntityRepository.findUsingBoolean(BOOLEAN).aBoolean, is(BOOLEAN));
        assertThat(simpleTypeEntityRepository.findUsingByte(BYTE).aByte, is(BYTE));
        assertThat(simpleTypeEntityRepository.findUsingShort(SHORT).aShort, is(SHORT));
        assertThat(simpleTypeEntityRepository.findUsingInt(INT).anInt, is(INT));
        assertThat(simpleTypeEntityRepository.findUsingLong(LONG).aLong, is(LONG));
        assertThat(simpleTypeEntityRepository.findUsingFloat(FLOAT).aFloat, is(FLOAT));
        assertThat(simpleTypeEntityRepository.findUsingDouble(DOUBLE).aDouble, is(DOUBLE));
        assertThat(simpleTypeEntityRepository.findUsingChar(CHAR).aChar, is(CHAR));
        assertThat(simpleTypeEntityRepository.findUsingString(STRING).aString, is(STRING));
    }
}
