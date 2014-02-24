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
class SimpleTypeArrayEntity {
    @GraphId
    Long id;

    boolean[] booleans = {false, false, false};
    byte[] bytes = "Hello, world!\n".getBytes();
    short[] shorts = {1, 2, 3};
    int[] ints = {4, 5, 6};
    long[] longs = {7,8,9};
    float[] floats = {1.0f, 1.1f, 1.2f};
    double[] doubles = {1.3, 1.4, 1.5};
    char[] chars = "Hello Darkness, my old friend...".toCharArray();
    String[] strings = {"bigger", "better", "faster", "stronger"};

    SimpleTypeArrayEntity() {
    }

    SimpleTypeArrayEntity(boolean[] booleans, byte[] bytes, short[] shorts, int[] ints, long[] longs, float[] floats, double[] doubles, char[] chars, String[] strings) {
        this.booleans = booleans;
        this.bytes = bytes;
        this.shorts = shorts;
        this.ints = ints;
        this.longs = longs;
        this.floats = floats;
        this.doubles = doubles;
        this.chars = chars;
        this.strings = strings;
    }
}

interface SimpleTypeArrayEntityRepository extends CRUDRepository<SimpleTypeArrayEntity> {
    @Query("start n=node(*) where n.booleans in {0} return n")
    SimpleTypeArrayEntity findUsingBoolean(Iterable<boolean[]> value);

    @Query("start n=node(*) where n.bytes in {0} return n")
    SimpleTypeArrayEntity findUsingByte(Iterable<byte[]> value);

    @Query("start n=node(*) where n.shorts in {0} return n")
    SimpleTypeArrayEntity findUsingShort(Iterable<short[]> value);

    @Query("start n=node(*) where n.ints in {0} return n")
    SimpleTypeArrayEntity findUsingInt(Iterable<int[]> value);

    @Query("start n=node(*) where n.longs in {0} return n")
    SimpleTypeArrayEntity findUsingLong(Iterable<long[]> value);

    @Query("start n=node(*) where n.floats in {0} return n")
    SimpleTypeArrayEntity findUsingFloat(Iterable<float[]> value);

    @Query("start n=node(*) where n.doubles in {0} return n")
    SimpleTypeArrayEntity findUsingDouble(Iterable<double[]> value);

    @Query("start n=node(*) where n.chars in {0} return n")
    SimpleTypeArrayEntity findUsingChar(Iterable<char[]> value);

    @Query("start n=node(*) where n.strings in {0} return n")
    SimpleTypeArrayEntity findUsingString(Iterable<String[]> value);
}

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class InParameterisedByArrayOfSimpleTypeTests {
    public static final byte[] BYTES = new byte[]{0x42, 0x43};
    public static final boolean[] BOOLEANS = new boolean[]{true, false};
    public static final short[] SHORTS = new short[]{23, 24};
    public static final int[] INTS = new int[]{63, 64};
    public static final long[] LONGS = new long[]{87l, 88l};
    public static final float[] FLOATS = new float[]{3.14f, 3.15f};
    public static final double[] DOUBLES = new double[]{2.97, 2.98};
    public static final char[] CHARS = new char[]{'!', '@'};
    public static final String[] STRINGS = new String[]{"dub", "idub"};

    @Configuration
    @EnableNeo4jRepositories
    static class TestConfig extends Neo4jConfiguration {

        TestConfig() throws ClassNotFoundException {
            setBasePackage(SimpleTypeArrayEntity.class.getPackage().getName());
        }

        @Bean
        GraphDatabaseService graphDatabaseService() {
            return new TestGraphDatabaseFactory().newImpermanentDatabase();
        }
    }

    @Autowired
    GraphDatabaseService graphDatabaseService;

    @Autowired
    SimpleTypeArrayEntityRepository simpleTypeArrayEntityRepository;

    @Before
    public void before() {
        Neo4jHelper.cleanDb(graphDatabaseService, true);

        Transaction transaction = graphDatabaseService.beginTx();

        try {
            simpleTypeArrayEntityRepository.save(new SimpleTypeArrayEntity());
            simpleTypeArrayEntityRepository.save(new SimpleTypeArrayEntity(BOOLEANS, BYTES, SHORTS, INTS, LONGS, FLOATS, DOUBLES, CHARS, STRINGS));

            transaction.success();
        } finally {
            transaction.finish();
        }
    }

    @Test
    public void shouldSupportStringArraysAsParameterForIn() throws Exception {
        assertThat(simpleTypeArrayEntityRepository.findUsingString(asList(STRINGS, new String[]{"foo"})).strings, is(STRINGS));
    }

    @Test
    public void shouldSupportBooleanArrayAsParameterForIn() throws Exception {
        assertThat(simpleTypeArrayEntityRepository.findUsingBoolean(asList(BOOLEANS)).booleans, is(BOOLEANS));
    }

    @Test
    public void shouldSupportByteArrayAsParameterForIn() throws Exception {
        assertThat(simpleTypeArrayEntityRepository.findUsingByte(asList(BYTES)).bytes, is(BYTES));
    }

    @Test
    public void shouldSupportShortArrayAsParameterForIn() throws Exception {
        assertThat(simpleTypeArrayEntityRepository.findUsingShort(asList(SHORTS)).shorts, is(SHORTS));
    }

    @Test
    public void shouldSupportIntArrayAsParameterForIn() throws Exception {
        assertThat(simpleTypeArrayEntityRepository.findUsingInt(asList(INTS)).ints, is(INTS));
    }

    @Test
    public void shouldSupportLongArrayAsParameterForIn() throws Exception {
        assertThat(simpleTypeArrayEntityRepository.findUsingLong(asList(LONGS)).longs, is(LONGS));
    }

    @Test
    public void shouldSupportFloatArrayAsParameterForIn() throws Exception {
        assertThat(simpleTypeArrayEntityRepository.findUsingFloat(asList(FLOATS)).floats, is(FLOATS));
    }

    @Test
    public void shouldSupportDoubleArrayAsParameterForIn() throws Exception {
        assertThat(simpleTypeArrayEntityRepository.findUsingDouble(asList(DOUBLES)).doubles, is(DOUBLES));
    }

    @Test
    public void shouldSupportCharArrayAsParameterForIn() throws Exception {
        assertThat(simpleTypeArrayEntityRepository.findUsingChar(asList(CHARS)).chars, is(CHARS));
    }
}
