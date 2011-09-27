package com.springone.myrestaurants.data;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.springone.myrestaurants.domain.UserAccount;

@Repository
public class UserAccountRepository {

	@PersistenceContext
    private EntityManager entityManager;

	@Transactional
	public UserAccount findUserAccount(Long id) {
        if (id == null) return null;
        final UserAccount userAccount = entityManager.find(UserAccount.class, id);
        if (userAccount != null) {
        	userAccount.persist();
        }
        return userAccount;
    }
    @Transactional
	public UserAccount findByName(String name) {
		if (name == null) return null;		
		Query q = entityManager.createQuery("SELECT u FROM UserAccount u WHERE u.userName = :username");
		q.setParameter("username", name);
		
		java.util.List resultList = q.getResultList();
		if (resultList.size() > 0)
		{
            final UserAccount userAccount = (UserAccount) resultList.get(0);
            if (userAccount != null) {
            	userAccount.persist();
            }
            return userAccount;
		} 
		return null;
	}
    
	@SuppressWarnings("unchecked")
	@Transactional(readOnly=true)
    public List<UserAccount> findAllUserAccounts() {
        return entityManager.createQuery("select o from UserAccount o").getResultList();
    }
	   
	@SuppressWarnings("unchecked")
	@Transactional(readOnly=true)
    public List<UserAccount> findUserAccountEntries(int firstResult, int maxResults) {
        return entityManager.createQuery("select o from UserAccount o").setFirstResult(firstResult).setMaxResults(maxResults).getResultList();
    }
	
	
	@Transactional
	public long countUserAccounts() {
        return ((Number) entityManager.createQuery("select count(o) from UserAccount o").getSingleResult()).longValue();
    }

	@Transactional
    public void persist(UserAccount userAccount) {
        this.entityManager.persist(userAccount);
        this.entityManager.flush();
        userAccount.persist();
    }

	@Transactional
    public UserAccount merge(UserAccount userAccount) {
        userAccount.persist();
        UserAccount merged = this.entityManager.merge(userAccount);
        this.entityManager.flush();
        return merged;
    }
}
