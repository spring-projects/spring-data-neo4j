package com.springone.myrestaurants.data;

import java.util.Date;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;

import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.node.Neo4jHelper;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

import com.springone.myrestaurants.domain.UserAccount;

public class AbstractTestWithUserAccount {

	protected final Logger log = LoggerFactory.getLogger(getClass());
	protected Long userId;

	@Autowired
	private Neo4jTemplate template;

	@PersistenceContext
	protected EntityManager em;

	@PersistenceUnit
	protected EntityManagerFactory emf;

	@BeforeTransaction
	public void setUpBeforeTransaction() {
		EntityManager setUpEm = emf.createEntityManager();
		EntityTransaction setUpTx = setUpEm.getTransaction();
		setUpTx.begin();
		UserAccount u = new UserAccount();
		u.setFirstName("Bubba");
		u.setLastName("Jones");
		u.setBirthDate(new Date());
		u.setUserName("user");
		setUpEm.persist(u);
		setUpEm.flush();
        u.persist();
		this.userId = u.getId();
		setUpTx.commit();
	}

    @Before
    public void setUp() throws Exception {
    //    em = emf.createEntityManager();
    }

    @Transactional
	@BeforeTransaction
	public void cleanDb() {
	    Neo4jHelper.cleanDb(template);
	}

	@AfterTransaction
	public void tearDown() {
		EntityManager tearDownEm = emf.createEntityManager();
		EntityTransaction tearDownTx = tearDownEm.getTransaction();
		tearDownTx.begin();
		UserAccount u = tearDownEm.find(UserAccount.class, this.userId);
		tearDownEm.remove(u);
		tearDownEm.flush();
		tearDownTx.commit();    	
	}

}