package com.springone.myrestaurants.dao;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.springone.myrestaurants.domain.UserAccount;

@Repository
public class UserAccountDao {

	@PersistenceContext
    private EntityManager entityManager;

	public UserAccount findUserAccount(Long id) {
        if (id == null) return null;
        return entityManager.find(UserAccount.class, id);
    }
	
	public UserAccount findByName(String name) {
		if (name == null) return null;		
		Query q = entityManager.createQuery("SELECT u FROM UserAccount u WHERE u.userName = :username");
		q.setParameter("username", name);
		
		java.util.List resultList = q.getResultList();
		if (resultList.size() > 0)
		{
			return (UserAccount) resultList.get(0);
		} 
		return null;
	}

	@Transactional
    public void persist(UserAccount userAccount) {
        this.entityManager.persist(userAccount);
    }

	@Transactional
    public UserAccount merge(UserAccount userAccount) {
        UserAccount merged = this.entityManager.merge(userAccount);
        this.entityManager.flush();
        return merged;
    }
}
