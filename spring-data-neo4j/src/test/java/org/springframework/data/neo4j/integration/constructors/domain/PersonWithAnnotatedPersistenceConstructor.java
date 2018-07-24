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

package org.springframework.data.neo4j.integration.constructors.domain;

import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.util.Assert;

/**
 * @author Nicolas Mervaillie
 */
@NodeEntity
public class PersonWithAnnotatedPersistenceConstructor {

	@Id @GeneratedValue private Long id;
	private String firstName;
	private String lastName;

	public PersonWithAnnotatedPersistenceConstructor(String lastName) {
		this.lastName = lastName;
	}

	@PersistenceConstructor
	public PersonWithAnnotatedPersistenceConstructor(String firstName, String lastName) {
		Assert.notNull(firstName, "firstName should not be null");
		Assert.notNull(lastName, "lastName should not be null");
		this.firstName = firstName;
		this.lastName = lastName;
	}

	public String getFirstName() {
		return firstName;
	}

	public String getLastName() {
		return lastName;
	}
}
