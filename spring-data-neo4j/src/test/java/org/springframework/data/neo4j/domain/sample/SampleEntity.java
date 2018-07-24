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

import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.springframework.util.Assert;

/**
 * @author Mark Angrish
 * @author Mark Paluch
 * @author Michael J. Simons
 */
@NodeEntity
public class SampleEntity {

	@Id @GeneratedValue protected Long id;
	private String first;
	private String second;

	protected SampleEntity() {

	}

	public SampleEntity(String first, String second) {
		Assert.notNull(first, "First must not be null!");
		Assert.notNull(second, "Second mot be null!");
		this.first = first;
		this.second = second;
	}

	public Long getId() {
		return id;
	}
}
