package com.springone.myrestaurants.web;

public class RatedRestaurantBean {

	private long id;
	
	// Restaurant name
	private String name;
	
	private long recommendations;
	
	private double rating;
		
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public long getId() {
		return id;
	}

	public void setId(long restaurantId) {
		this.id = restaurantId;
	}

	public long getRecommendations() {
		return recommendations;
	}

	public void setRecommendations(long recommendations) {
		this.recommendations = recommendations;
	}

	public double getRating() {
		return rating;
	}

	public void setRating(double rating) {
		this.rating = rating;
	}

	@Override
	public String toString() {
		return "RatedRestaurantBean [restaurantId=" + id + ", name="
				+ name + ", rating=" + rating + "]";
	}

}
