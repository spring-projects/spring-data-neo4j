package com.springone.myrestaurants.domain;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Michael Hunger
 * @since 02.10.2010
 */
public class RatedRestaurant {
    private final Restaurant restaurant;
    private final Collection<Recommendation> recommendations = new ArrayList<Recommendation>();

    public RatedRestaurant(final Restaurant restaurant) {
        this.restaurant = restaurant;
    }

    public void add(final Recommendation recommendation) {
        recommendations.add(recommendation);
    }

    public Collection<Recommendation> getRecommendations() {
        return recommendations;
    }

    public Restaurant getRestaurant() {
        return restaurant;
    }
}
	