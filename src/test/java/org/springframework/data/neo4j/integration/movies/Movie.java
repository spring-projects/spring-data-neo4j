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
package org.springframework.data.neo4j.integration.movies;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.schema.Relationship.Direction;

/**
 * @author Michael J. Simons
 * @soundtrack Body Count - Manslaughter
 */
@Node
public final class Movie {

	@Id
	private final String title;

	@Property("tagline")
	private final String description;

	private Integer released;

	@Relationship(value = "ACTED_IN", direction = Direction.INCOMING)
	private final List<Actor> actors;

	@Relationship(value = "DIRECTED", direction = Direction.INCOMING)
	private final List<Person> directors;

	@Relationship(value = "IS_SEQUEL_OF", direction = Direction.INCOMING)
	private final Movie sequel;

	public Movie(String title, String description) {
		this.title = title;
		this.description = description;
		this.actors = new ArrayList<>();
		this.directors = new ArrayList<>();
		this.sequel = null;
	}

	@PersistenceConstructor
	public Movie(String title, String description, List<Actor> actors, List<Person> directors, Movie sequel) {
		this.title = title;
		this.description = description;
		this.actors = actors == null ? Collections.emptyList() : new ArrayList<>(actors);
		this.directors = directors == null ? Collections.emptyList() : new ArrayList<>(directors);
		this.sequel = sequel;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public List<Actor> getActors() {
		return Collections.unmodifiableList(this.actors);
	}

	public List<Person> getDirectors() {
		return Collections.unmodifiableList(this.directors);
	}

	public Integer getReleased() {
		return released;
	}

	public void setReleased(Integer released) {
		this.released = released;
	}

	public Movie getSequel() {
		return sequel;
	}
}
