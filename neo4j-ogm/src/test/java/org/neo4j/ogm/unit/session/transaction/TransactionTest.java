/*
 * Copyright (c) 2014-2015 "GraphAware"
 *
 * GraphAware Ltd
 *
 * This file is part of Neo4j-OGM.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.neo4j.ogm.unit.session.transaction;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.ogm.cypher.compiler.CypherContext;
import org.neo4j.ogm.domain.education.Teacher;
import org.neo4j.ogm.mapper.MappingContext;
import org.neo4j.ogm.metadata.MetaData;
import org.neo4j.ogm.session.transaction.SimpleTransaction;
import org.neo4j.ogm.session.transaction.Transaction;
import org.neo4j.ogm.session.transaction.TransactionException;

import static org.junit.Assert.*;

public class TransactionTest {

    private Transaction tx;
    private MappingContext mappingContext;

    private static final MetaData metaData = new MetaData("org.neo4j.ogm.domain.education");

    @Before
    public void setUp() {
        mappingContext = new MappingContext(metaData);
        tx = new SimpleTransaction(mappingContext, "");
    }

    @Test public void assertNewTransactionIsOpen() {
        assertEquals(Transaction.Status.OPEN, tx.status());
    }

    @Test public void assertCommitOperation() {
        tx.append(new CypherContext());
        tx.commit();
        assertEquals(Transaction.Status.COMMITTED, tx.status());
    }

    @Test public void assertRollbackOperation() {
        tx.append(new CypherContext());
        tx.rollback();
        assertEquals(Transaction.Status.ROLLEDBACK, tx.status());

    }

    @Test(expected = TransactionException.class) public void failRollbackIfCommitted() {
        tx.append(new CypherContext());
        tx.commit();
        tx.rollback();
    }

    @Test(expected = TransactionException.class) public void failRollbackIfRolledBack() {
        tx.append(new CypherContext());
        tx.rollback();
        tx.rollback();
    }

    @Test(expected = TransactionException.class) public void failCommitIfCommitted() {
        tx.append(new CypherContext());
        tx.commit();
        tx.commit();
    }

    @Test(expected = TransactionException.class) public void failCommitIfRolledBack() {
        tx.append(new CypherContext());
        tx.rollback();
        tx.commit();
    }

    @Test(expected = TransactionException.class) public void failNewOperationIfRolledBack() {
        tx.append(new CypherContext());
        tx.rollback();
        tx.append(new CypherContext());
    }

    @Test(expected = TransactionException.class) public void failNewOperationIfCommitted() {
        tx.append(new CypherContext());
        tx.commit();
        tx.append(new CypherContext());
    }

    @Test public void assertNotDirtyAfterCommit() {
        // 'load' a teacher
        Teacher teacher = new Teacher();
        teacher.setId(1L);
        mappingContext.remember(teacher);

        // change the teacher's properties
        teacher.setName("Richard Feynman");
        assertTrue(mappingContext.isDirty(teacher));

        // create a new cypher context, representing the response from saving the teacher
        CypherContext cypherContext = new CypherContext();
        cypherContext.log(teacher);

        tx.append(cypherContext);
        tx.commit();

        // the mapping context should now be in sync with the persistent state
        assertFalse(mappingContext.isDirty(teacher));

    }

}
