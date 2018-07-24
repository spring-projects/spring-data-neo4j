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

package org.springframework.data.neo4j.web.domain;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.Index;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;
import org.neo4j.ogm.annotation.typeconversion.Convert;
import org.neo4j.ogm.typeconversion.UuidStringConverter;

/**
 * @author Michal Bachman
 * @author Mark Angrish
 */
@NodeEntity
public class Cinema {

	@Id @GeneratedValue private Long id;

	@Convert(UuidStringConverter.class) @Index(unique = true) private UUID uuid;

	private String name;

	@Relationship(direction = Relationship.INCOMING) private Set<User> visited;

	public Cinema() {}

	public Cinema(String name) {
		this.visited = new HashSet<>();
		this.uuid = UUID.randomUUID();
		this.name = name;
	}

	public void addVisitor(User user) {
		visited.add(user);
	}

	public String getName() {
		return name;
	}
}
