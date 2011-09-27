package com.springone.myrestaurants.domain;

import org.springframework.data.neo4j.annotation.EndNode;
import org.springframework.data.neo4j.annotation.RelationshipEntity;
import org.springframework.data.neo4j.annotation.StartNode;

@RelationshipEntity
public class Recommendation {
    @StartNode
    private UserAccount user;
    @EndNode
    private Restaurant restaurant;

    private int stars;
    private String comment;


    public Recommendation() {
    }

    public void rate(int stars, String comment) {
        this.stars = stars;
        this.comment = comment;
    }

	public int getStars() {
		return stars;
	}

	public String getComment() {
		return comment;
	}

	public UserAccount getUser() {
		return user;
	}

	public Restaurant getRestaurant() {
		return restaurant;
	}



    
    
}
