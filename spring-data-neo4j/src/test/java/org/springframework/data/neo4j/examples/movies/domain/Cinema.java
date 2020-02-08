/*
 * Copyright 2011-2020 the original author or authors.
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
import org.neo4j.ogm.annotation.Property;
import org.neo4j.ogm.annotation.Relationship;

/**
 * @author Michal Bachman
 * @author Luanne Misquitta
 * @author Jasper Blues
 */
@NodeEntity(label = "Theatre")
public class Cinema {

	private Long id;
	private String name;

	@Property(name = "city") private String location;

	@Relationship(type = "VISITED", direction = Relationship.INCOMING) private Set<User> visited = new HashSet<>();

	@Relationship(type = "BLOCKBUSTER", direction = Relationship.OUTGOING) private TempMovie blockbusterOfTheWeek;

	private int capacity;

	public Cinema() {}

	public Cinema(String name) {
		this.name = name;
	}

	public Cinema(String name, int capacity) {
		this.name = name;
		this.capacity = capacity;
	}

	public void addVisitor(User user) {
		visited.add(user);
	}

	public String getName() {
		return name;
	}

	public String getLocation() {
		return location;
	}

	public Set<User> getVisited() {
		return visited;
	}

	public int getCapacity() {
		return capacity;
	}

	public void setCapacity(int capacity) {
		this.capacity = capacity;
	}

	public TempMovie getBlockbusterOfTheWeek() {
		return blockbusterOfTheWeek;
	}

	public void setBlockbusterOfTheWeek(TempMovie blockbusterOfTheWeek) {
		this.blockbusterOfTheWeek = blockbusterOfTheWeek;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		Cinema cinema = (Cinema) o;

		return !(name != null ? !name.equals(cinema.name) : cinema.name != null);
	}

	@Override
	public int hashCode() {
		return name != null ? name.hashCode() : 0;
	}
}
