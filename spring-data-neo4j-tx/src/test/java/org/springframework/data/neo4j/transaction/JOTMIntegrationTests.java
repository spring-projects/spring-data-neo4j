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

package org.springframework.data.neo4j.transaction;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.kernel.AbstractGraphDatabase;
import org.neo4j.kernel.GraphDatabaseAPI;
import org.neo4j.kernel.KernelData;
import org.neo4j.kernel.configuration.Config;
import org.objectweb.jotm.Current;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.transaction.jta.ManagedTransactionAdapter;

import javax.transaction.NotSupportedException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author mh
 * @since 21.02.11
 */

public class JOTMIntegrationTests {
    private ClassPathXmlApplicationContext ctx;
    private GraphDatabaseService gds;

    @Before
    public void setUp() throws Exception {
        ctx = new ClassPathXmlApplicationContext("classpath:spring-tx-text-context.xml");
        gds = ctx.getBean(GraphDatabaseService.class);
    }

    @After
    public void tearDown() throws Exception {
        if (ctx != null) ctx.close();
    }

    @Test
    public void createdNodeShouldBeFoundAfterCommit() throws Exception {
        org.neo4j.graphdb.Transaction transaction = gds.beginTx();
        Node node = null;
        try {
            node = gds.createNode();
            assertNotNull(node);
            transaction.success();
        } finally {
            transaction.finish();
        }

        transaction = gds.beginTx();
        try {
            Node readBackOutsideOfTx = gds.getNodeById(node.getId());
            Assert.assertEquals(node, readBackOutsideOfTx);
        } finally {
            transaction.success();
            transaction.finish();
        }
        try {
            transaction = gds.beginTx();
            Node readBackInsideOfTx = gds.getNodeById(node.getId());
            Assert.assertEquals(node, readBackInsideOfTx);
            transaction.success();
        } finally {
            transaction.finish();
        }
    }

    @Test
    public void indexedNodeShouldBeFound() throws Exception {
        org.neo4j.graphdb.Transaction transaction = gds.beginTx();
        Node node = null;
        try {
            node = gds.createNode();
            gds.index().forNodes("node").add(node, "name", "value");
            transaction.success();
        } finally {
            transaction.finish();
        }
        transaction = gds.beginTx();
        try {
            Node retrievedNode = gds.index().forNodes("node").get("name", "value").getSingle();
            Assert.assertEquals(node, retrievedNode);
        } finally {
            transaction.success();
            transaction.finish();
        }
    }

    @Test(expected = NotFoundException.class)
    public void createdNodeShouldBeNotAvailableAfterRollback() throws Exception {
        org.neo4j.graphdb.Transaction tx = gds.beginTx();
        long nodeId=0;
        try {
            Node node = gds.createNode();
            nodeId = node.getId();
            tx.failure();
        } finally {
            tx.close();
        }
        tx = gds.beginTx();
        try {
            gds.getNodeById(nodeId);
        } finally {
            tx.success();
            tx.close();
        }
    }

    @Test
    public void databaseConfiguredWithSpringJtaShouldUseJtaTransactionManager() throws SystemException, NotSupportedException {
        final Config config = ((GraphDatabaseAPI) gds).getDependencyResolver().resolveDependency(Config.class);
        Assert.assertEquals("spring-jta", config.getParams().get(GraphDatabaseSettings.tx_manager_impl.name()));

        JtaTransactionManager tm = ctx.getBean("transactionManager", JtaTransactionManager.class);
        Transaction transaction = tm.createTransaction("jotm", 1000);

        Assert.assertEquals(ManagedTransactionAdapter.class, transaction.getClass());
        assertEquals(Current.class, ((ManagedTransactionAdapter) transaction).getTransactionManager().getClass());
    }
}
