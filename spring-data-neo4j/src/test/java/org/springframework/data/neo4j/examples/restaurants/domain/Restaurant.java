/*
 * Copyright (c)  [2011-2018] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 *
 */

package org.springframework.data.neo4j.examples.restaurants.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.Relationship;
import org.neo4j.ogm.annotation.typeconversion.Convert;
import org.neo4j.ogm.annotation.typeconversion.DateString;
import org.springframework.data.geo.Point;
import org.springframework.data.neo4j.conversion.PointConverter;

/**
 * @author Jasper Blues
 * @author Michael J. Simons
 */
public class Restaurant implements Comparable<Restaurant> {

	@Id @GeneratedValue private Long id;
	private String name;
	@Convert(PointConverter.class) private Point location;
	private int zip;
	private double score;
	private String description;
	private boolean halal;

	@Relationship(type = "REGULAR_DINER",
			direction = Relationship.OUTGOING) private List<Diner> regularDiners = new ArrayList<>();

	@Relationship(type = "SIMILAR_RESTAURANT",
			direction = Relationship.OUTGOING) private List<Restaurant> similarRestaurants = new ArrayList<>();

	@DateString private Date launchDate;

	public Restaurant() {}

	public Restaurant(String name, Point location, int zip) {
		this.name = name;
		this.location = location;
		this.zip = zip;
	}

	public Restaurant(String name, double score) {
		this.name = name;
		this.score = score;
	}

	public Restaurant(String name, String description) {
		this.name = name;
		this.description = description;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Point getLocation() {
		return location;
	}

	public void setLocation(Point location) {
		this.location = location;
	}

	public int getZip() {
		return zip;
	}

	public void setZip(int zip) {
		this.zip = zip;
	}

	public double getScore() {
		return score;
	}

	public void setScore(double score) {
		this.score = score;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Date getLaunchDate() {
		return launchDate;
	}

	public void setLaunchDate(Date launchDate) {
		this.launchDate = launchDate;
	}

	public boolean halal() {
		return halal;
	}

	public void setHalal(boolean halal) {
		this.halal = halal;
	}

	public void addRegularDiner(Diner diner) {
		this.regularDiners.add(diner);
	}

	public List<Diner> getRegularDiners() {
		return this.regularDiners;
	}

	public void addSimilarRestaurant(Restaurant restaurant) {
		this.similarRestaurants.add(restaurant);
	}

	public List<Restaurant> getSimilarRestaurants() {
		return this.similarRestaurants;
	}

	@Override
	public String toString() {
		return "Restaurant{" + "name='" + name + '\'' + ", score=" + score + '}';
	}

	@Override
	public int compareTo(Restaurant o) {
		if (this == o) {
			return 0;
		}
		return this.getName().compareTo(o.getName());
	}
}
