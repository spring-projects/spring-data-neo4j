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
package org.springframework.data.neo4j.examples.movies.domain;

import java.util.HashSet;
import java.util.Set;

import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

/**
 * @author Michal Bachman
 * @author Luanne Misquitta
 */
// todo merge with movie when tests fixed
@NodeEntity(label = "Movie")
public class TempMovie extends AbstractEntity {

	private String name;
	@Relationship(type = "RATED", direction = Relationship.INCOMING) private Set<Rating> ratings = new HashSet<>();

	public TempMovie() {}

	public TempMovie(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void addRating(Rating rating) {
		ratings.add(rating);
	}

	public Set<Rating> getRatings() {
		return ratings;
	}
}
