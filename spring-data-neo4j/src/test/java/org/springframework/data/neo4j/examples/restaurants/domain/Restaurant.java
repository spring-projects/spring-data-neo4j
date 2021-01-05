/*
 * Copyright 2011-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
