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
package org.springframework.data.neo4j.examples.jsr303.domain;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;

/**
 * @author Vince Bickers
 * @author Mark Angrish
 */
@NodeEntity
public class Adult {

	@Id @GeneratedValue private Long id;

	@NotNull @Size(min = 2, max = 50) private String name;

	@Min(18) private Integer age;

	@AssertTrue private boolean votingAge;

	public Adult() {}

	public Adult(String name, Integer age) {
		this.name = name;
		this.age = age;
		this.votingAge = true;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getAge() {
		return age;
	}

	public void setAge(Integer age) {
		this.age = age;
	}

	public boolean isVotingAge() {
		return votingAge;
	}

	public void setVotingAge(boolean votingAge) {
		this.votingAge = votingAge;
	}
}
