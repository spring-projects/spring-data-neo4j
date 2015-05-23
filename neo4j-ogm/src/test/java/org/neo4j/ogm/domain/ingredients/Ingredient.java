/*
 * Copyright (c)  [2011-2015] "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package org.neo4j.ogm.domain.ingredients;

import java.util.HashSet;
import java.util.Set;

import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

/**
 * @author Luanne Misquitta
 */
@NodeEntity
public class Ingredient {

	private Long id;
	private String name;

	@Relationship(type = "PAIRS_WITH", direction = "UNDIRECTED")
	private Set<Pairing> pairings = new HashSet<>();

	public Ingredient() {
	}

	public Ingredient(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}


	public Set<Pairing> getPairings() {
		return pairings;
	}

	public void addPairing(Pairing pairing) {
		pairing.getFirst().getPairings().add(pairing);
		pairing.getSecond().getPairings().add(pairing);
	}

}
