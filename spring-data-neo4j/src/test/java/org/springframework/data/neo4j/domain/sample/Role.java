/*
 * Copyright (c)  [2011-2018] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
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
package org.springframework.data.neo4j.domain.sample;

import java.util.UUID;

import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;

/**
 * Sample domain class representing roles.
 *
 * @author Mark Angrish
 * @author Michael J. Simons
 */
@NodeEntity
public class Role {

	private static final String PREFIX = "ROLE_";

	@Id @GeneratedValue private Long id;

	private UUID uuid;
	private String name;

	/**
	 * Creates a new instance of {@code Role}.
	 */
	public Role() {}

	/**
	 * Creates a new preconfigured {@code Role}.
	 *
	 * @param name
	 */
	public Role(final String name) {
		this.name = name;
	}

	/**
	 * Returns the id.
	 *
	 * @return
	 */
	public Long getId() {

		return id;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {

		return PREFIX + name;
	}

	/**
	 * Returns whether the role is to be considered new.
	 *
	 * @return
	 */
	public boolean isNew() {

		return id == null;
	}
}
