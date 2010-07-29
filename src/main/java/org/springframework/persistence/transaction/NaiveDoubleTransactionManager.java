package org.springframework.persistence.transaction;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionStatus;

public class NaiveDoubleTransactionManager implements PlatformTransactionManager {
	
	private final PlatformTransactionManager a;
	
	private final PlatformTransactionManager b;
	
	public NaiveDoubleTransactionManager(PlatformTransactionManager a, PlatformTransactionManager b) {
		System.err.println("WARNING: Naive JTA/Neo4j Spring transaction manager--must implement properly");
		this.a = a;
		this.b = b;
	}

	@Override
	public void commit(TransactionStatus ts) throws TransactionException {
		TransactionStatus tsb = copyTransactionStatus(ts);
		try {
			a.commit(ts);
		}
		catch (Throwable t) {
			System.err.println("Continuing to commit tx despite this:" + t);
		}
		try {
			b.commit(tsb);
		}
		catch (Throwable t) {
			System.err.println("Can't commit tx" + t);
			throw new TransactionException(t.getMessage(), t) {}; 
		}
	}

	private TransactionStatus copyTransactionStatus(TransactionStatus ts) {
		return new DefaultTransactionStatus(null, ts.isNewTransaction(), false,  false, false, null);
	}
	
	@Override
	public TransactionStatus getTransaction(TransactionDefinition td)
			throws TransactionException {
		TransactionStatus atx = a.getTransaction(td);
		TransactionStatus btx = b.getTransaction(td);
		return atx;
	}

	@Override
	public void rollback(TransactionStatus ts) throws TransactionException {
		TransactionStatus tsb = copyTransactionStatus(ts);
		try {
			a.rollback(ts);
		}
		catch (Throwable t) {
			System.err.println("Continuing to rollback tx despite this:" + t);
		}
		try {
			b.rollback(tsb);
		}
		catch (Throwable t) {
			System.err.println("Can't rollback tx" + t);
			throw new TransactionException(t.getMessage(), t) {}; 
		}
	}

}
