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

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@NodeEntity
class SimpleTypeArrayEntity {
    @GraphId
    Long id;

    boolean[] bo = {false, false, false};
    byte[] by = "Hello, world!\n".getBytes();
    short[] sh = {1, 2, 3};
    int[] i = {4, 5, 6};
    long[] l = {7,8,9};
    float[] f = {1.0f, 1.1f, 1.2f};
    double[] d = {1.3, 1.4, 1.5};
    char[] c = "Hello Darkness, my old friend...".toCharArray();
    String[] st = {"bigger", "better", "faster", "stronger"};

    SimpleTypeArrayEntity() {
    }

    SimpleTypeArrayEntity(boolean[] bo, byte[] by, short[] sh, int[] i, long[] l, float[] f, double[] d, char[] c, String[] st) {
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

interface SimpleTypeArrayEntityRepository extends CRUDRepository<SimpleTypeArrayEntity> {
    @Query("start n=node(*) where n.bo in {0} return n")
    SimpleTypeArrayEntity findUsingBoolean(Iterable<boolean[]> value);

    @Query("start n=node(*) where n.by in {0} return n")
    SimpleTypeArrayEntity findUsingByte(Iterable<byte[]> value);

    @Query("start n=node(*) where n.sh in {0} return n")
    SimpleTypeArrayEntity findUsingShort(Iterable<short[]> value);

    @Query("start n=node(*) where n.i in {0} return n")
    SimpleTypeArrayEntity findUsingInt(Iterable<int[]> value);

    @Query("start n=node(*) where n.l in {0} return n")
    SimpleTypeArrayEntity findUsingLong(Iterable<long[]> value);

    @Query("start n=node(*) where n.f in {0} return n")
    SimpleTypeArrayEntity findUsingFloat(Iterable<float[]> value);

    @Query("start n=node(*) where n.d in {0} return n")
    SimpleTypeArrayEntity findUsingDouble(Iterable<double[]> value);

    @Query("start n=node(*) where n.c in {0} return n")
    SimpleTypeArrayEntity findUsingChar(Iterable<char[]> value);

    @Query("start n=node(*) where n.st in {0} return n")
    SimpleTypeArrayEntity findUsingString(Iterable<String[]> value);
}

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class InParameterisedByArrayOfSimpleTypeTests {
    public static final byte[] BY = new byte[]{0x42, 0x43};
    public static final boolean[] BO = new boolean[]{true, false};
    public static final short[] S = new short[]{23, 24};
    public static final int[] I = new int[]{63, 64};
    public static final long[] L = new long[]{87l, 88l};
    public static final float[] F = new float[]{3.14f, 3.15f};
    public static final double[] D = new double[]{2.97, 2.98};
    public static final char[] C = new char[]{'!', '@'};
    public static final String[] ST = new String[]{"dub", "idub"};

    @Configuration
    @EnableNeo4jRepositories
    static class TestConfig extends Neo4jConfiguration {
        @Bean
        GraphDatabaseService graphDatabaseService() {
            return new ImpermanentGraphDatabase();
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
            simpleTypeArrayEntityRepository.save(new SimpleTypeArrayEntity(BO, BY, S, I, L, F, D, C, ST));

            transaction.success();
        } finally {
            transaction.finish();
        }
    }

    @Test
    public void shouldSupportStringArraysAsParameterForIn() throws Exception {
        assertThat(simpleTypeArrayEntityRepository.findUsingString(asList(ST, new String[]{"foo"})).st, is(ST));
    }

    @Test
    public void shouldSupportBooleanArrayAsParameterForIn() throws Exception {
        assertThat(simpleTypeArrayEntityRepository.findUsingBoolean(asList(BO)).bo, is(BO));
    }

    @Test
    @Ignore("see https://github.com/neo4j/community/issues/788")
    public void shouldSupportByteArrayAsParameterForIn() throws Exception {
        assertThat(simpleTypeArrayEntityRepository.findUsingByte(asList(BY)).by, is(BY));
    }

    @Test
    public void shouldSupportShortArrayAsParameterForIn() throws Exception {
        assertThat(simpleTypeArrayEntityRepository.findUsingShort(asList(S)).sh, is(S));
    }

    @Test
    public void shouldSupportIntArrayAsParameterForIn() throws Exception {
        assertThat(simpleTypeArrayEntityRepository.findUsingInt(asList(I)).i, is(I));
    }

    @Test
    public void shouldSupportLongArrayAsParameterForIn() throws Exception {
        assertThat(simpleTypeArrayEntityRepository.findUsingLong(asList(L)).l, is(L));
    }

    @Test
    public void shouldSupportFloatArrayAsParameterForIn() throws Exception {
        assertThat(simpleTypeArrayEntityRepository.findUsingFloat(asList(F)).f, is(F));
    }

    @Test
    public void shouldSupportDoubleArrayAsParameterForIn() throws Exception {
        assertThat(simpleTypeArrayEntityRepository.findUsingDouble(asList(D)).d, is(D));
    }

    @Test
    public void shouldSupportCharArrayAsParameterForIn() throws Exception {
        assertThat(simpleTypeArrayEntityRepository.findUsingChar(asList(C)).c, is(C));
    }
}