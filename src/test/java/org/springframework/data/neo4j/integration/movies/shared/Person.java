/*
 * Copyright 2011-present the original author or authors.
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
package org.springframework.data.neo4j.integration.movies.shared;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

/**
 * @author Michael J. Simons
 */
@Node
public final class Person {

	@Id
	@GeneratedValue
	private final Long id;

	private final String name;

	private Integer born;

	@Relationship("REVIEWED")
	private List<Movie> reviewed = new ArrayList<>();

	@Relationship("ACTED_IN")
	private List<Movie> actedIn = new ArrayList<>();

	@PersistenceCreator
	private Person(Long id, String name, Integer born) {
		this.id = id;
		this.born = born;
		this.name = name;
	}

	public Person(String name, Integer born) {
		this(null, name, born);
	}

	public Long getId() {
		return this.id;
	}

	public String getName() {
		return this.name;
	}

	public Integer getBorn() {
		return this.born;
	}

	public void setBorn(Integer born) {
		this.born = born;
	}

	public List<Movie> getReviewed() {
		return this.reviewed;
	}

	@Override
	public String toString() {
		return "Person{" + "id=" + this.id + ", name='" + this.name + '\'' + ", born=" + this.born + '}';
	}

}
