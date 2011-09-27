package com.springone.myrestaurants.dao;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;

import com.springone.myrestaurants.domain.Restaurant;

@Repository
public class RestaurantDao {

	@PersistenceContext
    private EntityManager entityManager;

	public Restaurant findRestaurant(Long id) {
        if (id == null) return null;
        return entityManager.find(Restaurant.class, id);
    }

	@SuppressWarnings("unchecked")
    public List<Restaurant> findAllRestaurants() {
        return entityManager.createQuery("select o from Restaurant o").getResultList();
    }

	@SuppressWarnings("unchecked")
    public List<Restaurant> findRestaurantEntries(int firstResult, int maxResults) {
        return entityManager.createQuery("select o from Restaurant o").setFirstResult(firstResult).setMaxResults(maxResults).getResultList();
    }

	public long countRestaurants() {
        return ((Number) entityManager.createQuery("select count(o) from Restaurant o").getSingleResult()).longValue();
    }

}
