package com.springone.myrestaurants.data;

import com.springone.myrestaurants.domain.UserAccount;
import junit.framework.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import java.util.Date;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@DirtiesContext
public class UserAccountRepositoryTests extends AbstractTestWithUserAccount {

    @Autowired
    UserAccountRepository repo;

    @Transactional
    @Test
    public void testFindUser() {
    	UserAccount o = repo.findUserAccount(userId);
    	Assert.assertNotNull("should have found something" ,o);
    	Assert.assertEquals("should have found the right one", "user", o.getUserName());
    }

    @Transactional
    @Test
    public void testFindByName() {
    	UserAccount o = repo.findByName("user");
    	Assert.assertNotNull("should have found something" ,o);
    	Assert.assertEquals("should have found the right one", "user", o.getUserName());
    }

    @Transactional
    @Test
    public void testPersist() {
    	UserAccount newUser = new UserAccount();
    	newUser.setFirstName("John");
    	newUser.setLastName("Doe");
    	newUser.setBirthDate(new Date());
    	newUser.setNickname("Bubba");
    	newUser.setUserName("jdoe");
    	repo.persist(newUser);
    	em.flush();
		List results = em.createNativeQuery("select id, user_name, first_name from user_account where user_name = ?")
    			.setParameter(1, newUser.getUserName()).getResultList();
    	Assert.assertEquals("should have found the entry", 1, results.size());
    	Assert.assertEquals("should have found the correct entry", "John", ((Object[])results.get(0))[2]);
    	UserAccount persistedUser = repo.findByName(newUser.getUserName());
    	Assert.assertEquals("should have the correct value", newUser.getFirstName(), persistedUser.getFirstName());
    	Assert.assertEquals("should have the correct value", newUser.getLastName(), persistedUser.getLastName());
    	Assert.assertEquals("should have the correct value", newUser.getNickname(), persistedUser.getNickname());
    	Assert.assertEquals("should have the correct value", newUser.getUserName(), persistedUser.getUserName());
    	Assert.assertEquals("should have the correct value", newUser.getBirthDate(), persistedUser.getBirthDate());
    }
    
    @Transactional
    @Test
    public void testMerge() {
    	EntityManager separateTxEm = emf.createEntityManager();
    	EntityTransaction separateTx = separateTxEm.getTransaction();
    	separateTx.begin();
    	UserAccount user = separateTxEm.find(UserAccount.class, userId);
    	separateTxEm.flush();
    	Assert.assertTrue("entity is part of separate em", separateTxEm.contains(user));
    	separateTx.commit();
    	separateTxEm.detach(user);
    	Assert.assertFalse("entity is no longer part of separate em", separateTxEm.contains(user));
    	Assert.assertFalse("entity is not part of main em", em.contains(user));
    	user.setLastName("Hendrix");
    	UserAccount mergedUser = repo.merge(user);
    	em.flush();
    	Assert.assertTrue("entity is now part of main em", em.contains(mergedUser));
		List results = em.createNativeQuery("select id, user_name, last_name from user_account where id = ?")
				.setParameter(1, userId).getResultList();
		Assert.assertEquals("should have found the entry", 1, results.size());
		Assert.assertEquals("should have found the updated entry", "Hendrix", ((Object[])results.get(0))[2]);
    }

}
