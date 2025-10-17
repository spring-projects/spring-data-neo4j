/*
 * Copyright 2011-2025 the original author or authors.
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
package org.springframework.data.falkordb.examples;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.falkordb.core.schema.Id;
import org.springframework.data.falkordb.core.schema.Node;
import org.springframework.data.falkordb.core.schema.Property;
import org.springframework.data.falkordb.core.schema.Relationship;

/**
 * Example Movie entity demonstrating relationship with properties.
 *
 * @author Shahar Biron (FalkorDB adaptation)
 * @since 1.0
 */
@Node
public class Movie {

	@Id
	private String title;

	@Property("tagline")
	private String description;

	private Integer released;

	@Relationship(type = "ACTED_IN", direction = Relationship.Direction.INCOMING)
	private List<ActedIn> actors = new ArrayList<>();

	@Relationship(type = "DIRECTED", direction = Relationship.Direction.INCOMING)
	private List<Person> directors = new ArrayList<>();

	// Default constructor
	public Movie() {}

	// Constructor
	public Movie(String title, String description, Integer released) {
		this.title = title;
		this.description = description;
		this.released = released;
	}

	// Getters and setters
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Integer getReleased() {
		return released;
	}

	public void setReleased(Integer released) {
		this.released = released;
	}

	public List<ActedIn> getActors() {
		return actors;
	}

	public void setActors(List<ActedIn> actors) {
		this.actors = actors;
	}

	public List<Person> getDirectors() {
		return directors;
	}

	public void setDirectors(List<Person> directors) {
		this.directors = directors;
	}

}