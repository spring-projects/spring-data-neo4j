/*
 * Copyright (c) 2023-2024 FalkorDB Ltd.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
	public Movie() {
	}

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
