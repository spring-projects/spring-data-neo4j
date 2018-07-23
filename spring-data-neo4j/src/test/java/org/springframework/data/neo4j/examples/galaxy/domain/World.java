/*
 * Copyright (c)  [2011-2016] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
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

package org.springframework.data.neo4j.examples.galaxy.domain;

import java.util.HashSet;
import java.util.Set;

import org.neo4j.ogm.annotation.Relationship;

/**
 * @author Vince Bickers
 * @author Luanne Misquitta
 */
public class World {

	private final static String REACHABLE_BY_ROCKET = "REACHABLE_BY_ROCKET";

	private Long id;

	private String name;

	private int moons;

	private Float radius;

	private Long updated;

	public Long getUpdated() {
		return updated;
	}

	public void setUpdated(long updated) {
		this.updated = updated;
	}

	@Relationship(type = REACHABLE_BY_ROCKET,
			direction = "UNDIRECTED") private Set<World> reachableByRocket = new HashSet<>();

	public World(String name, int moons) {
		this.name = name;
		this.moons = moons;
	}

	public World() {}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public int getMoons() {
		return moons;
	}

	public void addRocketRouteTo(World otherWorld) {
		reachableByRocket.add(otherWorld);
		// symmetric relationship.
		otherWorld.reachableByRocket.add(this); // bi-directional in domain.
	}

	public Set<World> getReachableByRocket() {
		return this.reachableByRocket;
	}

	public void setReachableByRocket(Set<World> reachableByRocket) {
		this.reachableByRocket.clear();
		this.reachableByRocket = reachableByRocket;
	}

	public boolean canBeReachedFrom(World otherWorld) {
		for (World world : reachableByRocket) {
			if (world.equals(otherWorld)) {
				return true;
			}
		}
		return false;
	}

	public Float getRadius() {
		return radius;
	}

	public void setRadius(Float radius) {
		this.radius = radius;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		World world = (World) o;

		if (moons != world.moons)
			return false;
		if (id != null ? !id.equals(world.id) : world.id != null)
			return false;
		return !(name != null ? !name.equals(world.name) : world.name != null);
	}

	@Override
	public int hashCode() {
		int result = id != null ? id.hashCode() : 0;
		result = 31 * result + (name != null ? name.hashCode() : 0);
		result = 31 * result + moons;
		return result;
	}

	@Override
	public String toString() {
		return String.format("World{name='%s', moons=%d}", name, moons);
	}
}
