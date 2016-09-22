/*
 * Copyright (c)  [2011-2016] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 *
 */

package org.springframework.data.neo4j.transactions;

import static org.junit.Assert.*;

import java.util.Iterator;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.testutil.MultiDriverTestClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.transactions.service.ServiceA;
import org.springframework.data.neo4j.transactions.service.ServiceB;
import org.springframework.data.neo4j.transactions.service.WrapperService;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author: Vince Bickers
 * @see DATAGRAPH-602
 */
@ContextConfiguration(classes = {ApplicationConfig.class})
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
public class ExtendedTransactionsIT extends MultiDriverTestClass {

	@Autowired
	ServiceA serviceA;

	@Autowired
	ServiceB serviceB;

	@Autowired
	WrapperService wrapperService;

	@Autowired
	SessionFactory sessionFactory;

	@Autowired
	PlatformTransactionManager annotationDrivenTransactionManager;

	@Test
	public void shouldRollbackSuccessThenFail() {

		try {
			wrapperService.composeSuccessThenFail();
			fail("should have thrown exception");
		} catch (Exception e) {
			assertEquals("Deliberately throwing exception", e.getLocalizedMessage());
			assertEquals(0, countNodes());
		}
	}

	@Test
	public void shouldCommitSuccessSuccess() {

		try {
			wrapperService.composeSuccessThenSuccess();
			assertEquals(2, countNodes());
		} catch (Exception e) {
			fail("should not have thrown exception");
		}
	}

	@Test
	public void shouldRollbackFailThenSuccess() {
		try {
			wrapperService.composeFailThenSuccess();
			fail("should have thrown exception");
		} catch (Exception e) {
			assertEquals("Deliberately throwing exception", e.getLocalizedMessage());
			assertEquals(0, countNodes());
		}
	}

	@Test
	public void shouldRollbackFailThenFail() {
		try {
			wrapperService.composeFailThenFail();
			fail("should have thrown exception");
		} catch (Exception e) {
			assertEquals("Deliberately throwing exception", e.getLocalizedMessage());
			assertEquals(0, countNodes());
		}
	}

	@Test
	public void shouldRollbackWithCheckedException() {
		try {
			wrapperService.rollbackWithCheckedException();
			fail("should have thrown exception");
		} catch (Exception e) {
			assertEquals("Deliberately throwing exception", e.getLocalizedMessage());
			assertEquals(0, countNodes());
		}
	}

	@Test
	public void shouldRollbackRepositoryMethodOnCheckedException() {
		try {
			serviceA.run();
		} catch (Exception e) {
			assertNull(serviceB.getBilbo());
		}
	}


	@Transactional(readOnly = true)
	@Test
	public void shouldCreateReadOnlyTransaction() {

		assertTrue(((DelegatingTransactionManager) annotationDrivenTransactionManager).getTransactionDefinition().isReadOnly());
	}


	@Transactional(readOnly = false)
	@Test
	public void shouldCreateReadWriteTransaction() {

		assertFalse(((DelegatingTransactionManager) annotationDrivenTransactionManager).getTransactionDefinition().isReadOnly());
	}

	private int countNodes() {
		Iterator iterator = wrapperService.fetch().iterator();
		int i = 0;
		while (iterator.hasNext()) {
			iterator.next();
			i++;
		}
		return i;
	}

	static class DelegatingTransactionManager implements PlatformTransactionManager {

		private PlatformTransactionManager transactionManager;
		private TransactionDefinition transactionDefinition;

		public DelegatingTransactionManager(PlatformTransactionManager platformTransactionManager) {
			this.transactionManager = platformTransactionManager;
		}

		@Override
		public TransactionStatus getTransaction(TransactionDefinition transactionDefinition) throws TransactionException {
			this.transactionDefinition = transactionDefinition;
			return transactionManager.getTransaction(transactionDefinition);
		}

		@Override
		public void commit(TransactionStatus transactionStatus) throws TransactionException {
			transactionManager.commit(transactionStatus);
		}

		@Override
		public void rollback(TransactionStatus transactionStatus) throws TransactionException {
			transactionManager.rollback(transactionStatus);
		}

		public TransactionDefinition getTransactionDefinition() {
			return transactionDefinition;
		}
	}
}
