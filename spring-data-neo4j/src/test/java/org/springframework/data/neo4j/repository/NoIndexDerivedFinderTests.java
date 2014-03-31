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

package org.springframework.data.neo4j.repository;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.test.TestGraphDatabaseFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.conversion.Result;
import org.springframework.data.neo4j.model.Group;
import org.springframework.data.neo4j.model.Person;
import org.springframework.data.neo4j.repositories.GroupRepository;
import org.springframework.data.neo4j.repositories.PersonRepository;
import org.springframework.test.context.CleanContextCacheTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:org/springframework/data/neo4j/repository/GraphRepositoryTests-context.xml")
@TestExecutionListeners({CleanContextCacheTestExecutionListener.class, DependencyInjectionTestExecutionListener.class, TransactionalTestExecutionListener.class})
public class NoIndexDerivedFinderTests {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private PersonRepository personRepository;
    @Autowired
    private GroupRepository groupRepository;

    @Test @Transactional
    public void findByAge() {
        Iterable<Person> result = personRepository.findByAge(0);
        assertEquals(0,IteratorUtil.count(result));
    }

    @Test @Transactional
    public void findAllInitiallyWithoutIndexCreation() {
        Result<Person> result = personRepository.findByHeight((short) 100);
        assertEquals(0,IteratorUtil.count( result ));
    }

    @Test @Transactional
    public void findFullTextWithoutIndexCreation() {
        Iterable<Group> result = groupRepository.findByFullTextNameLike( "foo" );
        assertEquals(0,IteratorUtil.count( result ));
    }

    @Test @Transactional
    public void findByName() {
        Iterable<Person> result = personRepository.findByName("");
        assertEquals(0,IteratorUtil.count(result));
    }

    @Test
    public void testWithSeparateTransactions() throws Exception {
        final GraphDatabaseService gdb = new TestGraphDatabaseFactory().newImpermanentDatabase();
        final Thread t = new Thread() {
            @Override
            public void run() {
                final Transaction tx = gdb.beginTx();
                final Index<Node> index = gdb.index().forNodes("Test");
                assertEquals("Test", gdb.index().nodeIndexNames()[0]);
                tx.success();
                tx.close();
            }
        };
        t.start();t.join();
        Transaction tx = gdb.beginTx();
        try {
            final ExecutionResult result = new ExecutionEngine(gdb).execute("start n=node:Test('name:*') return n");
            assertEquals(0,IteratorUtil.count(result));
            assertEquals("Test", gdb.index().nodeIndexNames()[0]);
        } finally {
            tx.success();tx.close();
        }
    }
}
