/*
 * Copyright (c)  [2011-2015] "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
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
