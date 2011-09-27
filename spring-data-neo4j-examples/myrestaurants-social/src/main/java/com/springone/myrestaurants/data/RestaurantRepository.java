package com.springone.myrestaurants.data;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.springone.myrestaurants.domain.Restaurant;

@Repository
public class RestaurantRepository {

	@PersistenceContext
    private EntityManager entityManager;

	@Transactional
	public Restaurant findRestaurant(Long id) {
		if (id == null) return null;
		final Restaurant rest = entityManager.find(Restaurant.class, id);
		if (rest != null) {
			rest.persist();
		}
		return rest;
    }

	@SuppressWarnings("unchecked")
	@Transactional(readOnly=true)
    public List<Restaurant> findAllRestaurants() {
        return entityManager.createQuery("select o from Restaurant o").getResultList();
    }

	@SuppressWarnings("unchecked")
	@Transactional(readOnly=true)
    public List<Restaurant> findRestaurantEntries(int firstResult, int maxResults) {
        return entityManager.createQuery("select o from Restaurant o").setFirstResult(firstResult).setMaxResults(maxResults).getResultList();
    }

	@Transactional
	public long countRestaurants() {
        return ((Number) entityManager.createQuery("select count(o) from Restaurant o").getSingleResult()).longValue();
    }

}
